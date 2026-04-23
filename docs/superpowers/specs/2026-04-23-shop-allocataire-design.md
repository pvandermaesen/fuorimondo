# Shop Allocataire — Design

**Date** : 2026-04-23
**Phase** : 2 — E-commerce allocataire (suite de la Phase 1 "Products admin CRUD")
**Status** : Design approuvé — prêt pour planification d'implémentation

## Objectif

Permettre aux utilisateurs authentifiés avec le statut `ALLOCATAIRE` d'acheter un produit unitaire via paiement en ligne (Mollie) et de recevoir un email de confirmation.

## Décisions produit

| Sujet | Choix |
|---|---|
| Portée | Chaîne complète : parcourir → acheter → payer → confirmer |
| Panier | **Non** — achat direct unitaire, un seul produit par transaction |
| Quantité | Toujours **1 unité** par achat |
| PSP | **Mollie** (redirection Mollie Checkout, webhook) |
| Adresse de livraison | Sélecteur parmi les adresses `SHIPPING` existantes de l'allocataire |
| Stock sous paiement | Réservation via requête dérivée de `orders`, TTL **15 min** |
| Dashboard admin commandes | Oui, inclus dans cette phase |
| Dev : webhook Mollie | **Endpoint simulate** uniquement (pas de ngrok). Mollie réel testé en staging/prod |

## Hors périmètre (explicit)

- Panier multi-produits
- Quantité > 1
- Codes promo, réductions
- Facture PDF
- Remboursements automatiques (traités via dashboard Mollie manuellement)
- Suivi colis / statuts `SHIPPED`, `DELIVERED`
- Avoirs, ré-expéditions
- Notifications push, WebSocket, SSE

---

## Section 1 — Modèle de données

### Approche de réservation de stock : **dérivation depuis `orders`**

Pas de table de réservation séparée. Le stock "disponible" d'un produit se calcule à la volée :

```
available = product.stock
          - Σ qty(orders
                 WHERE product_id = X
                   AND status = 'PENDING_PAYMENT'
                   AND expires_at > now())
```

Puisque qty est toujours 1, chaque order `PENDING_PAYMENT` non expirée réserve 1 unité.

**Avantage** : une seule source de vérité (la commande). Pas de désynchro possible entre une table de réservation et les commandes.

**Complément** : un job `@Scheduled` (toutes les 5 min) passe les orders `PENDING_PAYMENT` expirées en `EXPIRED` pour tracabilité (mais le calcul de stock ne dépend pas du job — si le job tarde, le `expires_at > now()` suffit à libérer implicitement).

### Table `orders`

```sql
-- V4__orders.sql
CREATE TABLE orders (
  id                  UUID        PRIMARY KEY,
  user_id             UUID        NOT NULL REFERENCES users(id),
  product_id          UUID        NOT NULL REFERENCES products(id),
  product_snapshot    JSONB       NOT NULL,
  unit_price_eur      NUMERIC(10,2) NOT NULL,
  total_eur           NUMERIC(10,2) NOT NULL,  -- = unit_price_eur (qty=1)
  shipping_address_id UUID        REFERENCES addresses(id),
  shipping_snapshot   JSONB,
  status              VARCHAR(32) NOT NULL,
  mollie_payment_id   VARCHAR(64),
  mollie_checkout_url VARCHAR(512),
  expires_at          TIMESTAMPTZ,
  paid_at             TIMESTAMPTZ,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_product_status ON orders(product_id, status);
CREATE INDEX idx_orders_mollie_payment ON orders(mollie_payment_id);
```

**Portabilité H2 ↔ PostgreSQL** : on utilise un convertisseur JPA `AttributeConverter<String, String>` (no-op) et on type la colonne en `TEXT` à la fois sur H2 et sur PG. Les snapshots restent du JSON sérialisé par Jackson côté applicatif. Pas de requête SQL sur le contenu JSON — le typage natif JSONB n'apporte rien ici. Migration unique `V4__orders.sql` avec `TEXT` pour les colonnes snapshot.

### Snapshots JSON

`product_snapshot` et `shipping_snapshot` archivent l'état au moment de l'achat. Rationale : si l'admin modifie le prix ou supprime un produit, ou si l'utilisateur supprime/modifie son adresse, la commande conserve son contenu d'origine (audit, facturation, SAV).

Structure (JSON) :
```json
// product_snapshot
{"id":"...","name":"Cuvée 2019","description":"...","priceEur":"180.00",
 "photoFilename":"abc.jpg","weightKg":"1.5","delivery":true,
 "tiers":["TIER_1"]}

// shipping_snapshot
{"fullName":"...","street":"...","streetExtra":null,"postalCode":"...",
 "city":"...","country":"..."}
```

### Machine à états

```
PENDING_PAYMENT ──(Mollie paid)────────► PAID
              ├─(Mollie failed)────────► FAILED
              ├─(Mollie canceled)──────► CANCELLED
              └─(expires_at < now OR
                 Mollie expired)──────► EXPIRED
```

Transitions **idempotentes** : si l'order est déjà dans l'état cible, le handler est no-op.

Aucune transition inverse n'est permise (une commande `PAID` ne peut pas redevenir `PENDING_PAYMENT`).

### Entité JPA `Order`

Package : `com.fuorimondo.orders`

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id UUID id;
    @ManyToOne User user;
    @ManyToOne Product product;

    @Column(name="product_snapshot", columnDefinition="jsonb")
    String productSnapshot;  // serialized JSON

    BigDecimal unitPriceEur;
    BigDecimal totalEur;

    @ManyToOne @JoinColumn(name="shipping_address_id") Address shippingAddress;
    @Column(name="shipping_snapshot", columnDefinition="jsonb") String shippingSnapshot;

    @Enumerated(EnumType.STRING) OrderStatus status;

    String molliePaymentId;
    String mollieCheckoutUrl;

    Instant expiresAt;
    Instant paidAt;
    Instant createdAt;
    Instant updatedAt;
}

public enum OrderStatus { PENDING_PAYMENT, PAID, FAILED, CANCELLED, EXPIRED }
```

---

## Section 2 — Backend API + flux Mollie

### Endpoints

#### Allocataire (authentifié)

| Méthode | Path | Description |
|---|---|---|
| GET | `/api/products` | Liste filtrée (tier utilisateur ∈ product.tiers, sale window ouverte, `stock=null OR available>0`) |
| GET | `/api/products/{id}` | Détail. 404 si filtres non satisfaits (pas de leak d'existence) |
| POST | `/api/orders` | Crée commande + paiement Mollie. Body `{ productId, shippingAddressId? }`. Retour `{ orderId, checkoutUrl }` |
| GET | `/api/orders` | Historique perso paginé |
| GET | `/api/orders/{id}` | Détail d'une commande de l'utilisateur |

#### Admin

| Méthode | Path | Description |
|---|---|---|
| GET | `/api/admin/orders` | Liste, filtres `status`, `userId`, `productId`, pagination |
| GET | `/api/admin/orders/{id}` | Détail admin |

#### Webhook (public, exclu CSRF)

| Méthode | Path | Description |
|---|---|---|
| POST | `/api/webhooks/mollie` | Body `id=tr_XXX`. Lit le vrai statut via API Mollie, met à jour l'order |

#### Dev uniquement (`@Profile("dev")`)

| Méthode | Path | Description |
|---|---|---|
| POST | `/api/dev/orders/{id}/simulate-webhook?status=paid\|failed\|cancelled\|expired` | Bypass Mollie, applique la transition |

### Flux Mollie

```
[User] clique "Acheter"
   │
   ▼
[Front] POST /api/orders { productId, shippingAddressId }
   │
   ▼
[Back] Validate (tier, window, stock atomic SELECT FOR UPDATE)
       ├─> Fail → 409 out_of_stock / 422 no_shipping_address / 403 tier_mismatch
       └─> Success:
            INSERT order (PENDING_PAYMENT, expires_at = now+15min)
            Mollie.createPayment({
              amount, description, redirectUrl, webhookUrl,
              metadata: { orderId }
            })
            UPDATE order SET mollie_payment_id, mollie_checkout_url
            return { orderId, checkoutUrl }
   │
   ▼
[Front] window.location = checkoutUrl
   │
   ▼
[User paye sur Mollie — durée variable]
   │
   ├───────────────┐
   │               ▼
   │        [Mollie] POST /api/webhooks/mollie { id: tr_XXX }
   │               │
   │               ▼
   │        [Back] Mollie.getPayment(tr_XXX)  ← source de vérité
   │               Transition état
   │               If PAID → send confirmation email (sync)
   ▼
[Mollie] GET redirect → /shop/order/:id/return
   │
   ▼
[Front] Poll GET /api/orders/:id toutes les 2s (max 15× = 30s)
        Rend PAID / FAILED / CANCELLED / EXPIRED / timeout
```

### Validation atomique du stock

```sql
SELECT p.stock,
       COALESCE(SUM(1) FILTER (
         WHERE o.status='PENDING_PAYMENT' AND o.expires_at > now()
       ), 0) AS reserved
FROM products p
LEFT JOIN orders o ON o.product_id = p.id
WHERE p.id = :productId
GROUP BY p.id
FOR UPDATE OF p;
```

Si `p.stock` est null → illimité, on passe. Sinon si `stock - reserved < 1` → 409 `OUT_OF_STOCK`. Le `FOR UPDATE` sur `products` sérialise les clics concurrents sur le même produit.

Transaction englobant ce SELECT + l'INSERT order → atomique.

### Sécurité webhook

- `/api/webhooks/mollie` : `permitAll()` + **exclu de CSRF** (même pattern que `/h2-console/**`).
- Handler vérifie que l'id reçu correspond à un `mollie_payment_id` connu en DB. Sinon : 200 silencieux (pas de fuite d'info, Mollie arrête retry).
- On appelle **toujours** `Mollie.getPayment(id)` pour vérifier le statut réel — jamais confiance au body webhook.
- Réponse 200 si traité ou ignoré volontairement. Réponse 500 uniquement sur vraie erreur serveur (Mollie retentera).

### Job de nettoyage

```java
@Scheduled(fixedDelayString = "${fuorimondo.order.expiration-job-interval-minutes}m")
public void expireStaleOrders() {
    orderRepo.expireOlderThan(Instant.now()); // UPDATE ... WHERE status='PENDING_PAYMENT' AND expires_at<now()
}
```

### Intégration Mollie

**Pas de SDK externe** — appels HTTP directs via `RestClient` Spring. L'API Mollie est petite et stable (3-4 endpoints utiles).

Interface d'abstraction :
```java
public interface MolliePaymentGateway {
    MolliePayment createPayment(CreatePaymentRequest req);
    MolliePayment getPayment(String molliePaymentId);
}
```

Deux implémentations, sélection par profil + config :
- `MolliePaymentGatewayHttp` — actif quand `mollie.enabled=true`. Appels HTTPS réels à `https://api.mollie.com/v2/payments`.
- `MolliePaymentGatewayFake` — actif quand `mollie.enabled=false` (défaut en `dev`/`test`). `createPayment` retourne un id Mollie fictif (`tr_fake_{uuid}`) et un `checkoutUrl` pointant vers `/shop/order/{orderId}/return?sim=1` (le `?sim=1` active le panneau de simulation côté front). `getPayment` retourne le dernier statut enregistré en mémoire, alimenté par l'endpoint `/api/dev/orders/{id}/simulate-webhook`.
- Sélection via `@ConditionalOnProperty(name="mollie.enabled", havingValue="true")` pour le gateway HTTP, et la fake en fallback.

### Config

```yaml
# application.yml
fuorimondo:
  order:
    reservation-ttl-minutes: 15
    expiration-job-interval-minutes: 5

mollie:
  api-key: ${MOLLIE_API_KEY:}
  enabled: ${MOLLIE_ENABLED:true}
  api-base-url: https://api.mollie.com/v2
  redirect-base-url: ${APP_BASE_URL:http://localhost:5273}
  webhook-base-url: ${WEBHOOK_BASE_URL:}

# application-dev.yml
mollie:
  enabled: false   # utilise le gateway fake + endpoint simulate
```

### Codes d'erreur API

| HTTP | Code | Cas |
|---|---|---|
| 400 | validation_error | Champs invalides (Bean Validation) |
| 403 | tier_mismatch | Produit hors du cercle de l'user |
| 404 | not_found | Produit invisible (tier / window / deleted) |
| 409 | out_of_stock | Stock insuffisant (après sérialisation) |
| 409 | sale_window_closed | Achat après `saleEndAt` |
| 422 | no_shipping_address | `delivery=true` sans `shippingAddressId` valide |

---

## Section 3 — Frontend

### Routes ajoutées

| Path | Nom | Accès | Composant |
|---|---|---|---|
| `/shop` | `shop` | auth | `ShopView.vue` |
| `/shop/:id` | `shop-product` | auth | `ShopProductView.vue` |
| `/shop/checkout/:productId` | `shop-checkout` | auth | `ShopCheckoutView.vue` |
| `/shop/order/:id/return` | `shop-return` | auth | `ShopReturnView.vue` |
| `/orders` | `my-orders` | auth | `MyOrdersView.vue` |
| `/orders/:id` | `order-detail` | auth | `OrderDetailView.vue` |
| `/admin/orders` | `admin-orders` | admin | `AdminOrdersView.vue` |
| `/admin/orders/:id` | `admin-order-detail` | admin | `AdminOrderDetailView.vue` |

Lien "Boutique" ajouté dans la sidebar (`AppLayout.vue`) pour `auth.isAllocataire`. "Mes commandes" aussi. "Commandes" dans le bloc admin pour `auth.isAdmin`.

### `/shop` — liste produits

- Grille responsive : 1 col mobile, 2 cols tablette, 3 cols `desk:` (utilise le breakpoint existant à 1000px).
- Card produit : photo, nom (serif italique), prix (font-logo), `TierBadge` pour chaque tier applicable, indicateur stock si limité ("Plus que N"), mention "Livraison" si `delivery=true`.
- Produit épuisé → card grisée, badge "Épuisé", non cliquable.
- État vide → message "Aucun produit disponible pour vous actuellement."

### `/shop/:id` — détail

- Photo grande, nom, description (texte simple, respecte les retours ligne avec `white-space: pre-line`), prix, fenêtre de vente affichée si pertinente ("Disponible jusqu'au {date}"), mention livraison/bon numérique.
- CTA principal : `FmButton variant="primary" block` "Acheter — {price} €". Désactivé si hors stock / hors fenêtre (re-check côté front avec message).
- Cas spécial : si l'user a déjà un order `PENDING_PAYMENT` pour ce produit, afficher "Reprendre le paiement en cours" qui redirige vers le `mollie_checkout_url` existant (pas de recréation).

### `/shop/checkout/:productId` — confirmation + adresse

- Récapitulatif produit + prix (read-only).
- Si `delivery=true` :
  - Sélecteur `FmSelect` listant les adresses SHIPPING (`GET /api/addresses?type=SHIPPING`).
  - Si aucune adresse : encart "Ajoute une adresse de livraison avant de commander" + lien `/addresses`.
- Si `delivery=false` : pas d'étape adresse.
- Checkbox obligatoire "J'accepte les CGV" + lien vers `/legal/cgv`.
- Bouton "Payer {price} €" → `POST /api/orders` → `window.location = checkoutUrl` (ou en mode dev fake, redirection interne vers `/shop/order/:id/return`).
- État busy pendant la requête.

### `/shop/order/:id/return` — page de retour Mollie

- Chargement initial : `GET /api/orders/:id`.
- Polling toutes les 2 s tant que `status === 'PENDING_PAYMENT'`, jusqu'à 30 s max.
- Rendu selon statut final :
  - `PAID` : ✓ "Commande confirmée" + résumé + liens "Voir la commande" (`/orders/:id`) / "Retour à la boutique" (`/shop`). Toast une fois "Email de confirmation envoyé".
  - `FAILED` / `CANCELLED` : message doux + CTA "Réessayer" (qui recrée un order sur ce produit → redirige vers `/shop/:productId`).
  - `EXPIRED` : "Délai de paiement dépassé" + CTA "Réessayer".
  - Timeout 30 s sans statut final : "Paiement en cours de traitement. Consulte tes commandes dans quelques instants." + lien `/orders`.

### `/orders` + `/orders/:id`

- Liste chronologique desc paginée. `OrderStatusBadge` coloré. Filtre optionnel statut.
- Détail : produit (snapshot), prix, statut, date, adresse livraison (snapshot), id Mollie. Read-only.

### `/admin/orders` + `/admin/orders/:id`

- Liste avec filtres statut/user/product (reuse pattern `AdminUsersView`).
- Détail admin : mêmes infos + lien vers profil admin de l'user.

### Composants

- Pas de nouveau composant structurant sauf **`OrderStatusBadge.vue`** (5 couleurs : gris pending, vert paid, rouge failed/cancelled/expired avec nuances).
- Reuse `FmButton`, `FmSelect`, `FmInput`, `FmCheckbox`, `TierBadge`.
- Typographie : titres produit en `font-serif italic`, prix en `font-logo` (cohérent avec design existant).

### i18n

Nouvelles sections :

**`shop.*`** : `title`, `empty`, `outOfStock`, `limitedStock`, `delivery`, `digital`, `availableUntil`, `buyCta`, `resumePayment`.

**`checkout.*`** : `title`, `summary`, `shippingAddress`, `noShippingAddress`, `addAddressLink`, `acceptCgv`, `payCta`.

**`orderReturn.*`** : `paid`, `paidMessage`, `failed`, `cancelled`, `expired`, `timeout`, `viewOrder`, `retry`, `backToShop`.

**`order.*`** : `title`, `statusPendingPayment`, `statusPaid`, `statusFailed`, `statusCancelled`, `statusExpired`, `orderId`, `paidAt`, `mollieRef`.

**`errors.*`** : `outOfStock`, `saleWindowClosed`, `tierMismatch`, `noShippingAddress`, `paymentError`.

Traductions FR/IT/EN pour toutes les clés.

### Gestion d'erreurs front

| HTTP | Réaction |
|---|---|
| 409 `out_of_stock` | Message "Ce produit vient d'être épuisé" + refresh liste auto |
| 409 `sale_window_closed` | "Ce produit n'est plus disponible à la vente" |
| 403 `tier_mismatch` | "Ce produit n'est pas ouvert à votre cercle" + retour `/shop` |
| 422 `no_shipping_address` | Message + focus sur encart "Ajoute une adresse" |
| 5xx | Fallback `common.error` + bouton "Réessayer" |

---

## Section 4 — Email, tests, dev, migrations

### Email de confirmation

Déclencheur : transition `PENDING_PAYMENT → PAID` dans le webhook handler, sync/transactionnel.

En cas d'échec d'envoi : log WARN, **pas de rollback** de la transition (on ne veut pas un utilisateur qui a payé sans commande marquée payée en DB).

Contenu (trois locales FR/IT/EN selon `user.locale`) :
- Sujet : "Confirmation de votre commande — Fuori Marmo"
- Corps texte/HTML simple :
  - Salutation `Bonjour {firstName}`
  - Récapitulatif : produit (nom), prix, date
  - Adresse de livraison si `delivery=true`
  - Id de commande + lien `/orders/{id}`
  - Signature Fuori Marmo

Reuse `EmailSender` existant. `ConsoleEmailSender` logge en dev (aucun changement requis sur l'infrastructure email).

### Tests

**Backend (JUnit 5 + MockMvc, H2 in-memory, profil `test`, `MolliePaymentGatewayFake` injecté)** :

| Classe | Couverture |
|---|---|
| `ProductPublicControllerTest` | Filtrage tier, sale window, stock (dispo vs épuisé) |
| `OrderCreationTest` | Création OK, out_of_stock, tier_mismatch, no_shipping_address, sale_window_closed, concurrent (2 threads sur dernière unité → 1 succès + 1 conflict) |
| `OrderStatusPollingTest` | `GET /api/orders/{id}` renvoie le bon statut, propriétaire seulement (401/403 sinon) |
| `MollieWebhookTest` | Idempotence (double appel = no-op), transitions paid/failed/cancelled/expired, email envoyé sur paid, id Mollie inconnu = 200 silencieux |
| `OrderExpirationJobTest` | Passe PENDING_PAYMENT expirés en EXPIRED, n'affecte pas les autres statuts |
| `AdminOrdersControllerTest` | Filtres, pagination, ADMIN only |
| `DevSimulateWebhookTest` | Actif en `dev` uniquement, applique correctement les transitions |

**Frontend (Playwright E2E, backend en profil `dev` avec `mollie.enabled=false`)** :

| Spec | Couverture |
|---|---|
| `shop-browse.spec.ts` | Allocataire voit sa liste filtrée, clique produit, voit détail |
| `shop-tier-filter.spec.ts` | User TIER_3 ne voit pas les produits TIER_1 dans la liste ni en direct URL (404) |
| `shop-out-of-stock.spec.ts` | Produit stock=0 → card désactivée, bouton "Acheter" inactif avec message |
| `shop-purchase.spec.ts` | Achat complet : détail → checkout → adresse → "Payer" → return page → simulate-webhook paid → confirmation affichée |
| `admin-orders.spec.ts` | Admin voit toutes les commandes, filtre par statut |

### Migrations Flyway

- `V4__orders.sql` — création table `orders` + index. Compat PG + H2 (utiliser `CLOB` ou `JSON` selon dialecte pour les snapshots, via conversion JPA).
- Pas de seed initial.

### Dépendances

- **Pas de SDK Mollie** — `RestClient` Spring + mapping Jackson manuel (~80 lignes).
- **Job scheduling** : `@EnableScheduling` à ajouter sur `FuoriMondoApplication` si absent.

### Dev workflow

1. Profil `dev` avec `mollie.enabled=false` → gateway fake.
2. User clique "Payer" → order créée, `mollieCheckoutUrl` pointe vers `/shop/order/{id}/return?sim=1`.
3. Page retour détecte `sim=1` et affiche un panneau dev "Simuler un retour Mollie" avec boutons paid/failed/cancelled/expired → appelle l'endpoint `/api/dev/orders/{id}/simulate-webhook?status=...`.
4. Statut mis à jour, email logué en console, page retour affiche l'état final.

### Staging / prod

- Variables env `MOLLIE_API_KEY`, `MOLLIE_ENABLED=true`, `WEBHOOK_BASE_URL=https://fuorimondo.com`, `APP_BASE_URL=https://fuorimondo.com`.
- Mollie configuré avec clé test en staging, clé live en prod.
- Tests manuels du vrai flux Mollie réalisés en staging avant mise en prod.

---

## Ordre d'implémentation suggéré

1. Backend : entité `Order` + migration V4 + repo
2. Backend : `ProductPublicController` (GET list + detail) avec filtrage tier/window/stock
3. Backend : `OrderService` + `OrderController` (POST create avec validation atomique, GET)
4. Backend : `MolliePaymentGateway` interface + Fake + Http (stub appels)
5. Backend : Webhook `/api/webhooks/mollie` + security exclusion CSRF
6. Backend : Endpoint dev `/api/dev/orders/{id}/simulate-webhook`
7. Backend : Job `@Scheduled` expiration
8. Backend : Email de confirmation (template multilingue)
9. Backend : `AdminOrderController`
10. Frontend : routes + stores + i18n
11. Frontend : `ShopView` + `ShopProductView`
12. Frontend : `ShopCheckoutView` + `ShopReturnView` (avec panneau dev sim)
13. Frontend : `MyOrdersView` + `OrderDetailView`
14. Frontend : `AdminOrdersView` + `AdminOrderDetailView`
15. Frontend : lien sidebar + OrderStatusBadge
16. Tests backend + E2E

# FuoriMondo — Sous-projet « Products » — Phase 1 : Admin CRUD — Design

**Date** : 2026-04-23
**Statut** : Proposé, en attente de revue utilisateur
**Auteurs** : session de brainstorming pvandermaesen@noctis.be + Claude

## 1. Contexte & décomposition

L'utilisateur souhaite introduire la notion de **produit** (un vin proposé à la vente : nom, prix, photo, poids, cercle concerné, fenêtre de vente, stock). L'ensemble — admin CRUD, e-shop allocataire, panier, commande, paiement — est trop volumineux pour un seul cycle spec / plan. Découpage convenu :

- **Phase 1 (ce document)** — CRUD produit côté admin (backend + frontend) avec upload photo.
- **Phase 2** — E-shop « browse only » : page allocataire qui liste les produits visibles pour son cercle et dans la fenêtre de vente.
- **Phase 3** — Panier (store Pinia, page `/cart`).
- **Phase 4** — Commande (entité `Order`, lien adresse livraison, vue admin, emails).
- **Phase 5** — Paiement (Stripe, webhook, décrémentation stock définitive, remboursements).

Ce spec couvre **uniquement la Phase 1**. Les phases suivantes feront chacune l'objet d'un spec + plan dédiés.

### Ce que couvre la Phase 1

- Entité JPA `Product` + migration + table de liaison `product_tiers`.
- API admin `/api/admin/products` (list / get / create / update / delete + upload/download/delete photo).
- Stockage photo sur disque local, dossier configurable.
- Frontend admin : écrans list / create / detail-edit + lien de navigation.
- Validation Bean Validation côté back, validation simple côté front.
- i18n FR uniquement (IT/EN suivront hors scope).

### Ce que ne couvre PAS la Phase 1

- Pas d'endpoint public allocataire (Phase 2).
- Pas de filtrage par tier ni par fenêtre de vente pour les lecteurs (Phase 2).
- Pas de pagination ni de recherche (l'utilisateur a précisé « il n'y en aura jamais beaucoup »).
- Pas de preview/cropping d'image, pas de versionning photo.
- Pas de historique/audit log des modifications produit.
- Pas de gestion des prix par devise multiple (EUR implicite).

## 2. Choix fonctionnels (issus du brainstorming)

| Sujet | Choix retenu |
|---|---|
| Photo | Upload multipart, stockage disque local dans `fuorimondo.uploads.dir`. |
| Cercles | Multi-sélection (`Set<TierCode>`), pas de logique cumulative implicite. |
| Dates de vente | `saleStartAt` obligatoire, `saleEndAt` optionnel (vente ouverte tant que non fermée). |
| Prix | `BigDecimal(10,2)`, EUR implicite (pas de champ devise). |
| Stock | `Integer` nullable ; `null` = illimité / non tracké ; sinon `≥ 0`. |
| Poids | `BigDecimal(6,3)` en kg. |
| i18n frontend | FR uniquement en Phase 1. |

## 3. Modèle de données

### Table `products`

| Colonne | Type | Contraintes |
|---|---|---|
| `id` | UUID | PK (cohérent avec `BaseEntity` existant) |
| `name` | VARCHAR(200) | NOT NULL |
| `description` | VARCHAR(4000) | NULL |
| `price_eur` | NUMERIC(10,2) | NOT NULL, `≥ 0` |
| `photo_filename` | VARCHAR(255) | NULL |
| `delivery` | BOOLEAN | NOT NULL |
| `weight_kg` | NUMERIC(6,3) | NULL |
| `sale_start_at` | TIMESTAMP | NOT NULL |
| `sale_end_at` | TIMESTAMP | NULL |
| `stock` | INTEGER | NULL ; si non null : `≥ 0` |
| `created_at` | TIMESTAMP | NOT NULL, défaut `now()` |
| `updated_at` | TIMESTAMP | NOT NULL, auto-update |

Index :
- `idx_products_sale_start_at` sur `sale_start_at` (utile pour Phase 2).

### Table `product_tiers`

| Colonne | Type | Contraintes |
|---|---|---|
| `product_id` | UUID | NOT NULL, FK `products.id` ON DELETE CASCADE |
| `tier_code` | VARCHAR(16) | NOT NULL, CHECK dans (`TIER_1`, `TIER_2`, `TIER_3`) |

PK composée (`product_id`, `tier_code`). Au moins une ligne par produit (invariant applicatif, non SQL).

### Migration

Nouvelle migration Flyway `V3__products.sql` dans `backend/src/main/resources/db/migration/`. Elle crée les deux tables + l'index.

## 4. Entité & services (backend)

### Package

Nouveau package `com.fuorimondo.products`, parallèle à `users`, `addresses`, `legal`.

### Entité `Product.java`

- Champs mappés 1-pour-1 avec la table.
- `tiers` : `Set<TierCode>` via `@ElementCollection(fetch = FetchType.EAGER)` + `@CollectionTable(name = "product_tiers", joinColumns = ...)` + `@Enumerated(EnumType.STRING)` + `@Column(name = "tier_code")`.
- `createdAt` / `updatedAt` via `@PrePersist` / `@PreUpdate`.
- `TierCode` réutilise l'énum existant de `com.fuorimondo.users`.

### Repository

`ProductRepository extends JpaRepository<Product, Long>` — aucune méthode custom en Phase 1 (findAll suffit, pas de filtrage).

### Service `ProductService`

Responsabilités :
- Création / mise à jour (mapping DTO ↔ entité, validation tier-set non vide, `saleEndAt ≥ saleStartAt`).
- Suppression (suppression du fichier photo associé si présent).
- Upload photo : validation type MIME (`image/jpeg`, `image/png`, `image/webp`), validation taille (max 5 MB), écriture atomique (fichier temporaire puis `Files.move(... REPLACE_EXISTING, ATOMIC_MOVE)`), suppression de l'ancien fichier si remplacement.
- Lecture photo : retourne `Resource` + `MediaType` inféré de l'extension.
- Suppression photo : supprime le fichier + met `photoFilename` à null.

Le dossier racine est configuré via `fuorimondo.uploads.dir` (défaut `./uploads` en dev, à override en prod). Sous-dossier `products/` créé au démarrage du service si absent.

### Controller `AdminProductController`

Base path `/api/admin/products`, `@PreAuthorize("hasRole('ADMIN')")` au niveau classe.

| Méthode | Chemin | Body | Retour |
|---|---|---|---|
| `GET` | `/` | — | `List<ProductResponse>` |
| `GET` | `/{id}` | — | `ProductResponse` |
| `POST` | `/` | `ProductRequest` (JSON) | `201` + `ProductResponse` |
| `PATCH` | `/{id}` | `ProductRequest` partiel | `ProductResponse` |
| `DELETE` | `/{id}` | — | `204` |
| `POST` | `/{id}/photo` | multipart `file` | `ProductResponse` |
| `DELETE` | `/{id}/photo` | — | `ProductResponse` |
| `GET` | `/{id}/photo` | — | binaire image |

`PATCH` utilise un DTO avec champs `@Nullable` : seuls les champs présents sont mis à jour (pour la Phase 1, on peut simplifier en envoyant toujours l'objet complet depuis le frontend — le back reste tolérant aux omissions).

### DTOs

```
ProductRequest {
  @NotBlank @Size(max=200) name;
  @Size(max=4000) description;
  @NotNull @DecimalMin("0.00") priceEur; // BigDecimal
  @NotNull delivery;
  weightKg; // BigDecimal nullable
  @NotEmpty tiers; // Set<TierCode>
  @NotNull saleStartAt;
  saleEndAt; // Instant nullable
  @PositiveOrZero stock; // Integer nullable
}

ProductResponse {
  id, name, description, priceEur, photoFilename (nullable),
  delivery, weightKg, tiers (List), saleStartAt, saleEndAt, stock,
  createdAt, updatedAt
}
```

`photoFilename` est renvoyé tel quel (le frontend construit l'URL `/api/admin/products/{id}/photo`).

### Sécurité

Les endpoints `/api/admin/products/**` sont déjà couverts par la règle existante `admin/**` du `SecurityConfig`. Aucune modification nécessaire sauf autoriser les multipart (vérifier la config existante).

## 5. Frontend

### Routes (dans `frontend/src/router.ts`, meta `admin: true`)

- `/admin/products` → `AdminProductsView.vue`
- `/admin/products/create` → `AdminCreateProductView.vue`
- `/admin/products/:id` → `AdminProductDetailView.vue`

### Navigation

Ajout d'un lien « Produits » dans le drawer [AppLayout.vue](../../../frontend/src/components/AppLayout.vue), section admin, sous « Utilisateurs », visible si `auth.user.role === 'ADMIN'`.

### Vues

**`AdminProductsView.vue`** — titre + bouton `+ Créer un produit`, liste `<ul>` avec pour chaque produit : nom, prix formaté, tiers (badges), fenêtre de vente, stock (ou « ∞ » si null). Clic ligne → détail.

**`AdminCreateProductView.vue`** — formulaire minimal (sans photo), champs : name, description (textarea), priceEur, delivery (checkbox), weightKg, tiers (3 checkboxes TIER_1/2/3), saleStartAt (`<input type="datetime-local">`), saleEndAt (idem), stock. Soumet `POST /api/admin/products` → redirige vers détail.

**`AdminProductDetailView.vue`** — deux zones :
- **Photo** : `<img :src="/api/admin/products/{id}/photo">` (avec fallback « Pas de photo »). `<input type="file" accept="image/*">` + bouton « Téléverser/Remplacer ». Bouton « Supprimer photo » si présente.
- **Formulaire** : mêmes champs que création, pré-remplis ; bouton « Mettre à jour » + bouton « Supprimer produit » (confirmation `window.confirm`).

### API client

Le wrapper `api` actuel ([frontend/src/api/client.ts](../../../frontend/src/api/client.ts)) est JSON-only. Ajout d'une fonction :

```ts
export async function uploadMultipart<T>(path: string, file: File, field = 'file'): Promise<T>
```

qui construit un `FormData`, incrémente `inFlight`, gère CSRF (même logique que `request()`), et poste le fichier. Placée dans `client.ts` à côté de `request()`.

### Types

Ajout dans `frontend/src/api/types.ts` :
- `ProductResponse` (champs identiques au DTO back)
- `ProductRequest`
- `type TierCode = 'TIER_1' | 'TIER_2' | 'TIER_3'` si pas déjà présent.

### i18n (FR uniquement)

Nouveau bloc dans `frontend/src/i18n/fr.ts` :

```
admin.products: {
  title, list, create, edit, empty,
  name, description, price, photo, noPhoto, uploadPhoto, removePhoto,
  delivery, weight, weightUnit, tiers, saleStart, saleEnd, stock, stockUnlimited,
  save, delete, confirmDelete, created,
}
```

## 6. Contraintes et edge cases

- **Suppression produit** : le service supprime d'abord le fichier photo (si existant), puis l'entité (cascade supprime les liens `product_tiers`).
- **Remplacement photo** : l'ancien fichier est supprimé après écriture réussie de la nouvelle.
- **Erreurs upload** : si l'écriture disque échoue, on remonte 500 avec message clair ; `photoFilename` reste inchangé.
- **Concurrence** : en Phase 1, on ne gère pas les races (admin unique en pratique).
- **Dates** : `saleEndAt` peut être dans le passé (produit fermé). L'admin peut l'éditer pour rouvrir ; le frontend n'empêche rien, c'est volontaire.
- **Stock à 0** : produit visible côté admin, sera filtré côté allocataire en Phase 2.

## 7. Tests

Pas de couverture formelle demandée. On s'appuiera sur :
- un test d'intégration backend minimal pour le happy path POST + GET + DELETE (sans photo),
- vérification manuelle du parcours admin complet après implémentation.

## 8. Configuration & déploiement

Ajout dans `application.yml` :

```yaml
fuorimondo:
  uploads:
    dir: ${FUORIMONDO_UPLOADS_DIR:./uploads}
```

En prod, mettre un chemin persistant hors du jar. Pas de changement de build / pipeline.

## 9. Hors scope & points reportés

- Pagination, tri, recherche.
- Historique / audit log.
- Preview / recadrage photo.
- Traductions IT / EN des libellés produit (à faire en batch plus tard).
- Multi-devises.

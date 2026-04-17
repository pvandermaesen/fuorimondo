# FuoriMondo — Sous-projet #1 « Socle » — Design

**Date** : 2026-04-17
**Statut** : Proposé, en attente de revue utilisateur
**Auteurs** : session de brainstorming pvandermaesen@noctis.be + Claude

## 1. Contexte & périmètre

FuoriMondo est une application web mobile-first pour la marque de vin exclusive **Fuori Marmo**. L'application sert trois publics :

- Les **allocataires** (clients ayant déjà acheté) qui accèdent à leur profil, leur cave virtuelle, les ventes proposées, leurs avantages
- Les **personnes en liste d'attente** souhaitant devenir allocataires
- Les **administrateurs** internes gérant la plateforme

Le projet global a été découpé en 8 sous-projets. Le présent document décrit le **Socle**, premier sous-projet, fondation technique et fonctionnelle des sept suivants. Les points 4 (personnalisation étiquette/caisse/vélin) et 7 (registre des points de vente) sont remis à plus tard.

### Ce que couvre le Socle

- Bootstrap du projet (backend Spring Boot + frontend Vue 3)
- Système d'authentification : activation par code pour allocataires existants, inscription libre pour la liste d'attente, login/logout, mot de passe oublié
- Gestion des profils utilisateurs avec trois tiers (`TIER_1`, `TIER_2`, `TIER_3`) et affichage différencié par couleur
- Gestion des adresses de facturation et de livraison pour les allocataires
- Internationalisation FR / IT / EN dès l'origine
- Pages légales (CGU, CGV, politique de confidentialité, politique cookies, mentions légales)
- Back-office minimal pour créer un allocataire à l'unité et générer un code d'activation

### Ce que ne couvre PAS le Socle (renvoyé aux sous-projets suivants)

- Cave virtuelle, historique de commandes (→ #2)
- Eshop, ventes annuelles, paiement (→ #3)
- Liste d'attente : promotion, cooptation, notifications de délai (→ #5)
- Événements, requêtes spéciales (→ #6)
- Back-office riche : dashboard, stats, édition tiers, import CSV (→ #8)
- Calcul automatique du tier selon le cumul de dépenses (dépendant du module ventes #3, branché ultérieurement)
- Envoi SMTP réel d'emails (le Socle log en console en dev ; l'interface d'envoi existe, le branchement viendra plus tard)
- Éditeur WYSIWYG des pages légales (contenu en markdown dans le repo, édition par commit)

## 2. Architecture & structure du projet

### Monorepo

```
FuoriMondo/
├── backend/        Spring Boot (Maven)
├── frontend/       Vue 3 + Vite + Tailwind
├── docs/           specs, plans
└── ressources/     design, analyse fonctionnelle
```

### Backend

- **Spring Boot 3.x, Java 21, Maven**
- **Base de données** :
  - Dev : H2 en mode file-based (fichier local, persistance entre runs)
  - Prod : PostgreSQL
  - Test : H2 en mémoire, profil Spring `test`
- **Migrations** : Flyway, mêmes scripts sur H2 et Postgres (SQL portable)
- **Sécurité** : Spring Security + Spring Session (store JDBC, pas de Redis à opérer) + CSRF
- **Profils Spring** : `dev`, `prod`, `test`
- **Découpage** : un seul module Maven, packages par domaine (`auth`, `users`, `tiers`, `addresses`, `admin`, `legal`)

### Frontend

- **Vue 3 (Composition API), Vite, Tailwind CSS, vue-i18n, Pinia, Vue Router**
- En dev : Vite sert le frontend, proxy `/api` → `http://localhost:8080`
- En prod : build statique servi par nginx, lequel reverse-proxie `/api/*` vers Spring Boot

### Déploiement (indicatif, non bloquant pour le dev)

Un seul domaine, nginx en frontal : sert le bundle Vue statique par défaut, proxy `/api/*` vers le backend. Postgres + JVM sur un serveur applicatif. Détails à trancher plus tard.

## 3. Modèle de données

### Table `users` (unifiée : waiting list, allocataires, admins)

| Colonne | Type | Description |
|---|---|---|
| `id` | UUID PK | |
| `email` | string unique, lowercase | |
| `first_name` | string | |
| `last_name` | string | |
| `civility` | enum `MR` / `MRS` / `OTHER` / `NONE` | |
| `birth_date` | date, nullable | Vérification majorité vente d'alcool |
| `phone` | string, nullable | Optionnel pour waiting list |
| `country` | string | |
| `city` | string | |
| `password_hash` | string, nullable | Null tant que compte non activé (ALLOCATAIRE_PENDING) |
| `status` | enum `WAITING_LIST` / `ALLOCATAIRE_PENDING` / `ALLOCATAIRE` / `SUSPENDED` | |
| `role` | enum `USER` / `ADMIN` | |
| `tier_code` | string nullable | `TIER_1` / `TIER_2` / `TIER_3`, valorisé si `ALLOCATAIRE` |
| `referrer_info` | text nullable | Champ « parrain / comment avez-vous connu Fuori Marmo » renseigné à l'inscription waiting list |
| `locale` | enum `FR` / `IT` / `EN`, défaut `FR` | Modifiable dans le profil |
| `admin_notes` | text nullable | Visible uniquement côté back-office |
| `created_at`, `updated_at` | timestamp | |

L'unification permet la promotion naturelle waiting-list → allocataire sans migration de données (changement de `status` + renseignement de `tier_code`).

### Table `addresses`

| Colonne | Type | Description |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | FK `users` | |
| `type` | enum `BILLING` / `SHIPPING` | |
| `full_name` | string | Peut différer du nom du compte (envoi tiers) |
| `street` | string | |
| `street_extra` | string nullable | |
| `postal_code` | string | |
| `city` | string | |
| `country` | string | |
| `is_default` | boolean | Par type |
| `created_at`, `updated_at` | timestamp | |

Un allocataire peut avoir 0..N adresses de chaque type.

### Table `invitation_codes`

| Colonne | Type | Description |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | FK `users`, unique | Un seul code actif par user |
| `code` | string unique, 6 caractères alphanumériques sans ambiguïté (exclut `0`, `O`, `1`, `I`) | |
| `generated_at` | timestamp | |
| `generated_by` | FK `users` | L'admin |
| `expires_at` | timestamp | Défaut +90 jours |
| `used_at` | timestamp nullable | Renseigné à l'activation |

Régénération par l'admin : remplace l'entrée existante pour le même `user_id` (l'ancien code devient invalide).

### Table `password_reset_tokens`

| Colonne | Type | Description |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | FK `users` | |
| `token` | string unique, hash du token envoyé par email | Le token clair n'est jamais stocké |
| `created_at` | timestamp | |
| `expires_at` | timestamp | Défaut +1 heure |
| `used_at` | timestamp nullable | Renseigné après reset réussi |

Un user peut avoir plusieurs tokens non utilisés en théorie ; en pratique, la demande d'un nouveau reset invalide les précédents du même user.

### Pages légales

Pas de table dédiée. Les contenus sont en markdown dans `backend/src/main/resources/legal/` : `cgu.fr.md`, `cgu.it.md`, `cgu.en.md`, etc., servis via l'endpoint `GET /api/legal/{slug}?locale={locale}`. Les textes initiaux sont des **modèles RGPD-compatibles par défaut**, avec un bandeau visible haut de page : « Projet de texte — en attente de validation juridique ».

### Configuration Tiers

Pas de table `tiers`. Les codes `TIER_1` / `TIER_2` / `TIER_3` sont des constantes. Le nom affiché et la couleur sont gérés côté configuration (fichier i18n pour le nom, variables Tailwind pour la couleur) — modifiables sans migration.

## 4. Flux d'authentification

### Flux A — Activation allocataire existant

1. Admin crée le user en back-office → `status = ALLOCATAIRE_PENDING`, code généré (6 chars)
2. Admin communique le code par email / courrier (le Socle logue l'email en console en dev ; SMTP plus tard)
3. L'allocataire ouvre l'app → écran « Activer mon compte »
4. Saisit email + code → le back vérifie : code existe, correspond au user, non utilisé, non expiré
5. Écran suivant : définition du mot de passe (min 12 caractères, score zxcvbn ≥ 3)
6. `status = ALLOCATAIRE`, code `used_at` renseigné
7. Connexion automatique → redirection vers le profil

### Flux B — Inscription liste d'attente

1. Écran « Je n'ai pas de code »
2. Formulaire : email, nom, prénom, téléphone, pays, ville, parrain / origine, locale, mot de passe
3. Cases à cocher : acceptation CGU + politique de confidentialité
4. `status = WAITING_LIST`, `role = USER`
5. Email de confirmation (logué en dev)
6. Connexion automatique → écran « Votre demande est en attente »

### Flux C — Connexion classique

1. Email + mot de passe
2. Vérification + création de session (cookie `HttpOnly`, `SameSite=Strict`, `Secure` en prod)
3. Token CSRF renvoyé en header, Vue le stocke et le renvoie à chaque mutation
4. Redirection selon le statut :
   - `WAITING_LIST` → écran « En attente » + édition des infos de contact
   - `ALLOCATAIRE` → dashboard profil complet
   - `ALLOCATAIRE_PENDING` → cas impossible (pas de mot de passe défini)
   - `SUSPENDED` → message « Contactez-nous »

### Flux D — Mot de passe oublié

1. Saisie de l'email → email de reset envoyé (token single-use, expiration 1h)
2. Clic sur le lien → écran « Nouveau mot de passe »
3. Token vérifié et invalidé → mise à jour du password

### Sécurité transverse

- Rate limiting sur login / activation / reset : 5 tentatives par 15 min par IP
- Bcrypt (paramètres Spring Security par défaut)
- Pas de révélation d'existence de compte sur le reset password
- Logout invalide la session côté serveur
- Point d'extension `EmailSender` interface, implémentation Socle = logger console ; SMTP brancheable plus tard

## 5. UI — écrans & navigation mobile

### Navigation

**Menu burger** (icône ☰ top-left, drawer coulissant). Structure :

- **Allocataire** : Profil, Cave (placeholder « Bientôt disponible »), Paramètres, Mentions légales, Déconnexion
- **Waiting list** : Profil (avec bandeau « demande en attente »), Paramètres, Mentions légales, Déconnexion
- **Admin** : mêmes entrées que USER selon son statut, plus une section « Administration » donnant accès aux routes `/admin/*`

### Écrans du Socle

1. **Splash / accueil non connecté** — logo Fuori Marmo centré. Deux CTA : « J'ai un code d'activation » / « Rejoindre la liste d'attente » + lien « Me connecter »
2. **Activation** — étape 1 : email + code 6 caractères / étape 2 : définition mot de passe / étape 3 : écran de bienvenue
3. **Inscription waiting list** — formulaire scrollable single-page, bouton submit → écran de confirmation
4. **Login** — email + mot de passe, lien « Mot de passe oublié »
5. **Reset password** — demande email / définition du nouveau mot de passe
6. **Profil (allocataire)** — header affichant la pastille colorée du tier + libellé, identité éditable, liste des adresses (facturation / livraison) avec ajout / modification / suppression / choix par défaut
7. **Profil (waiting list)** — bandeau « Votre demande est en attente », infos éditables (pas d'adresses, pas de tier)
8. **Paramètres** — changement de locale (FR / IT / EN), changement de mot de passe, lien déconnexion
9. **Pages légales** — markdown rendu, bandeau haut « Projet de texte — en attente de validation juridique »
10. **Admin — liste utilisateurs** — table filtrable par statut, recherche par email / nom
11. **Admin — créer allocataire** — formulaire (email, nom, prénom, civilité, date de naissance, téléphone, pays, ville, tier initial) → crée le user en `ALLOCATAIRE_PENDING`, génère un code, affiche le code à communiquer
12. **Admin — détail user** — tous les champs, possibilité de régénérer un code, modifier le tier, suspendre

### Design tokens (configuration Tailwind)

- **Typographie** : Adobe Fonts kit `zfz7fmi` (famille Fuori Marmo) pour titres et corps
- **Palette** : neutres beige/pierre inspirés du marbre, plus trois accents pour les tiers (couleurs exactes à extraire du PDF de charte `ressources/design/FUORI-MARMO_guidelines.pdf` pendant l'implémentation — proposition initiale : or pâle, cuivre, bronze, à valider)
- **Espacement** : généreux, peu d'éléments par écran
- **Distinction tier** : pastille colorée + libellé sur l'écran Profil

### Responsive

Mobile-first (viewport < 640px). Sur tablette et desktop : contenu centré, largeur max 640-720px. Pas de layout desktop étendu — l'application est pensée mobile, les écrans plus grands sont un bonus.

## 6. Stratégie de test

### Backend

- **Tests unitaires JUnit 5** : logique métier (validation des codes d'invitation, transitions de statut, calcul d'expiration)
- **Tests d'intégration Spring Boot Test** : endpoints REST (activation, login, CRUD adresses, actions admin), profil `test` avec H2 en mémoire
- Pas de tests de performance / charge

### Frontend

- **Tests unitaires Vitest** : composables et stores Pinia critiques (auth store, validation des formulaires)
- **Tests E2E Playwright** sur les parcours clés : activation complète, inscription waiting list, login + logout, admin crée allocataire
- Pas de tests visuels (screenshot) pour le Socle

## 7. Critères d'acceptation

Le Socle est considéré terminé quand :

1. Un admin peut se connecter, créer un allocataire, obtenir le code à lui communiquer
2. Un allocataire avec code peut activer son compte, se connecter, voir son profil avec son tier, gérer ses adresses
3. Un visiteur peut s'inscrire en liste d'attente et se reconnecter pour consulter son statut
4. Les trois locales FR / IT / EN fonctionnent sur les écrans et les messages d'erreur
5. Les cinq pages légales s'affichent dans chaque langue
6. Les tests passent (unitaires back, unitaires front, E2E)
7. Le front build sans erreur ; le back démarre en profils `dev` (H2) et `prod` (Postgres)

## 8. Dépendances externes (hors code)

Non bloquantes pour le développement, à configurer avant la mise en production :

- Compte Adobe Fonts avec accès au kit `zfz7fmi`
- Fournisseur SMTP pour les emails transactionnels (à brancher sur l'interface `EmailSender`)
- Hébergement prod : base Postgres, JVM, nginx
- Validation juridique des textes par défaut des pages légales

## 9. Points reportés pour validation ultérieure

- Noms définitifs des trois tiers (actuellement codes `TIER_1` / `TIER_2` / `TIER_3`)
- Couleurs exactes des tiers (extraction du PDF de charte pendant l'implémentation)
- Choix de l'hébergeur et de la stack d'emails transactionnels
- Relecture juridique des textes légaux par défaut

# Socle — Plan B : Frontend Vue & E2E — Implementation Plan

> **For agentic workers:** Exécution inline dans cette session. Commits atomiques par tâche.

**Goal:** Livrer le frontend Vue 3 mobile-first complet du Socle FuoriMondo : shell de navigation avec menu burger, 12 écrans (non-connecté, allocataire, waiting list, admin), i18n FR/IT/EN, design tokens Tailwind inspirés de la charte Fuori Marmo (Or/Noir/Gris/Rouge + typo Tiller/Univers/Garamond), tests Playwright E2E sur les parcours clés.

**Architecture:** `frontend/` module dans le monorepo. Vue 3 Composition API, Vite, Tailwind CSS, Pinia (stores), Vue Router 4, vue-i18n v9. API client fetch-based avec CSRF (cookie XSRF-TOKEN → header X-XSRF-TOKEN). Playwright pour E2E. En dev : proxy Vite `/api` → `localhost:8080`.

**Tech Stack:**
- Vue 3.4+, Vite 5+, Node 18+
- Tailwind CSS 3, PostCSS
- Pinia 2, Vue Router 4, vue-i18n 9
- Playwright 1.49+
- TypeScript (strict mode)

**Référence spec:** `docs/superpowers/specs/2026-04-17-socle-design.md`
**Backend (déjà livré):** `docs/superpowers/plans/2026-04-17-socle-plan-a-backend.md`

## Design tokens extraits de la charte

**Palette :**
- `fm-white` (dominant fond): `#FFFFFF`
- `fm-gold` (TIER_1, accents premium): `#C9A96E` (approximation LUXOR 420)
- `fm-black` (texte, structure): `#1A1A1A`
- `fm-gray` (texte secondaire, TIER_3): `#BCB4A8` (Warm Grey 4)
- `fm-red` (TIER_2, accents intensité): `#E03A00` (LUXOR 964)
- `fm-stone` (fonds secondaires, inspiré marbre): `#F5F2ED`

**Typographie :**
- Headlines logo: **Tiller** (Adobe Fonts kit `zfz7fmi`)
- Corps: **Univers** (fallback système sans-serif)
- Accent italic: **Adobe Garamond Italic** (fallback serif italic)

**Mapping Tiers :**
- `TIER_1` → Or `#C9A96E` — Prestige
- `TIER_2` → Rouge `#E03A00` — Intensité
- `TIER_3` → Gris `#BCB4A8` — Base

## Phases & tâches

### Phase 1 — Scaffolding
- **T1** : `npm create vite@latest frontend -- --template vue-ts`, installer dépendances (tailwindcss, pinia, vue-router, vue-i18n, @types/node)
- **T2** : Config Vite (proxy `/api`), Tailwind config avec design tokens, `src/main.ts`, `src/App.vue`, CSS base + fonts
- **T3** : API client (`src/api/client.ts`) — fetch wrapper avec CSRF, types TypeScript pour les DTOs, gestion 401/403

### Phase 2 — Infra transverse
- **T4** : Store Pinia `auth` (login, logout, fetchMe, état currentUser)
- **T5** : Router + guards (routes public/auth/admin)
- **T6** : i18n setup + 3 fichiers locale (fr.ts, it.ts, en.ts) avec clés de toutes les chaînes
- **T7** : Composants UI de base : `FmButton`, `FmInput`, `FmCard`, `TierBadge`
- **T8** : Shell layout — `AppLayout.vue` avec header + burger menu drawer, footer

### Phase 3 — Écrans non-connecté
- **T9** : Splash / accueil (logo + 2 CTA + lien login)
- **T10** : Login
- **T11** : Inscription waiting list (formulaire complet, validation)
- **T12** : Activation (3 étapes : email+code / password / welcome)
- **T13** : Reset password (demande + confirmation)

### Phase 4 — Écrans authentifiés
- **T14** : Profil allocataire (header tier, identité éditable)
- **T15** : Gestion adresses (liste + form create/edit/delete)
- **T16** : Profil waiting list (bandeau "en attente" + infos éditables)
- **T17** : Paramètres (locale, change password)
- **T18** : Pages légales (viewer markdown avec bandeau "projet")

### Phase 5 — Admin
- **T19** : Admin users list (table avec recherche + filtre par status)
- **T20** : Admin create allocataire (form + affichage du code généré)
- **T21** : Admin user detail (vue + regénérer code + update tier/status)

### Phase 6 — E2E + finalisation
- **T22** : Playwright setup + config
- **T23** : 4 parcours E2E (activation complète, inscription waiting list, login+logout, admin crée allocataire)
- **T24** : README frontend + build production vérifié

## Critères d'acceptation (fin Plan B = Socle complet)

- `npm run build` produit un bundle sans erreur
- `npm run test:e2e` passe les 4 parcours (back démarré en parallèle)
- Navigation mobile fluide (burger, transitions discrètes)
- Les 3 locales basculent correctement sur tous les écrans
- Les 3 tiers affichent leur couleur correcte
- Admin peut créer un allocataire, le code affiché fonctionne pour l'activation par un autre navigateur

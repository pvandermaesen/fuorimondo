# FuoriMondo — Frontend

Vue 3 + Vite + Tailwind + TypeScript. Mobile-first.

## Prérequis

- Node 20+ (testé avec Node 24)
- Backend FuoriMondo démarré (défaut: port 8081, via `java -jar ... --server.port=8081 --spring.profiles.active=dev`)

## Développement

```bash
npm install
npm run dev
```

Vite dev sur http://localhost:5273 avec proxy `/api` → `http://localhost:8081` (configurable via `VITE_API_URL`).

## Tests E2E (Playwright)

```bash
# dans un terminal : démarrer le backend
cd ../backend && java -jar target/fuorimondo-backend-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=dev --server.port=8081

# dans un autre terminal : lancer les tests (Playwright démarre Vite auto)
npm run test:e2e
```

4 parcours couverts : splash + CTAs, page légale, inscription waiting list, admin crée allocataire.

## Build production

```bash
npm run build
# → dist/  (bundle gzip ~200 KB)
```

## Structure

```
src/
├── api/            client fetch + types DTO
├── components/     FmButton, FmInput, FmSelect, FmCheckbox, FmLogo, TierBadge, AppLayout
├── i18n/           fr/it/en + bootstrap
├── stores/         Pinia (auth)
├── views/          écrans publics + authentifiés
└── views/admin/    écrans back-office
```

## Design tokens (extraits de la charte Fuori Marmo)

| Token | Valeur | Usage |
|---|---|---|
| `fm-white` | `#FFFFFF` | Fond dominant |
| `fm-stone` | `#F5F2ED` | Fond secondaire |
| `fm-gold` (TIER_1) | `#C9A96E` | Premier cercle, accent premium |
| `fm-red` (TIER_2) | `#E03A00` | Deuxième cercle, accent intensité |
| `fm-gray` (TIER_3) | `#BCB4A8` | Troisième cercle, texte secondaire |
| `fm-black` | `#1A1A1A` | Texte, structure |

**Typo** : `font-logo` (Tiller, via Adobe Fonts kit `zfz7fmi`), `font-sans` (Univers), `font-serif` (Garamond italic).

## Admin dev par défaut

`admin@fuorimondo.local` / `Admin!Password123` (seedé automatiquement par le backend en profil `dev`).

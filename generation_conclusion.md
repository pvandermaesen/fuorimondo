# Plan B (Frontend) terminé — Socle FuoriMondo complet

## Livraison

- 29 commits au total sur `main` (25 backend + 4 frontend)
- Frontend Vue 3 TS + Vite 8 + Tailwind 3 + Pinia + Vue Router 4 + vue-i18n 9
- **12 écrans** : splash, login, register (waiting list), activate (3 étapes), reset password, profile, addresses (CRUD), settings, legal viewer, admin users list/search, admin create allocataire, admin user detail
- Design tokens extraits de la charte : palette Or `#C9A96E` (TIER_1) / Rouge `#E03A00` (TIER_2) / Gris `#BCB4A8` (TIER_3) + Tiller/Univers/Garamond via Adobe Fonts `zfz7fmi`
- i18n **FR / IT / EN** (~100 clés, persistance localStorage, détection navigator.language)
- Navigation : menu burger avec drawer, switcher de langue intégré
- Shell sobre : bordures fines, uppercase tracking large, pas d'animations superflues
- Sécurité : session cookie HttpOnly + CSRF via header `X-XSRF-TOKEN` (amorcé automatiquement)
- `npm run build` → bundle ≈ 200 KB gzipped
- **4/4 tests E2E Playwright passent** : splash, page légale, inscription waiting list, admin crée allocataire (code 6 chars affiché)

## Pour lancer en dev

```bash
# Terminal 1
cd backend && java -jar target/fuorimondo-backend-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=dev --server.port=8081

# Terminal 2
cd frontend && npm run dev    # http://localhost:5273
```

**Admin dev** : `admin@fuorimondo.local` / `Admin!Password123`

## Points à revisiter plus tard (non-bloquants)

- Noms/couleurs finaux des tiers (validation marketing)
- Textes légaux définitifs (relecture juridique)
- Branchement SMTP pour emails transactionnels
- Versions production de JDK 21 + Java 21 feature usage si souhaité

## Prochaines étapes

Tu peux tester manuellement via `npm run dev` après avoir lancé le backend. Sous-projets #2, #3, #5, #6, #8 attendent leur tour quand tu es prêt.

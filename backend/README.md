# FuoriMondo — Backend

Spring Boot 3.3 / Java 17 / Maven.

## Prérequis

- JDK 17
- Maven 3.9+

## Développement

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Démarre sur http://localhost:8080 avec H2 en fichier (`./data/fuorimondo-dev`).

**Console H2** : http://localhost:8080/h2-console — URL JDBC : `jdbc:h2:file:./data/fuorimondo-dev`, user `sa`, pas de password.

**Swagger UI** : http://localhost:8080/swagger-ui.html

**Admin par défaut (dev)** : `admin@fuorimondo.local` / `Admin!Password123` (logué en console au premier démarrage).

## Tests

```bash
mvn test
```

Tests d'intégration Spring Boot sur H2 in-memory (profil `test`).

## Profils

- `dev` (défaut) — H2 file-based, logs DEBUG, console H2 exposée
- `prod` — PostgreSQL via variables `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `test` — H2 in-memory, pas de store session

## Structure

```
src/main/java/com/fuorimondo/
├── admin/        back-office admin
├── addresses/    gestion des adresses
├── auth/         authentification, codes, tokens, reset
├── common/       BaseEntity, ApiError, GlobalExceptionHandler
├── email/        interface + implémentation console
├── legal/        pages légales
├── security/     Spring Security config, rate limiting
└── users/        entité User + profil
```

## Endpoints principaux

| Méthode | Chemin | Description |
|---|---|---|
| POST | `/api/auth/login` | Connexion (email/password) |
| POST | `/api/auth/logout` | Déconnexion |
| POST | `/api/auth/register` | Inscription liste d'attente |
| POST | `/api/auth/activate/verify` | Vérifie email+code |
| POST | `/api/auth/activate` | Définit le mot de passe après validation code |
| POST | `/api/auth/password-reset/request` | Demande de reset |
| POST | `/api/auth/password-reset/confirm` | Confirmation avec token |
| GET | `/api/me` | Profil courant |
| PATCH | `/api/me` | Mise à jour profil |
| POST | `/api/me/password` | Changement mdp |
| GET/POST/PUT/DELETE | `/api/me/addresses` | CRUD adresses |
| GET | `/api/legal/{slug}` | Contenu markdown d'une page légale (CGU, CGV, privacy, cookies, mentions) |
| GET | `/api/admin/users` | Lister utilisateurs (ADMIN) |
| GET | `/api/admin/users/{id}` | Détail user (ADMIN) |
| POST | `/api/admin/users` | Créer un allocataire + code (ADMIN) |
| POST | `/api/admin/users/{id}/regenerate-code` | Régénérer un code (ADMIN) |
| PATCH | `/api/admin/users/{id}` | MAJ status/tier/notes (ADMIN) |

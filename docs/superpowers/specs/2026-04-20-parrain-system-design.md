# Parrain System — Design

**Date**: 2026-04-20
**Scope**: Admin-only feature. Mark certain users as *parrains* (sponsors) and optionally link any user to an existing parrain. Surfaced in admin > Utilisateurs.

## Context

The waiting list registration form already captures `referrer_info` (free text, "Qui vous a parlé de Fuori Marmo ?"). That field is self-declared and unstructured. The parrain system adds a structured, admin-curated link alongside it. Both coexist.

No end-user surface for v1. Purely admin-facing.

## Data Model

Two columns added to `users`:

| Column       | Type                     | Constraint                                     | Purpose                                          |
|--------------|--------------------------|------------------------------------------------|--------------------------------------------------|
| `is_parrain` | `BOOLEAN NOT NULL`       | default `FALSE`                                | Flag: this user can be chosen as someone's parrain |
| `parrain_id` | `UUID NULL`              | `REFERENCES users(id) ON DELETE SET NULL`      | FK to the user's parrain (self-reference)        |

Index: partial index on `is_parrain` for fast autocomplete lookups:

```sql
CREATE INDEX idx_users_is_parrain ON users(is_parrain) WHERE is_parrain = TRUE;
```

Migration file: `backend/src/main/resources/db/migration/V5__parrain.sql`.

## Business Rules

**Promotion / demotion (`is_parrain` toggle)**
- Promotion: any user can be flagged `is_parrain = true`.
- Demotion: allowed unconditionally. Existing `parrain_id` references pointing to the demoted user are **preserved** (historical link). The demoted user simply stops appearing in search results.

**Assigning a parrain (`parrain_id`)**
- The target parrain must currently have `is_parrain = true`. Otherwise `409 CONFLICT`.
- A user cannot be their own parrain. Otherwise `400 BAD REQUEST`.
- No enforced depth limit. A parrain *can* themselves have a `parrain_id` in the database — but the UI hides that field for parrains (see Frontend), effectively preventing chains through the admin UX while leaving the data model flat and simple.
- Assigning `parrainId = null` unlinks.

**Interaction with `referrer_info`**
- Orthogonal. `referrer_info` stays as-is, displayed read-only in admin. `parrain_id` is a separate, independent link. Both shown side by side in the detail view.

## API

All endpoints under `/api/admin/users`, admin-only (existing auth rules).

### GET `/api/admin/users/parrains?q=<query>`
Autocomplete source.

- Filters: `is_parrain = true` **AND** (`first_name` / `last_name` / `email` ILIKE `%q%`).
- `q` optional; if absent, returns the first 20 parrains by last name ASC.
- Max 20 results, no pagination (autocomplete UX doesn't need it).
- Response: `List<ParrainOption>` where `ParrainOption = { id: UUID, firstName: String, lastName: String, email: String }`.

### PATCH `/api/admin/users/{id}` (extended)
Adds one optional field to the existing `UpdateUserByAdminRequest`:

```java
public record UpdateUserByAdminRequest(
    UserStatus status,
    TierCode tierCode,
    @Size(max = 2000) String adminNotes,
    Boolean isParrain                 // null = no change
) {}
```

Current null-skip semantics are preserved.

### PUT `/api/admin/users/{id}/parrain`
Dedicated endpoint for the link so we can cleanly express "unlink" (`null`) vs "don't change" (field absent).

- Body: `{ "parrainId": "uuid" | null }`.
- Validates:
  - `parrainId == id` → 400.
  - `parrainId != null` and target user has `is_parrain = false` → 409.
- On success: returns the updated `AdminUserResponse`.

### Extended `AdminUserResponse`

Adds:
- `isParrain: boolean`
- `parrainId: UUID | null`
- `parrainName: String | null` (concat `firstName + " " + lastName`, resolved from the linked parrain; `null` if no link).

## Frontend

**New components**
- `FmSwitch.vue` — minimal on/off toggle styled consistently with `FmCheckbox`. Props: `modelValue: boolean`, `label: string`, `disabled?: boolean`.
- `ParrainAutocomplete.vue` — controlled input + dropdown.
  - Props: `modelValue: { id, firstName, lastName, email } | null`, `placeholder: string`.
  - Emits: `update:modelValue`.
  - Behavior: debounce 250 ms on typing; calls `GET /admin/users/parrains?q=<input>`; renders dropdown `{firstName} {lastName} — {email}`; clicking a row selects; `×` button clears the selection; Esc / click-outside closes the dropdown.

**[AdminUserDetailView.vue](../../frontend/src/views/admin/AdminUserDetailView.vue) additions**

New "Parrainage" section, placed below the existing status/tier form:

1. `FmSwitch` labelled **"Cet utilisateur est un parrain"**, bound to `form.isParrain`. Change saves via the existing PATCH on submit (same button as status/tier).
2. Below the switch, **only when `!form.isParrain`**: `ParrainAutocomplete` labelled **"Parrain de cet utilisateur"**. Shows the current link (with `×` to unlink). Save uses a dedicated call to `PUT /admin/users/{id}/parrain`.

Hiding the autocomplete when `isParrain === true` is purely a UI choice to keep the mental model simple (parrains don't have parrains). The backend does not enforce this; if a user is promoted with an existing `parrain_id`, that link stays in DB but is not surfaced.

**i18n additions** (`fr.json` / `en.json`):
- `admin.parrainSection` — "Parrainage" / "Sponsorship"
- `admin.isParrain` — "Cet utilisateur est un parrain" / "This user is a sponsor"
- `admin.parrainLabel` — "Parrain de cet utilisateur" / "Sponsor of this user"
- `admin.parrainSearchPlaceholder` — "Rechercher un parrain…" / "Search for a sponsor…"
- `admin.parrainNone` — "Aucun parrain" / "No sponsor"
- `admin.parrainClear` — "Délier" / "Unlink"

## Testing

**Backend (`AdminUserServiceTest`, extended)**
- Toggle `is_parrain` flag.
- Assign a parrain: success.
- Assign a parrain who is not `is_parrain` → 409.
- Self-link → 400.
- Demote a parrain who has filleuls: link survives (fetch a filleul, assert `parrain_id` still points to the ex-parrain).
- Search filters on `is_parrain = true` (a non-parrain matching `q` must not appear).
- Search limited to 20.

**Migration (`@SpringBootTest`)**
- App boots with `V5__parrain.sql` applied (implicit — any test class is enough).

**Frontend**: no new E2E test for v1. Manual smoke test checklist below.

## Out of Scope (YAGNI)

- Listing filleuls on a parrain's detail page.
- Cap on number of filleuls per parrain.
- Notifying a parrain when a filleul is assigned.
- Any end-user surface (filleul seeing their parrain, parrain seeing their filleuls).
- Chaining parrains / hierarchy enforcement (flat model; UI hides the field for parrains but no DB check).
- Migration of existing `referrer_info` into `parrain_id`.

## Manual Smoke Test Checklist

1. Run migration → `users` table has both new columns, partial index present.
2. Admin UI: flip switch "Cet utilisateur est un parrain" → save → reload → still on.
3. On a non-parrain user, type 2 letters in the parrain search → 20 max results appear, all with `is_parrain = true`.
4. Select a parrain → save → reload → parrain name shown, `×` button present.
5. Click `×` → save → no parrain shown.
6. Try to assign a non-parrain as parrain via direct API call → 409.
7. Try to assign user to themselves → 400.
8. Demote a parrain who has one filleul → in DB, filleul's `parrain_id` unchanged; parrain no longer shows up in search.

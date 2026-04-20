# Parrain System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a boolean `is_parrain` flag and a self-referencing `parrain_id` link to users, exposed in the admin UI with a toggle switch and an autocomplete-based parrain picker.

**Architecture:** Additive schema change (two columns + partial index). Two admin API endpoints (one for the boolean extended on the existing PATCH, one new PUT for the link). Two new Vue components (a toggle switch and an autocomplete) wired into the existing admin user detail view.

**Tech Stack:** Spring Boot 3.3 (JDK 21), JPA, Flyway, Spring Security, Vue 3 + TypeScript, Pinia not involved (component-local state), Tailwind.

Spec: [docs/superpowers/specs/2026-04-20-parrain-system-design.md](../specs/2026-04-20-parrain-system-design.md)

---

## File Map

**Backend (create 3, modify 7)**
- Create: `backend/src/main/resources/db/migration/V5__parrain.sql`
- Create: `backend/src/main/java/com/fuorimondo/admin/dto/ParrainOption.java`
- Create: `backend/src/main/java/com/fuorimondo/admin/dto/SetParrainRequest.java`
- Modify: `backend/src/main/java/com/fuorimondo/users/User.java`
- Modify: `backend/src/main/java/com/fuorimondo/users/UserRepository.java`
- Modify: `backend/src/main/java/com/fuorimondo/admin/dto/UpdateUserByAdminRequest.java`
- Modify: `backend/src/main/java/com/fuorimondo/admin/dto/AdminUserResponse.java`
- Modify: `backend/src/main/java/com/fuorimondo/admin/AdminUserService.java`
- Modify: `backend/src/main/java/com/fuorimondo/admin/AdminUserController.java`
- Modify: `backend/src/test/java/com/fuorimondo/admin/AdminUserControllerTest.java`

**Frontend (create 2, modify 5)**
- Create: `frontend/src/components/FmSwitch.vue`
- Create: `frontend/src/components/ParrainAutocomplete.vue`
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/i18n/fr.ts`
- Modify: `frontend/src/i18n/en.ts`
- Modify: `frontend/src/i18n/it.ts`
- Modify: `frontend/src/views/admin/AdminUserDetailView.vue`

**Build commands**
- Backend tests: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=<TestClass> test`
- Backend full: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q test`
- Frontend type check: `cd frontend && npm run type-check`
- Frontend build: `cd frontend && npm run build`

---

## Task 1: DB Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__parrain.sql`

- [ ] **Step 1: Write the migration**

```sql
ALTER TABLE users ADD COLUMN is_parrain BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN parrain_id UUID NULL REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_users_is_parrain ON users(is_parrain) WHERE is_parrain = TRUE;
CREATE INDEX idx_users_parrain_id ON users(parrain_id);
```

- [ ] **Step 2: Verify migration applies cleanly**

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#nonAdminGets403 test`
Expected: PASS. (Any existing test class boots Spring Boot, which forces Flyway to run all migrations. If the SQL is malformed, boot fails.)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V5__parrain.sql
git commit -m "feat(users): V5 migration — is_parrain + parrain_id columns"
```

---

## Task 2: User Entity + Repository

**Files:**
- Modify: `backend/src/main/java/com/fuorimondo/users/User.java`
- Modify: `backend/src/main/java/com/fuorimondo/users/UserRepository.java`

- [ ] **Step 1: Add fields to User entity**

In `User.java`, after the `adminNotes` field (around line 59), add:

```java
    @Column(name = "is_parrain", nullable = false)
    private boolean isParrain = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parrain_id")
    private User parrain;
```

At the end of the class (after the last getter/setter), add:

```java
    public boolean isParrain() { return isParrain; }
    public void setIsParrain(boolean parrain) { this.isParrain = parrain; }
    public User getParrain() { return parrain; }
    public void setParrain(User parrain) { this.parrain = parrain; }
```

- [ ] **Step 2: Add repository method for parrain search**

In `UserRepository.java`, after the existing `findByEmailContainingIgnoreCaseOrLastNameContainingIgnoreCase` (line 18-19), add:

```java
    @org.springframework.data.jpa.repository.Query("""
        SELECT u FROM User u
        WHERE u.isParrain = true
          AND (:q IS NULL OR :q = ''
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY u.lastName ASC, u.firstName ASC
        """)
    java.util.List<User> searchParrains(@org.springframework.data.repository.query.Param("q") String q,
                                         org.springframework.data.domain.Pageable pageable);
```

(Use inline fully-qualified imports to avoid churning the import block; alternative: add proper imports at the top.)

- [ ] **Step 3: Compile to verify**

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Boot sanity check**

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#nonAdminGets403 test`
Expected: PASS. (Entity mapping matches new columns; context boots.)

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/users/User.java backend/src/main/java/com/fuorimondo/users/UserRepository.java
git commit -m "feat(users): isParrain flag + parrain self-ref + search repo method"
```

---

## Task 3: DTOs

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/admin/dto/ParrainOption.java`
- Create: `backend/src/main/java/com/fuorimondo/admin/dto/SetParrainRequest.java`
- Modify: `backend/src/main/java/com/fuorimondo/admin/dto/UpdateUserByAdminRequest.java`
- Modify: `backend/src/main/java/com/fuorimondo/admin/dto/AdminUserResponse.java`

- [ ] **Step 1: Create ParrainOption.java**

```java
package com.fuorimondo.admin.dto;

import com.fuorimondo.users.User;
import java.util.UUID;

public record ParrainOption(UUID id, String firstName, String lastName, String email) {
    public static ParrainOption from(User u) {
        return new ParrainOption(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail());
    }
}
```

- [ ] **Step 2: Create SetParrainRequest.java**

```java
package com.fuorimondo.admin.dto;

import java.util.UUID;

public record SetParrainRequest(UUID parrainId) {}
```

- [ ] **Step 3: Extend UpdateUserByAdminRequest.java**

Replace the entire file:

```java
package com.fuorimondo.admin.dto;

import com.fuorimondo.users.TierCode;
import com.fuorimondo.users.UserStatus;
import jakarta.validation.constraints.Size;

public record UpdateUserByAdminRequest(
    UserStatus status,
    TierCode tierCode,
    @Size(max = 2000) String adminNotes,
    Boolean isParrain
) {}
```

- [ ] **Step 4: Extend AdminUserResponse.java**

Replace the entire file:

```java
package com.fuorimondo.admin.dto;

import com.fuorimondo.auth.InvitationCode;
import com.fuorimondo.users.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminUserResponse(
    UUID id, String email, String firstName, String lastName, Civility civility,
    LocalDate birthDate, String phone, String country, String city,
    UserStatus status, UserRole role, TierCode tierCode, Locale locale,
    String referrerInfo, String adminNotes, Instant createdAt,
    String invitationCode, Instant invitationCodeExpiresAt, Instant invitationCodeUsedAt,
    boolean isParrain, UUID parrainId, String parrainFirstName, String parrainLastName
) {
    public static AdminUserResponse from(User u) {
        return from(u, null);
    }

    public static AdminUserResponse from(User u, InvitationCode ic) {
        User p = u.getParrain();
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
            u.getCivility(), u.getBirthDate(), u.getPhone(), u.getCountry(), u.getCity(),
            u.getStatus(), u.getRole(), u.getTierCode(), u.getLocale(),
            u.getReferrerInfo(), u.getAdminNotes(), u.getCreatedAt(),
            ic != null ? ic.getCode() : null,
            ic != null ? ic.getExpiresAt() : null,
            ic != null ? ic.getUsedAt() : null,
            u.isParrain(),
            p != null ? p.getId() : null,
            p != null ? p.getFirstName() : null,
            p != null ? p.getLastName() : null);
    }
}
```

- [ ] **Step 5: Compile**

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/admin/dto/
git commit -m "feat(admin-users): DTOs for parrain (ParrainOption, SetParrainRequest, extended responses)"
```

---

## Task 4: Service — toggle + link/unlink + search

**Files:**
- Modify: `backend/src/main/java/com/fuorimondo/admin/AdminUserService.java`

- [ ] **Step 1: Add `ParrainException` to AdminUserService (inline static record)**

In `AdminUserService.java`, just before the final closing brace (after `CreateAllocataireResult`), add:

```java
    public static class ParrainException extends RuntimeException {
        public enum Reason { SELF_LINK, TARGET_NOT_PARRAIN }
        private final Reason reason;
        public ParrainException(Reason r, String msg) { super(msg); this.reason = r; }
        public Reason getReason() { return reason; }
    }
```

- [ ] **Step 2: Update `update(...)` method to handle `isParrain`**

Replace the body of the `update` method (lines ~94-101) with:

```java
    @Transactional
    public User update(UUID userId, UpdateUserByAdminRequest req) {
        User u = userRepository.findById(userId).orElseThrow();
        if (req.status() != null) u.setStatus(req.status());
        if (req.tierCode() != null) u.setTierCode(req.tierCode());
        if (req.adminNotes() != null) u.setAdminNotes(req.adminNotes());
        if (req.isParrain() != null) u.setIsParrain(req.isParrain().booleanValue());
        return u;
    }
```

- [ ] **Step 3: Add `setParrain` method (before `newCodeFor`)**

```java
    @Transactional
    public User setParrain(UUID userId, UUID parrainId) {
        User u = userRepository.findById(userId).orElseThrow();
        if (parrainId == null) {
            u.setParrain(null);
            return u;
        }
        if (parrainId.equals(userId)) {
            throw new ParrainException(ParrainException.Reason.SELF_LINK, "A user cannot be their own parrain");
        }
        User p = userRepository.findById(parrainId).orElseThrow();
        if (!p.isParrain()) {
            throw new ParrainException(ParrainException.Reason.TARGET_NOT_PARRAIN, "Target user is not a parrain");
        }
        u.setParrain(p);
        return u;
    }
```

- [ ] **Step 4: Add `searchParrains` method (before `newCodeFor`)**

```java
    @Transactional(readOnly = true)
    public java.util.List<User> searchParrains(String q) {
        org.springframework.data.domain.Pageable limit20 = org.springframework.data.domain.PageRequest.of(0, 20);
        return userRepository.searchParrains(q == null ? "" : q.trim(), limit20);
    }
```

- [ ] **Step 5: Compile**

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/admin/AdminUserService.java
git commit -m "feat(admin-users): service — toggle parrain, set/clear link, search parrains"
```

---

## Task 5: Controller endpoints + exception mapping

**Files:**
- Modify: `backend/src/main/java/com/fuorimondo/admin/AdminUserController.java`
- Modify: `backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java` (if ParrainException isn't mapped elsewhere)

- [ ] **Step 1: Add `searchParrains` and `setParrain` endpoints**

In `AdminUserController.java`, add at the end (before the final `}`):

```java
    @GetMapping("/parrains")
    public java.util.List<com.fuorimondo.admin.dto.ParrainOption> searchParrains(
            @RequestParam(required = false) String q) {
        return service.searchParrains(q).stream()
            .map(com.fuorimondo.admin.dto.ParrainOption::from)
            .toList();
    }

    @PutMapping("/{id}/parrain")
    public AdminUserResponse setParrain(@PathVariable UUID id,
                                         @RequestBody com.fuorimondo.admin.dto.SetParrainRequest req) {
        return AdminUserResponse.from(service.setParrain(id, req.parrainId()));
    }
```

- [ ] **Step 2: Read GlobalExceptionHandler to see current mapping style**

Run: `cat backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java`
Expected: you see how other exceptions (e.g. `AuthException`) are mapped. Follow that pattern.

- [ ] **Step 3: Add `@ExceptionHandler` for `ParrainException`**

In `GlobalExceptionHandler.java`, add a new handler method (mirror existing style):

```java
    @ExceptionHandler(AdminUserService.ParrainException.class)
    public ResponseEntity<ApiError> handleParrain(AdminUserService.ParrainException e) {
        int status = switch (e.getReason()) {
            case SELF_LINK            -> 400;
            case TARGET_NOT_PARRAIN   -> 409;
        };
        return ResponseEntity.status(status).body(
            new ApiError(e.getReason().name(), e.getMessage(), java.util.List.of()));
    }
```

Add import if missing: `import com.fuorimondo.admin.AdminUserService;`

If the file's style uses `Map<String,Object>` for `ApiError`, mirror that — do NOT invent a new constructor. Adjust the body to match the existing `ApiError` constructor.

- [ ] **Step 4: Compile**

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/admin/AdminUserController.java backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java
git commit -m "feat(admin-users): endpoints — GET /parrains search, PUT /{id}/parrain link"
```

---

## Task 6: Controller integration tests (TDD cycle for API behavior)

**Files:**
- Modify: `backend/src/test/java/com/fuorimondo/admin/AdminUserControllerTest.java`

Each sub-step: write the test, run it, confirm it passes (the backend already implements the behavior from Task 5 — we're sealing it with tests). If a test fails, fix the production code before moving on.

- [ ] **Step 1: Add helper for a parrain seed user**

At the top of the test class, after the existing `@BeforeEach seed()` method, add a helper method:

```java
    private User seedParrain(String email, String firstName, String lastName) {
        User p = new User();
        p.setEmail(email);
        p.setFirstName(firstName); p.setLastName(lastName);
        p.setCivility(Civility.NONE);
        p.setCountry("FR"); p.setCity("X");
        p.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        p.setStatus(UserStatus.ALLOCATAIRE);
        p.setRole(UserRole.USER);
        p.setLocale(Locale.FR);
        p.setIsParrain(true);
        return userRepository.save(p);
    }
```

- [ ] **Step 2: Test — search parrains filters on isParrain=true**

Add test method:

```java
    @Test
    void searchParrains_returnsOnlyParrains() throws Exception {
        seedParrain("p1@fm.com", "Alice", "Martin");
        seedParrain("p2@fm.com", "Bob", "Martin");
        // regular (non-parrain, seeded in @BeforeEach) is named "R E" and must NOT show up even though it matches the query

        mvc.perform(get("/api/admin/users/parrains?q=Martin")
                .with(user(new CustomUserDetails(admin))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].email").value(org.hamcrest.Matchers.startsWith("p")));
    }
```

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#searchParrains_returnsOnlyParrains test`
Expected: PASS.

- [ ] **Step 3: Test — PATCH toggles isParrain**

```java
    @Test
    void patch_togglesIsParrain() throws Exception {
        String body = "{\"isParrain\": true}";
        mvc.perform(patch("/api/admin/users/" + regular.getId())
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isParrain").value(true));
    }
```

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#patch_togglesIsParrain test`
Expected: PASS.

- [ ] **Step 4: Test — PUT /parrain sets the link**

```java
    @Test
    void putParrain_linksToParrain() throws Exception {
        User parrain = seedParrain("p@fm.com", "Papa", "Rain");
        String body = "{\"parrainId\": \"" + parrain.getId() + "\"}";
        mvc.perform(put("/api/admin/users/" + regular.getId() + "/parrain")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parrainId").value(parrain.getId().toString()))
            .andExpect(jsonPath("$.parrainFirstName").value("Papa"))
            .andExpect(jsonPath("$.parrainLastName").value("Rain"));
    }
```

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#putParrain_linksToParrain test`
Expected: PASS.

- [ ] **Step 5: Test — PUT /parrain with null unlinks**

```java
    @Test
    void putParrain_nullUnlinks() throws Exception {
        User parrain = seedParrain("p@fm.com", "Papa", "Rain");
        regular.setParrain(parrain);
        userRepository.save(regular);

        mvc.perform(put("/api/admin/users/" + regular.getId() + "/parrain")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parrainId\": null}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parrainId").doesNotExist());
    }
```

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#putParrain_nullUnlinks test`
Expected: PASS.

- [ ] **Step 6: Test — self-link rejected (400)**

```java
    @Test
    void putParrain_selfLinkRejected() throws Exception {
        regular.setIsParrain(true); // make self-target a parrain so we test self-link, not non-parrain
        userRepository.save(regular);
        String body = "{\"parrainId\": \"" + regular.getId() + "\"}";
        mvc.perform(put("/api/admin/users/" + regular.getId() + "/parrain")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
```

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#putParrain_selfLinkRejected test`
Expected: PASS.

- [ ] **Step 7: Test — target not-parrain rejected (409)**

```java
    @Test
    void putParrain_nonParrainTargetRejected() throws Exception {
        User other = seedParrain("other@fm.com", "Non", "Parrain");
        other.setIsParrain(false); // explicitly demote
        userRepository.save(other);
        String body = "{\"parrainId\": \"" + other.getId() + "\"}";
        mvc.perform(put("/api/admin/users/" + regular.getId() + "/parrain")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }
```

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#putParrain_nonParrainTargetRejected test`
Expected: PASS.

- [ ] **Step 8: Test — demotion preserves existing links**

```java
    @Test
    void demotingParrain_preservesFilleulsLink() throws Exception {
        User parrain = seedParrain("p@fm.com", "Papa", "Rain");
        regular.setParrain(parrain);
        userRepository.save(regular);

        // demote parrain
        String body = "{\"isParrain\": false}";
        mvc.perform(patch("/api/admin/users/" + parrain.getId())
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        // filleul still linked to the (now ex-) parrain
        User filleul = userRepository.findById(regular.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(parrain.getId(), filleul.getParrain().getId());

        // and the ex-parrain does not show up in searches anymore
        mvc.perform(get("/api/admin/users/parrains?q=Rain")
                .with(user(new CustomUserDetails(admin))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
```

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest#demotingParrain_preservesFilleulsLink test`
Expected: PASS.

- [ ] **Step 9: Run full controller test class**

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q -Dtest=AdminUserControllerTest test`
Expected: all tests PASS.

- [ ] **Step 10: Commit**

```bash
git add backend/src/test/java/com/fuorimondo/admin/AdminUserControllerTest.java
git commit -m "test(admin-users): parrain toggle, link/unlink, self-link + non-parrain guards, demotion preserves links"
```

---

## Task 7: Frontend types + FmSwitch

**Files:**
- Modify: `frontend/src/api/types.ts`
- Create: `frontend/src/components/FmSwitch.vue`

- [ ] **Step 1: Extend types.ts**

In `frontend/src/api/types.ts`, modify `AdminUserResponse` (around line 24-31) to:

```typescript
export interface AdminUserResponse extends UserResponse {
  referrerInfo: string | null;
  adminNotes: string | null;
  createdAt: string;
  invitationCode: string | null;
  invitationCodeExpiresAt: string | null;
  invitationCodeUsedAt: string | null;
  isParrain: boolean;
  parrainId: string | null;
  parrainFirstName: string | null;
  parrainLastName: string | null;
}
```

Modify `UpdateUserByAdminRequest` (around line 124-128):

```typescript
export interface UpdateUserByAdminRequest {
  status?: UserStatus;
  tierCode?: TierCode;
  adminNotes?: string;
  isParrain?: boolean;
}
```

Append at the end of the file:

```typescript
export interface ParrainOption {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface SetParrainRequest {
  parrainId: string | null;
}
```

- [ ] **Step 2: Create FmSwitch.vue**

```vue
<script setup lang="ts">
defineProps<{ modelValue: boolean; label: string; disabled?: boolean; }>();
defineEmits<{ (e: 'update:modelValue', v: boolean): void }>();
</script>

<template>
  <label class="flex items-center gap-3 mb-3 cursor-pointer" :class="{ 'opacity-50 pointer-events-none': disabled }">
    <span class="relative inline-block w-10 h-6">
      <input
        type="checkbox"
        :checked="modelValue"
        :disabled="disabled"
        @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
        class="peer sr-only"
      />
      <span class="absolute inset-0 rounded-full bg-fm-black/20 transition peer-checked:bg-fm-black"></span>
      <span class="absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white transition peer-checked:translate-x-4"></span>
    </span>
    <span class="text-sm">{{ label }}</span>
  </label>
</template>
```

- [ ] **Step 3: Type-check**

Run: `cd frontend && npm run type-check`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/types.ts frontend/src/components/FmSwitch.vue
git commit -m "feat(frontend): parrain types + FmSwitch toggle component"
```

---

## Task 8: ParrainAutocomplete component

**Files:**
- Create: `frontend/src/components/ParrainAutocomplete.vue`

- [ ] **Step 1: Create the component**

```vue
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue';
import { api } from '../api/client';
import type { ParrainOption } from '../api/types';

const props = defineProps<{
  modelValue: ParrainOption | null;
  label: string;
  placeholder?: string;
}>();
const emit = defineEmits<{ (e: 'update:modelValue', v: ParrainOption | null): void }>();

const query = ref('');
const results = ref<ParrainOption[]>([]);
const open = ref(false);
const loading = ref(false);
let debounceId: number | null = null;

watch(query, (q) => {
  if (debounceId !== null) window.clearTimeout(debounceId);
  debounceId = window.setTimeout(async () => {
    loading.value = true;
    try {
      results.value = await api.get<ParrainOption[]>(`/admin/users/parrains?q=${encodeURIComponent(q)}`);
      open.value = true;
    } finally {
      loading.value = false;
    }
  }, 250);
});

function pick(p: ParrainOption) {
  emit('update:modelValue', p);
  query.value = '';
  results.value = [];
  open.value = false;
}

function clear() {
  emit('update:modelValue', null);
}

function onBlur() {
  // delay to allow click on a result to register
  setTimeout(() => { open.value = false; }, 150);
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') open.value = false;
}

onBeforeUnmount(() => {
  if (debounceId !== null) window.clearTimeout(debounceId);
});
</script>

<template>
  <div class="mb-3">
    <label class="block text-sm mb-1">{{ label }}</label>
    <div v-if="modelValue" class="flex items-center justify-between border border-fm-black/20 rounded px-3 py-2 mb-2">
      <span class="text-sm">{{ modelValue.firstName }} {{ modelValue.lastName }} — {{ modelValue.email }}</span>
      <button type="button" class="text-xs underline text-fm-black/60 hover:text-fm-black" @click="clear" data-testid="parrain-clear">×</button>
    </div>
    <div class="relative">
      <input
        v-model="query"
        :placeholder="placeholder"
        class="w-full border border-fm-black/20 rounded px-3 py-2 text-sm"
        @focus="open = query.length > 0 || results.length > 0"
        @blur="onBlur"
        @keydown="onKey"
        data-testid="parrain-search"
      />
      <ul v-if="open && results.length > 0"
          class="absolute left-0 right-0 top-full mt-1 bg-white border border-fm-black/20 rounded shadow max-h-60 overflow-auto z-10"
          data-testid="parrain-results">
        <li v-for="p in results" :key="p.id"
            @mousedown.prevent="pick(p)"
            class="px-3 py-2 text-sm hover:bg-fm-black/5 cursor-pointer">
          {{ p.firstName }} {{ p.lastName }} <span class="text-fm-black/60">— {{ p.email }}</span>
        </li>
      </ul>
      <p v-else-if="open && !loading && query.length > 0" class="absolute left-0 right-0 top-full mt-1 bg-white border border-fm-black/20 rounded px-3 py-2 text-sm text-fm-black/60">
        —
      </p>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Type-check**

Run: `cd frontend && npm run type-check`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/ParrainAutocomplete.vue
git commit -m "feat(frontend): ParrainAutocomplete component (debounced search, selection, clear)"
```

---

## Task 9: i18n keys (FR / EN / IT)

**Files:**
- Modify: `frontend/src/i18n/fr.ts`
- Modify: `frontend/src/i18n/en.ts`
- Modify: `frontend/src/i18n/it.ts`

- [ ] **Step 1: Add keys to fr.ts**

In `frontend/src/i18n/fr.ts`, inside the `admin:` block (after `updateUser:` line ~153, before `createdAt:`), add:

```typescript
    parrainSection: 'Parrainage',
    isParrain: 'Cet utilisateur est un parrain',
    parrainLabel: 'Parrain de cet utilisateur',
    parrainSearchPlaceholder: 'Rechercher un parrain…',
    parrainNone: 'Aucun parrain',
    parrainClear: 'Délier',
```

- [ ] **Step 2: Add equivalent keys to en.ts**

Find the same `admin:` block in `frontend/src/i18n/en.ts` and add:

```typescript
    parrainSection: 'Sponsorship',
    isParrain: 'This user is a sponsor',
    parrainLabel: 'Sponsor of this user',
    parrainSearchPlaceholder: 'Search for a sponsor…',
    parrainNone: 'No sponsor',
    parrainClear: 'Unlink',
```

- [ ] **Step 3: Add equivalent keys to it.ts**

Find the `admin:` block in `frontend/src/i18n/it.ts` and add:

```typescript
    parrainSection: 'Sponsorizzazione',
    isParrain: 'Questo utente è uno sponsor',
    parrainLabel: 'Sponsor di questo utente',
    parrainSearchPlaceholder: 'Cerca uno sponsor…',
    parrainNone: 'Nessuno sponsor',
    parrainClear: 'Scollega',
```

- [ ] **Step 4: Type-check**

Run: `cd frontend && npm run type-check`
Expected: no errors. (If the i18n setup uses a typed schema that derives from one locale, the other locales must match its shape. Add keys to all three as shown above to avoid drift.)

- [ ] **Step 5: Commit**

```bash
git add frontend/src/i18n/fr.ts frontend/src/i18n/en.ts frontend/src/i18n/it.ts
git commit -m "i18n: parrain keys (FR/EN/IT)"
```

---

## Task 10: Wire up AdminUserDetailView

**Files:**
- Modify: `frontend/src/views/admin/AdminUserDetailView.vue`

- [ ] **Step 1: Add imports and form state**

In the `<script setup>` block of `AdminUserDetailView.vue`:

- Add imports near the existing component imports (line 7-10 area):

```typescript
import FmSwitch from '../../components/FmSwitch.vue';
import ParrainAutocomplete from '../../components/ParrainAutocomplete.vue';
import type { ParrainOption } from '../../api/types';
```

- Extend the form ref init (line 19). Replace:

```typescript
const form = ref<UpdateUserByAdminRequest>({ status: undefined, tierCode: undefined, adminNotes: '' });
```

with:

```typescript
const form = ref<UpdateUserByAdminRequest>({ status: undefined, tierCode: undefined, adminNotes: '', isParrain: false });
const parrainSelected = ref<ParrainOption | null>(null);
```

- Extend the `load()` function. Replace:

```typescript
async function load() {
  const u = await api.get<AdminUserResponse>(`/admin/users/${route.params.id}`);
  user.value = u;
  form.value = { status: u.status, tierCode: u.tierCode ?? undefined, adminNotes: u.adminNotes ?? '' };
}
```

with:

```typescript
async function load() {
  const u = await api.get<AdminUserResponse>(`/admin/users/${route.params.id}`);
  user.value = u;
  form.value = { status: u.status, tierCode: u.tierCode ?? undefined, adminNotes: u.adminNotes ?? '', isParrain: u.isParrain };
  parrainSelected.value = u.parrainId
    ? { id: u.parrainId, firstName: u.parrainFirstName ?? '', lastName: u.parrainLastName ?? '', email: '' }
    : null;
}
```

- Add a `saveParrain` function after `save()`:

```typescript
async function saveParrain(next: ParrainOption | null) {
  parrainSelected.value = next;
  busy.value = true;
  try {
    user.value = await api.put<AdminUserResponse>(`/admin/users/${route.params.id}/parrain`, { parrainId: next ? next.id : null });
  } catch (e: any) {
    // revert local state on failure
    parrainSelected.value = user.value?.parrainId
      ? { id: user.value.parrainId, firstName: user.value.parrainFirstName ?? '', lastName: user.value.parrainLastName ?? '', email: '' }
      : null;
    alert(e?.payload?.message ?? 'Erreur');
  } finally { busy.value = false; }
}
```

- [ ] **Step 2: Add the Parrainage section in the template**

In the `<template>`, just before the closing `</form>` tag (after the `<div class="pt-3 flex gap-3">...</div>` with the submit buttons), add a new section **after** the `</form>`:

```vue
    <section class="fm-card mt-6 space-y-3" data-testid="parrain-section">
      <h3 class="text-lg">{{ t('admin.parrainSection') }}</h3>
      <FmSwitch v-model="form.isParrain as unknown as boolean"
                :label="t('admin.isParrain')"
                @update:modelValue="(v: boolean) => { form.isParrain = v; save(); }" />

      <ParrainAutocomplete
        v-if="!form.isParrain"
        :modelValue="parrainSelected"
        :label="t('admin.parrainLabel')"
        :placeholder="t('admin.parrainSearchPlaceholder')"
        @update:modelValue="saveParrain" />
    </section>
```

Note: the switch auto-saves on toggle (calls `save()` which does PATCH). The autocomplete auto-saves on selection or clear via `saveParrain`.

- [ ] **Step 3: Type-check and dev build**

Run: `cd frontend && npm run type-check`
Expected: no errors.

Run: `cd frontend && npm run build`
Expected: build succeeds (production build catches some issues type-check misses).

- [ ] **Step 4: Manual smoke test in dev**

Start backend: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (background).

Start frontend: `cd frontend && npm run dev` (background).

Login as admin (see `backend/README.md:22` for creds), go to `/admin/users/<some-user-id>`:
- [ ] Parrainage section is visible.
- [ ] Flip the switch → reloads with `isParrain = true` → autocomplete disappears.
- [ ] Flip back → autocomplete reappears.
- [ ] Promote another user first; then in the autocomplete type 1-2 letters → see the promoted user in the dropdown.
- [ ] Select → the chosen parrain appears above the search field with an × button.
- [ ] Click × → parrain unlinked, search field is empty again.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/admin/AdminUserDetailView.vue
git commit -m "feat(admin-ui): parrainage section — switch + autocomplete in user detail view"
```

---

## Task 11: Full test suite + PR prep

- [ ] **Step 1: Run full backend test suite**

Run: `cd backend && JAVA_HOME=/c/opt/jdk-26 ./mvnw -q test`
Expected: all tests PASS except the pre-existing `DevSimulateWebhookTest.simulate_paid_transitions_status` (204 vs 200 mismatch, documented as unrelated).

- [ ] **Step 2: Frontend build**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Final review of the branch**

Run: `git log --oneline main..HEAD`
Expected: 11 tidy commits matching the task titles above.

Run: `git diff --stat main..HEAD`
Check: no unexpected files changed, no leftover debug code.

- [ ] **Step 4: Handoff**

Feature is ready for user acceptance. No PR created automatically — await user instruction per project convention.

---

## Self-Review Notes

**Spec coverage** — every section of the spec maps to a task:
- Data model → Task 1 (migration) + Task 2 (entity)
- Business rules (demotion preserves, self-link 400, non-parrain 409) → Tasks 4 + 5 (logic) + Task 6 (tests)
- GET /parrains → Tasks 2 (repo) + 4 (service) + 5 (controller) + 6 (test step 2)
- PATCH with isParrain → Tasks 3 (DTO) + 4 (service update) + 5 (controller) + 6 (test step 3)
- PUT /{id}/parrain → Tasks 3 (DTO) + 4 (service) + 5 (controller) + 6 (tests steps 4-7)
- Extended AdminUserResponse → Task 3
- FmSwitch → Task 7
- ParrainAutocomplete → Task 8
- AdminUserDetailView integration → Task 10
- i18n (FR/EN/IT) → Task 9
- Manual smoke test → Task 10 step 4

**Type consistency** — method names verified:
- `User.setIsParrain(boolean)` and `User.setParrain(User)` (no overload — distinct names)
- `User.isParrain()` / `User.getParrain()` — getter pair matches Hibernate's expectation for `boolean` vs reference types
- `AdminUserService.setParrain(UUID, UUID)` — same name across service
- `searchParrains(String q)` consistent in repo + service + controller
- DTO field names `isParrain`, `parrainId`, `parrainFirstName`, `parrainLastName` consistent backend/frontend

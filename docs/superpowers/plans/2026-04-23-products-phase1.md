# Products — Phase 1 (Admin CRUD) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `Product` domain with full admin CRUD (list/create/edit/delete + photo upload/download/delete) in the FuoriMondo backend and an admin frontend UI (list / create / edit views) — no public/allocataire endpoint yet (Phase 2).

**Architecture:** Spring Boot JPA entity `Product` + join table `product_tiers`, REST controller at `/api/admin/products`, photo stored on local disk under configurable `fuorimondo.uploads.dir`. Frontend: three new Vue 3 views under `views/admin/products/`, matching existing user-admin patterns. FR-only i18n.

**Tech Stack:** Spring Boot 3 (Java 17, JPA/Hibernate, Flyway), Vue 3 Composition API, Pinia, Tailwind, Vite. Reference spec: `docs/superpowers/specs/2026-04-23-products-phase1-design.md`.

---

## File Structure

**Backend (new files unless noted):**
- `backend/src/main/resources/db/migration/V3__products.sql` — create
- `backend/src/main/java/com/fuorimondo/products/Product.java` — entity
- `backend/src/main/java/com/fuorimondo/products/ProductRepository.java` — repo
- `backend/src/main/java/com/fuorimondo/products/ProductService.java` — service (CRUD + photo)
- `backend/src/main/java/com/fuorimondo/products/AdminProductController.java` — controller
- `backend/src/main/java/com/fuorimondo/products/dto/ProductRequest.java` — DTO
- `backend/src/main/java/com/fuorimondo/products/dto/ProductResponse.java` — DTO
- `backend/src/main/resources/application.yml` — modify (add `fuorimondo.uploads.dir`)
- `backend/src/main/resources/application-dev.yml` — modify (dev uploads dir)
- `backend/src/test/java/com/fuorimondo/products/ProductAdminIntegrationTest.java` — create

**Frontend (new files unless noted):**
- `frontend/src/api/types.ts` — modify (add `ProductRequest`, `ProductResponse`, `TierCode`)
- `frontend/src/api/client.ts` — modify (add `uploadMultipart`)
- `frontend/src/i18n/fr.ts` — modify (add `admin.products` block)
- `frontend/src/i18n/it.ts` — modify (add same FR strings as fallback)
- `frontend/src/i18n/en.ts` — modify (add same FR strings as fallback)
- `frontend/src/router.ts` — modify (add three product routes)
- `frontend/src/components/AppLayout.vue` — modify (add "Produits" link)
- `frontend/src/views/admin/AdminProductsView.vue` — create
- `frontend/src/views/admin/AdminCreateProductView.vue` — create
- `frontend/src/views/admin/AdminProductDetailView.vue` — create

---

## Task 1: Database migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__products.sql`

- [ ] **Step 1: Write the migration SQL**

Create `backend/src/main/resources/db/migration/V3__products.sql`:

```sql
CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    price_eur NUMERIC(10,2) NOT NULL CHECK (price_eur >= 0),
    photo_filename VARCHAR(255),
    delivery BOOLEAN NOT NULL,
    weight_kg NUMERIC(6,3),
    sale_start_at TIMESTAMP NOT NULL,
    sale_end_at TIMESTAMP,
    stock INTEGER CHECK (stock IS NULL OR stock >= 0),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_products_sale_start_at ON products(sale_start_at);

CREATE TABLE product_tiers (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tier_code VARCHAR(16) NOT NULL CHECK (tier_code IN ('TIER_1', 'TIER_2', 'TIER_3')),
    PRIMARY KEY (product_id, tier_code)
);
```

- [ ] **Step 2: Start the backend to verify Flyway applies it**

Run from `backend/`:

```
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Expected: startup logs include `Migrating schema ... to version "3 - products"` and `Successfully applied 1 migration`. No stack trace. Ctrl-C to stop.

- [ ] **Step 3: Commit**

```
git add backend/src/main/resources/db/migration/V3__products.sql
git commit -m "feat(products): V3 migration — products + product_tiers"
```

---

## Task 2: Product entity

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/products/Product.java`

- [ ] **Step 1: Write the entity**

Create `backend/src/main/java/com/fuorimondo/products/Product.java`:

```java
package com.fuorimondo.products;

import com.fuorimondo.common.BaseEntity;
import com.fuorimondo.users.TierCode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 4000)
    private String description;

    @Column(name = "price_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceEur;

    @Column(name = "photo_filename", length = 255)
    private String photoFilename;

    @Column(nullable = false)
    private boolean delivery;

    @Column(name = "weight_kg", precision = 6, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "sale_start_at", nullable = false)
    private Instant saleStartAt;

    @Column(name = "sale_end_at")
    private Instant saleEndAt;

    @Column
    private Integer stock;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = TierCode.class)
    @CollectionTable(name = "product_tiers", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tier_code", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private Set<TierCode> tiers = EnumSet.noneOf(TierCode.class);

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPriceEur() { return priceEur; }
    public void setPriceEur(BigDecimal priceEur) { this.priceEur = priceEur; }
    public String getPhotoFilename() { return photoFilename; }
    public void setPhotoFilename(String photoFilename) { this.photoFilename = photoFilename; }
    public boolean isDelivery() { return delivery; }
    public void setDelivery(boolean delivery) { this.delivery = delivery; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public Instant getSaleStartAt() { return saleStartAt; }
    public void setSaleStartAt(Instant saleStartAt) { this.saleStartAt = saleStartAt; }
    public Instant getSaleEndAt() { return saleEndAt; }
    public void setSaleEndAt(Instant saleEndAt) { this.saleEndAt = saleEndAt; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Set<TierCode> getTiers() { return tiers; }
    public void setTiers(Set<TierCode> tiers) { this.tiers = tiers; }
}
```

- [ ] **Step 2: Compile to verify**

```
cd backend && ./mvnw.cmd compile
```

Expected: `BUILD SUCCESS`.

---

## Task 3: Repository

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/products/ProductRepository.java`

- [ ] **Step 1: Write the repository**

Create `backend/src/main/java/com/fuorimondo/products/ProductRepository.java`:

```java
package com.fuorimondo.products;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}
```

- [ ] **Step 2: Commit entity + repo**

```
git add backend/src/main/java/com/fuorimondo/products/
git commit -m "feat(products): Product entity and repository"
```

---

## Task 4: DTOs

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/products/dto/ProductRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/products/dto/ProductResponse.java`

- [ ] **Step 1: Write `ProductRequest` (validation DTO used for POST and PATCH)**

Create `backend/src/main/java/com/fuorimondo/products/dto/ProductRequest.java`:

```java
package com.fuorimondo.products.dto;

import com.fuorimondo.users.TierCode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal priceEur,
        @NotNull Boolean delivery,
        @DecimalMin("0.000") BigDecimal weightKg,
        @NotEmpty Set<TierCode> tiers,
        @NotNull Instant saleStartAt,
        Instant saleEndAt,
        @PositiveOrZero Integer stock
) {}
```

- [ ] **Step 2: Write `ProductResponse`**

Create `backend/src/main/java/com/fuorimondo/products/dto/ProductResponse.java`:

```java
package com.fuorimondo.products.dto;

import com.fuorimondo.products.Product;
import com.fuorimondo.users.TierCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal priceEur,
        String photoFilename,
        boolean delivery,
        BigDecimal weightKg,
        List<TierCode> tiers,
        Instant saleStartAt,
        Instant saleEndAt,
        Integer stock,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPriceEur(),
                p.getPhotoFilename(), p.isDelivery(), p.getWeightKg(),
                p.getTiers().stream().sorted().toList(),
                p.getSaleStartAt(), p.getSaleEndAt(), p.getStock(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 3: Compile**

```
cd backend && ./mvnw.cmd compile
```

Expected: `BUILD SUCCESS`.

---

## Task 5: Service — CRUD without photo

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/products/ProductService.java`

- [ ] **Step 1: Write the service (CRUD only — photo methods added in later tasks)**

Create `backend/src/main/java/com/fuorimondo/products/ProductService.java`:

```java
package com.fuorimondo.products;

import com.fuorimondo.products.dto.ProductRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Product> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    @Transactional
    public Product create(ProductRequest req) {
        validateSaleWindow(req);
        Product p = new Product();
        apply(p, req);
        return repository.save(p);
    }

    @Transactional
    public Product update(UUID id, ProductRequest req) {
        validateSaleWindow(req);
        Product p = getById(id);
        apply(p, req);
        return repository.save(p);
    }

    @Transactional
    public void delete(UUID id) {
        // photo file cleanup will be wired in Task 8 when PhotoStorage is introduced
        repository.deleteById(id);
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name());
        p.setDescription(req.description());
        p.setPriceEur(req.priceEur());
        p.setDelivery(Boolean.TRUE.equals(req.delivery()));
        p.setWeightKg(req.weightKg());
        p.setSaleStartAt(req.saleStartAt());
        p.setSaleEndAt(req.saleEndAt());
        p.setStock(req.stock());
        p.setTiers(EnumSet.copyOf(req.tiers()));
    }

    private void validateSaleWindow(ProductRequest req) {
        if (req.saleEndAt() != null && req.saleEndAt().isBefore(req.saleStartAt())) {
            throw new IllegalArgumentException("saleEndAt must be >= saleStartAt");
        }
    }
}
```

- [ ] **Step 2: Compile**

```
cd backend && ./mvnw.cmd compile
```

Expected: `BUILD SUCCESS`.

---

## Task 6: Controller — CRUD without photo

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/products/AdminProductController.java`

- [ ] **Step 1: Write the controller**

Create `backend/src/main/java/com/fuorimondo/products/AdminProductController.java`:

```java
package com.fuorimondo.products;

import com.fuorimondo.products.dto.ProductRequest;
import com.fuorimondo.products.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return service.list().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        Product created = service.create(req);
        return ResponseEntity.status(201).body(ProductResponse.from(created));
    }

    @PatchMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Compile**

```
cd backend && ./mvnw.cmd compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit CRUD slice**

```
git add backend/src/main/java/com/fuorimondo/products/
git commit -m "feat(products): DTOs, service, admin controller (no photo)"
```

---

## Task 7: Integration test for CRUD happy path

**Files:**
- Create: `backend/src/test/java/com/fuorimondo/products/ProductAdminIntegrationTest.java`

- [ ] **Step 1: Find an existing integration test to mirror**

Run from `backend/`:

```
./mvnw.cmd test -Dtest=NONE -q 2>&1 | head -5
```

Then:

```
find src/test -name "*.java" | head -5
```

Read one existing admin integration test to mirror its auth/MockMvc setup. Replicate its class-level annotations and its pattern for obtaining an admin-authenticated MockMvc call.

- [ ] **Step 2: Write a happy-path test**

Create `backend/src/test/java/com/fuorimondo/products/ProductAdminIntegrationTest.java` mirroring the existing integration-test style. At minimum cover:

```java
// Create product (POST /api/admin/products) as ADMIN
//   - expect 201
//   - expect body.id present
//   - expect body.tiers contains TIER_1
// List (GET) — expect size 1
// Get by id — expect name matches
// Update (PATCH) with a new name — expect 200 + name updated
// Delete (DELETE) — expect 204
// Get again — expect 404
```

Build the JSON body inline as a text block; use `ObjectMapper` from the context if the existing style does. Do not guess the auth helper — copy from the reference test.

- [ ] **Step 3: Run the test**

```
cd backend && ./mvnw.cmd test -Dtest=ProductAdminIntegrationTest
```

Expected: `BUILD SUCCESS`, `Tests run: N, Failures: 0`.

- [ ] **Step 4: Commit**

```
git add backend/src/test/java/com/fuorimondo/products/
git commit -m "test(products): integration test for admin CRUD happy path"
```

---

## Task 8: Photo storage — config + PhotoStorage component

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/java/com/fuorimondo/products/PhotoStorage.java`

- [ ] **Step 1: Add config property**

Append to `backend/src/main/resources/application.yml` (under the existing `fuorimondo:` key, or create it if absent):

```yaml
fuorimondo:
  uploads:
    dir: ${FUORIMONDO_UPLOADS_DIR:./uploads}
```

(Check first whether `fuorimondo:` already exists — if yes, add the `uploads:` block under it, do not duplicate the top-level key.)

In `application-dev.yml`, override if needed, e.g.:

```yaml
fuorimondo:
  uploads:
    dir: ./uploads
```

- [ ] **Step 2: Write `PhotoStorage`**

Create `backend/src/main/java/com/fuorimondo/products/PhotoStorage.java`:

```java
package com.fuorimondo.products;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Component
public class PhotoStorage {

    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final Path rootDir;

    public PhotoStorage(@Value("${fuorimondo.uploads.dir:./uploads}") String uploadsDir) {
        this.rootDir = Paths.get(uploadsDir, "products").toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(rootDir);
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Empty file");
        if (file.getSize() > MAX_SIZE_BYTES) throw new IllegalArgumentException("File too large");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType.toLowerCase()))
            throw new IllegalArgumentException("Unsupported media type: " + contentType);

        String ext = switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalStateException();
        };
        String filename = UUID.randomUUID() + ext;
        Path target = rootDir.resolve(filename);
        Path temp = Files.createTempFile(rootDir, "upload-", ".tmp");
        try {
            file.transferTo(temp.toFile());
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        return filename;
    }

    public Resource load(String filename) {
        Path path = rootDir.resolve(filename).normalize();
        if (!path.startsWith(rootDir) || !Files.exists(path))
            throw new IllegalArgumentException("Photo not found: " + filename);
        return new FileSystemResource(path);
    }

    public String contentTypeFor(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    public void delete(String filename) {
        if (filename == null) return;
        Path path = rootDir.resolve(filename).normalize();
        if (!path.startsWith(rootDir)) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }
}
```

- [ ] **Step 3: Compile**

```
cd backend && ./mvnw.cmd compile
```

Expected: `BUILD SUCCESS`.

---

## Task 9: Wire photo into service + controller

**Files:**
- Modify: `backend/src/main/java/com/fuorimondo/products/ProductService.java`
- Modify: `backend/src/main/java/com/fuorimondo/products/AdminProductController.java`

- [ ] **Step 1: Inject `PhotoStorage` in `ProductService` and add photo methods**

Edit `ProductService.java`. Add field + constructor param + three methods:

```java
// field
private final PhotoStorage storage;

// constructor becomes
public ProductService(ProductRepository repository, PhotoStorage storage) {
    this.repository = repository;
    this.storage = storage;
}

// delete: update to remove photo file first
@Transactional
public void delete(UUID id) {
    Product p = getById(id);
    if (p.getPhotoFilename() != null) storage.delete(p.getPhotoFilename());
    repository.delete(p);
}

// new
@Transactional
public Product setPhoto(UUID id, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
    Product p = getById(id);
    String oldName = p.getPhotoFilename();
    String newName = storage.store(file);
    p.setPhotoFilename(newName);
    Product saved = repository.save(p);
    if (oldName != null) storage.delete(oldName);
    return saved;
}

@Transactional
public Product clearPhoto(UUID id) {
    Product p = getById(id);
    if (p.getPhotoFilename() != null) {
        storage.delete(p.getPhotoFilename());
        p.setPhotoFilename(null);
        return repository.save(p);
    }
    return p;
}

public org.springframework.core.io.Resource loadPhoto(UUID id) {
    Product p = getById(id);
    if (p.getPhotoFilename() == null) throw new jakarta.persistence.EntityNotFoundException("No photo");
    return storage.load(p.getPhotoFilename());
}

public String photoContentType(UUID id) {
    Product p = getById(id);
    return storage.contentTypeFor(p.getPhotoFilename());
}
```

- [ ] **Step 2: Add photo endpoints to `AdminProductController`**

Add to `AdminProductController.java`:

```java
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@PostMapping("/{id}/photo")
public ProductResponse uploadPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) throws IOException {
    return ProductResponse.from(service.setPhoto(id, file));
}

@DeleteMapping("/{id}/photo")
public ProductResponse deletePhoto(@PathVariable UUID id) {
    return ProductResponse.from(service.clearPhoto(id));
}

@GetMapping("/{id}/photo")
public ResponseEntity<Resource> getPhoto(@PathVariable UUID id) {
    Resource r = service.loadPhoto(id);
    String ct = service.photoContentType(id);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(ct)).body(r);
}
```

- [ ] **Step 3: Compile + restart backend + smoke-test manually**

```
cd backend && ./mvnw.cmd compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit photo layer**

```
git add backend/src/main/java/com/fuorimondo/products/ backend/src/main/resources/application.yml backend/src/main/resources/application-dev.yml
git commit -m "feat(products): photo upload/download/delete via PhotoStorage"
```

---

## Task 10: Frontend — types + upload helper

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/client.ts`

- [ ] **Step 1: Add `TierCode` + `ProductRequest` + `ProductResponse`**

Open `frontend/src/api/types.ts`. At the end of the file add:

```ts
export type TierCode = 'TIER_1' | 'TIER_2' | 'TIER_3';

export interface ProductRequest {
  name: string;
  description: string | null;
  priceEur: string;           // decimal as string to preserve precision
  delivery: boolean;
  weightKg: string | null;    // decimal as string
  tiers: TierCode[];
  saleStartAt: string;        // ISO instant
  saleEndAt: string | null;
  stock: number | null;
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string | null;
  priceEur: string;
  photoFilename: string | null;
  delivery: boolean;
  weightKg: string | null;
  tiers: TierCode[];
  saleStartAt: string;
  saleEndAt: string | null;
  stock: number | null;
  createdAt: string;
  updatedAt: string;
}
```

(If `TierCode` already exists in the file, skip its redeclaration — grep first.)

- [ ] **Step 2: Add `uploadMultipart` in `client.ts`**

Open `frontend/src/api/client.ts`. Below the existing `export const api = { ... }` block, append:

```ts
export async function uploadMultipart<T>(path: string, file: File, field = 'file'): Promise<T> {
  await primeCsrfCookie();
  const form = new FormData();
  form.append(field, file);
  const headers: Record<string, string> = {};
  const csrf = readCookie('XSRF-TOKEN');
  if (csrf) headers['X-XSRF-TOKEN'] = csrf;

  inFlight.value++;
  try {
    const res = await fetch(BASE + path, {
      method: 'POST',
      credentials: 'include',
      headers,
      body: form,
    });
    if (!res.ok) {
      let payload: ApiError | null = null;
      try { payload = await res.json(); } catch { /* noop */ }
      throw new ApiException(res.status, payload, payload?.message || res.statusText);
    }
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
  } finally {
    inFlight.value--;
  }
}
```

(Note: `ApiError` is imported via the existing `import type { ApiError } from './types';` at top — verify.)

- [ ] **Step 3: Typecheck**

```
cd frontend && npm run build
```

Expected: build succeeds (types compile).

---

## Task 11: i18n — add products block

**Files:**
- Modify: `frontend/src/i18n/fr.ts`
- Modify: `frontend/src/i18n/it.ts`
- Modify: `frontend/src/i18n/en.ts`

- [ ] **Step 1: Add `admin.products` block in `fr.ts`**

Inside the existing `admin: { ... }` object in `fr.ts`, add a nested `products` key (before the closing `}` of `admin:`):

```ts
    products: {
      title: 'Produits',
      list: 'Liste des produits',
      create: 'Créer un produit',
      edit: 'Modifier le produit',
      empty: 'Aucun produit',
      name: 'Nom',
      description: 'Description',
      price: 'Prix (EUR)',
      photo: 'Photo',
      noPhoto: 'Pas de photo',
      uploadPhoto: 'Téléverser une photo',
      removePhoto: 'Supprimer la photo',
      delivery: 'Livraison',
      weight: 'Poids (kg)',
      tiers: 'Cercles concernés',
      saleStart: 'Début de vente',
      saleEnd: 'Fin de vente (optionnel)',
      stock: 'Stock',
      stockUnlimited: '∞',
      save: 'Enregistrer',
      deleteProduct: 'Supprimer le produit',
      confirmDelete: 'Supprimer ce produit ?',
    },
```

Also in the top-level `nav: {}` block, add:

```ts
    adminProducts: 'Produits',
```

- [ ] **Step 2: Mirror the same FR strings into `it.ts` and `en.ts`**

Paste the same two blocks (with identical French content — per user convention for this project, translations come later). Make sure both `nav.adminProducts` and `admin.products.*` exist in all three files.

- [ ] **Step 3: Typecheck**

```
cd frontend && npm run build
```

Expected: build succeeds.

---

## Task 12: Router — add product routes

**Files:**
- Modify: `frontend/src/router.ts`

- [ ] **Step 1: Add three routes**

Open `frontend/src/router.ts`. Next to the existing `/admin/users*` routes, add:

```ts
  { path: '/admin/products', name: 'admin-products', component: () => import('./views/admin/AdminProductsView.vue'), meta: { admin: true } },
  { path: '/admin/products/create', name: 'admin-products-create', component: () => import('./views/admin/AdminCreateProductView.vue'), meta: { admin: true } },
  { path: '/admin/products/:id', name: 'admin-product-detail', component: () => import('./views/admin/AdminProductDetailView.vue'), meta: { admin: true } },
```

- [ ] **Step 2: Verify `beforeEach` admin guard already covers `meta: { admin: true }`**

Open `router.ts`, confirm there is a guard like:

```ts
router.beforeEach(async (to) => {
  if (to.meta.admin && !auth.isAdmin) return { name: 'profile' };
});
```

No change needed if present. If absent, stop and report — other admin routes won't work either.

---

## Task 13: Nav — add "Produits" link

**Files:**
- Modify: `frontend/src/components/AppLayout.vue`

- [ ] **Step 1: Add link under "Utilisateurs" in the admin nav section**

In `AppLayout.vue`, find the block:

```vue
<div v-if="auth.isAdmin" class="pt-3 mt-3 border-t border-fm-black/10">
  <div class="text-xs uppercase tracking-widest text-fm-black/50 py-2">{{ t('nav.admin') }}</div>
  <router-link to="/admin/users" class="block py-2 text-base">{{ t('nav.adminUsers') }}</router-link>
</div>
```

Add a second `<router-link>` right after the existing one:

```vue
<router-link to="/admin/products" class="block py-2 text-base">{{ t('nav.adminProducts') }}</router-link>
```

---

## Task 14: `AdminProductsView` — list

**Files:**
- Create: `frontend/src/views/admin/AdminProductsView.vue`

- [ ] **Step 1: Write the view**

Create `frontend/src/views/admin/AdminProductsView.vue`:

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { api } from '../../api/client';
import type { ProductResponse } from '../../api/types';
import FmButton from '../../components/FmButton.vue';

const { t } = useI18n();
const router = useRouter();

const products = ref<ProductResponse[]>([]);
const loading = ref(true);

async function load() {
  loading.value = true;
  products.value = await api.get<ProductResponse[]>('/admin/products');
  loading.value = false;
}

function fmtPrice(v: string) { return `${Number(v).toFixed(2)} €`; }
function fmtDate(v: string | null) { return v ? new Date(v).toLocaleString('fr-FR') : '—'; }
function fmtStock(v: number | null) { return v == null ? '∞' : String(v); }

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl">{{ t('admin.products.list') }}</h2>
      <FmButton variant="primary" @click="router.push({ name: 'admin-products-create' })" data-testid="admin-product-create-btn">
        + {{ t('admin.products.create') }}
      </FmButton>
    </div>

    <p v-if="loading" class="text-sm">{{ t('common.loading') }}</p>
    <p v-else-if="products.length === 0" class="text-sm text-fm-black/60">{{ t('admin.products.empty') }}</p>
    <ul v-else class="divide-y divide-fm-black/10">
      <li v-for="p in products" :key="p.id" class="py-3">
        <button class="w-full text-left flex justify-between items-start gap-3"
                @click="router.push({ name: 'admin-product-detail', params: { id: p.id } })">
          <div>
            <div class="font-medium">{{ p.name }}</div>
            <div class="text-xs text-fm-black/60">{{ fmtPrice(p.priceEur) }} · {{ p.tiers.join(', ') }}</div>
            <div class="text-xs text-fm-black/60">{{ t('admin.products.saleStart') }} : {{ fmtDate(p.saleStartAt) }} · {{ t('admin.products.stock') }} : {{ fmtStock(p.stock) }}</div>
          </div>
          <span class="text-xs text-fm-black/40">→</span>
        </button>
      </li>
    </ul>
  </div>
</template>
```

---

## Task 15: `AdminCreateProductView` — create form

**Files:**
- Create: `frontend/src/views/admin/AdminCreateProductView.vue`

- [ ] **Step 1: Write the view**

Create `frontend/src/views/admin/AdminCreateProductView.vue`:

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { api } from '../../api/client';
import { ApiException } from '../../api/client';
import type { ProductRequest, ProductResponse, TierCode } from '../../api/types';
import FmInput from '../../components/FmInput.vue';
import FmButton from '../../components/FmButton.vue';

const { t } = useI18n();
const router = useRouter();

const form = reactive<{
  name: string; description: string;
  priceEur: string; delivery: boolean;
  weightKg: string; tiers: Set<TierCode>;
  saleStartAt: string; saleEndAt: string;
  stock: string;
}>({
  name: '', description: '',
  priceEur: '0.00', delivery: false,
  weightKg: '', tiers: new Set<TierCode>(),
  saleStartAt: '', saleEndAt: '',
  stock: '',
});

const error = ref<string | null>(null);
const busy = ref(false);

function toggleTier(t: TierCode) {
  if (form.tiers.has(t)) form.tiers.delete(t);
  else form.tiers.add(t);
}

async function submit() {
  error.value = null;
  if (form.tiers.size === 0) { error.value = t('admin.products.tiers') + ' ?'; return; }
  if (!form.saleStartAt) { error.value = t('admin.products.saleStart') + ' ?'; return; }
  busy.value = true;
  try {
    const body: ProductRequest = {
      name: form.name,
      description: form.description || null,
      priceEur: form.priceEur,
      delivery: form.delivery,
      weightKg: form.weightKg || null,
      tiers: Array.from(form.tiers),
      saleStartAt: new Date(form.saleStartAt).toISOString(),
      saleEndAt: form.saleEndAt ? new Date(form.saleEndAt).toISOString() : null,
      stock: form.stock === '' ? null : Number(form.stock),
    };
    const created = await api.post<ProductResponse>('/admin/products', body);
    router.push({ name: 'admin-product-detail', params: { id: created.id } });
  } catch (e) {
    error.value = e instanceof ApiException ? (e.payload?.message || t('common.error')) : t('common.error');
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div class="fm-page max-w-lg">
    <h2 class="text-2xl mb-6">{{ t('admin.products.create') }}</h2>

    <form @submit.prevent="submit" class="space-y-4">
      <FmInput v-model="form.name" :label="t('admin.products.name')" required maxlength="200" />
      <label class="block text-sm">
        {{ t('admin.products.description') }}
        <textarea v-model="form.description" rows="4" maxlength="4000"
                  class="mt-1 w-full border border-fm-black/20 rounded p-2" />
      </label>
      <FmInput v-model="form.priceEur" :label="t('admin.products.price')" type="text" required />

      <label class="flex items-center gap-2">
        <input type="checkbox" v-model="form.delivery" />
        {{ t('admin.products.delivery') }}
      </label>

      <FmInput v-model="form.weightKg" :label="t('admin.products.weight')" type="text" />

      <fieldset>
        <legend class="text-sm mb-2">{{ t('admin.products.tiers') }}</legend>
        <label v-for="tc in ['TIER_1','TIER_2','TIER_3'] as const" :key="tc" class="mr-4 inline-flex items-center gap-1">
          <input type="checkbox" :checked="form.tiers.has(tc)" @change="toggleTier(tc)" />
          {{ t(`tiers.${tc}`) }}
        </label>
      </fieldset>

      <FmInput v-model="form.saleStartAt" :label="t('admin.products.saleStart')" type="datetime-local" required />
      <FmInput v-model="form.saleEndAt" :label="t('admin.products.saleEnd')" type="datetime-local" />
      <FmInput v-model="form.stock" :label="t('admin.products.stock')" type="number" />

      <p v-if="error" class="text-sm text-fm-red">{{ error }}</p>
      <FmButton type="submit" variant="primary" block :disabled="busy">
        {{ t('admin.products.save') }}
      </FmButton>
    </form>
  </div>
</template>
```

---

## Task 16: `AdminProductDetailView` — edit/delete + photo

**Files:**
- Create: `frontend/src/views/admin/AdminProductDetailView.vue`

- [ ] **Step 1: Write the view**

Create `frontend/src/views/admin/AdminProductDetailView.vue`:

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { api, uploadMultipart, ApiException } from '../../api/client';
import type { ProductRequest, ProductResponse, TierCode } from '../../api/types';
import FmInput from '../../components/FmInput.vue';
import FmButton from '../../components/FmButton.vue';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const product = ref<ProductResponse | null>(null);
const busy = ref(false);
const error = ref<string | null>(null);

const form = ref({
  name: '', description: '',
  priceEur: '0.00', delivery: false,
  weightKg: '', tiers: new Set<TierCode>(),
  saleStartAt: '', saleEndAt: '',
  stock: '',
});

function toLocalDT(iso: string | null) {
  if (!iso) return '';
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function hydrate(p: ProductResponse) {
  product.value = p;
  form.value = {
    name: p.name, description: p.description || '',
    priceEur: p.priceEur, delivery: p.delivery,
    weightKg: p.weightKg || '', tiers: new Set(p.tiers),
    saleStartAt: toLocalDT(p.saleStartAt),
    saleEndAt: toLocalDT(p.saleEndAt),
    stock: p.stock == null ? '' : String(p.stock),
  };
}

async function load() {
  const p = await api.get<ProductResponse>(`/admin/products/${route.params.id}`);
  hydrate(p);
}

function toggleTier(tc: TierCode) {
  if (form.value.tiers.has(tc)) form.value.tiers.delete(tc);
  else form.value.tiers.add(tc);
}

async function save() {
  error.value = null;
  if (form.value.tiers.size === 0) { error.value = t('admin.products.tiers') + ' ?'; return; }
  busy.value = true;
  try {
    const body: ProductRequest = {
      name: form.value.name,
      description: form.value.description || null,
      priceEur: form.value.priceEur,
      delivery: form.value.delivery,
      weightKg: form.value.weightKg || null,
      tiers: Array.from(form.value.tiers),
      saleStartAt: new Date(form.value.saleStartAt).toISOString(),
      saleEndAt: form.value.saleEndAt ? new Date(form.value.saleEndAt).toISOString() : null,
      stock: form.value.stock === '' ? null : Number(form.value.stock),
    };
    const updated = await api.patch<ProductResponse>(`/admin/products/${route.params.id}`, body);
    hydrate(updated);
  } catch (e) {
    error.value = e instanceof ApiException ? (e.payload?.message || t('common.error')) : t('common.error');
  } finally {
    busy.value = false;
  }
}

async function remove() {
  if (!window.confirm(t('admin.products.confirmDelete'))) return;
  await api.delete(`/admin/products/${route.params.id}`);
  router.push({ name: 'admin-products' });
}

async function onPhotoChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const updated = await uploadMultipart<ProductResponse>(`/admin/products/${route.params.id}/photo`, file);
  hydrate(updated);
}

async function removePhoto() {
  const updated = await api.delete<ProductResponse>(`/admin/products/${route.params.id}/photo`);
  hydrate(updated);
}

onMounted(load);
</script>

<template>
  <div v-if="product" class="fm-page max-w-lg">
    <h2 class="text-2xl mb-6">{{ t('admin.products.edit') }}</h2>

    <section class="mb-8">
      <h3 class="text-sm uppercase tracking-widest text-fm-black/60 mb-2">{{ t('admin.products.photo') }}</h3>
      <div v-if="product.photoFilename" class="mb-3">
        <img :src="`/api/admin/products/${product.id}/photo`" alt="" class="max-h-60 rounded" />
      </div>
      <p v-else class="text-sm text-fm-black/60 mb-3">{{ t('admin.products.noPhoto') }}</p>
      <input type="file" accept="image/jpeg,image/png,image/webp" @change="onPhotoChange" />
      <FmButton v-if="product.photoFilename" variant="ghost" @click="removePhoto" class="ml-2">
        {{ t('admin.products.removePhoto') }}
      </FmButton>
    </section>

    <form @submit.prevent="save" class="space-y-4">
      <FmInput v-model="form.name" :label="t('admin.products.name')" required maxlength="200" />
      <label class="block text-sm">
        {{ t('admin.products.description') }}
        <textarea v-model="form.description" rows="4" maxlength="4000"
                  class="mt-1 w-full border border-fm-black/20 rounded p-2" />
      </label>
      <FmInput v-model="form.priceEur" :label="t('admin.products.price')" type="text" required />

      <label class="flex items-center gap-2">
        <input type="checkbox" v-model="form.delivery" />
        {{ t('admin.products.delivery') }}
      </label>

      <FmInput v-model="form.weightKg" :label="t('admin.products.weight')" type="text" />

      <fieldset>
        <legend class="text-sm mb-2">{{ t('admin.products.tiers') }}</legend>
        <label v-for="tc in ['TIER_1','TIER_2','TIER_3'] as const" :key="tc" class="mr-4 inline-flex items-center gap-1">
          <input type="checkbox" :checked="form.tiers.has(tc)" @change="toggleTier(tc)" />
          {{ t(`tiers.${tc}`) }}
        </label>
      </fieldset>

      <FmInput v-model="form.saleStartAt" :label="t('admin.products.saleStart')" type="datetime-local" required />
      <FmInput v-model="form.saleEndAt" :label="t('admin.products.saleEnd')" type="datetime-local" />
      <FmInput v-model="form.stock" :label="t('admin.products.stock')" type="number" />

      <p v-if="error" class="text-sm text-fm-red">{{ error }}</p>
      <div class="flex gap-2">
        <FmButton type="submit" variant="primary" :disabled="busy">{{ t('admin.products.save') }}</FmButton>
        <FmButton type="button" variant="ghost" @click="remove">{{ t('admin.products.deleteProduct') }}</FmButton>
      </div>
    </form>
  </div>
</template>
```

- [ ] **Step 2: Build frontend**

```
cd frontend && npm run build
```

Expected: build succeeds.

- [ ] **Step 3: Commit frontend slice**

```
git add frontend/src/api/types.ts frontend/src/api/client.ts frontend/src/i18n/ frontend/src/router.ts frontend/src/components/AppLayout.vue frontend/src/views/admin/AdminProductsView.vue frontend/src/views/admin/AdminCreateProductView.vue frontend/src/views/admin/AdminProductDetailView.vue
git commit -m "feat(products): admin CRUD frontend (list/create/edit + photo upload)"
```

---

## Task 17: Manual E2E check

**Files:** none

- [ ] **Step 1: Start backend and frontend**

Two terminals:

```
cd backend && ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

```
cd frontend && npm run dev
```

- [ ] **Step 2: Test in browser at http://localhost:5273**

1. Log in as `admin@fuorimondo.local` / `Admin!Password123`.
2. Open drawer → confirm "Produits" link present.
3. Navigate → click "Créer un produit", fill every field (check TIER_1 + TIER_2, set `saleStartAt` to tomorrow), submit.
4. Expect redirect to detail view.
5. Upload a JPEG (< 5 MB) via the file input → image appears.
6. Change name, save → reload → name persisted.
7. Click "Supprimer la photo" → image disappears, placeholder shows.
8. Go back to list → one entry visible.
9. Open detail → delete → confirm → list empty.

If all 9 pass, Phase 1 is done.

- [ ] **Step 3: Final commit marker (if any cleanup)**

If fixes were made during the manual check, commit them:

```
git commit -am "fix(products): <describe>"
```

Otherwise skip.

---

## Out of scope (reminders)

- No public/allocataire endpoint yet (Phase 2).
- No tier-based or sale-window filtering for readers.
- No pagination, sorting, search.
- No image preview / cropping.
- No IT/EN translations.
- No unit-level tests beyond the integration happy-path from Task 7.

When Phase 2 begins, the first task will be opening up `GET /api/products` (public authenticated) and `GET /api/products/{id}/photo` (public authenticated) with the tier + sale-window filter.

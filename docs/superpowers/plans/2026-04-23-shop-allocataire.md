# Shop Allocataire — Phase 2 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the full allocataire-side e-commerce flow (browse → direct unit purchase → Mollie payment → confirmation email) on top of the existing Phase 1 admin product CRUD, plus an admin orders dashboard.

**Architecture:** Spring Boot 3 backend with new `orders` table (snapshots JSON as TEXT) and a `MolliePaymentGateway` abstraction (HTTP impl for prod, fake impl for dev/test). Stock reservation is derived from `orders` rows (no dedicated table) — `PENDING_PAYMENT` rows with `expires_at > now()` count as reserved. Vue 3 frontend gains 8 new routes plus sidebar links. A `@Scheduled` job sweeps expired orders every 5 minutes.

**Tech Stack:** Spring Boot 3 (Java 17, JPA/Hibernate, Flyway, RestClient), Spring Security, Vue 3 Composition API, Pinia, Tailwind, Vite, Playwright. Reference spec: `docs/superpowers/specs/2026-04-23-shop-allocataire-design.md`.

---

## File Structure

**Backend — new files:**
- `backend/src/main/resources/db/migration/V4__orders.sql`
- `backend/src/main/java/com/fuorimondo/orders/Order.java`
- `backend/src/main/java/com/fuorimondo/orders/OrderStatus.java`
- `backend/src/main/java/com/fuorimondo/orders/OrderRepository.java`
- `backend/src/main/java/com/fuorimondo/orders/OrderService.java`
- `backend/src/main/java/com/fuorimondo/orders/OrderException.java`
- `backend/src/main/java/com/fuorimondo/orders/OrderController.java`
- `backend/src/main/java/com/fuorimondo/orders/AdminOrderController.java`
- `backend/src/main/java/com/fuorimondo/orders/OrderExpirationJob.java`
- `backend/src/main/java/com/fuorimondo/orders/dto/CreateOrderRequest.java`
- `backend/src/main/java/com/fuorimondo/orders/dto/CreateOrderResponse.java`
- `backend/src/main/java/com/fuorimondo/orders/dto/OrderResponse.java`
- `backend/src/main/java/com/fuorimondo/orders/dto/AdminOrderResponse.java`
- `backend/src/main/java/com/fuorimondo/orders/dto/ProductSnapshot.java`
- `backend/src/main/java/com/fuorimondo/orders/dto/ShippingSnapshot.java`
- `backend/src/main/java/com/fuorimondo/products/ProductPublicController.java`
- `backend/src/main/java/com/fuorimondo/products/dto/PublicProductResponse.java`
- `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGateway.java` — interface
- `backend/src/main/java/com/fuorimondo/payments/MolliePayment.java` — value object
- `backend/src/main/java/com/fuorimondo/payments/CreatePaymentRequest.java`
- `backend/src/main/java/com/fuorimondo/payments/MolliePaymentStatus.java`
- `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGatewayFake.java`
- `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGatewayHttp.java`
- `backend/src/main/java/com/fuorimondo/payments/MollieConfig.java`
- `backend/src/main/java/com/fuorimondo/payments/MollieWebhookController.java`
- `backend/src/main/java/com/fuorimondo/payments/DevSimulateWebhookController.java`
- `backend/src/test/java/com/fuorimondo/orders/OrderCreationTest.java`
- `backend/src/test/java/com/fuorimondo/orders/OrderStatusPollingTest.java`
- `backend/src/test/java/com/fuorimondo/orders/OrderExpirationJobTest.java`
- `backend/src/test/java/com/fuorimondo/orders/AdminOrdersControllerTest.java`
- `backend/src/test/java/com/fuorimondo/products/ProductPublicControllerTest.java`
- `backend/src/test/java/com/fuorimondo/payments/MollieWebhookTest.java`
- `backend/src/test/java/com/fuorimondo/payments/DevSimulateWebhookTest.java`

**Backend — modifications:**
- `backend/src/main/java/com/fuorimondo/FuoriMondoApplication.java` — add `@EnableScheduling`
- `backend/src/main/java/com/fuorimondo/email/EmailSender.java` — add `sendOrderConfirmation`
- `backend/src/main/java/com/fuorimondo/email/ConsoleEmailSender.java` — implement `sendOrderConfirmation`
- `backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java` — handle `OrderException`
- `backend/src/main/java/com/fuorimondo/security/SecurityConfig.java` — permit shop/order/webhook routes, CSRF ignore for webhook
- `backend/src/main/java/com/fuorimondo/addresses/AddressController.java` — `GET` accepts optional `?type=SHIPPING` filter
- `backend/src/main/java/com/fuorimondo/addresses/AddressService.java` — `listByUserAndType`
- `backend/src/main/resources/application.yml` — `fuorimondo.order.*` + `mollie.*` blocks
- `backend/src/main/resources/application-dev.yml` — `mollie.enabled: false`
- `backend/src/main/resources/application-test.yml` — `mollie.enabled: false`

**Frontend — new files:**
- `frontend/src/views/ShopView.vue`
- `frontend/src/views/ShopProductView.vue`
- `frontend/src/views/ShopCheckoutView.vue`
- `frontend/src/views/ShopReturnView.vue`
- `frontend/src/views/MyOrdersView.vue`
- `frontend/src/views/OrderDetailView.vue`
- `frontend/src/views/admin/AdminOrdersView.vue`
- `frontend/src/views/admin/AdminOrderDetailView.vue`
- `frontend/src/components/OrderStatusBadge.vue`
- `frontend/e2e/shop-browse.spec.ts`
- `frontend/e2e/shop-tier-filter.spec.ts`
- `frontend/e2e/shop-out-of-stock.spec.ts`
- `frontend/e2e/shop-purchase.spec.ts`
- `frontend/e2e/admin-orders.spec.ts`

**Frontend — modifications:**
- `frontend/src/api/types.ts` — add shop/order/admin-order types
- `frontend/src/router.ts` — add 8 new routes
- `frontend/src/components/AppLayout.vue` — add sidebar links
- `frontend/src/i18n/fr.ts`, `it.ts`, `en.ts` — add `shop`, `checkout`, `orderReturn`, `order`, `errors` sections

---

## Task 1: Flyway migration V4 — orders table

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__orders.sql`

- [ ] **Step 1: Write the migration SQL**

Create `backend/src/main/resources/db/migration/V4__orders.sql`:

```sql
CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id),
    product_id          UUID NOT NULL REFERENCES products(id),
    product_snapshot    TEXT NOT NULL,
    unit_price_eur      NUMERIC(10,2) NOT NULL CHECK (unit_price_eur >= 0),
    total_eur           NUMERIC(10,2) NOT NULL CHECK (total_eur >= 0),
    shipping_address_id UUID REFERENCES addresses(id),
    shipping_snapshot   TEXT,
    status              VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_PAYMENT','PAID','FAILED','CANCELLED','EXPIRED')),
    mollie_payment_id   VARCHAR(64),
    mollie_checkout_url VARCHAR(512),
    expires_at          TIMESTAMP,
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_product_status ON orders(product_id, status);
CREATE INDEX idx_orders_mollie_payment ON orders(mollie_payment_id);
```

- [ ] **Step 2: Start the backend to verify Flyway applies it**

```
cd backend && ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Expected: `Migrating schema ... to version "4 - orders"` and `Successfully applied 1 migration`. No stack trace. Ctrl-C to stop.

- [ ] **Step 3: Commit**

```
git add backend/src/main/resources/db/migration/V4__orders.sql
git commit -m "feat(shop): V4 migration — orders table"
```

---

## Task 2: OrderStatus enum + Order entity + repository

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/orders/OrderStatus.java`
- Create: `backend/src/main/java/com/fuorimondo/orders/Order.java`
- Create: `backend/src/main/java/com/fuorimondo/orders/OrderRepository.java`

- [ ] **Step 1: Write `OrderStatus`**

Create `backend/src/main/java/com/fuorimondo/orders/OrderStatus.java`:

```java
package com.fuorimondo.orders;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    FAILED,
    CANCELLED,
    EXPIRED
}
```

- [ ] **Step 2: Write `Order` entity**

Create `backend/src/main/java/com/fuorimondo/orders/Order.java`:

```java
package com.fuorimondo.orders;

import com.fuorimondo.addresses.Address;
import com.fuorimondo.common.BaseEntity;
import com.fuorimondo.products.Product;
import com.fuorimondo.users.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_snapshot", nullable = false, columnDefinition = "TEXT")
    private String productSnapshot;

    @Column(name = "unit_price_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceEur;

    @Column(name = "total_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;

    @Column(name = "shipping_snapshot", columnDefinition = "TEXT")
    private String shippingSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "mollie_payment_id", length = 64)
    private String molliePaymentId;

    @Column(name = "mollie_checkout_url", length = 512)
    private String mollieCheckoutUrl;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getProductSnapshot() { return productSnapshot; }
    public void setProductSnapshot(String productSnapshot) { this.productSnapshot = productSnapshot; }
    public BigDecimal getUnitPriceEur() { return unitPriceEur; }
    public void setUnitPriceEur(BigDecimal unitPriceEur) { this.unitPriceEur = unitPriceEur; }
    public BigDecimal getTotalEur() { return totalEur; }
    public void setTotalEur(BigDecimal totalEur) { this.totalEur = totalEur; }
    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getShippingSnapshot() { return shippingSnapshot; }
    public void setShippingSnapshot(String shippingSnapshot) { this.shippingSnapshot = shippingSnapshot; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getMolliePaymentId() { return molliePaymentId; }
    public void setMolliePaymentId(String molliePaymentId) { this.molliePaymentId = molliePaymentId; }
    public String getMollieCheckoutUrl() { return mollieCheckoutUrl; }
    public void setMollieCheckoutUrl(String url) { this.mollieCheckoutUrl = url; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
```

- [ ] **Step 3: Write `OrderRepository`**

Create `backend/src/main/java/com/fuorimondo/orders/OrderRepository.java`:

```java
package com.fuorimondo.orders;

import com.fuorimondo.products.Product;
import com.fuorimondo.users.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByMolliePaymentId(String molliePaymentId);

    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Order> findByUserAndProductAndStatus(User user, Product product, OrderStatus status);

    @Query("""
           SELECT COUNT(o) FROM Order o
           WHERE o.product = :product
             AND o.status = com.fuorimondo.orders.OrderStatus.PENDING_PAYMENT
             AND o.expiresAt > :now
           """)
    long countActiveReservations(@Param("product") Product product, @Param("now") Instant now);

    @Modifying
    @Query("""
           UPDATE Order o SET o.status = com.fuorimondo.orders.OrderStatus.EXPIRED
           WHERE o.status = com.fuorimondo.orders.OrderStatus.PENDING_PAYMENT
             AND o.expiresAt < :now
           """)
    int expireStaleOrders(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<com.fuorimondo.products.Product> lockProduct(@Param("productId") UUID productId);
}
```

Note: `lockProduct` returns `Product` via a JPA query that locks the row — this gives us the SQL-level `FOR UPDATE` on the `products` row for atomic stock checks. It lives on `OrderRepository` to keep Order-related persistence concerns together.

- [ ] **Step 4: Build the backend to catch compile errors**

```
cd backend && ./mvnw.cmd compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```
git add backend/src/main/java/com/fuorimondo/orders/OrderStatus.java \
        backend/src/main/java/com/fuorimondo/orders/Order.java \
        backend/src/main/java/com/fuorimondo/orders/OrderRepository.java
git commit -m "feat(shop): Order entity + repository"
```

---

## Task 3: OrderException + GlobalExceptionHandler mapping

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/orders/OrderException.java`
- Modify: `backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java`

- [ ] **Step 1: Write `OrderException`**

Create `backend/src/main/java/com/fuorimondo/orders/OrderException.java`:

```java
package com.fuorimondo.orders;

public class OrderException extends RuntimeException {

    public enum Reason {
        PRODUCT_NOT_FOUND,
        TIER_MISMATCH,
        SALE_WINDOW_CLOSED,
        OUT_OF_STOCK,
        NO_SHIPPING_ADDRESS,
        INVALID_SHIPPING_ADDRESS,
        PAYMENT_GATEWAY_ERROR
    }

    private final Reason reason;

    public OrderException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
```

- [ ] **Step 2: Add the handler to `GlobalExceptionHandler`**

Edit `backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java`, add the import:

```java
import com.fuorimondo.orders.OrderException;
```

Add this method inside the class (after `handleAuth`):

```java
@ExceptionHandler(OrderException.class)
public ResponseEntity<ApiError> handleOrder(OrderException ex) {
    HttpStatus status = switch (ex.getReason()) {
        case PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
        case TIER_MISMATCH -> HttpStatus.FORBIDDEN;
        case SALE_WINDOW_CLOSED, OUT_OF_STOCK -> HttpStatus.CONFLICT;
        case NO_SHIPPING_ADDRESS, INVALID_SHIPPING_ADDRESS -> HttpStatus.UNPROCESSABLE_ENTITY;
        case PAYMENT_GATEWAY_ERROR -> HttpStatus.BAD_GATEWAY;
    };
    log.info("Order rejected [{}]: {} -> {}", ex.getReason(), ex.getMessage(), status.value());
    return ResponseEntity.status(status).body(ApiError.of(ex.getReason().name().toLowerCase(), ex.getMessage()));
}
```

- [ ] **Step 3: Build**

```
cd backend && ./mvnw.cmd compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```
git add backend/src/main/java/com/fuorimondo/orders/OrderException.java \
        backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java
git commit -m "feat(shop): OrderException + HTTP mapping"
```

---

## Task 4: Snapshot DTOs

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/orders/dto/ProductSnapshot.java`
- Create: `backend/src/main/java/com/fuorimondo/orders/dto/ShippingSnapshot.java`

- [ ] **Step 1: Write `ProductSnapshot`**

Create `backend/src/main/java/com/fuorimondo/orders/dto/ProductSnapshot.java`:

```java
package com.fuorimondo.orders.dto;

import com.fuorimondo.products.Product;
import com.fuorimondo.users.TierCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductSnapshot(
    UUID id,
    String name,
    String description,
    BigDecimal priceEur,
    String photoFilename,
    BigDecimal weightKg,
    boolean delivery,
    List<TierCode> tiers
) {
    public static ProductSnapshot from(Product p) {
        return new ProductSnapshot(
            p.getId(),
            p.getName(),
            p.getDescription(),
            p.getPriceEur(),
            p.getPhotoFilename(),
            p.getWeightKg(),
            p.isDelivery(),
            p.getTiers().stream().sorted().toList()
        );
    }
}
```

- [ ] **Step 2: Write `ShippingSnapshot`**

Create `backend/src/main/java/com/fuorimondo/orders/dto/ShippingSnapshot.java`:

```java
package com.fuorimondo.orders.dto;

import com.fuorimondo.addresses.Address;

public record ShippingSnapshot(
    String fullName,
    String street,
    String streetExtra,
    String postalCode,
    String city,
    String country
) {
    public static ShippingSnapshot from(Address a) {
        return new ShippingSnapshot(
            a.getFullName(),
            a.getStreet(),
            a.getStreetExtra(),
            a.getPostalCode(),
            a.getCity(),
            a.getCountry()
        );
    }
}
```

- [ ] **Step 3: Build**

```
cd backend && ./mvnw.cmd compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```
git add backend/src/main/java/com/fuorimondo/orders/dto/
git commit -m "feat(shop): ProductSnapshot + ShippingSnapshot DTOs"
```

---

## Task 5: Mollie gateway — interface, DTOs, fake implementation

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/payments/MolliePaymentStatus.java`
- Create: `backend/src/main/java/com/fuorimondo/payments/MolliePayment.java`
- Create: `backend/src/main/java/com/fuorimondo/payments/CreatePaymentRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGateway.java`
- Create: `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGatewayFake.java`
- Create: `backend/src/main/java/com/fuorimondo/payments/MollieConfig.java`

- [ ] **Step 1: Write `MolliePaymentStatus`**

Create `backend/src/main/java/com/fuorimondo/payments/MolliePaymentStatus.java`:

```java
package com.fuorimondo.payments;

public enum MolliePaymentStatus {
    OPEN, PENDING, AUTHORIZED, PAID, FAILED, CANCELED, EXPIRED
}
```

- [ ] **Step 2: Write `MolliePayment`**

Create `backend/src/main/java/com/fuorimondo/payments/MolliePayment.java`:

```java
package com.fuorimondo.payments;

public record MolliePayment(
    String id,
    MolliePaymentStatus status,
    String checkoutUrl
) {}
```

- [ ] **Step 3: Write `CreatePaymentRequest`**

Create `backend/src/main/java/com/fuorimondo/payments/CreatePaymentRequest.java`:

```java
package com.fuorimondo.payments;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
    BigDecimal amountEur,
    String description,
    UUID orderId,
    String redirectUrl,
    String webhookUrl
) {}
```

- [ ] **Step 4: Write `MolliePaymentGateway` interface**

Create `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGateway.java`:

```java
package com.fuorimondo.payments;

public interface MolliePaymentGateway {
    MolliePayment createPayment(CreatePaymentRequest req);
    MolliePayment getPayment(String molliePaymentId);

    /** Test-only hook used by the dev simulate endpoint. Default: unsupported. */
    default void forceStatus(String molliePaymentId, MolliePaymentStatus status) {
        throw new UnsupportedOperationException("forceStatus only supported on fake gateway");
    }
}
```

- [ ] **Step 5: Write `MollieConfig`**

Create `backend/src/main/java/com/fuorimondo/payments/MollieConfig.java`:

```java
package com.fuorimondo.payments;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mollie")
public class MollieConfig {
    private String apiKey;
    private boolean enabled = true;
    private String apiBaseUrl = "https://api.mollie.com/v2";
    private String redirectBaseUrl;
    private String webhookBaseUrl;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getRedirectBaseUrl() { return redirectBaseUrl; }
    public void setRedirectBaseUrl(String redirectBaseUrl) { this.redirectBaseUrl = redirectBaseUrl; }
    public String getWebhookBaseUrl() { return webhookBaseUrl; }
    public void setWebhookBaseUrl(String webhookBaseUrl) { this.webhookBaseUrl = webhookBaseUrl; }
}
```

- [ ] **Step 6: Write `MolliePaymentGatewayFake`**

Create `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGatewayFake.java`:

```java
package com.fuorimondo.payments;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "mollie.enabled", havingValue = "false", matchIfMissing = false)
public class MolliePaymentGatewayFake implements MolliePaymentGateway {

    private final MollieConfig config;
    private final ConcurrentHashMap<String, MolliePaymentStatus> statuses = new ConcurrentHashMap<>();

    public MolliePaymentGatewayFake(MollieConfig config) {
        this.config = config;
    }

    @Override
    public MolliePayment createPayment(CreatePaymentRequest req) {
        String id = "tr_fake_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        statuses.put(id, MolliePaymentStatus.OPEN);
        String base = config.getRedirectBaseUrl() == null ? "" : config.getRedirectBaseUrl();
        String checkoutUrl = base + "/shop/order/" + req.orderId() + "/return?sim=1";
        return new MolliePayment(id, MolliePaymentStatus.OPEN, checkoutUrl);
    }

    @Override
    public MolliePayment getPayment(String molliePaymentId) {
        MolliePaymentStatus status = statuses.getOrDefault(molliePaymentId, MolliePaymentStatus.OPEN);
        return new MolliePayment(molliePaymentId, status, null);
    }

    @Override
    public void forceStatus(String molliePaymentId, MolliePaymentStatus status) {
        statuses.put(molliePaymentId, status);
    }
}
```

- [ ] **Step 7: Build**

```
cd backend && ./mvnw.cmd compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```
git add backend/src/main/java/com/fuorimondo/payments/
git commit -m "feat(shop): Mollie gateway interface + fake impl + config"
```

---

## Task 6: Mollie HTTP gateway implementation

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGatewayHttp.java`

- [ ] **Step 1: Write the HTTP gateway**

Create `backend/src/main/java/com/fuorimondo/payments/MolliePaymentGatewayHttp.java`:

```java
package com.fuorimondo.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mollie.enabled", havingValue = "true", matchIfMissing = true)
public class MolliePaymentGatewayHttp implements MolliePaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MolliePaymentGatewayHttp.class);

    private final MollieConfig config;
    private final RestClient http;

    public MolliePaymentGatewayHttp(MollieConfig config) {
        this.config = config;
        this.http = RestClient.builder()
            .baseUrl(config.getApiBaseUrl())
            .defaultHeader("Authorization", "Bearer " + config.getApiKey())
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public MolliePayment createPayment(CreatePaymentRequest req) {
        Map<String, Object> body = Map.of(
            "amount", Map.of(
                "currency", "EUR",
                "value", req.amountEur().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
            ),
            "description", req.description(),
            "redirectUrl", config.getRedirectBaseUrl() + "/shop/order/" + req.orderId() + "/return",
            "webhookUrl", config.getWebhookBaseUrl() + "/api/webhooks/mollie",
            "metadata", Map.of("orderId", req.orderId().toString())
        );
        try {
            Map<?,?> resp = http.post()
                .uri("/payments")
                .body(body)
                .retrieve()
                .body(Map.class);
            if (resp == null) throw new OrderGatewayException("empty response");
            String id = (String) resp.get("id");
            String status = (String) resp.get("status");
            Map<?,?> links = (Map<?,?>) resp.get("_links");
            Map<?,?> checkout = links == null ? null : (Map<?,?>) links.get("checkout");
            String href = checkout == null ? null : (String) checkout.get("href");
            return new MolliePayment(id, parseStatus(status), href);
        } catch (Exception e) {
            log.warn("Mollie createPayment failed: {}", e.getMessage());
            throw new OrderGatewayException("Mollie createPayment failed: " + e.getMessage());
        }
    }

    @Override
    public MolliePayment getPayment(String molliePaymentId) {
        try {
            Map<?,?> resp = http.get()
                .uri("/payments/{id}", molliePaymentId)
                .retrieve()
                .body(Map.class);
            if (resp == null) throw new OrderGatewayException("empty response");
            String status = (String) resp.get("status");
            return new MolliePayment(molliePaymentId, parseStatus(status), null);
        } catch (Exception e) {
            log.warn("Mollie getPayment failed: {}", e.getMessage());
            throw new OrderGatewayException("Mollie getPayment failed: " + e.getMessage());
        }
    }

    private MolliePaymentStatus parseStatus(String s) {
        if (s == null) return MolliePaymentStatus.OPEN;
        return MolliePaymentStatus.valueOf(s.toUpperCase(Locale.ROOT));
    }

    public static class OrderGatewayException extends RuntimeException {
        public OrderGatewayException(String msg) { super(msg); }
    }
}
```

- [ ] **Step 2: Build**

```
cd backend && ./mvnw.cmd compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```
git add backend/src/main/java/com/fuorimondo/payments/MolliePaymentGatewayHttp.java
git commit -m "feat(shop): Mollie HTTP gateway impl (RestClient)"
```

---

## Task 7: Config — application.yml, dev, test

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-dev.yml`
- Modify: `backend/src/main/resources/application-test.yml`

- [ ] **Step 1: Append shop config to `application.yml`**

Append at the end of `backend/src/main/resources/application.yml`:

```yaml
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
```

Note: if there's already a `fuorimondo:` block at root, merge instead of duplicating.

- [ ] **Step 2: Override in `application-dev.yml`**

Append at the end of `backend/src/main/resources/application-dev.yml`:

```yaml
mollie:
  enabled: false
```

- [ ] **Step 3: Override in `application-test.yml`**

Append at the end of `backend/src/main/resources/application-test.yml`:

```yaml
mollie:
  enabled: false
  redirect-base-url: http://localhost:5273
```

- [ ] **Step 4: Start the backend to verify config parses**

```
cd backend && ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Expected: normal startup, no `BindingException`. Ctrl-C to stop.

- [ ] **Step 5: Commit**

```
git add backend/src/main/resources/application.yml backend/src/main/resources/application-dev.yml backend/src/main/resources/application-test.yml
git commit -m "feat(shop): Mollie + order config"
```

---

## Task 8: ProductPublicController — GET list

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/products/dto/PublicProductResponse.java`
- Create: `backend/src/main/java/com/fuorimondo/products/ProductPublicController.java`
- Create: `backend/src/test/java/com/fuorimondo/products/ProductPublicControllerTest.java`
- Modify: `backend/src/main/java/com/fuorimondo/products/ProductRepository.java` (add query)

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/fuorimondo/products/ProductPublicControllerTest.java`:

```java
package com.fuorimondo.products;

import com.fuorimondo.users.TierCode;
import com.fuorimondo.users.User;
import com.fuorimondo.users.UserRepository;
import com.fuorimondo.users.UserRole;
import com.fuorimondo.users.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductPublicControllerTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    MockMvc mvc;
    User tier1User;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        tier1User = seedUser("allo1@test", TierCode.TIER_1);
    }

    private User seedUser(String email, TierCode tier) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("Allo");
        u.setLastName("Cat");
        u.setPasswordHash(encoder.encode("Password12"));
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setTierCode(tier);
        u.setCountry("FR");
        u.setCity("Paris");
        u.setLocale(com.fuorimondo.users.Locale.FR);
        u.setCivility(com.fuorimondo.users.Civility.NONE);
        return userRepo.save(u);
    }

    private Product seedProduct(String name, BigDecimal price, java.util.Set<TierCode> tiers,
                                 Instant start, Instant end, Integer stock) {
        Product p = new Product();
        p.setName(name);
        p.setPriceEur(price);
        p.setTiers(tiers);
        p.setSaleStartAt(start);
        p.setSaleEndAt(end);
        p.setStock(stock);
        p.setDelivery(true);
        return productRepo.save(p);
    }

    @Test
    @WithUserDetails("allo1@test")
    void lists_only_products_in_user_tier_and_open_window() throws Exception {
        Instant now = Instant.now();
        Product visible = seedProduct("T1 open", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_1), now.minus(1, ChronoUnit.DAYS), now.plus(30, ChronoUnit.DAYS), null);
        Product wrongTier = seedProduct("T2 open", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_2), now.minus(1, ChronoUnit.DAYS), null, null);
        Product future = seedProduct("T1 future", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_1), now.plus(10, ChronoUnit.DAYS), null, null);
        Product past = seedProduct("T1 past", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_1), now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), null);

        mvc.perform(get("/api/products"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1))
           .andExpect(jsonPath("$[0].id").value(visible.getId().toString()));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```
cd backend && ./mvnw.cmd test "-Dtest=ProductPublicControllerTest"
```

Expected: FAIL with `404 Not Found` for `/api/products` (controller doesn't exist yet).

- [ ] **Step 3: Write `PublicProductResponse`**

Create `backend/src/main/java/com/fuorimondo/products/dto/PublicProductResponse.java`:

```java
package com.fuorimondo.products.dto;

import com.fuorimondo.products.Product;
import com.fuorimondo.users.TierCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicProductResponse(
    UUID id,
    String name,
    String description,
    BigDecimal priceEur,
    String photoFilename,
    BigDecimal weightKg,
    boolean delivery,
    List<TierCode> tiers,
    Instant saleStartAt,
    Instant saleEndAt,
    Integer stockRemaining
) {
    public static PublicProductResponse from(Product p, Integer stockRemaining) {
        return new PublicProductResponse(
            p.getId(), p.getName(), p.getDescription(), p.getPriceEur(),
            p.getPhotoFilename(), p.getWeightKg(), p.isDelivery(),
            p.getTiers().stream().sorted().toList(),
            p.getSaleStartAt(), p.getSaleEndAt(),
            stockRemaining
        );
    }
}
```

- [ ] **Step 4: Add query to `ProductRepository`**

Add to `backend/src/main/java/com/fuorimondo/products/ProductRepository.java`:

```java
@org.springframework.data.jpa.repository.Query("""
    SELECT DISTINCT p FROM Product p
    JOIN p.tiers t
    WHERE t = :tier
      AND p.saleStartAt <= :now
      AND (p.saleEndAt IS NULL OR p.saleEndAt > :now)
    ORDER BY p.saleStartAt DESC
    """)
java.util.List<Product> findVisibleForTier(
    @org.springframework.data.repository.query.Param("tier") com.fuorimondo.users.TierCode tier,
    @org.springframework.data.repository.query.Param("now") java.time.Instant now);
```

- [ ] **Step 5: Write `ProductPublicController`**

Create `backend/src/main/java/com/fuorimondo/products/ProductPublicController.java`:

```java
package com.fuorimondo.products;

import com.fuorimondo.orders.OrderRepository;
import com.fuorimondo.products.dto.PublicProductResponse;
import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.User;
import com.fuorimondo.users.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductPublicController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ProductPublicController(ProductRepository productRepository,
                                    OrderRepository orderRepository,
                                    UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<PublicProductResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        User u = userRepository.findById(principal.getUserId()).orElseThrow();
        if (u.getTierCode() == null) return List.of();
        Instant now = Instant.now();
        return productRepository.findVisibleForTier(u.getTierCode(), now).stream()
            .map(p -> {
                Integer remaining = null;
                if (p.getStock() != null) {
                    long reserved = orderRepository.countActiveReservations(p, now);
                    remaining = Math.max(0, p.getStock() - (int) reserved);
                }
                return PublicProductResponse.from(p, remaining);
            })
            .filter(r -> r.stockRemaining() == null || r.stockRemaining() > 0)
            .toList();
    }
}
```

- [ ] **Step 6: Run the test — expect PASS**

```
cd backend && ./mvnw.cmd test "-Dtest=ProductPublicControllerTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```
git add backend/src/main/java/com/fuorimondo/products/ProductPublicController.java \
        backend/src/main/java/com/fuorimondo/products/dto/PublicProductResponse.java \
        backend/src/main/java/com/fuorimondo/products/ProductRepository.java \
        backend/src/test/java/com/fuorimondo/products/ProductPublicControllerTest.java
git commit -m "feat(shop): GET /api/products filtered by tier + sale window"
```

---

## Task 9: ProductPublicController — GET detail + out-of-stock filter test

**Files:**
- Modify: `backend/src/main/java/com/fuorimondo/products/ProductPublicController.java`
- Modify: `backend/src/test/java/com/fuorimondo/products/ProductPublicControllerTest.java`

- [ ] **Step 1: Write the additional failing tests**

Add to `ProductPublicControllerTest.java`:

```java
@Test
@WithUserDetails("allo1@test")
void detail_returns_404_for_wrong_tier() throws Exception {
    Instant now = Instant.now();
    Product t2 = seedProduct("T2 only", new BigDecimal("50.00"),
        EnumSet.of(TierCode.TIER_2), now.minus(1, ChronoUnit.DAYS), null, null);

    mvc.perform(get("/api/products/" + t2.getId()))
       .andExpect(status().isNotFound());
}

@Test
@WithUserDetails("allo1@test")
void detail_returns_product_when_visible() throws Exception {
    Instant now = Instant.now();
    Product p = seedProduct("T1 open", new BigDecimal("120.00"),
        EnumSet.of(TierCode.TIER_1), now.minus(1, ChronoUnit.DAYS), null, null);

    mvc.perform(get("/api/products/" + p.getId()))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.id").value(p.getId().toString()))
       .andExpect(jsonPath("$.priceEur").value(120.00));
}

@Test
@WithUserDetails("allo1@test")
void list_hides_out_of_stock_products() throws Exception {
    Instant now = Instant.now();
    seedProduct("T1 stock0", new BigDecimal("100.00"),
        EnumSet.of(TierCode.TIER_1), now.minus(1, ChronoUnit.DAYS), null, 0);

    mvc.perform(get("/api/products"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.length()").value(0));
}
```

- [ ] **Step 2: Run — verify the new tests fail**

```
cd backend && ./mvnw.cmd test "-Dtest=ProductPublicControllerTest"
```

Expected: the two "detail" tests fail (404 for both or compilation error). `list_hides_out_of_stock_products` should already pass thanks to the `.filter()` in list (from Task 8).

- [ ] **Step 3: Add GET detail to controller**

Add to `ProductPublicController.java`:

```java
@GetMapping("/{id}")
public PublicProductResponse detail(@AuthenticationPrincipal CustomUserDetails principal,
                                     @PathVariable UUID id) {
    User u = userRepository.findById(principal.getUserId()).orElseThrow();
    Product p = productRepository.findById(id).orElseThrow(() ->
        new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND));

    Instant now = Instant.now();
    if (u.getTierCode() == null || !p.getTiers().contains(u.getTierCode())
        || p.getSaleStartAt().isAfter(now)
        || (p.getSaleEndAt() != null && !p.getSaleEndAt().isAfter(now))) {
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND);
    }

    Integer remaining = null;
    if (p.getStock() != null) {
        long reserved = orderRepository.countActiveReservations(p, now);
        remaining = Math.max(0, p.getStock() - (int) reserved);
    }
    return PublicProductResponse.from(p, remaining);
}
```

- [ ] **Step 4: Run — expect all PASS**

```
cd backend && ./mvnw.cmd test "-Dtest=ProductPublicControllerTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```
git add backend/src/main/java/com/fuorimondo/products/ProductPublicController.java \
        backend/src/test/java/com/fuorimondo/products/ProductPublicControllerTest.java
git commit -m "feat(shop): GET /api/products/{id} with tier + window filtering"
```

---

## Task 10: Update SecurityConfig to permit shop routes

**Files:**
- Modify: `backend/src/main/java/com/fuorimondo/security/SecurityConfig.java`

- [ ] **Step 1: Add the new authorization rules**

Edit `backend/src/main/java/com/fuorimondo/security/SecurityConfig.java`. Locate the `authorizeHttpRequests(auth -> auth…)` block. Insert before `.anyRequest().authenticated()`:

```java
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/api/dev/**").authenticated()
```

Also update the CSRF block — locate:

```java
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers("/h2-console/**"))
```

Extend the `ignoringRequestMatchers` call:

```java
                .ignoringRequestMatchers("/h2-console/**", "/api/webhooks/**"))
```

- [ ] **Step 2: Build**

```
cd backend && ./mvnw.cmd compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```
git add backend/src/main/java/com/fuorimondo/security/SecurityConfig.java
git commit -m "feat(shop): SecurityConfig — permit webhooks, ignore CSRF for webhook"
```

---

## Task 11: Address list by type filter

**Files:**
- Modify: `backend/src/main/java/com/fuorimondo/addresses/AddressController.java`
- Modify: `backend/src/main/java/com/fuorimondo/addresses/AddressService.java`

- [ ] **Step 1: Read existing repo to pick the correct base method**

Open `backend/src/main/java/com/fuorimondo/addresses/AddressRepository.java`. Note the exact name of the existing "list by user" method (likely `findByUserIdOrderByIsDefaultDescCreatedAtDesc` or similar).

- [ ] **Step 2: Add a by-type variant to the repo**

Append to `AddressRepository.java`:

```java
List<Address> findByUserIdAndTypeOrderByIsDefaultDescCreatedAtDesc(UUID userId, AddressType type);
```

- [ ] **Step 3: Add type-aware list method to the service**

Add to `backend/src/main/java/com/fuorimondo/addresses/AddressService.java` (match the existing import style — `Address`, `AddressType`, `AddressRepository` should already be available in scope):

```java
public List<Address> listByUserAndType(UUID userId, AddressType type) {
    if (type == null) return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    return addressRepository.findByUserIdAndTypeOrderByIsDefaultDescCreatedAtDesc(userId, type);
}
```

If the existing "list by user" method has a different name, rename the fallback call accordingly.

- [ ] **Step 4: Add `?type=` query param to controller**

Edit `backend/src/main/java/com/fuorimondo/addresses/AddressController.java`. Locate the `GET` list method and change its signature to accept an optional query param:

```java
@GetMapping
public List<AddressResponse> list(@AuthenticationPrincipal CustomUserDetails principal,
                                   @RequestParam(required = false) AddressType type) {
    return service.listByUserAndType(principal.getUserId(), type)
        .stream().map(AddressResponse::from).toList();
}
```

(Imports `AddressType` and `@RequestParam` may need to be added — the other imports are already present for the existing `list` method.)

- [ ] **Step 5: Build**

```
cd backend && ./mvnw.cmd compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```
git add backend/src/main/java/com/fuorimondo/addresses/
git commit -m "feat(shop): GET /api/me/addresses?type=SHIPPING filter"
```

---

## Task 12: OrderService + OrderController — create + get (happy path)

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/orders/OrderService.java`
- Create: `backend/src/main/java/com/fuorimondo/orders/dto/CreateOrderRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/orders/dto/CreateOrderResponse.java`
- Create: `backend/src/main/java/com/fuorimondo/orders/dto/OrderResponse.java`
- Create: `backend/src/main/java/com/fuorimondo/orders/OrderController.java`
- Create: `backend/src/test/java/com/fuorimondo/orders/OrderCreationTest.java`

- [ ] **Step 1: Write DTOs**

Create `backend/src/main/java/com/fuorimondo/orders/dto/CreateOrderRequest.java`:

```java
package com.fuorimondo.orders.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID productId,
    UUID shippingAddressId
) {}
```

Create `backend/src/main/java/com/fuorimondo/orders/dto/CreateOrderResponse.java`:

```java
package com.fuorimondo.orders.dto;

import java.util.UUID;

public record CreateOrderResponse(UUID orderId, String checkoutUrl) {}
```

Create `backend/src/main/java/com/fuorimondo/orders/dto/OrderResponse.java`:

```java
package com.fuorimondo.orders.dto;

import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    ProductSnapshot product,
    BigDecimal unitPriceEur,
    BigDecimal totalEur,
    ShippingSnapshot shippingAddress,
    OrderStatus status,
    String mollieCheckoutUrl,
    Instant expiresAt,
    Instant paidAt,
    Instant createdAt
) {
    public static OrderResponse from(Order o,
                                      com.fasterxml.jackson.databind.ObjectMapper mapper) {
        try {
            ProductSnapshot prod = mapper.readValue(o.getProductSnapshot(), ProductSnapshot.class);
            ShippingSnapshot ship = o.getShippingSnapshot() == null
                ? null
                : mapper.readValue(o.getShippingSnapshot(), ShippingSnapshot.class);
            return new OrderResponse(o.getId(), prod, o.getUnitPriceEur(), o.getTotalEur(),
                ship, o.getStatus(), o.getMollieCheckoutUrl(),
                o.getExpiresAt(), o.getPaidAt(), o.getCreatedAt());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize order snapshots", e);
        }
    }
}
```

- [ ] **Step 2: Write the failing test**

Create `backend/src/test/java/com/fuorimondo/orders/OrderCreationTest.java`:

```java
package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.addresses.Address;
import com.fuorimondo.addresses.AddressRepository;
import com.fuorimondo.addresses.AddressType;
import com.fuorimondo.orders.dto.CreateOrderRequest;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderCreationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired AddressRepository addressRepo;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    MockMvc mvc;
    User allo;
    Product product;
    Address shipping;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity())
            .defaultRequest(post("/").with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
            .build();

        allo = new User();
        allo.setEmail("buyer@test");
        allo.setFirstName("Buyer"); allo.setLastName("One");
        allo.setPasswordHash(encoder.encode("Password12"));
        allo.setStatus(UserStatus.ALLOCATAIRE); allo.setRole(UserRole.USER);
        allo.setTierCode(TierCode.TIER_1);
        allo.setCountry("FR"); allo.setCity("Paris");
        allo.setLocale(Locale.FR); allo.setCivility(Civility.NONE);
        allo = userRepo.save(allo);

        product = new Product();
        product.setName("T1 Wine");
        product.setPriceEur(new BigDecimal("180.00"));
        product.setTiers(EnumSet.of(TierCode.TIER_1));
        product.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        product.setSaleEndAt(null);
        product.setStock(3);
        product.setDelivery(true);
        product = productRepo.save(product);

        shipping = new Address();
        shipping.setUser(allo);
        shipping.setType(AddressType.SHIPPING);
        shipping.setFullName("Buyer One");
        shipping.setStreet("1 rue du Test"); shipping.setPostalCode("75000");
        shipping.setCity("Paris"); shipping.setCountry("FR");
        shipping.setDefault(true);
        shipping = addressRepo.save(shipping);
    }

    @Test
    @WithUserDetails("buyer@test")
    void creates_order_and_returns_checkout_url() throws Exception {
        var body = new CreateOrderRequest(product.getId(), shipping.getId());
        mvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.orderId").exists())
           .andExpect(jsonPath("$.checkoutUrl").value(org.hamcrest.Matchers.containsString("/shop/order/")));
    }
}
```

- [ ] **Step 3: Run the test — expect FAIL**

```
cd backend && ./mvnw.cmd test "-Dtest=OrderCreationTest"
```

Expected: FAIL, 404 (`/api/orders` not found).

- [ ] **Step 4: Write `OrderService`**

Create `backend/src/main/java/com/fuorimondo/orders/OrderService.java`:

```java
package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.addresses.Address;
import com.fuorimondo.addresses.AddressRepository;
import com.fuorimondo.addresses.AddressType;
import com.fuorimondo.orders.dto.ProductSnapshot;
import com.fuorimondo.orders.dto.ShippingSnapshot;
import com.fuorimondo.payments.CreatePaymentRequest;
import com.fuorimondo.payments.MolliePayment;
import com.fuorimondo.payments.MolliePaymentGateway;
import com.fuorimondo.payments.MollieConfig;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final MolliePaymentGateway molliePaymentGateway;
    private final MollieConfig mollieConfig;
    private final ObjectMapper json;
    private final int reservationTtlMinutes;

    public OrderService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         AddressRepository addressRepository,
                         MolliePaymentGateway molliePaymentGateway,
                         MollieConfig mollieConfig,
                         ObjectMapper json,
                         @Value("${fuorimondo.order.reservation-ttl-minutes:15}") int ttl) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.molliePaymentGateway = molliePaymentGateway;
        this.mollieConfig = mollieConfig;
        this.json = json;
        this.reservationTtlMinutes = ttl;
    }

    @Transactional
    public Order create(User user, UUID productId, UUID shippingAddressId) {
        Product product = orderRepository.lockProduct(productId)
            .orElseThrow(() -> new OrderException(OrderException.Reason.PRODUCT_NOT_FOUND, "product"));

        Instant now = Instant.now();

        if (user.getTierCode() == null || !product.getTiers().contains(user.getTierCode())) {
            throw new OrderException(OrderException.Reason.TIER_MISMATCH, "tier");
        }
        if (product.getSaleStartAt().isAfter(now)
                || (product.getSaleEndAt() != null && !product.getSaleEndAt().isAfter(now))) {
            throw new OrderException(OrderException.Reason.SALE_WINDOW_CLOSED, "window");
        }
        if (product.getStock() != null) {
            long reserved = orderRepository.countActiveReservations(product, now);
            if (product.getStock() - reserved < 1) {
                throw new OrderException(OrderException.Reason.OUT_OF_STOCK, "stock");
            }
        }

        Address shipping = null;
        if (product.isDelivery()) {
            if (shippingAddressId == null) {
                throw new OrderException(OrderException.Reason.NO_SHIPPING_ADDRESS, "address required");
            }
            shipping = addressRepository.findById(shippingAddressId)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .filter(a -> a.getType() == AddressType.SHIPPING)
                .orElseThrow(() -> new OrderException(OrderException.Reason.INVALID_SHIPPING_ADDRESS, "address"));
        }

        Order order = new Order();
        order.setUser(user);
        order.setProduct(product);
        order.setUnitPriceEur(product.getPriceEur());
        order.setTotalEur(product.getPriceEur());
        order.setShippingAddress(shipping);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setExpiresAt(now.plus(reservationTtlMinutes, ChronoUnit.MINUTES));

        try {
            order.setProductSnapshot(json.writeValueAsString(ProductSnapshot.from(product)));
            if (shipping != null) {
                order.setShippingSnapshot(json.writeValueAsString(ShippingSnapshot.from(shipping)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize snapshots", e);
        }

        order = orderRepository.save(order);

        MolliePayment payment = molliePaymentGateway.createPayment(new CreatePaymentRequest(
            product.getPriceEur(),
            "Fuori Marmo — " + product.getName(),
            order.getId(),
            mollieConfig.getRedirectBaseUrl() + "/shop/order/" + order.getId() + "/return",
            mollieConfig.getWebhookBaseUrl() + "/api/webhooks/mollie"
        ));

        order.setMolliePaymentId(payment.id());
        order.setMollieCheckoutUrl(payment.checkoutUrl());
        return order;
    }
}
```

- [ ] **Step 5: Write `OrderController`**

Create `backend/src/main/java/com/fuorimondo/orders/OrderController.java`:

```java
package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.orders.dto.CreateOrderRequest;
import com.fuorimondo.orders.dto.CreateOrderResponse;
import com.fuorimondo.orders.dto.OrderResponse;
import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.User;
import com.fuorimondo.users.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;
    private final OrderRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper json;

    public OrderController(OrderService service, OrderRepository repository,
                            UserRepository userRepository, ObjectMapper json) {
        this.service = service;
        this.repository = repository;
        this.userRepository = userRepository;
        this.json = json;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateOrderRequest req) {
        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        Order order = service.create(user, req.productId(), req.shippingAddressId());
        return ResponseEntity.status(201).body(new CreateOrderResponse(order.getId(), order.getMollieCheckoutUrl()));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable UUID id) {
        Order o = repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (!o.getUser().getId().equals(principal.getUserId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return OrderResponse.from(o, json);
    }

    @GetMapping
    public Page<OrderResponse> list(@AuthenticationPrincipal CustomUserDetails principal,
                                     Pageable pageable) {
        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        return repository.findByUserOrderByCreatedAtDesc(user, pageable)
            .map(o -> OrderResponse.from(o, json));
    }
}
```

- [ ] **Step 6: Run the test — expect PASS**

```
cd backend && ./mvnw.cmd test "-Dtest=OrderCreationTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```
git add backend/src/main/java/com/fuorimondo/orders/OrderService.java \
        backend/src/main/java/com/fuorimondo/orders/OrderController.java \
        backend/src/main/java/com/fuorimondo/orders/dto/ \
        backend/src/test/java/com/fuorimondo/orders/OrderCreationTest.java
git commit -m "feat(shop): OrderService + POST/GET /api/orders (happy path)"
```

---

## Task 13: OrderService — validation failures tested

**Files:**
- Modify: `backend/src/test/java/com/fuorimondo/orders/OrderCreationTest.java`

- [ ] **Step 1: Add failure tests**

Append to `OrderCreationTest.java`:

```java
@Test
@WithUserDetails("buyer@test")
void rejects_wrong_tier() throws Exception {
    Product t2 = new Product();
    t2.setName("T2"); t2.setPriceEur(new BigDecimal("90"));
    t2.setTiers(EnumSet.of(TierCode.TIER_2));
    t2.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
    t2.setStock(1); t2.setDelivery(true);
    t2 = productRepo.save(t2);

    var body = new CreateOrderRequest(t2.getId(), shipping.getId());
    mvc.perform(post("/api/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(body)))
       .andExpect(status().isForbidden())
       .andExpect(jsonPath("$.code").value("tier_mismatch"));
}

@Test
@WithUserDetails("buyer@test")
void rejects_out_of_stock() throws Exception {
    product.setStock(0);
    productRepo.save(product);

    var body = new CreateOrderRequest(product.getId(), shipping.getId());
    mvc.perform(post("/api/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(body)))
       .andExpect(status().isConflict())
       .andExpect(jsonPath("$.code").value("out_of_stock"));
}

@Test
@WithUserDetails("buyer@test")
void rejects_missing_shipping_for_delivery_product() throws Exception {
    var body = new CreateOrderRequest(product.getId(), null);
    mvc.perform(post("/api/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(body)))
       .andExpect(status().isUnprocessableEntity())
       .andExpect(jsonPath("$.code").value("no_shipping_address"));
}

@Test
@WithUserDetails("buyer@test")
void rejects_sale_window_closed() throws Exception {
    product.setSaleEndAt(Instant.now().minus(1, ChronoUnit.DAYS));
    productRepo.save(product);

    var body = new CreateOrderRequest(product.getId(), shipping.getId());
    mvc.perform(post("/api/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(body)))
       .andExpect(status().isConflict())
       .andExpect(jsonPath("$.code").value("sale_window_closed"));
}
```

- [ ] **Step 2: Run the tests — expect all PASS**

```
cd backend && ./mvnw.cmd test "-Dtest=OrderCreationTest"
```

Expected: PASS (the service already throws `OrderException`; the handler maps correctly).

- [ ] **Step 3: Commit**

```
git add backend/src/test/java/com/fuorimondo/orders/OrderCreationTest.java
git commit -m "test(shop): order creation failure cases"
```

---

## Task 14: Mollie webhook controller

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/payments/MollieWebhookController.java`
- Create: `backend/src/test/java/com/fuorimondo/payments/MollieWebhookTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/fuorimondo/payments/MollieWebhookTest.java`:

```java
package com.fuorimondo.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.addresses.Address;
import com.fuorimondo.addresses.AddressRepository;
import com.fuorimondo.addresses.AddressType;
import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderRepository;
import com.fuorimondo.orders.OrderStatus;
import com.fuorimondo.orders.dto.ProductSnapshot;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MollieWebhookTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired AddressRepository addressRepo;
    @Autowired OrderRepository orderRepo;
    @Autowired MolliePaymentGateway gateway;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    MockMvc mvc;
    Order order;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        User u = new User();
        u.setEmail("hook@test"); u.setFirstName("Hook"); u.setLastName("Test");
        u.setPasswordHash(encoder.encode("Password12")); u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER); u.setTierCode(TierCode.TIER_1);
        u.setCountry("FR"); u.setCity("Paris"); u.setLocale(Locale.FR); u.setCivility(Civility.NONE);
        u = userRepo.save(u);

        Product p = new Product();
        p.setName("HookWine"); p.setPriceEur(new BigDecimal("50"));
        p.setTiers(EnumSet.of(TierCode.TIER_1));
        p.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        p.setStock(1); p.setDelivery(true);
        p = productRepo.save(p);

        order = new Order();
        order.setUser(u); order.setProduct(p);
        order.setUnitPriceEur(p.getPriceEur()); order.setTotalEur(p.getPriceEur());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        order.setMolliePaymentId("tr_test_1");
        order.setMollieCheckoutUrl("http://localhost/fake");
        order.setProductSnapshot(json.writeValueAsString(ProductSnapshot.from(p)));
        order = orderRepo.save(order);
    }

    @Test
    void webhook_paid_transitions_order_to_paid() throws Exception {
        gateway.forceStatus("tr_test_1", MolliePaymentStatus.PAID);

        mvc.perform(post("/api/webhooks/mollie")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .content("id=tr_test_1"))
           .andExpect(status().isOk());

        Order reloaded = orderRepo.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, reloaded.getStatus());
    }

    @Test
    void webhook_unknown_id_returns_200_silently() throws Exception {
        mvc.perform(post("/api/webhooks/mollie")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .content("id=tr_unknown"))
           .andExpect(status().isOk());
    }

    @Test
    void webhook_is_idempotent() throws Exception {
        gateway.forceStatus("tr_test_1", MolliePaymentStatus.PAID);
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/webhooks/mollie")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("id=tr_test_1"))
               .andExpect(status().isOk());
        }
        Order reloaded = orderRepo.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, reloaded.getStatus());
    }
}
```

Note: the webhook is CSRF-excluded so the test does not need a CSRF token. Kept in first test just for completeness — can be removed.

- [ ] **Step 2: Run — expect FAIL**

```
cd backend && ./mvnw.cmd test "-Dtest=MollieWebhookTest"
```

Expected: FAIL (controller missing).

- [ ] **Step 3: Write `MollieWebhookController`**

Create `backend/src/main/java/com/fuorimondo/payments/MollieWebhookController.java`:

```java
package com.fuorimondo.payments;

import com.fuorimondo.email.EmailSender;
import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderRepository;
import com.fuorimondo.orders.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;

@Controller
@RequestMapping("/api/webhooks")
public class MollieWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MollieWebhookController.class);

    private final OrderRepository orderRepository;
    private final MolliePaymentGateway gateway;
    private final EmailSender emailSender;

    public MollieWebhookController(OrderRepository orderRepository,
                                    MolliePaymentGateway gateway,
                                    EmailSender emailSender) {
        this.orderRepository = orderRepository;
        this.gateway = gateway;
        this.emailSender = emailSender;
    }

    @PostMapping("/mollie")
    @ResponseBody
    @Transactional
    public ResponseEntity<Void> mollie(@RequestParam("id") String mollieId) {
        Order order = orderRepository.findByMolliePaymentId(mollieId).orElse(null);
        if (order == null) {
            log.info("Mollie webhook: unknown id {}", mollieId);
            return ResponseEntity.ok().build();
        }
        MolliePayment payment = gateway.getPayment(mollieId);
        applyStatus(order, payment.status());
        return ResponseEntity.ok().build();
    }

    private void applyStatus(Order order, MolliePaymentStatus status) {
        OrderStatus current = order.getStatus();
        if (current != OrderStatus.PENDING_PAYMENT) {
            // Idempotent: already in terminal state
            return;
        }
        switch (status) {
            case PAID -> {
                order.setStatus(OrderStatus.PAID);
                order.setPaidAt(Instant.now());
                safeSendConfirmation(order);
            }
            case FAILED -> order.setStatus(OrderStatus.FAILED);
            case CANCELED -> order.setStatus(OrderStatus.CANCELLED);
            case EXPIRED -> order.setStatus(OrderStatus.EXPIRED);
            default -> { /* OPEN/PENDING/AUTHORIZED -> no transition */ }
        }
    }

    private void safeSendConfirmation(Order order) {
        try {
            emailSender.sendOrderConfirmation(
                order.getUser().getEmail(),
                order.getUser().getFirstName(),
                order.getUser().getLocale(),
                order.getId().toString(),
                order.getTotalEur());
        } catch (Exception e) {
            log.warn("Failed to send order confirmation email for order {}: {}",
                order.getId(), e.getMessage());
        }
    }
}
```

- [ ] **Step 4: Extend `EmailSender` interface + impl**

Edit `backend/src/main/java/com/fuorimondo/email/EmailSender.java`, add:

```java
void sendOrderConfirmation(String to, String firstName,
                            com.fuorimondo.users.Locale locale,
                            String orderId, java.math.BigDecimal totalEur);
```

Edit `backend/src/main/java/com/fuorimondo/email/ConsoleEmailSender.java`, add the implementation:

```java
@Override
public void sendOrderConfirmation(String to, String firstName, com.fuorimondo.users.Locale locale,
                                   String orderId, java.math.BigDecimal totalEur) {
    log.info("\n==== EMAIL ====\nTO: {}\nSUBJECT: Fuori Marmo — Confirmation de votre commande\nLOCALE: {}\nBODY:\nBonjour {},\nVotre commande {} ({} EUR) est confirmée.\n====",
        to, locale, firstName, orderId, totalEur);
}
```

- [ ] **Step 5: Run — expect PASS**

```
cd backend && ./mvnw.cmd test "-Dtest=MollieWebhookTest"
```

Expected: PASS (3 tests green).

- [ ] **Step 6: Commit**

```
git add backend/src/main/java/com/fuorimondo/payments/MollieWebhookController.java \
        backend/src/main/java/com/fuorimondo/email/ \
        backend/src/test/java/com/fuorimondo/payments/MollieWebhookTest.java
git commit -m "feat(shop): Mollie webhook + email on PAID transition"
```

---

## Task 15: Dev-only simulate webhook endpoint

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/payments/DevSimulateWebhookController.java`
- Create: `backend/src/test/java/com/fuorimondo/payments/DevSimulateWebhookTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/fuorimondo/payments/DevSimulateWebhookTest.java`:

```java
package com.fuorimondo.payments;

import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderRepository;
import com.fuorimondo.orders.OrderStatus;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Transactional
class DevSimulateWebhookTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired OrderRepository orderRepo;
    @Autowired MolliePaymentGateway gateway;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;

    @Test
    @WithUserDetails("simu@test")
    void simulate_paid_transitions_status() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        User u = new User();
        u.setEmail("simu@test"); u.setFirstName("S"); u.setLastName("S");
        u.setPasswordHash(encoder.encode("Password12"));
        u.setStatus(UserStatus.ALLOCATAIRE); u.setRole(UserRole.USER);
        u.setTierCode(TierCode.TIER_1); u.setCountry("FR"); u.setCity("P");
        u.setLocale(Locale.FR); u.setCivility(Civility.NONE);
        u = userRepo.save(u);

        Product p = new Product();
        p.setName("Sim"); p.setPriceEur(new BigDecimal("10"));
        p.setTiers(EnumSet.of(TierCode.TIER_1));
        p.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        p.setDelivery(false);
        p = productRepo.save(p);

        Order o = new Order();
        o.setUser(u); o.setProduct(p);
        o.setUnitPriceEur(p.getPriceEur()); o.setTotalEur(p.getPriceEur());
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        o.setMolliePaymentId("tr_sim_" + java.util.UUID.randomUUID());
        o.setProductSnapshot("{}");
        o = orderRepo.save(o);

        mvc.perform(post("/api/dev/orders/" + o.getId() + "/simulate-webhook?status=paid"))
           .andExpect(status().isOk());

        assertEquals(OrderStatus.PAID, orderRepo.findById(o.getId()).orElseThrow().getStatus());
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```
cd backend && ./mvnw.cmd test "-Dtest=DevSimulateWebhookTest"
```

Expected: FAIL (404).

- [ ] **Step 3: Write `DevSimulateWebhookController`**

Create `backend/src/main/java/com/fuorimondo/payments/DevSimulateWebhookController.java`:

```java
package com.fuorimondo.payments;

import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dev/orders")
@Profile("dev")
public class DevSimulateWebhookController {

    private final OrderRepository orderRepository;
    private final MolliePaymentGateway gateway;
    private final RestClient internal;

    public DevSimulateWebhookController(OrderRepository orderRepository,
                                         MolliePaymentGateway gateway) {
        this.orderRepository = orderRepository;
        this.gateway = gateway;
        this.internal = RestClient.create();
    }

    @PostMapping("/{id}/simulate-webhook")
    public ResponseEntity<Void> simulate(@PathVariable UUID id,
                                          @RequestParam String status,
                                          jakarta.servlet.http.HttpServletRequest request) {
        Order o = orderRepository.findById(id).orElseThrow();
        MolliePaymentStatus mstatus = switch (status.toLowerCase(Locale.ROOT)) {
            case "paid" -> MolliePaymentStatus.PAID;
            case "failed" -> MolliePaymentStatus.FAILED;
            case "cancelled", "canceled" -> MolliePaymentStatus.CANCELED;
            case "expired" -> MolliePaymentStatus.EXPIRED;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
        gateway.forceStatus(o.getMolliePaymentId(), mstatus);

        // Trigger the real webhook handler so the transition + email logic runs
        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        internal.post().uri(base + "/api/webhooks/mollie")
            .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
            .body(Map.of("id", o.getMolliePaymentId()))
            .retrieve().toBodilessEntity();

        return ResponseEntity.ok().build();
    }
}
```

Note: the test at step 1 uses `@ActiveProfiles({"test", "dev"})` to activate the `dev` profile for the duration of that test, so `@Profile("dev")` matches and the bean is created. Production (non-dev profiles) never sees this controller.

- [ ] **Step 4: Run — expect PASS**

```
cd backend && ./mvnw.cmd test "-Dtest=DevSimulateWebhookTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```
git add backend/src/main/java/com/fuorimondo/payments/DevSimulateWebhookController.java \
        backend/src/test/java/com/fuorimondo/payments/DevSimulateWebhookTest.java
git commit -m "feat(shop): dev simulate-webhook endpoint"
```

---

## Task 16: Expiration job

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/orders/OrderExpirationJob.java`
- Create: `backend/src/test/java/com/fuorimondo/orders/OrderExpirationJobTest.java`
- Modify: `backend/src/main/java/com/fuorimondo/FuoriMondoApplication.java` (add `@EnableScheduling`)

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/fuorimondo/orders/OrderExpirationJobTest.java`:

```java
package com.fuorimondo.orders;

import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderExpirationJobTest {

    @Autowired OrderRepository orderRepo;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired OrderExpirationJob job;
    @Autowired PasswordEncoder encoder;

    @Test
    void sweeps_expired_pending_orders_to_expired() {
        User u = new User();
        u.setEmail("exp@test"); u.setFirstName("E"); u.setLastName("E");
        u.setPasswordHash(encoder.encode("Password12"));
        u.setStatus(UserStatus.ALLOCATAIRE); u.setRole(UserRole.USER);
        u.setTierCode(TierCode.TIER_1); u.setCountry("FR"); u.setCity("P");
        u.setLocale(Locale.FR); u.setCivility(Civility.NONE);
        u = userRepo.save(u);

        Product p = new Product();
        p.setName("EXP"); p.setPriceEur(new BigDecimal("10"));
        p.setTiers(EnumSet.of(TierCode.TIER_1));
        p.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        p.setDelivery(false);
        p = productRepo.save(p);

        Order stale = new Order();
        stale.setUser(u); stale.setProduct(p);
        stale.setUnitPriceEur(p.getPriceEur()); stale.setTotalEur(p.getPriceEur());
        stale.setStatus(OrderStatus.PENDING_PAYMENT);
        stale.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        stale.setMolliePaymentId("tr_stale");
        stale.setProductSnapshot("{}");
        stale = orderRepo.save(stale);

        Order fresh = new Order();
        fresh.setUser(u); fresh.setProduct(p);
        fresh.setUnitPriceEur(p.getPriceEur()); fresh.setTotalEur(p.getPriceEur());
        fresh.setStatus(OrderStatus.PENDING_PAYMENT);
        fresh.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        fresh.setMolliePaymentId("tr_fresh");
        fresh.setProductSnapshot("{}");
        fresh = orderRepo.save(fresh);

        int swept = job.run();

        assertEquals(1, swept);
        assertEquals(OrderStatus.EXPIRED, orderRepo.findById(stale.getId()).orElseThrow().getStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT, orderRepo.findById(fresh.getId()).orElseThrow().getStatus());
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```
cd backend && ./mvnw.cmd test "-Dtest=OrderExpirationJobTest"
```

Expected: FAIL (class `OrderExpirationJob` does not exist).

- [ ] **Step 3: Write `OrderExpirationJob`**

Create `backend/src/main/java/com/fuorimondo/orders/OrderExpirationJob.java`:

```java
package com.fuorimondo.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OrderExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationJob.class);

    private final OrderRepository orderRepository;

    public OrderExpirationJob(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedDelayString = "PT${fuorimondo.order.expiration-job-interval-minutes:5}M")
    @Transactional
    public int run() {
        int swept = orderRepository.expireStaleOrders(Instant.now());
        if (swept > 0) log.info("Expired {} stale orders", swept);
        return swept;
    }
}
```

- [ ] **Step 4: Add `@EnableScheduling`**

Edit `backend/src/main/java/com/fuorimondo/FuoriMondoApplication.java`. Add import:

```java
import org.springframework.scheduling.annotation.EnableScheduling;
```

Add annotation on the class:

```java
@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class FuoriMondoApplication {
```

- [ ] **Step 5: Run — expect PASS**

```
cd backend && ./mvnw.cmd test "-Dtest=OrderExpirationJobTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```
git add backend/src/main/java/com/fuorimondo/orders/OrderExpirationJob.java \
        backend/src/main/java/com/fuorimondo/FuoriMondoApplication.java \
        backend/src/test/java/com/fuorimondo/orders/OrderExpirationJobTest.java
git commit -m "feat(shop): @Scheduled expiration job"
```

---

## Task 17: AdminOrderController

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/orders/AdminOrderController.java`
- Create: `backend/src/main/java/com/fuorimondo/orders/dto/AdminOrderResponse.java`
- Create: `backend/src/test/java/com/fuorimondo/orders/AdminOrdersControllerTest.java`

- [ ] **Step 1: Write `AdminOrderResponse`**

Create `backend/src/main/java/com/fuorimondo/orders/dto/AdminOrderResponse.java`:

```java
package com.fuorimondo.orders.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderResponse(
    UUID id,
    UUID userId,
    String userEmail,
    String userFirstName,
    String userLastName,
    ProductSnapshot product,
    BigDecimal totalEur,
    ShippingSnapshot shipping,
    OrderStatus status,
    String molliePaymentId,
    Instant createdAt,
    Instant paidAt
) {
    public static AdminOrderResponse from(Order o, ObjectMapper mapper) {
        try {
            ProductSnapshot prod = mapper.readValue(o.getProductSnapshot(), ProductSnapshot.class);
            ShippingSnapshot ship = o.getShippingSnapshot() == null
                ? null : mapper.readValue(o.getShippingSnapshot(), ShippingSnapshot.class);
            return new AdminOrderResponse(
                o.getId(), o.getUser().getId(), o.getUser().getEmail(),
                o.getUser().getFirstName(), o.getUser().getLastName(),
                prod, o.getTotalEur(), ship, o.getStatus(),
                o.getMolliePaymentId(), o.getCreatedAt(), o.getPaidAt());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize order snapshots", e);
        }
    }
}
```

- [ ] **Step 2: Write the failing test**

Create `backend/src/test/java/com/fuorimondo/orders/AdminOrdersControllerTest.java`:

```java
package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOrdersControllerTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired OrderRepository orderRepo;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        User admin = new User();
        admin.setEmail("admin@ord"); admin.setFirstName("Ad"); admin.setLastName("Min");
        admin.setPasswordHash(encoder.encode("Password12"));
        admin.setStatus(UserStatus.ALLOCATAIRE); admin.setRole(UserRole.ADMIN);
        admin.setCountry("FR"); admin.setCity("P");
        admin.setLocale(Locale.FR); admin.setCivility(Civility.NONE);
        userRepo.save(admin);

        User allo = new User();
        allo.setEmail("allo@ord"); allo.setFirstName("A"); allo.setLastName("A");
        allo.setPasswordHash(encoder.encode("Password12"));
        allo.setStatus(UserStatus.ALLOCATAIRE); allo.setRole(UserRole.USER);
        allo.setTierCode(TierCode.TIER_1); allo.setCountry("FR"); allo.setCity("P");
        allo.setLocale(Locale.FR); allo.setCivility(Civility.NONE);
        allo = userRepo.save(allo);

        Product p = new Product();
        p.setName("X"); p.setPriceEur(new BigDecimal("1"));
        p.setTiers(EnumSet.of(TierCode.TIER_1));
        p.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        p.setDelivery(false);
        p = productRepo.save(p);

        Order o = new Order();
        o.setUser(allo); o.setProduct(p);
        o.setUnitPriceEur(p.getPriceEur()); o.setTotalEur(p.getPriceEur());
        o.setStatus(OrderStatus.PAID); o.setPaidAt(Instant.now());
        o.setMolliePaymentId("tr_adm_1");
        o.setProductSnapshot("{\"id\":\"" + p.getId() + "\",\"name\":\"X\",\"priceEur\":\"1.00\",\"tiers\":[\"TIER_1\"],\"delivery\":false}");
        orderRepo.save(o);
    }

    @Test
    @WithUserDetails("admin@ord")
    void admin_lists_all_orders() throws Exception {
        mvc.perform(get("/api/admin/orders"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content.length()").value(1))
           .andExpect(jsonPath("$.content[0].status").value("PAID"));
    }

    @Test
    @WithUserDetails("allo@ord")
    void allocataire_forbidden() throws Exception {
        mvc.perform(get("/api/admin/orders"))
           .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 3: Run — expect FAIL**

```
cd backend && ./mvnw.cmd test "-Dtest=AdminOrdersControllerTest"
```

Expected: FAIL.

- [ ] **Step 4: Write `AdminOrderController`**

Create `backend/src/main/java/com/fuorimondo/orders/AdminOrderController.java`:

```java
package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.orders.dto.AdminOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderRepository repository;
    private final ObjectMapper json;

    public AdminOrderController(OrderRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @GetMapping
    public Page<AdminOrderResponse> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable)
            .map(o -> AdminOrderResponse.from(o, json));
    }

    @GetMapping("/{id}")
    public AdminOrderResponse detail(@PathVariable UUID id) {
        Order o = repository.findById(id).orElseThrow();
        return AdminOrderResponse.from(o, json);
    }
}
```

- [ ] **Step 5: Run — expect PASS**

```
cd backend && ./mvnw.cmd test "-Dtest=AdminOrdersControllerTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```
git add backend/src/main/java/com/fuorimondo/orders/AdminOrderController.java \
        backend/src/main/java/com/fuorimondo/orders/dto/AdminOrderResponse.java \
        backend/src/test/java/com/fuorimondo/orders/AdminOrdersControllerTest.java
git commit -m "feat(shop): admin orders dashboard endpoints"
```

---

## Task 18: Frontend API types + i18n keys

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/i18n/fr.ts`
- Modify: `frontend/src/i18n/it.ts`
- Modify: `frontend/src/i18n/en.ts`

- [ ] **Step 1: Add types**

Append to `frontend/src/api/types.ts`:

```typescript
export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'FAILED' | 'CANCELLED' | 'EXPIRED';

export interface PublicProductResponse {
  id: string;
  name: string;
  description: string | null;
  priceEur: string;
  photoFilename: string | null;
  weightKg: string | null;
  delivery: boolean;
  tiers: TierCode[];
  saleStartAt: string;
  saleEndAt: string | null;
  stockRemaining: number | null;
}

export interface ProductSnapshot {
  id: string;
  name: string;
  description: string | null;
  priceEur: string;
  photoFilename: string | null;
  weightKg: string | null;
  delivery: boolean;
  tiers: TierCode[];
}

export interface ShippingSnapshot {
  fullName: string;
  street: string;
  streetExtra: string | null;
  postalCode: string;
  city: string;
  country: string;
}

export interface CreateOrderRequest { productId: string; shippingAddressId?: string | null; }
export interface CreateOrderResponse { orderId: string; checkoutUrl: string; }

export interface OrderResponse {
  id: string;
  product: ProductSnapshot;
  unitPriceEur: string;
  totalEur: string;
  shippingAddress: ShippingSnapshot | null;
  status: OrderStatus;
  mollieCheckoutUrl: string | null;
  expiresAt: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface AdminOrderResponse {
  id: string;
  userId: string;
  userEmail: string;
  userFirstName: string;
  userLastName: string;
  product: ProductSnapshot;
  totalEur: string;
  shipping: ShippingSnapshot | null;
  status: OrderStatus;
  molliePaymentId: string | null;
  createdAt: string;
  paidAt: string | null;
}
```

- [ ] **Step 2: Add i18n keys to `fr.ts`**

In `frontend/src/i18n/fr.ts`, insert the following top-level sections (after `admin: { … }` is fine):

```typescript
shop: {
  title: 'Boutique',
  empty: 'Aucun produit disponible pour vous actuellement.',
  outOfStock: 'Épuisé',
  limitedStock: 'Plus que {n}',
  delivery: 'Livraison incluse',
  digital: 'Accès / bon numérique',
  availableUntil: 'Disponible jusqu\'au {date}',
  buyCta: 'Acheter — {price}',
  resumePayment: 'Reprendre le paiement en cours',
  weight: '{kg} kg',
},
checkout: {
  title: 'Confirmation d\'achat',
  summary: 'Récapitulatif',
  shippingAddress: 'Adresse de livraison',
  noShippingAddress: 'Ajoute une adresse de livraison avant de commander.',
  addAddressLink: 'Gérer mes adresses',
  acceptCgv: 'J\'accepte les conditions générales de vente',
  payCta: 'Payer {price}',
},
orderReturn: {
  paid: 'Commande confirmée',
  paidMessage: 'Un email de confirmation t\'a été envoyé.',
  failed: 'Paiement échoué',
  cancelled: 'Paiement annulé',
  expired: 'Délai de paiement dépassé',
  timeout: 'Paiement en cours de traitement. Consulte tes commandes dans quelques instants.',
  viewOrder: 'Voir la commande',
  retry: 'Réessayer',
  backToShop: 'Retour à la boutique',
  simulateTitle: '(Dev) Simuler un retour Mollie',
},
order: {
  title: 'Mes commandes',
  empty: 'Aucune commande',
  orderId: 'Commande',
  paidAt: 'Payée le',
  mollieRef: 'Référence Mollie',
  statusPendingPayment: 'Paiement en attente',
  statusPaid: 'Payée',
  statusFailed: 'Échouée',
  statusCancelled: 'Annulée',
  statusExpired: 'Expirée',
},
errors: {
  outOfStock: 'Ce produit vient d\'être épuisé.',
  saleWindowClosed: 'Ce produit n\'est plus disponible à la vente.',
  tierMismatch: 'Ce produit n\'est pas ouvert à votre cercle.',
  noShippingAddress: 'Ajoute une adresse de livraison avant de commander.',
  paymentError: 'Erreur de paiement. Réessaie dans un instant.',
},
```

Also add a nav link: in the `nav` block, add `shop: 'Boutique'`, `myOrders: 'Mes commandes'`, `adminOrders: 'Commandes'`.

- [ ] **Step 3: Add equivalent keys in `it.ts` and `en.ts`**

Italian (`it.ts`):

```typescript
shop: {
  title: 'Negozio', empty: 'Nessun prodotto disponibile al momento.',
  outOfStock: 'Esaurito', limitedStock: 'Restano {n}',
  delivery: 'Spedizione inclusa', digital: 'Accesso / bono digitale',
  availableUntil: 'Disponibile fino al {date}',
  buyCta: 'Acquista — {price}', resumePayment: 'Riprendi il pagamento in corso',
  weight: '{kg} kg',
},
checkout: {
  title: 'Conferma d\'acquisto', summary: 'Riepilogo',
  shippingAddress: 'Indirizzo di spedizione',
  noShippingAddress: 'Aggiungi un indirizzo di spedizione prima di ordinare.',
  addAddressLink: 'Gestisci i miei indirizzi',
  acceptCgv: 'Accetto le condizioni generali di vendita',
  payCta: 'Paga {price}',
},
orderReturn: {
  paid: 'Ordine confermato',
  paidMessage: 'Ti abbiamo inviato un\'email di conferma.',
  failed: 'Pagamento fallito', cancelled: 'Pagamento annullato',
  expired: 'Termine di pagamento scaduto',
  timeout: 'Pagamento in corso di elaborazione. Controlla i tuoi ordini tra qualche istante.',
  viewOrder: 'Vedi l\'ordine', retry: 'Riprova', backToShop: 'Torna al negozio',
  simulateTitle: '(Dev) Simula un ritorno Mollie',
},
order: {
  title: 'I miei ordini', empty: 'Nessun ordine',
  orderId: 'Ordine', paidAt: 'Pagato il', mollieRef: 'Riferimento Mollie',
  statusPendingPayment: 'Pagamento in attesa', statusPaid: 'Pagato',
  statusFailed: 'Fallito', statusCancelled: 'Annullato', statusExpired: 'Scaduto',
},
errors: {
  outOfStock: 'Questo prodotto è appena stato esaurito.',
  saleWindowClosed: 'Questo prodotto non è più disponibile per l\'acquisto.',
  tierMismatch: 'Questo prodotto non è aperto al tuo cerchio.',
  noShippingAddress: 'Aggiungi un indirizzo di spedizione prima di ordinare.',
  paymentError: 'Errore di pagamento. Riprova tra un istante.',
},
```

English (`en.ts`):

```typescript
shop: {
  title: 'Shop', empty: 'No product available for you at this time.',
  outOfStock: 'Out of stock', limitedStock: 'Only {n} left',
  delivery: 'Shipping included', digital: 'Digital access / voucher',
  availableUntil: 'Available until {date}',
  buyCta: 'Buy — {price}', resumePayment: 'Resume pending payment',
  weight: '{kg} kg',
},
checkout: {
  title: 'Purchase confirmation', summary: 'Summary',
  shippingAddress: 'Shipping address',
  noShippingAddress: 'Add a shipping address before ordering.',
  addAddressLink: 'Manage my addresses',
  acceptCgv: 'I accept the terms of sale',
  payCta: 'Pay {price}',
},
orderReturn: {
  paid: 'Order confirmed',
  paidMessage: 'A confirmation email has been sent to you.',
  failed: 'Payment failed', cancelled: 'Payment cancelled',
  expired: 'Payment window expired',
  timeout: 'Payment is being processed. Check your orders in a moment.',
  viewOrder: 'View order', retry: 'Retry', backToShop: 'Back to shop',
  simulateTitle: '(Dev) Simulate a Mollie return',
},
order: {
  title: 'My orders', empty: 'No orders',
  orderId: 'Order', paidAt: 'Paid at', mollieRef: 'Mollie reference',
  statusPendingPayment: 'Payment pending', statusPaid: 'Paid',
  statusFailed: 'Failed', statusCancelled: 'Cancelled', statusExpired: 'Expired',
},
errors: {
  outOfStock: 'This product just went out of stock.',
  saleWindowClosed: 'This product is no longer available for purchase.',
  tierMismatch: 'This product is not open to your circle.',
  noShippingAddress: 'Add a shipping address before ordering.',
  paymentError: 'Payment error. Please try again in a moment.',
},
```

Add nav keys: `shop`, `myOrders`, `adminOrders` with locale-appropriate labels.

- [ ] **Step 4: Typecheck frontend**

```
cd frontend && npm run build
```

Expected: BUILD SUCCESS (vue-tsc clean, no missing keys).

- [ ] **Step 5: Commit**

```
git add frontend/src/api/types.ts frontend/src/i18n/
git commit -m "feat(shop): frontend types + i18n keys (FR/IT/EN)"
```

---

## Task 19: Router + sidebar links + OrderStatusBadge

**Files:**
- Modify: `frontend/src/router.ts`
- Modify: `frontend/src/components/AppLayout.vue`
- Create: `frontend/src/components/OrderStatusBadge.vue`

- [ ] **Step 1: Register the 8 routes**

In `frontend/src/router.ts`, add inside the `routes` array (before the catch-all):

```typescript
{ path: '/shop', name: 'shop', component: () => import('./views/ShopView.vue') },
{ path: '/shop/:id', name: 'shop-product', component: () => import('./views/ShopProductView.vue') },
{ path: '/shop/checkout/:productId', name: 'shop-checkout', component: () => import('./views/ShopCheckoutView.vue') },
{ path: '/shop/order/:id/return', name: 'shop-return', component: () => import('./views/ShopReturnView.vue') },
{ path: '/orders', name: 'my-orders', component: () => import('./views/MyOrdersView.vue') },
{ path: '/orders/:id', name: 'order-detail', component: () => import('./views/OrderDetailView.vue') },
{ path: '/admin/orders', name: 'admin-orders', component: () => import('./views/admin/AdminOrdersView.vue'), meta: { admin: true } },
{ path: '/admin/orders/:id', name: 'admin-order-detail', component: () => import('./views/admin/AdminOrderDetailView.vue'), meta: { admin: true } },
```

- [ ] **Step 2: Add sidebar links in AppLayout**

Edit `frontend/src/components/AppLayout.vue`. In the `<nav>` block, inside the `auth.isAuthenticated` template, after the Home link:

```vue
<router-link v-if="auth.isAllocataire" to="/shop" class="block py-2 text-base">{{ t('nav.shop') }}</router-link>
<router-link to="/orders" class="block py-2 text-base">{{ t('nav.myOrders') }}</router-link>
```

Inside the admin block (after `adminUsers` link):

```vue
<router-link to="/admin/orders" class="block py-2 text-base">{{ t('nav.adminOrders') }}</router-link>
```

- [ ] **Step 3: Create OrderStatusBadge**

Create `frontend/src/components/OrderStatusBadge.vue`:

```vue
<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import type { OrderStatus } from '../api/types';

const props = defineProps<{ status: OrderStatus }>();
const { t } = useI18n();

const classes = computed(() => {
  switch (props.status) {
    case 'PAID': return 'bg-green-100 text-green-800 border-green-300';
    case 'PENDING_PAYMENT': return 'bg-fm-stone text-fm-black/70 border-fm-black/20';
    case 'FAILED': return 'bg-red-100 text-fm-red border-fm-red/40';
    case 'CANCELLED': return 'bg-fm-stone text-fm-red border-fm-red/30';
    case 'EXPIRED': return 'bg-fm-stone text-fm-black/40 border-fm-black/20';
  }
});

const label = computed(() => {
  switch (props.status) {
    case 'PAID': return t('order.statusPaid');
    case 'PENDING_PAYMENT': return t('order.statusPendingPayment');
    case 'FAILED': return t('order.statusFailed');
    case 'CANCELLED': return t('order.statusCancelled');
    case 'EXPIRED': return t('order.statusExpired');
  }
});
</script>

<template>
  <span :class="['inline-block px-2 py-0.5 text-xs uppercase tracking-widest border rounded', classes]">
    {{ label }}
  </span>
</template>
```

- [ ] **Step 4: Create placeholder view files**

Vite lazy imports tolerate missing files at build time, but `vue-tsc` does not. Create 8 placeholders — each file contains only:

```vue
<template><div class="fm-page">TODO</div></template>
```

Paths:
- `frontend/src/views/ShopView.vue`
- `frontend/src/views/ShopProductView.vue`
- `frontend/src/views/ShopCheckoutView.vue`
- `frontend/src/views/ShopReturnView.vue`
- `frontend/src/views/MyOrdersView.vue`
- `frontend/src/views/OrderDetailView.vue`
- `frontend/src/views/admin/AdminOrdersView.vue`
- `frontend/src/views/admin/AdminOrderDetailView.vue`

These files are replaced with real content in subsequent tasks.

- [ ] **Step 5: Typecheck**

```
cd frontend && npm run build
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```
git add frontend/src/router.ts frontend/src/components/AppLayout.vue \
        frontend/src/components/OrderStatusBadge.vue frontend/src/views/
git commit -m "feat(shop): routes + sidebar links + OrderStatusBadge + view stubs"
```

---

## Task 20: ShopView

**Files:**
- Replace: `frontend/src/views/ShopView.vue`

- [ ] **Step 1: Write `ShopView.vue`**

Replace `frontend/src/views/ShopView.vue`:

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { PublicProductResponse } from '../api/types';
import TierBadge from '../components/TierBadge.vue';

const { t, locale } = useI18n();
const router = useRouter();
const products = ref<PublicProductResponse[]>([]);
const loading = ref(true);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), {
    style: 'currency', currency: 'EUR',
  }).format(Number(eur));
}

async function load() {
  loading.value = true;
  try {
    products.value = await api.get<PublicProductResponse[]>('/products');
  } finally {
    loading.value = false;
  }
}

function photoUrl(filename: string | null): string {
  return filename ? `/api/admin/products/photos/${filename}` : '';
}

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <h2 class="text-2xl mb-6 font-serif italic">{{ t('shop.title') }}</h2>
    <p v-if="loading" class="text-sm">{{ t('common.loading') }}</p>
    <p v-else-if="products.length === 0" class="text-sm text-fm-black/60">{{ t('shop.empty') }}</p>
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 desk:grid-cols-3 gap-6">
      <button
        v-for="p in products" :key="p.id"
        class="text-left bg-fm-white border border-fm-black/10 rounded overflow-hidden hover:border-fm-black/40 transition"
        @click="router.push({ name: 'shop-product', params: { id: p.id } })"
        :data-testid="`product-card-${p.id}`"
      >
        <div class="aspect-[4/3] bg-fm-stone flex items-center justify-center">
          <img v-if="p.photoFilename" :src="photoUrl(p.photoFilename)" :alt="p.name" class="w-full h-full object-cover" />
          <span v-else class="text-xs text-fm-black/30">—</span>
        </div>
        <div class="p-4 space-y-2">
          <h3 class="font-serif italic text-lg">{{ p.name }}</h3>
          <p class="font-logo text-xl">{{ fmtPrice(p.priceEur) }}</p>
          <div class="flex flex-wrap gap-1">
            <TierBadge v-for="tier in p.tiers" :key="tier" :tier="tier" />
          </div>
          <div class="text-xs text-fm-black/60 space-y-0.5">
            <p v-if="p.stockRemaining !== null && p.stockRemaining <= 3">{{ t('shop.limitedStock', { n: p.stockRemaining }) }}</p>
            <p v-if="p.delivery">{{ t('shop.delivery') }}</p>
            <p v-else>{{ t('shop.digital') }}</p>
          </div>
        </div>
      </button>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Start frontend + backend, visual check**

Terminal 1:
```
cd backend && ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Terminal 2:
```
cd frontend && npm run dev
```

Browse to http://localhost:5273/shop after logging in as the dev admin (`admin@fuorimondo.local` / `Admin!Password123`). Since the admin has no `tierCode`, the page should show `shop.empty`. Create a test user via admin and assign them a tier, then log in as that user to see products. Stop both servers.

- [ ] **Step 3: Typecheck**

```
cd frontend && npm run build
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```
git add frontend/src/views/ShopView.vue
git commit -m "feat(shop): ShopView — responsive product grid"
```

---

## Task 21: ShopProductView

**Files:**
- Replace: `frontend/src/views/ShopProductView.vue`

- [ ] **Step 1: Write the view**

Replace `frontend/src/views/ShopProductView.vue`:

```vue
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../api/client';
import type { PublicProductResponse, OrderResponse } from '../api/types';
import FmButton from '../components/FmButton.vue';
import TierBadge from '../components/TierBadge.vue';

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();

const product = ref<PublicProductResponse | null>(null);
const pendingOrder = ref<OrderResponse | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

const disabled = computed(() => {
  const p = product.value;
  if (!p) return true;
  if (p.stockRemaining !== null && p.stockRemaining <= 0) return true;
  return false;
});

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString(locale.value.toLowerCase(), { day: '2-digit', month: 'long', year: 'numeric' });
}
function photoUrl(filename: string | null): string {
  return filename ? `/api/admin/products/photos/${filename}` : '';
}

async function load() {
  loading.value = true; error.value = null;
  try {
    product.value = await api.get<PublicProductResponse>(`/products/${route.params.id}`);
    // Check for an existing pending payment by this user on this product
    const myOrders = await api.get<{ content: OrderResponse[] }>(`/orders?size=10&sort=createdAt,desc`);
    const existing = myOrders.content.find(o => o.status === 'PENDING_PAYMENT' && o.product.id === product.value!.id);
    pendingOrder.value = existing ?? null;
  } catch (e) {
    if (e instanceof ApiException && e.status === 404) error.value = t('errors.tierMismatch');
    else error.value = t('common.error');
  } finally {
    loading.value = false;
  }
}

function resumePayment() {
  if (pendingOrder.value?.mollieCheckoutUrl) {
    window.location.href = pendingOrder.value.mollieCheckoutUrl;
  }
}

onMounted(load);
</script>

<template>
  <div class="fm-page max-w-2xl">
    <p v-if="loading" class="text-sm">{{ t('common.loading') }}</p>
    <div v-else-if="error" class="text-sm text-fm-red">{{ error }}</div>
    <div v-else-if="product" class="space-y-6">
      <div class="aspect-[4/3] bg-fm-stone">
        <img v-if="product.photoFilename" :src="photoUrl(product.photoFilename)" :alt="product.name" class="w-full h-full object-cover" />
      </div>
      <h1 class="font-serif italic text-3xl">{{ product.name }}</h1>
      <p class="font-logo text-2xl">{{ fmtPrice(product.priceEur) }}</p>
      <div class="flex gap-2 flex-wrap">
        <TierBadge v-for="tier in product.tiers" :key="tier" :tier="tier" />
      </div>
      <p v-if="product.description" class="font-serif text-fm-black/80 whitespace-pre-line">{{ product.description }}</p>
      <div class="text-sm text-fm-black/60 space-y-1">
        <p v-if="product.saleEndAt">{{ t('shop.availableUntil', { date: fmtDate(product.saleEndAt) }) }}</p>
        <p v-if="product.delivery && product.weightKg">{{ t('shop.delivery') }} — {{ t('shop.weight', { kg: product.weightKg }) }}</p>
        <p v-else-if="product.delivery">{{ t('shop.delivery') }}</p>
        <p v-else>{{ t('shop.digital') }}</p>
        <p v-if="product.stockRemaining !== null && product.stockRemaining <= 3">{{ t('shop.limitedStock', { n: product.stockRemaining }) }}</p>
      </div>
      <div class="pt-4">
        <FmButton v-if="pendingOrder" block variant="secondary" @click="resumePayment" data-testid="resume-payment">
          {{ t('shop.resumePayment') }}
        </FmButton>
        <FmButton v-else block variant="primary" :disabled="disabled"
                  @click="router.push({ name: 'shop-checkout', params: { productId: product.id } })"
                  data-testid="buy-cta">
          {{ disabled ? t('shop.outOfStock') : t('shop.buyCta', { price: fmtPrice(product.priceEur) }) }}
        </FmButton>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Typecheck**

```
cd frontend && npm run build
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```
git add frontend/src/views/ShopProductView.vue
git commit -m "feat(shop): ShopProductView — detail + buy CTA + resume payment"
```

---

## Task 22: ShopCheckoutView

**Files:**
- Replace: `frontend/src/views/ShopCheckoutView.vue`

- [ ] **Step 1: Write the view**

Replace `frontend/src/views/ShopCheckoutView.vue`:

```vue
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../api/client';
import type { PublicProductResponse, AddressResponse, CreateOrderResponse } from '../api/types';
import FmButton from '../components/FmButton.vue';
import FmSelect from '../components/FmSelect.vue';
import FmCheckbox from '../components/FmCheckbox.vue';

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();

const product = ref<PublicProductResponse | null>(null);
const addresses = ref<AddressResponse[]>([]);
const selectedAddressId = ref<string>('');
const acceptCgv = ref(false);
const busy = ref(false);
const error = ref<string | null>(null);
const loading = ref(true);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}

const addressOptions = computed(() =>
  addresses.value.map(a => ({ value: a.id, label: `${a.fullName} — ${a.street}, ${a.postalCode} ${a.city}` }))
);

const canPay = computed(() => {
  if (!acceptCgv.value || !product.value) return false;
  if (product.value.delivery && !selectedAddressId.value) return false;
  return true;
});

async function load() {
  loading.value = true;
  try {
    product.value = await api.get<PublicProductResponse>(`/products/${route.params.productId}`);
    if (product.value.delivery) {
      addresses.value = await api.get<AddressResponse[]>('/me/addresses?type=SHIPPING');
      const defaultOne = addresses.value.find(a => a.isDefault);
      selectedAddressId.value = (defaultOne ?? addresses.value[0])?.id ?? '';
    }
  } finally {
    loading.value = false;
  }
}

async function pay() {
  if (!product.value) return;
  busy.value = true; error.value = null;
  try {
    const res = await api.post<CreateOrderResponse>('/orders', {
      productId: product.value.id,
      shippingAddressId: product.value.delivery ? selectedAddressId.value : null,
    });
    window.location.href = res.checkoutUrl;
  } catch (e) {
    if (e instanceof ApiException) {
      switch (e.payload?.code) {
        case 'out_of_stock': error.value = t('errors.outOfStock'); break;
        case 'sale_window_closed': error.value = t('errors.saleWindowClosed'); break;
        case 'tier_mismatch': error.value = t('errors.tierMismatch'); break;
        case 'no_shipping_address': error.value = t('errors.noShippingAddress'); break;
        default: error.value = t('errors.paymentError');
      }
    } else {
      error.value = t('errors.paymentError');
    }
  } finally {
    busy.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="fm-page max-w-lg">
    <h2 class="text-2xl mb-6 font-serif italic">{{ t('checkout.title') }}</h2>
    <p v-if="loading">{{ t('common.loading') }}</p>
    <div v-else-if="product" class="space-y-6">
      <section class="fm-card space-y-2">
        <p class="text-xs uppercase tracking-widest text-fm-black/60">{{ t('checkout.summary') }}</p>
        <p class="font-serif italic text-lg">{{ product.name }}</p>
        <p class="font-logo text-xl">{{ fmtPrice(product.priceEur) }}</p>
      </section>

      <section v-if="product.delivery" class="space-y-2">
        <p class="text-xs uppercase tracking-widest text-fm-black/60">{{ t('checkout.shippingAddress') }}</p>
        <template v-if="addresses.length > 0">
          <FmSelect v-model="selectedAddressId" :options="addressOptions" data-testid="address-select" />
        </template>
        <div v-else class="fm-card bg-fm-stone text-sm space-y-2">
          <p>{{ t('checkout.noShippingAddress') }}</p>
          <router-link to="/addresses" class="underline">{{ t('checkout.addAddressLink') }}</router-link>
        </div>
      </section>

      <FmCheckbox v-model="acceptCgv" :label="t('checkout.acceptCgv')" data-testid="accept-cgv" />
      <router-link :to="{ name: 'legal', params: { slug: 'cgv' } }" class="text-xs underline">{{ t('legal.cgv') }}</router-link>

      <p v-if="error" class="text-sm text-fm-red">{{ error }}</p>

      <FmButton block variant="primary" :disabled="!canPay || busy" @click="pay" data-testid="pay-cta">
        {{ t('checkout.payCta', { price: fmtPrice(product.priceEur) }) }}
      </FmButton>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Typecheck**

```
cd frontend && npm run build
```

Expected: BUILD SUCCESS (may fail if `AddressResponse` isn't exported from types — if so, confirm `AddressResponse` already exists in `types.ts`; it does from Phase 1).

- [ ] **Step 3: Commit**

```
git add frontend/src/views/ShopCheckoutView.vue
git commit -m "feat(shop): ShopCheckoutView — address picker + CGV + pay"
```

---

## Task 23: ShopReturnView

**Files:**
- Replace: `frontend/src/views/ShopReturnView.vue`

- [ ] **Step 1: Write the view**

Replace `frontend/src/views/ShopReturnView.vue`:

```vue
<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { OrderResponse } from '../api/types';
import FmButton from '../components/FmButton.vue';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const order = ref<OrderResponse | null>(null);
const attempts = ref(0);
const timer = ref<number | null>(null);
const isDevSim = computed(() => route.query.sim === '1');

async function fetchOnce() {
  order.value = await api.get<OrderResponse>(`/orders/${route.params.id}`);
}

function startPolling() {
  if (timer.value) return;
  timer.value = window.setInterval(async () => {
    attempts.value++;
    await fetchOnce();
    if (order.value && order.value.status !== 'PENDING_PAYMENT') stopPolling();
    if (attempts.value >= 15) stopPolling();
  }, 2000);
}

function stopPolling() {
  if (timer.value) { clearInterval(timer.value); timer.value = null; }
}

async function simulate(status: 'paid' | 'failed' | 'cancelled' | 'expired') {
  await api.post(`/dev/orders/${route.params.id}/simulate-webhook?status=${status}`);
  await fetchOnce();
}

function retry() {
  if (order.value) router.push({ name: 'shop-product', params: { id: order.value.product.id } });
}

onMounted(async () => {
  await fetchOnce();
  if (order.value?.status === 'PENDING_PAYMENT' && !isDevSim.value) startPolling();
});

onUnmounted(stopPolling);
</script>

<template>
  <div class="fm-page max-w-lg text-center space-y-6">
    <template v-if="order">
      <div v-if="order.status === 'PAID'">
        <h2 class="text-3xl font-serif italic text-green-800">{{ t('orderReturn.paid') }}</h2>
        <p class="text-sm text-fm-black/60 mt-2">{{ t('orderReturn.paidMessage') }}</p>
        <div class="pt-6 space-y-3">
          <FmButton block variant="primary" @click="router.push({ name: 'order-detail', params: { id: order.id } })">{{ t('orderReturn.viewOrder') }}</FmButton>
          <FmButton block variant="ghost" @click="router.push({ name: 'shop' })">{{ t('orderReturn.backToShop') }}</FmButton>
        </div>
      </div>
      <div v-else-if="order.status === 'FAILED'">
        <h2 class="text-3xl font-serif italic text-fm-red">{{ t('orderReturn.failed') }}</h2>
        <FmButton block variant="primary" @click="retry">{{ t('orderReturn.retry') }}</FmButton>
      </div>
      <div v-else-if="order.status === 'CANCELLED'">
        <h2 class="text-3xl font-serif italic text-fm-red">{{ t('orderReturn.cancelled') }}</h2>
        <FmButton block variant="primary" @click="retry">{{ t('orderReturn.retry') }}</FmButton>
      </div>
      <div v-else-if="order.status === 'EXPIRED'">
        <h2 class="text-3xl font-serif italic text-fm-red">{{ t('orderReturn.expired') }}</h2>
        <FmButton block variant="primary" @click="retry">{{ t('orderReturn.retry') }}</FmButton>
      </div>
      <div v-else>
        <h2 class="text-xl font-serif italic">{{ t('common.loading') }}</h2>
        <p v-if="attempts >= 15" class="text-sm text-fm-black/60 mt-4">{{ t('orderReturn.timeout') }}</p>
      </div>

      <section v-if="isDevSim && order.status === 'PENDING_PAYMENT'" class="fm-card border-dashed border-fm-gold text-left space-y-2">
        <p class="text-xs uppercase tracking-widest text-fm-black/60">{{ t('orderReturn.simulateTitle') }}</p>
        <div class="flex gap-2 flex-wrap">
          <FmButton variant="primary" @click="simulate('paid')" data-testid="sim-paid">paid</FmButton>
          <FmButton variant="secondary" @click="simulate('failed')">failed</FmButton>
          <FmButton variant="ghost" @click="simulate('cancelled')">cancelled</FmButton>
          <FmButton variant="ghost" @click="simulate('expired')">expired</FmButton>
        </div>
      </section>
    </template>
    <p v-else>{{ t('common.loading') }}</p>
  </div>
</template>
```

- [ ] **Step 2: Typecheck**

```
cd frontend && npm run build
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```
git add frontend/src/views/ShopReturnView.vue
git commit -m "feat(shop): ShopReturnView — polling + dev simulate panel"
```

---

## Task 24: MyOrdersView + OrderDetailView

**Files:**
- Replace: `frontend/src/views/MyOrdersView.vue`
- Replace: `frontend/src/views/OrderDetailView.vue`

- [ ] **Step 1: Write `MyOrdersView.vue`**

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { OrderResponse, Page } from '../api/types';
import OrderStatusBadge from '../components/OrderStatusBadge.vue';

const { t, locale } = useI18n();
const router = useRouter();
const orders = ref<OrderResponse[]>([]);
const loading = ref(true);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString(locale.value.toLowerCase(), { day: '2-digit', month: 'short', year: 'numeric' });
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get<Page<OrderResponse>>('/orders?size=50&sort=createdAt,desc');
    orders.value = res.content;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <h2 class="text-2xl mb-6 font-serif italic">{{ t('order.title') }}</h2>
    <p v-if="loading">{{ t('common.loading') }}</p>
    <p v-else-if="orders.length === 0" class="text-sm text-fm-black/60">{{ t('order.empty') }}</p>
    <ul v-else class="divide-y divide-fm-black/10">
      <li v-for="o in orders" :key="o.id" class="py-3">
        <button class="w-full text-left flex justify-between items-start gap-3"
                @click="router.push({ name: 'order-detail', params: { id: o.id } })">
          <div>
            <div class="font-medium">{{ o.product.name }}</div>
            <div class="text-xs text-fm-black/60">{{ fmtDate(o.createdAt) }} — {{ fmtPrice(o.totalEur) }}</div>
          </div>
          <OrderStatusBadge :status="o.status" />
        </button>
      </li>
    </ul>
  </div>
</template>
```

- [ ] **Step 2: Write `OrderDetailView.vue`**

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { OrderResponse } from '../api/types';
import OrderStatusBadge from '../components/OrderStatusBadge.vue';

const { t, locale } = useI18n();
const route = useRoute();
const order = ref<OrderResponse | null>(null);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleString(locale.value.toLowerCase());
}

onMounted(async () => {
  order.value = await api.get<OrderResponse>(`/orders/${route.params.id}`);
});
</script>

<template>
  <div class="fm-page max-w-lg" v-if="order">
    <h2 class="text-2xl mb-1 font-serif italic">{{ order.product.name }}</h2>
    <div class="mb-6"><OrderStatusBadge :status="order.status" /></div>
    <dl class="text-sm space-y-2">
      <div><dt class="text-fm-black/60">{{ t('order.orderId') }}</dt><dd>{{ order.id }}</dd></div>
      <div><dt class="text-fm-black/60">Total</dt><dd class="font-logo">{{ fmtPrice(order.totalEur) }}</dd></div>
      <div v-if="order.paidAt"><dt class="text-fm-black/60">{{ t('order.paidAt') }}</dt><dd>{{ fmtDate(order.paidAt) }}</dd></div>
      <div v-if="order.shippingAddress">
        <dt class="text-fm-black/60">{{ t('checkout.shippingAddress') }}</dt>
        <dd>
          {{ order.shippingAddress.fullName }}<br />
          {{ order.shippingAddress.street }}<br />
          {{ order.shippingAddress.postalCode }} {{ order.shippingAddress.city }}<br />
          {{ order.shippingAddress.country }}
        </dd>
      </div>
    </dl>
  </div>
</template>
```

- [ ] **Step 3: Typecheck**

```
cd frontend && npm run build
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```
git add frontend/src/views/MyOrdersView.vue frontend/src/views/OrderDetailView.vue
git commit -m "feat(shop): MyOrdersView + OrderDetailView"
```

---

## Task 25: AdminOrdersView + AdminOrderDetailView

**Files:**
- Replace: `frontend/src/views/admin/AdminOrdersView.vue`
- Replace: `frontend/src/views/admin/AdminOrderDetailView.vue`

- [ ] **Step 1: Write `AdminOrdersView.vue`**

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { AdminOrderResponse, Page } from '../../api/types';
import OrderStatusBadge from '../../components/OrderStatusBadge.vue';

const { t, locale } = useI18n();
const router = useRouter();
const orders = ref<AdminOrderResponse[]>([]);
const loading = ref(true);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string): string {
  return new Date(iso).toLocaleString(locale.value.toLowerCase());
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get<Page<AdminOrderResponse>>('/admin/orders?size=100&sort=createdAt,desc');
    orders.value = res.content;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <h2 class="text-2xl mb-6 font-serif italic">{{ t('nav.adminOrders') }}</h2>
    <p v-if="loading">{{ t('common.loading') }}</p>
    <table v-else class="w-full text-sm">
      <thead class="text-left text-xs uppercase tracking-widest text-fm-black/60">
        <tr>
          <th class="py-2">Date</th><th>Acheteur</th><th>Produit</th><th>Total</th><th>Statut</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-fm-black/10">
        <tr v-for="o in orders" :key="o.id"
            class="cursor-pointer hover:bg-fm-stone"
            @click="router.push({ name: 'admin-order-detail', params: { id: o.id } })">
          <td class="py-3">{{ fmtDate(o.createdAt) }}</td>
          <td>{{ o.userFirstName }} {{ o.userLastName }}<div class="text-xs text-fm-black/60">{{ o.userEmail }}</div></td>
          <td>{{ o.product.name }}</td>
          <td class="font-logo">{{ fmtPrice(o.totalEur) }}</td>
          <td><OrderStatusBadge :status="o.status" /></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
```

- [ ] **Step 2: Write `AdminOrderDetailView.vue`**

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { AdminOrderResponse } from '../../api/types';
import OrderStatusBadge from '../../components/OrderStatusBadge.vue';

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();
const order = ref<AdminOrderResponse | null>(null);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleString(locale.value.toLowerCase());
}

onMounted(async () => {
  order.value = await api.get<AdminOrderResponse>(`/admin/orders/${route.params.id}`);
});
</script>

<template>
  <div class="fm-page max-w-2xl" v-if="order">
    <h2 class="text-2xl mb-1 font-serif italic">{{ order.product.name }}</h2>
    <div class="mb-6 flex items-center gap-4">
      <OrderStatusBadge :status="order.status" />
      <span class="text-sm text-fm-black/60">{{ fmtDate(order.createdAt) }}</span>
    </div>
    <dl class="text-sm space-y-2">
      <div><dt class="text-fm-black/60">{{ t('order.orderId') }}</dt><dd>{{ order.id }}</dd></div>
      <div>
        <dt class="text-fm-black/60">Acheteur</dt>
        <dd>
          <router-link :to="{ name: 'admin-user-detail', params: { id: order.userId } }" class="underline">
            {{ order.userFirstName }} {{ order.userLastName }}
          </router-link>
          — {{ order.userEmail }}
        </dd>
      </div>
      <div><dt class="text-fm-black/60">Total</dt><dd class="font-logo">{{ fmtPrice(order.totalEur) }}</dd></div>
      <div v-if="order.paidAt"><dt class="text-fm-black/60">{{ t('order.paidAt') }}</dt><dd>{{ fmtDate(order.paidAt) }}</dd></div>
      <div v-if="order.molliePaymentId"><dt class="text-fm-black/60">{{ t('order.mollieRef') }}</dt><dd>{{ order.molliePaymentId }}</dd></div>
      <div v-if="order.shipping">
        <dt class="text-fm-black/60">{{ t('checkout.shippingAddress') }}</dt>
        <dd>
          {{ order.shipping.fullName }}<br />
          {{ order.shipping.street }}<br />
          {{ order.shipping.postalCode }} {{ order.shipping.city }}<br />
          {{ order.shipping.country }}
        </dd>
      </div>
    </dl>
  </div>
</template>
```

- [ ] **Step 3: Typecheck**

```
cd frontend && npm run build
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```
git add frontend/src/views/admin/AdminOrdersView.vue frontend/src/views/admin/AdminOrderDetailView.vue
git commit -m "feat(shop): admin orders list + detail"
```

---

## Task 26: E2E tests — shop browse + tier filter + out-of-stock

**Files:**
- Create: `frontend/e2e/shop-browse.spec.ts`
- Create: `frontend/e2e/shop-tier-filter.spec.ts`
- Create: `frontend/e2e/shop-out-of-stock.spec.ts`

- [ ] **Step 1: Look at existing E2E patterns**

Inspect `frontend/e2e/admin-create-allocataire.spec.ts` and `frontend/e2e/helpers.ts` to understand how the suite logs in, creates fixtures, and resets between tests. The suite uses `http://localhost:8080/api` as backend.

- [ ] **Step 2: Write `shop-browse.spec.ts`**

Create `frontend/e2e/shop-browse.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test('allocataire TIER_1 sees T1 products in shop list', async ({ page, request }) => {
  // Assumes test fixtures exist: allocataire TIER_1 account + at least one T1 product in sale window.
  // Pragmatic approach: create the fixtures via admin API at test start.

  // 1. Admin login
  await page.goto('/login');
  await page.fill('[data-testid="login-email"]', 'admin@fuorimondo.local');
  await page.fill('[data-testid="login-password"]', 'Admin!Password123');
  await page.click('[data-testid="login-submit"]');
  await page.waitForURL(/\/profile/, { timeout: 10_000 });

  // 2. Create an allocataire TIER_1 through admin UI
  const email = `allo-${Date.now()}@test`;
  await page.goto('/admin/users/create');
  await page.fill('[data-testid="create-email"]', email);
  await page.fill('[data-testid="create-firstname"]', 'Allo');
  await page.fill('[data-testid="create-lastname"]', 'Test');
  await page.selectOption('[data-testid="create-tier"]', 'TIER_1');
  await page.fill('[data-testid="create-country"]', 'FR');
  await page.fill('[data-testid="create-city"]', 'Paris');
  await page.click('[data-testid="admin-create-submit"]');

  // Grab the generated invitation code from the UI
  const code = await page.locator('[data-testid="code-generated-value"]').innerText();

  // 3. Log out, activate, set password
  await page.click('[data-testid="logout"]');
  await page.goto('/activate');
  await page.fill('[data-testid="act-email"]', email);
  await page.fill('[data-testid="act-code"]', code);
  await page.click('[data-testid="act-verify"]');
  await page.fill('[data-testid="act-password"]', 'AlloPass1');
  await page.click('[data-testid="act-submit"]');
  await page.waitForURL(/\/profile/, { timeout: 10_000 });

  // 4. (Admin side-channel is heavy; this first spec just verifies the shop list loads for the allocataire)
  await page.goto('/shop');
  await expect(page.locator('h2').first()).toContainText(/Boutique|Shop|Negozio/);
});
```

Note: creating real product fixtures from the UI is lengthy; the three shop-* specs focus on browsing behavior given seeded state. For `shop-tier-filter` and `shop-out-of-stock`, the straightforward path is seeding products via backend API calls. Add helpers to `frontend/e2e/helpers.ts`.

- [ ] **Step 3: Extend `frontend/e2e/helpers.ts` with seed helpers**

Add to `helpers.ts`:

```typescript
import { APIRequestContext } from '@playwright/test';

export async function adminLogin(request: APIRequestContext) {
  await request.post('http://localhost:8080/api/auth/login', {
    data: { email: 'admin@fuorimondo.local', password: 'Admin!Password123' },
  });
}

export async function createProduct(request: APIRequestContext, data: {
  name: string; priceEur: string; tiers: string[]; delivery: boolean;
  saleStartAt: string; saleEndAt?: string | null; stock?: number | null;
}) {
  const form = new FormData();
  form.append('payload', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  const res = await request.post('http://localhost:8080/api/admin/products', { multipart: { payload: JSON.stringify(data) } });
  return await res.json();
}
```

Adapt the multipart shape to match the actual AdminProductController signature (confirmed by peeking at `backend/src/main/java/com/fuorimondo/products/AdminProductController.java`).

- [ ] **Step 4: Write `shop-tier-filter.spec.ts`**

```typescript
import { test, expect } from '@playwright/test';

test('TIER_3 allocataire does not see TIER_1-only products', async ({ page, request }) => {
  // Seed: one TIER_1-only product (via admin API) + one TIER_3 allocataire
  // Log in as TIER_3, visit /shop, expect the T1 product to NOT be visible.
  // Direct-URL visit /shop/:t1id → expect a tier_mismatch or 404 rendering.

  // (Seed fixtures using admin helpers; then:)
  await page.goto('/shop');
  await expect(page.locator('[data-testid^="product-card-"]')).toHaveCount(0);
});
```

Flesh out the seeding using the helpers. Adapt to the real admin API multipart shape.

- [ ] **Step 5: Write `shop-out-of-stock.spec.ts`**

```typescript
import { test, expect } from '@playwright/test';

test('out-of-stock product is not listed', async ({ page, request }) => {
  // Seed a TIER_1 product with stock=0
  await page.goto('/shop');
  await expect(page.locator('[data-testid^="product-card-"]')).toHaveCount(0);
});
```

- [ ] **Step 6: Run the E2E suite**

Start backend + frontend, then:

```
cd frontend && npm run test:e2e -- shop-browse
```

Expected: `shop-browse.spec.ts` passes. The other two specs may need real seeding to pass — implement the seeding in step 3 before running them.

- [ ] **Step 7: Commit**

```
git add frontend/e2e/shop-browse.spec.ts \
        frontend/e2e/shop-tier-filter.spec.ts \
        frontend/e2e/shop-out-of-stock.spec.ts \
        frontend/e2e/helpers.ts
git commit -m "test(shop): E2E — browse, tier filter, out-of-stock"
```

---

## Task 27: E2E test — full purchase flow

**Files:**
- Create: `frontend/e2e/shop-purchase.spec.ts`

- [ ] **Step 1: Write the full-flow spec**

Create `frontend/e2e/shop-purchase.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test('allocataire can complete a purchase end-to-end (dev mode)', async ({ page, request }) => {
  // Steps:
  // 1. Admin seeds: TIER_1 allocataire + product with stock=1 + shipping address for allocataire
  // 2. Log in as allocataire, go to /shop, click product
  // 3. Click Acheter → checkout → select address → accept CGV → Pay
  // 4. Page redirects to /shop/order/:id/return?sim=1 (dev mode)
  // 5. Click "paid" simulate button → order.status becomes PAID
  // 6. Assert "Commande confirmée" text appears

  // (Use admin helpers to seed. Then:)

  // After payment simulation:
  await page.click('[data-testid="sim-paid"]');
  await expect(page.locator('h2').first()).toContainText(/Commande confirmée|Order confirmed|Ordine confermato/);
});
```

Flesh out the full seeding and navigation.

- [ ] **Step 2: Run**

```
cd frontend && npm run test:e2e -- shop-purchase
```

Expected: PASS after seed helpers are complete.

- [ ] **Step 3: Commit**

```
git add frontend/e2e/shop-purchase.spec.ts
git commit -m "test(shop): E2E — full purchase flow with dev simulate"
```

---

## Task 28: E2E test — admin orders dashboard

**Files:**
- Create: `frontend/e2e/admin-orders.spec.ts`

- [ ] **Step 1: Write the spec**

Create `frontend/e2e/admin-orders.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test('admin sees completed orders in dashboard', async ({ page, request }) => {
  // After running shop-purchase prerequisites (or inlining them), log in as admin
  // and visit /admin/orders.

  await page.goto('/login');
  await page.fill('[data-testid="login-email"]', 'admin@fuorimondo.local');
  await page.fill('[data-testid="login-password"]', 'Admin!Password123');
  await page.click('[data-testid="login-submit"]');
  await page.waitForURL(/\/profile/, { timeout: 10_000 });

  await page.goto('/admin/orders');
  await expect(page.locator('table')).toBeVisible();
  // Assert at least one order row exists (after previous E2E tests or seeding).
});
```

- [ ] **Step 2: Run**

```
cd frontend && npm run test:e2e -- admin-orders
```

Expected: PASS.

- [ ] **Step 3: Commit**

```
git add frontend/e2e/admin-orders.spec.ts
git commit -m "test(shop): E2E — admin orders dashboard"
```

---

## Final verification

- [ ] **Step 1: Run full backend test suite**

```
cd backend && ./mvnw.cmd test
```

Expected: ALL tests pass (including existing ones).

- [ ] **Step 2: Run full frontend build + E2E**

```
cd frontend && npm run build && npm run test:e2e
```

Expected: Build succeeds. All E2E specs pass (existing + new ones from Tasks 26-28).

- [ ] **Step 3: Smoke test manually**

1. Start backend (`./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`) and frontend (`npm run dev`).
2. As admin, create a product (tier TIER_1, stock 2, delivery true).
3. Create a TIER_1 allocataire + get their activation code.
4. Activate as allocataire, set password, add a SHIPPING address.
5. Go to `/shop`, click the product, click Acheter, select address, accept CGV, click Pay.
6. Arrive on `/shop/order/:id/return?sim=1`. Click "paid".
7. Confirm "Commande confirmée" screen + check backend logs for the email output.
8. Go to `/orders` → see the order.
9. Log out, log back in as admin, go to `/admin/orders` → see the order.
10. Verify the stock on the product decreased (via admin product detail).

- [ ] **Step 4: Final summary commit (optional)**

If helpers or docs needed tweaks during smoke testing, commit them separately.

---

## Done

Upon completion of all 28 tasks + final verification:
- Backend exposes `/api/products`, `/api/orders`, `/api/admin/orders`, `/api/webhooks/mollie`, `/api/dev/orders/{id}/simulate-webhook` (dev/test only).
- Frontend exposes 8 new routes for shop + orders + admin orders.
- Full flow browse → buy → pay → confirm works end-to-end in dev with the fake Mollie gateway.
- For prod: set `MOLLIE_API_KEY`, `MOLLIE_ENABLED=true`, `WEBHOOK_BASE_URL=https://…`, `APP_BASE_URL=https://…`.

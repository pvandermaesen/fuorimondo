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

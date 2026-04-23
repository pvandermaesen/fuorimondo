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

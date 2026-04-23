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

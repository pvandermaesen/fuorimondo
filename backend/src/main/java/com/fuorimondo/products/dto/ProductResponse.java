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

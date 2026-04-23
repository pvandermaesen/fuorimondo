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

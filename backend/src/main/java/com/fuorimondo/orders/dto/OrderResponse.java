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

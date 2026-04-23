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

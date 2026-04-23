package com.fuorimondo.orders.dto;

import java.util.UUID;

public record CreateOrderResponse(UUID orderId, String checkoutUrl) {}

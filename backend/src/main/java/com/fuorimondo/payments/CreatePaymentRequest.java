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

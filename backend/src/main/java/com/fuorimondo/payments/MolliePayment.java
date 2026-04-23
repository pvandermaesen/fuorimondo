package com.fuorimondo.payments;

public record MolliePayment(
    String id,
    MolliePaymentStatus status,
    String checkoutUrl
) {}

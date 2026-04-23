package com.fuorimondo.orders;

public class OrderException extends RuntimeException {

    public enum Reason {
        PRODUCT_NOT_FOUND,
        TIER_MISMATCH,
        SALE_WINDOW_CLOSED,
        OUT_OF_STOCK,
        NO_SHIPPING_ADDRESS,
        INVALID_SHIPPING_ADDRESS,
        PAYMENT_GATEWAY_ERROR
    }

    private final Reason reason;

    public OrderException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}

package com.fuorimondo.payments;

public interface MolliePaymentGateway {
    MolliePayment createPayment(CreatePaymentRequest req);
    MolliePayment getPayment(String molliePaymentId);

    /** Test-only hook used by the dev simulate endpoint. Default: unsupported. */
    default void forceStatus(String molliePaymentId, MolliePaymentStatus status) {
        throw new UnsupportedOperationException("forceStatus only supported on fake gateway");
    }
}

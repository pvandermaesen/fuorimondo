package com.fuorimondo.payments;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "mollie.enabled", havingValue = "false", matchIfMissing = false)
public class MolliePaymentGatewayFake implements MolliePaymentGateway {

    private final MollieConfig config;
    private final ConcurrentHashMap<String, MolliePaymentStatus> statuses = new ConcurrentHashMap<>();

    public MolliePaymentGatewayFake(MollieConfig config) {
        this.config = config;
    }

    @Override
    public MolliePayment createPayment(CreatePaymentRequest req) {
        String id = "tr_fake_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        statuses.put(id, MolliePaymentStatus.OPEN);
        String base = config.getRedirectBaseUrl() == null ? "" : config.getRedirectBaseUrl();
        String checkoutUrl = base + "/shop/order/" + req.orderId() + "/return?sim=1";
        return new MolliePayment(id, MolliePaymentStatus.OPEN, checkoutUrl);
    }

    @Override
    public MolliePayment getPayment(String molliePaymentId) {
        MolliePaymentStatus status = statuses.getOrDefault(molliePaymentId, MolliePaymentStatus.OPEN);
        return new MolliePayment(molliePaymentId, status, null);
    }

    @Override
    public void forceStatus(String molliePaymentId, MolliePaymentStatus status) {
        statuses.put(molliePaymentId, status);
    }
}

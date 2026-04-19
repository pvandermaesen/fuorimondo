package com.fuorimondo.payments;

import com.fuorimondo.orders.OrderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mollie.enabled", havingValue = "true", matchIfMissing = true)
public class MolliePaymentGatewayHttp implements MolliePaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MolliePaymentGatewayHttp.class);

    private final MollieConfig config;
    private final RestClient http;

    public MolliePaymentGatewayHttp(MollieConfig config) {
        this.config = config;
        this.http = RestClient.builder()
            .baseUrl(config.getApiBaseUrl())
            .defaultHeader("Authorization", "Bearer " + config.getApiKey())
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public MolliePayment createPayment(CreatePaymentRequest req) {
        Map<String, Object> body = Map.of(
            "amount", Map.of(
                "currency", "EUR",
                "value", req.amountEur().setScale(2, RoundingMode.HALF_UP).toPlainString()
            ),
            "description", req.description(),
            "redirectUrl", config.getRedirectBaseUrl() + "/shop/order/" + req.orderId() + "/return",
            "webhookUrl", config.getWebhookBaseUrl() + "/api/webhooks/mollie",
            "metadata", Map.of("orderId", req.orderId().toString())
        );
        try {
            Map<?,?> resp = http.post()
                .uri("/payments")
                .body(body)
                .retrieve()
                .body(Map.class);
            if (resp == null) throw gatewayError("empty response");
            String id = (String) resp.get("id");
            String status = (String) resp.get("status");
            Map<?,?> links = (Map<?,?>) resp.get("_links");
            Map<?,?> checkout = links == null ? null : (Map<?,?>) links.get("checkout");
            String href = checkout == null ? null : (String) checkout.get("href");
            return new MolliePayment(id, parseStatus(status), href);
        } catch (OrderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Mollie createPayment failed: {}", e.getMessage());
            throw gatewayError("Mollie createPayment failed: " + e.getMessage());
        }
    }

    @Override
    public MolliePayment getPayment(String molliePaymentId) {
        try {
            Map<?,?> resp = http.get()
                .uri("/payments/{id}", molliePaymentId)
                .retrieve()
                .body(Map.class);
            if (resp == null) throw gatewayError("empty response");
            String status = (String) resp.get("status");
            return new MolliePayment(molliePaymentId, parseStatus(status), null);
        } catch (OrderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Mollie getPayment failed: {}", e.getMessage());
            throw gatewayError("Mollie getPayment failed: " + e.getMessage());
        }
    }

    private MolliePaymentStatus parseStatus(String s) {
        if (s == null) return MolliePaymentStatus.OPEN;
        return MolliePaymentStatus.valueOf(s.toUpperCase(Locale.ROOT));
    }

    private static OrderException gatewayError(String msg) {
        return new OrderException(OrderException.Reason.PAYMENT_GATEWAY_ERROR, msg);
    }
}

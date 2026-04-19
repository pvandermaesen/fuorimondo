package com.fuorimondo.payments;

import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/dev/orders")
@Profile("dev")
public class DevSimulateWebhookController {

    private final OrderRepository orderRepository;
    private final MolliePaymentGateway gateway;
    private final MollieWebhookController webhookController;

    public DevSimulateWebhookController(OrderRepository orderRepository,
                                         MolliePaymentGateway gateway,
                                         MollieWebhookController webhookController) {
        this.orderRepository = orderRepository;
        this.gateway = gateway;
        this.webhookController = webhookController;
    }

    @PostMapping("/{id}/simulate-webhook")
    public ResponseEntity<Void> simulate(@PathVariable UUID id,
                                          @RequestParam String status) {
        Order o = orderRepository.findById(id).orElseThrow();
        MolliePaymentStatus mstatus = switch (status.toLowerCase(Locale.ROOT)) {
            case "paid" -> MolliePaymentStatus.PAID;
            case "failed" -> MolliePaymentStatus.FAILED;
            case "cancelled", "canceled" -> MolliePaymentStatus.CANCELED;
            case "expired" -> MolliePaymentStatus.EXPIRED;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
        gateway.forceStatus(o.getMolliePaymentId(), mstatus);

        // Trigger the real webhook handler so the transition + email logic runs
        webhookController.mollie(o.getMolliePaymentId());

        return ResponseEntity.noContent().build();
    }
}

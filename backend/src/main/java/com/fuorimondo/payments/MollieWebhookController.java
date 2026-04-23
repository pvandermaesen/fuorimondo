package com.fuorimondo.payments;

import com.fuorimondo.email.EmailSender;
import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderRepository;
import com.fuorimondo.orders.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;

@Controller
@RequestMapping("/api/webhooks")
public class MollieWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MollieWebhookController.class);

    private final OrderRepository orderRepository;
    private final MolliePaymentGateway gateway;
    private final EmailSender emailSender;

    public MollieWebhookController(OrderRepository orderRepository,
                                    MolliePaymentGateway gateway,
                                    EmailSender emailSender) {
        this.orderRepository = orderRepository;
        this.gateway = gateway;
        this.emailSender = emailSender;
    }

    @PostMapping("/mollie")
    @ResponseBody
    @Transactional
    public ResponseEntity<Void> mollie(@RequestParam("id") String mollieId) {
        Order order = orderRepository.findByMolliePaymentId(mollieId).orElse(null);
        if (order == null) {
            log.info("Mollie webhook: unknown id {}", mollieId);
            return ResponseEntity.ok().build();
        }
        MolliePayment payment = gateway.getPayment(mollieId);
        applyStatus(order, payment.status());
        return ResponseEntity.ok().build();
    }

    private void applyStatus(Order order, MolliePaymentStatus status) {
        OrderStatus current = order.getStatus();
        if (current != OrderStatus.PENDING_PAYMENT) {
            // Idempotent: already in terminal state
            return;
        }
        switch (status) {
            case PAID -> {
                order.setStatus(OrderStatus.PAID);
                order.setPaidAt(Instant.now());
                safeSendConfirmation(order);
            }
            case FAILED -> order.setStatus(OrderStatus.FAILED);
            case CANCELED -> order.setStatus(OrderStatus.CANCELLED);
            case EXPIRED -> order.setStatus(OrderStatus.EXPIRED);
            default -> { /* OPEN/PENDING/AUTHORIZED -> no transition */ }
        }
    }

    private void safeSendConfirmation(Order order) {
        try {
            emailSender.sendOrderConfirmation(
                order.getUser().getEmail(),
                order.getUser().getFirstName(),
                order.getUser().getLocale(),
                order.getId().toString(),
                order.getTotalEur());
        } catch (Exception e) {
            log.warn("Failed to send order confirmation email for order {}: {}",
                order.getId(), e.getMessage());
        }
    }
}

package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.addresses.Address;
import com.fuorimondo.addresses.AddressRepository;
import com.fuorimondo.addresses.AddressType;
import com.fuorimondo.orders.dto.ProductSnapshot;
import com.fuorimondo.orders.dto.ShippingSnapshot;
import com.fuorimondo.payments.CreatePaymentRequest;
import com.fuorimondo.payments.MolliePayment;
import com.fuorimondo.payments.MolliePaymentGateway;
import com.fuorimondo.payments.MollieConfig;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final MolliePaymentGateway molliePaymentGateway;
    private final MollieConfig mollieConfig;
    private final ObjectMapper json;
    private final int reservationTtlMinutes;

    public OrderService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         AddressRepository addressRepository,
                         MolliePaymentGateway molliePaymentGateway,
                         MollieConfig mollieConfig,
                         ObjectMapper json,
                         @Value("${fuorimondo.order.reservation-ttl-minutes:15}") int ttl) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.molliePaymentGateway = molliePaymentGateway;
        this.mollieConfig = mollieConfig;
        this.json = json;
        this.reservationTtlMinutes = ttl;
    }

    @Transactional
    public Order create(User user, UUID productId, UUID shippingAddressId) {
        Product product = orderRepository.lockProduct(productId)
            .orElseThrow(() -> new OrderException(OrderException.Reason.PRODUCT_NOT_FOUND, "product"));

        Instant now = Instant.now();

        if (user.getTierCode() == null || !product.getTiers().contains(user.getTierCode())) {
            throw new OrderException(OrderException.Reason.TIER_MISMATCH, "tier");
        }
        if (product.getSaleStartAt().isAfter(now)
                || (product.getSaleEndAt() != null && !product.getSaleEndAt().isAfter(now))) {
            throw new OrderException(OrderException.Reason.SALE_WINDOW_CLOSED, "window");
        }
        if (product.getStock() != null) {
            long reserved = orderRepository.countActiveReservations(product, now);
            if (product.getStock() - reserved < 1) {
                throw new OrderException(OrderException.Reason.OUT_OF_STOCK, "stock");
            }
        }

        Address shipping = null;
        if (product.isDelivery()) {
            if (shippingAddressId == null) {
                throw new OrderException(OrderException.Reason.NO_SHIPPING_ADDRESS, "address required");
            }
            shipping = addressRepository.findById(shippingAddressId)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .filter(a -> a.getType() == AddressType.SHIPPING)
                .orElseThrow(() -> new OrderException(OrderException.Reason.INVALID_SHIPPING_ADDRESS, "address"));
        }

        Order order = new Order();
        order.setUser(user);
        order.setProduct(product);
        order.setUnitPriceEur(product.getPriceEur());
        order.setTotalEur(product.getPriceEur());
        order.setShippingAddress(shipping);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setExpiresAt(now.plus(reservationTtlMinutes, ChronoUnit.MINUTES));

        try {
            order.setProductSnapshot(json.writeValueAsString(ProductSnapshot.from(product)));
            if (shipping != null) {
                order.setShippingSnapshot(json.writeValueAsString(ShippingSnapshot.from(shipping)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize snapshots", e);
        }

        order = orderRepository.save(order);

        MolliePayment payment = molliePaymentGateway.createPayment(new CreatePaymentRequest(
            product.getPriceEur(),
            "Fuori Marmo — " + product.getName(),
            order.getId(),
            mollieConfig.getRedirectBaseUrl() + "/shop/order/" + order.getId() + "/return",
            mollieConfig.getWebhookBaseUrl() + "/api/webhooks/mollie"
        ));

        order.setMolliePaymentId(payment.id());
        order.setMollieCheckoutUrl(payment.checkoutUrl());
        return order;
    }
}

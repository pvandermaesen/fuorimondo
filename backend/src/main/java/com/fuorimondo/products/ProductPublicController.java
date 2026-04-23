package com.fuorimondo.products;

import com.fuorimondo.orders.OrderRepository;
import com.fuorimondo.products.dto.PublicProductResponse;
import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.User;
import com.fuorimondo.users.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductPublicController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ProductPublicController(ProductRepository productRepository,
                                    OrderRepository orderRepository,
                                    UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<PublicProductResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        User u = userRepository.findById(principal.getUserId()).orElseThrow();
        if (u.getTierCode() == null) return List.of();
        Instant now = Instant.now();
        return productRepository.findVisibleForTier(u.getTierCode(), now).stream()
            .map(p -> {
                Integer remaining = null;
                if (p.getStock() != null) {
                    long reserved = orderRepository.countActiveReservations(p, now);
                    remaining = Math.max(0, p.getStock() - (int) reserved);
                }
                return PublicProductResponse.from(p, remaining);
            })
            .filter(r -> r.stockRemaining() == null || r.stockRemaining() > 0)
            .toList();
    }

    @GetMapping("/{id}")
    public PublicProductResponse detail(@AuthenticationPrincipal CustomUserDetails principal,
                                         @PathVariable UUID id) {
        User u = userRepository.findById(principal.getUserId()).orElseThrow();
        Product p = productRepository.findById(id).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND));

        Instant now = Instant.now();
        if (u.getTierCode() == null || !p.getTiers().contains(u.getTierCode())
            || p.getSaleStartAt().isAfter(now)
            || (p.getSaleEndAt() != null && !p.getSaleEndAt().isAfter(now))) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND);
        }

        Integer remaining = null;
        if (p.getStock() != null) {
            long reserved = orderRepository.countActiveReservations(p, now);
            remaining = Math.max(0, p.getStock() - (int) reserved);
        }
        return PublicProductResponse.from(p, remaining);
    }
}

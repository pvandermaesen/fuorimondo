package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.orders.dto.CreateOrderRequest;
import com.fuorimondo.orders.dto.CreateOrderResponse;
import com.fuorimondo.orders.dto.OrderResponse;
import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.User;
import com.fuorimondo.users.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;
    private final OrderRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper json;

    public OrderController(OrderService service, OrderRepository repository,
                            UserRepository userRepository, ObjectMapper json) {
        this.service = service;
        this.repository = repository;
        this.userRepository = userRepository;
        this.json = json;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateOrderRequest req) {
        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        Order order = service.create(user, req.productId(), req.shippingAddressId());
        return ResponseEntity.status(201).body(new CreateOrderResponse(order.getId(), order.getMollieCheckoutUrl()));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable UUID id) {
        Order o = repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (!o.getUser().getId().equals(principal.getUserId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return OrderResponse.from(o, json);
    }

    @GetMapping
    public Page<OrderResponse> list(@AuthenticationPrincipal CustomUserDetails principal,
                                     Pageable pageable) {
        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        return repository.findByUserOrderByCreatedAtDesc(user, pageable)
            .map(o -> OrderResponse.from(o, json));
    }
}

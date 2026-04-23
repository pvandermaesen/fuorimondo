package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.orders.dto.AdminOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderRepository repository;
    private final ObjectMapper json;

    public AdminOrderController(OrderRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @GetMapping
    public Page<AdminOrderResponse> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable)
            .map(o -> AdminOrderResponse.from(o, json));
    }

    @GetMapping("/{id}")
    public AdminOrderResponse detail(@PathVariable UUID id) {
        Order o = repository.findById(id).orElseThrow();
        return AdminOrderResponse.from(o, json);
    }
}

package com.fuorimondo.products;

import com.fuorimondo.products.dto.ProductRequest;
import com.fuorimondo.products.dto.ProductResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return service.list().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        Product created = service.create(req);
        return ResponseEntity.status(201).body(ProductResponse.from(created));
    }

    @PatchMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "not_found", "message", ex.getMessage()));
    }
}

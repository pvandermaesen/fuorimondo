package com.fuorimondo.products;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.products.dto.ProductRequest;
import com.fuorimondo.products.dto.ProductResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService service;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AdminProductController(ProductService service, ObjectMapper objectMapper, Validator validator) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return service.list().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(service.getById(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        Product created = service.create(req);
        return ResponseEntity.status(201).body(ProductResponse.from(created));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createMultipart(
            @RequestPart("product") String productJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws IOException {
        ProductRequest req = objectMapper.readValue(productJson, ProductRequest.class);
        Set<jakarta.validation.ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }
        Product created = service.create(req);
        if (photo != null && !photo.isEmpty()) {
            created = service.setPhoto(created.getId(), photo);
        }
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

    @PostMapping("/{id}/photo")
    public ProductResponse uploadPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) throws IOException {
        return ProductResponse.from(service.setPhoto(id, file));
    }

    @DeleteMapping("/{id}/photo")
    public ProductResponse deletePhoto(@PathVariable UUID id) {
        return ProductResponse.from(service.clearPhoto(id));
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable UUID id) {
        Resource r = service.loadPhoto(id);
        String ct = service.photoContentType(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(ct)).body(r);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "not_found", "message", ex.getMessage()));
    }
}

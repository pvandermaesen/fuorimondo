package com.fuorimondo.products;

import com.fuorimondo.products.dto.ProductRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Product> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    @Transactional
    public Product create(ProductRequest req) {
        validateSaleWindow(req);
        Product p = new Product();
        apply(p, req);
        return repository.save(p);
    }

    @Transactional
    public Product update(UUID id, ProductRequest req) {
        validateSaleWindow(req);
        Product p = getById(id);
        apply(p, req);
        return repository.save(p);
    }

    @Transactional
    public void delete(UUID id) {
        // photo file cleanup will be wired in Task 8/9 when PhotoStorage is introduced
        repository.deleteById(id);
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name());
        p.setDescription(req.description());
        p.setPriceEur(req.priceEur());
        p.setDelivery(Boolean.TRUE.equals(req.delivery()));
        p.setWeightKg(req.weightKg());
        p.setSaleStartAt(req.saleStartAt());
        p.setSaleEndAt(req.saleEndAt());
        p.setStock(req.stock());
        p.setTiers(EnumSet.copyOf(req.tiers()));
    }

    private void validateSaleWindow(ProductRequest req) {
        if (req.saleEndAt() != null && req.saleEndAt().isBefore(req.saleStartAt())) {
            throw new IllegalArgumentException("saleEndAt must be >= saleStartAt");
        }
    }
}

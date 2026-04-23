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
    private final PhotoStorage storage;

    public ProductService(ProductRepository repository, PhotoStorage storage) {
        this.repository = repository;
        this.storage = storage;
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
        Product p = getById(id);
        if (p.getPhotoFilename() != null) {
            storage.delete(p.getPhotoFilename());
        }
        repository.delete(p);
    }

    @Transactional
    public Product setPhoto(UUID id, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        Product p = getById(id);
        String oldName = p.getPhotoFilename();
        String newName = storage.store(file);
        p.setPhotoFilename(newName);
        Product saved = repository.save(p);
        if (oldName != null) storage.delete(oldName);
        return saved;
    }

    @Transactional
    public Product clearPhoto(UUID id) {
        Product p = getById(id);
        if (p.getPhotoFilename() != null) {
            storage.delete(p.getPhotoFilename());
            p.setPhotoFilename(null);
            return repository.save(p);
        }
        return p;
    }

    public org.springframework.core.io.Resource loadPhoto(UUID id) {
        Product p = getById(id);
        if (p.getPhotoFilename() == null) throw new jakarta.persistence.EntityNotFoundException("No photo");
        return storage.load(p.getPhotoFilename());
    }

    public String photoContentType(UUID id) {
        Product p = getById(id);
        return storage.contentTypeFor(p.getPhotoFilename());
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

package com.fuorimondo.products;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
        SELECT DISTINCT p FROM Product p
        JOIN p.tiers t
        WHERE t = :tier
          AND p.saleStartAt <= :now
          AND (p.saleEndAt IS NULL OR p.saleEndAt > :now)
        ORDER BY p.saleStartAt DESC
        """)
    List<Product> findVisibleForTier(
        @Param("tier") com.fuorimondo.users.TierCode tier,
        @Param("now") Instant now);
}

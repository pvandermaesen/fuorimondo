package com.fuorimondo.orders;

import com.fuorimondo.products.Product;
import com.fuorimondo.users.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByMolliePaymentId(String molliePaymentId);

    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Order> findByUserAndProductAndStatus(User user, Product product, OrderStatus status);

    @Query("""
           SELECT COUNT(o) FROM Order o
           WHERE o.product = :product
             AND o.status = com.fuorimondo.orders.OrderStatus.PENDING_PAYMENT
             AND o.expiresAt > :now
           """)
    long countActiveReservations(@Param("product") Product product, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("""
           UPDATE Order o SET o.status = com.fuorimondo.orders.OrderStatus.EXPIRED
           WHERE o.status = com.fuorimondo.orders.OrderStatus.PENDING_PAYMENT
             AND o.expiresAt < :now
           """)
    int expireStaleOrders(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> lockProduct(@Param("productId") UUID productId);
}

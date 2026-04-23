package com.fuorimondo.orders;

import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderExpirationJobTest {

    @Autowired OrderRepository orderRepo;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired OrderExpirationJob job;
    @Autowired PasswordEncoder encoder;

    @Test
    void sweeps_expired_pending_orders_to_expired() {
        User u = new User();
        u.setEmail("exp@test"); u.setFirstName("E"); u.setLastName("E");
        u.setPasswordHash(encoder.encode("Password12"));
        u.setStatus(UserStatus.ALLOCATAIRE); u.setRole(UserRole.USER);
        u.setTierCode(TierCode.TIER_1); u.setCountry("FR"); u.setCity("P");
        u.setLocale(Locale.FR); u.setCivility(Civility.NONE);
        u = userRepo.save(u);

        Product p = new Product();
        p.setName("EXP"); p.setPriceEur(new BigDecimal("10"));
        p.setTiers(EnumSet.of(TierCode.TIER_1));
        p.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        p.setDelivery(false);
        p = productRepo.save(p);

        Order stale = new Order();
        stale.setUser(u); stale.setProduct(p);
        stale.setUnitPriceEur(p.getPriceEur()); stale.setTotalEur(p.getPriceEur());
        stale.setStatus(OrderStatus.PENDING_PAYMENT);
        stale.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        stale.setMolliePaymentId("tr_stale");
        stale.setProductSnapshot("{}");
        stale = orderRepo.save(stale);

        Order fresh = new Order();
        fresh.setUser(u); fresh.setProduct(p);
        fresh.setUnitPriceEur(p.getPriceEur()); fresh.setTotalEur(p.getPriceEur());
        fresh.setStatus(OrderStatus.PENDING_PAYMENT);
        fresh.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        fresh.setMolliePaymentId("tr_fresh");
        fresh.setProductSnapshot("{}");
        fresh = orderRepo.save(fresh);

        int swept = job.run();

        assertEquals(1, swept);
        assertEquals(OrderStatus.EXPIRED, orderRepo.findById(stale.getId()).orElseThrow().getStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT, orderRepo.findById(fresh.getId()).orElseThrow().getStatus());
    }
}

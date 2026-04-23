package com.fuorimondo.payments;

import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderRepository;
import com.fuorimondo.orders.OrderStatus;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Transactional
class DevSimulateWebhookTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired OrderRepository orderRepo;
    @Autowired MolliePaymentGateway gateway;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;

    @Test
    void simulate_paid_transitions_status() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        User u = new User();
        u.setEmail("simu@test"); u.setFirstName("S"); u.setLastName("S");
        u.setPasswordHash(encoder.encode("Password12"));
        u.setStatus(UserStatus.ALLOCATAIRE); u.setRole(UserRole.USER);
        u.setTierCode(TierCode.TIER_1); u.setCountry("FR"); u.setCity("P");
        u.setLocale(Locale.FR); u.setCivility(Civility.NONE);
        u = userRepo.save(u);

        Product p = new Product();
        p.setName("Sim"); p.setPriceEur(new BigDecimal("10"));
        p.setTiers(EnumSet.of(TierCode.TIER_1));
        p.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        p.setDelivery(false);
        p = productRepo.save(p);

        Order o = new Order();
        o.setUser(u); o.setProduct(p);
        o.setUnitPriceEur(p.getPriceEur()); o.setTotalEur(p.getPriceEur());
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        o.setMolliePaymentId("tr_sim_" + java.util.UUID.randomUUID());
        o.setProductSnapshot("{}");
        o = orderRepo.save(o);

        mvc.perform(post("/api/dev/orders/" + o.getId() + "/simulate-webhook?status=paid")
            .with(user(new CustomUserDetails(u)))
            .with(csrf()))
           .andExpect(status().isOk());

        assertEquals(OrderStatus.PAID, orderRepo.findById(o.getId()).orElseThrow().getStatus());
    }
}

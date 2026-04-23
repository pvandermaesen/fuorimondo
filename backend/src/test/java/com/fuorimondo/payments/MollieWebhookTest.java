package com.fuorimondo.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.addresses.Address;
import com.fuorimondo.addresses.AddressRepository;
import com.fuorimondo.addresses.AddressType;
import com.fuorimondo.orders.Order;
import com.fuorimondo.orders.OrderRepository;
import com.fuorimondo.orders.OrderStatus;
import com.fuorimondo.orders.dto.ProductSnapshot;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MollieWebhookTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired AddressRepository addressRepo;
    @Autowired OrderRepository orderRepo;
    @Autowired MolliePaymentGateway gateway;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    MockMvc mvc;
    Order order;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        User u = new User();
        u.setEmail("hook@test"); u.setFirstName("Hook"); u.setLastName("Test");
        u.setPasswordHash(encoder.encode("Password12")); u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER); u.setTierCode(TierCode.TIER_1);
        u.setCountry("FR"); u.setCity("Paris"); u.setLocale(Locale.FR); u.setCivility(Civility.NONE);
        u = userRepo.save(u);

        Product p = new Product();
        p.setName("HookWine"); p.setPriceEur(new BigDecimal("50"));
        p.setTiers(EnumSet.of(TierCode.TIER_1));
        p.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        p.setStock(1); p.setDelivery(true);
        p = productRepo.save(p);

        order = new Order();
        order.setUser(u); order.setProduct(p);
        order.setUnitPriceEur(p.getPriceEur()); order.setTotalEur(p.getPriceEur());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        order.setMolliePaymentId("tr_test_1");
        order.setMollieCheckoutUrl("http://localhost/fake");
        order.setProductSnapshot(json.writeValueAsString(ProductSnapshot.from(p)));
        order = orderRepo.save(order);
    }

    @Test
    void webhook_paid_transitions_order_to_paid() throws Exception {
        gateway.forceStatus("tr_test_1", MolliePaymentStatus.PAID);

        mvc.perform(post("/api/webhooks/mollie")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .content("id=tr_test_1"))
           .andExpect(status().isOk());

        Order reloaded = orderRepo.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, reloaded.getStatus());
    }

    @Test
    void webhook_unknown_id_returns_200_silently() throws Exception {
        mvc.perform(post("/api/webhooks/mollie")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .content("id=tr_unknown"))
           .andExpect(status().isOk());
    }

    @Test
    void webhook_is_idempotent() throws Exception {
        gateway.forceStatus("tr_test_1", MolliePaymentStatus.PAID);
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/webhooks/mollie")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("id=tr_test_1"))
               .andExpect(status().isOk());
        }
        Order reloaded = orderRepo.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, reloaded.getStatus());
    }
}

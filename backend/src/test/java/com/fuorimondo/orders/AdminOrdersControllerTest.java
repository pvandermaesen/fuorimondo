package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOrdersControllerTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired OrderRepository orderRepo;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    MockMvc mvc;
    User adminUser;
    User alloUser;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        adminUser = new User();
        adminUser.setEmail("admin@ord"); adminUser.setFirstName("Ad"); adminUser.setLastName("Min");
        adminUser.setPasswordHash(encoder.encode("Password12"));
        adminUser.setStatus(UserStatus.ALLOCATAIRE); adminUser.setRole(UserRole.ADMIN);
        adminUser.setCountry("FR"); adminUser.setCity("P");
        adminUser.setLocale(Locale.FR); adminUser.setCivility(Civility.NONE);
        adminUser = userRepo.save(adminUser);

        alloUser = new User();
        alloUser.setEmail("allo@ord"); alloUser.setFirstName("A"); alloUser.setLastName("A");
        alloUser.setPasswordHash(encoder.encode("Password12"));
        alloUser.setStatus(UserStatus.ALLOCATAIRE); alloUser.setRole(UserRole.USER);
        alloUser.setTierCode(TierCode.TIER_1); alloUser.setCountry("FR"); alloUser.setCity("P");
        alloUser.setLocale(Locale.FR); alloUser.setCivility(Civility.NONE);
        alloUser = userRepo.save(alloUser);

        Product p = new Product();
        p.setName("X"); p.setPriceEur(new BigDecimal("1"));
        p.setTiers(EnumSet.of(TierCode.TIER_1));
        p.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        p.setDelivery(false);
        p = productRepo.save(p);

        Order o = new Order();
        o.setUser(alloUser); o.setProduct(p);
        o.setUnitPriceEur(p.getPriceEur()); o.setTotalEur(p.getPriceEur());
        o.setStatus(OrderStatus.PAID); o.setPaidAt(Instant.now());
        o.setMolliePaymentId("tr_adm_1");
        o.setProductSnapshot("{\"id\":\"" + p.getId() + "\",\"name\":\"X\",\"priceEur\":\"1.00\",\"tiers\":[\"TIER_1\"],\"delivery\":false}");
        orderRepo.save(o);
    }

    @Test
    void admin_lists_all_orders() throws Exception {
        mvc.perform(get("/api/admin/orders")
            .with(user(new CustomUserDetails(adminUser)))
            .with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content.length()").value(1))
           .andExpect(jsonPath("$.content[0].status").value("PAID"));
    }

    @Test
    void allocataire_forbidden() throws Exception {
        mvc.perform(get("/api/admin/orders")
            .with(user(new CustomUserDetails(alloUser)))
            .with(csrf()))
           .andExpect(status().isForbidden());
    }
}

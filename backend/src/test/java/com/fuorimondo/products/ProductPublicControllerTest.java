package com.fuorimondo.products;

import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.TierCode;
import com.fuorimondo.users.User;
import com.fuorimondo.users.UserRepository;
import com.fuorimondo.users.UserRole;
import com.fuorimondo.users.UserStatus;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductPublicControllerTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    MockMvc mvc;
    User tier1User;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        tier1User = seedUser("allo1@test", TierCode.TIER_1);
    }

    private User seedUser(String email, TierCode tier) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("Allo");
        u.setLastName("Cat");
        u.setPasswordHash(encoder.encode("Password12"));
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setTierCode(tier);
        u.setCountry("FR");
        u.setCity("Paris");
        u.setLocale(com.fuorimondo.users.Locale.FR);
        u.setCivility(com.fuorimondo.users.Civility.NONE);
        return userRepo.save(u);
    }

    private Product seedProduct(String name, BigDecimal price, java.util.Set<TierCode> tiers,
                                 Instant start, Instant end, Integer stock) {
        Product p = new Product();
        p.setName(name);
        p.setPriceEur(price);
        p.setTiers(tiers);
        p.setSaleStartAt(start);
        p.setSaleEndAt(end);
        p.setStock(stock);
        p.setDelivery(true);
        return productRepo.save(p);
    }

    @Test
    void lists_only_products_in_user_tier_and_open_window() throws Exception {
        Instant now = Instant.now();
        Product visible = seedProduct("T1 open", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_1), now.minus(1, ChronoUnit.DAYS), now.plus(30, ChronoUnit.DAYS), null);
        Product wrongTier = seedProduct("T2 open", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_2), now.minus(1, ChronoUnit.DAYS), null, null);
        Product future = seedProduct("T1 future", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_1), now.plus(10, ChronoUnit.DAYS), null, null);
        Product past = seedProduct("T1 past", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_1), now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), null);

        mvc.perform(get("/api/products").with(user(new CustomUserDetails(tier1User))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1))
           .andExpect(jsonPath("$[0].id").value(visible.getId().toString()));
    }

    @Test
    void detail_returns_404_for_wrong_tier() throws Exception {
        Instant now = Instant.now();
        Product t2 = seedProduct("T2 only", new BigDecimal("50.00"),
            EnumSet.of(TierCode.TIER_2), now.minus(1, ChronoUnit.DAYS), null, null);

        mvc.perform(get("/api/products/" + t2.getId()).with(user(new CustomUserDetails(tier1User))))
           .andExpect(status().isNotFound());
    }

    @Test
    void detail_returns_product_when_visible() throws Exception {
        Instant now = Instant.now();
        Product p = seedProduct("T1 open", new BigDecimal("120.00"),
            EnumSet.of(TierCode.TIER_1), now.minus(1, ChronoUnit.DAYS), null, null);

        mvc.perform(get("/api/products/" + p.getId()).with(user(new CustomUserDetails(tier1User))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(p.getId().toString()))
           .andExpect(jsonPath("$.priceEur").value(120.00));
    }

    @Test
    void list_hides_out_of_stock_products() throws Exception {
        Instant now = Instant.now();
        seedProduct("T1 stock0", new BigDecimal("100.00"),
            EnumSet.of(TierCode.TIER_1), now.minus(1, ChronoUnit.DAYS), null, 0);

        mvc.perform(get("/api/products").with(user(new CustomUserDetails(tier1User))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(0));
    }
}

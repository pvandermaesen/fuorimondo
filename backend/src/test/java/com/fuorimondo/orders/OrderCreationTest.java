package com.fuorimondo.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.addresses.Address;
import com.fuorimondo.addresses.AddressRepository;
import com.fuorimondo.addresses.AddressType;
import com.fuorimondo.orders.dto.CreateOrderRequest;
import com.fuorimondo.products.Product;
import com.fuorimondo.products.ProductRepository;
import com.fuorimondo.security.CustomUserDetails;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderCreationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepo;
    @Autowired ProductRepository productRepo;
    @Autowired AddressRepository addressRepo;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    MockMvc mvc;
    User allo;
    Product product;
    Address shipping;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        allo = new User();
        allo.setEmail("buyer@test");
        allo.setFirstName("Buyer"); allo.setLastName("One");
        allo.setPasswordHash(encoder.encode("Password12"));
        allo.setStatus(UserStatus.ALLOCATAIRE); allo.setRole(UserRole.USER);
        allo.setTierCode(TierCode.TIER_1);
        allo.setCountry("FR"); allo.setCity("Paris");
        allo.setLocale(Locale.FR); allo.setCivility(Civility.NONE);
        allo = userRepo.save(allo);

        product = new Product();
        product.setName("T1 Wine");
        product.setPriceEur(new BigDecimal("180.00"));
        product.setTiers(EnumSet.of(TierCode.TIER_1));
        product.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        product.setSaleEndAt(null);
        product.setStock(3);
        product.setDelivery(true);
        product = productRepo.save(product);

        shipping = new Address();
        shipping.setUser(allo);
        shipping.setType(AddressType.SHIPPING);
        shipping.setFullName("Buyer One");
        shipping.setStreet("1 rue du Test"); shipping.setPostalCode("75000");
        shipping.setCity("Paris"); shipping.setCountry("FR");
        shipping.setDefault(true);
        shipping = addressRepo.save(shipping);
    }

    @Test
    void creates_order_and_returns_checkout_url() throws Exception {
        var body = new CreateOrderRequest(product.getId(), shipping.getId());
        mvc.perform(post("/api/orders")
            .with(user(new CustomUserDetails(allo)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.orderId").exists())
           .andExpect(jsonPath("$.checkoutUrl").value(org.hamcrest.Matchers.containsString("/shop/order/")));
    }

    @Test
    void rejects_wrong_tier() throws Exception {
        Product t2 = new Product();
        t2.setName("T2"); t2.setPriceEur(new BigDecimal("90"));
        t2.setTiers(EnumSet.of(TierCode.TIER_2));
        t2.setSaleStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        t2.setStock(1); t2.setDelivery(true);
        t2 = productRepo.save(t2);

        var body = new CreateOrderRequest(t2.getId(), shipping.getId());
        mvc.perform(post("/api/orders")
            .with(user(new CustomUserDetails(allo)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.code").value("tier_mismatch"));
    }

    @Test
    void rejects_out_of_stock() throws Exception {
        product.setStock(0);
        productRepo.save(product);

        var body = new CreateOrderRequest(product.getId(), shipping.getId());
        mvc.perform(post("/api/orders")
            .with(user(new CustomUserDetails(allo)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("out_of_stock"));
    }

    @Test
    void rejects_missing_shipping_for_delivery_product() throws Exception {
        var body = new CreateOrderRequest(product.getId(), null);
        mvc.perform(post("/api/orders")
            .with(user(new CustomUserDetails(allo)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)))
           .andExpect(status().isUnprocessableEntity())
           .andExpect(jsonPath("$.code").value("no_shipping_address"));
    }

    @Test
    void rejects_sale_window_closed() throws Exception {
        product.setSaleEndAt(Instant.now().minus(1, ChronoUnit.DAYS));
        productRepo.save(product);

        var body = new CreateOrderRequest(product.getId(), shipping.getId());
        mvc.perform(post("/api/orders")
            .with(user(new CustomUserDetails(allo)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("sale_window_closed"));
    }
}

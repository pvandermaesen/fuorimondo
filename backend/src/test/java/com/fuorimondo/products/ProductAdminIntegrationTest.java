package com.fuorimondo.products;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.products.dto.ProductRequest;
import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductAdminIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User admin;

    @BeforeEach
    void seed() {
        admin = new User();
        admin.setEmail("prod-admin@fm.com");
        admin.setFirstName("P"); admin.setLastName("Admin");
        admin.setCivility(Civility.NONE);
        admin.setCountry("FR"); admin.setCity("X");
        admin.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        admin.setStatus(UserStatus.ALLOCATAIRE);
        admin.setRole(UserRole.ADMIN);
        admin.setLocale(Locale.FR);
        userRepository.save(admin);
    }

    @Test
    void adminCrudHappyPath() throws Exception {
        ProductRequest createReq = new ProductRequest(
            "Cuvée Test",
            null,
            new BigDecimal("125.00"),
            true,
            null,
            Set.of(TierCode.TIER_1),
            Instant.parse("2026-05-01T00:00:00Z"),
            null,
            10
        );

        // 1. POST /api/admin/products -> 201, non-null id, tiers contains TIER_1
        MvcResult createResult = mvc.perform(post("/api/admin/products")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value("Cuvée Test"))
            .andExpect(jsonPath("$.tiers[0]").value("TIER_1"))
            .andReturn();

        JsonNode created = mapper.readTree(createResult.getResponse().getContentAsByteArray());
        UUID id = UUID.fromString(created.get("id").asText());

        // 2. GET /api/admin/products -> array size 1
        mvc.perform(get("/api/admin/products").with(user(new CustomUserDetails(admin))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id.toString()));

        // 3. GET /api/admin/products/{id} -> name matches posted
        mvc.perform(get("/api/admin/products/" + id).with(user(new CustomUserDetails(admin))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Cuvée Test"));

        // 4. PATCH /api/admin/products/{id} with new name -> 200 + updated name
        ProductRequest updateReq = new ProductRequest(
            "Cuvée Test (updated)",
            null,
            new BigDecimal("125.00"),
            true,
            null,
            Set.of(TierCode.TIER_1),
            Instant.parse("2026-05-01T00:00:00Z"),
            null,
            10
        );
        mvc.perform(patch("/api/admin/products/" + id)
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Cuvée Test (updated)"));

        // 5. DELETE /api/admin/products/{id} -> 204
        mvc.perform(delete("/api/admin/products/" + id)
                .with(user(new CustomUserDetails(admin)))
                .with(csrf()))
            .andExpect(status().isNoContent());

        // 6. GET /api/admin/products/{id} again -> 404
        mvc.perform(get("/api/admin/products/" + id).with(user(new CustomUserDetails(admin))))
            .andExpect(status().isNotFound());
    }
}

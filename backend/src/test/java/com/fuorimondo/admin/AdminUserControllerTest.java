package com.fuorimondo.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.admin.dto.CreateAllocataireRequest;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User admin;
    User regular;

    @BeforeEach
    void seed() {
        userRepository.deleteAll();
        admin = new User();
        admin.setEmail("admin@fm.com");
        admin.setFirstName("A"); admin.setLastName("Dmin");
        admin.setCivility(Civility.NONE);
        admin.setCountry("FR"); admin.setCity("X");
        admin.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        admin.setStatus(UserStatus.ALLOCATAIRE);
        admin.setRole(UserRole.ADMIN);
        admin.setLocale(Locale.FR);
        userRepository.save(admin);

        regular = new User();
        regular.setEmail("reg@fm.com");
        regular.setFirstName("R"); regular.setLastName("E");
        regular.setCivility(Civility.NONE);
        regular.setCountry("FR"); regular.setCity("X");
        regular.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        regular.setStatus(UserStatus.ALLOCATAIRE);
        regular.setRole(UserRole.USER);
        regular.setLocale(Locale.FR);
        userRepository.save(regular);
    }

    @Test
    void nonAdminGets403() throws Exception {
        mvc.perform(get("/api/admin/users").with(user(new CustomUserDetails(regular))))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAllocataireAndGetCode() throws Exception {
        CreateAllocataireRequest req = new CreateAllocataireRequest(
            "new.allo@fm.com", "New", "Allo", Civility.MR, null, "+33",
            "FR", "Paris", TierCode.TIER_2, Locale.FR, null);
        mvc.perform(post("/api/admin/users")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.status").value("ALLOCATAIRE_PENDING"))
            .andExpect(jsonPath("$.code").isNotEmpty());
    }
}

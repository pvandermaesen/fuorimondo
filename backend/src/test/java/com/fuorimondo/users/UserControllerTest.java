package com.fuorimondo.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.dto.UpdateProfileRequest;
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
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User u;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        u = new User();
        u.setEmail("me@fm.com");
        u.setFirstName("Me"); u.setLastName("User");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("Paris");
        u.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setTierCode(TierCode.TIER_1);
        u.setLocale(Locale.FR);
        userRepository.save(u);
    }

    @Test
    void getMeUnauthorized() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void getMeAuthenticated() throws Exception {
        mvc.perform(get("/api/me").with(user(new CustomUserDetails(u))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("me@fm.com"))
            .andExpect(jsonPath("$.tierCode").value("TIER_1"));
    }

    @Test
    void updateMe() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest(
            "New", "Name", Civility.MR, null, "+33100", "FR", "Lyon", Locale.IT);
        mvc.perform(patch("/api/me").with(user(new CustomUserDetails(u))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("New"))
            .andExpect(jsonPath("$.locale").value("IT"));
    }
}

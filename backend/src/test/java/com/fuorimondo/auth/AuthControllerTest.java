package com.fuorimondo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.auth.dto.RegisterWaitingListRequest;
import com.fuorimondo.users.Locale;
import com.fuorimondo.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;

    @Test
    void registerWaitingList201() throws Exception {
        RegisterWaitingListRequest req = new RegisterWaitingListRequest(
            "controller-test@ex.com", "A", "B", null, "FR", "X", null,
            Locale.FR, "aVerySecurePass123!", true, true);
        mvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void registerWithoutAcceptingTermsIs400() throws Exception {
        RegisterWaitingListRequest req = new RegisterWaitingListRequest(
            "no-terms@ex.com", "A", "B", null, "FR", "X", null,
            Locale.FR, "aVerySecurePass123!", false, true);
        mvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}

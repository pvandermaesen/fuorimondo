package com.fuorimondo.addresses;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.addresses.dto.AddressRequest;
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
class AddressControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User u;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();
        u = new User();
        u.setEmail("addr@fm.com");
        u.setFirstName("A"); u.setLastName("B");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("Paris");
        u.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setLocale(Locale.FR);
        userRepository.save(u);
    }

    @Test
    void createAndListAddress() throws Exception {
        AddressRequest req = new AddressRequest(AddressType.BILLING, "A B",
            "1 rue X", null, "75001", "Paris", "FR", true);
        mvc.perform(post("/api/me/addresses").with(user(new CustomUserDetails(u))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/me/addresses").with(user(new CustomUserDetails(u))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("BILLING"))
            .andExpect(jsonPath("$[0].isDefault").value(true));
    }
}

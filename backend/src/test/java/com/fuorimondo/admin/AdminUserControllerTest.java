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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User admin;
    User regular;

    @BeforeEach
    void seed() {
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

    private User seedParrain(String email, String firstName, String lastName) {
        User p = new User();
        p.setEmail(email);
        p.setFirstName(firstName); p.setLastName(lastName);
        p.setCivility(Civility.NONE);
        p.setCountry("FR"); p.setCity("X");
        p.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        p.setStatus(UserStatus.ALLOCATAIRE);
        p.setRole(UserRole.USER);
        p.setLocale(Locale.FR);
        p.setIsParrain(true);
        return userRepository.save(p);
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

    @Test
    void searchParrains_returnsOnlyParrains() throws Exception {
        seedParrain("p1@fm.com", "Alice", "Martin");
        seedParrain("p2@fm.com", "Bob", "Martin");
        // regular (non-parrain, seeded in @BeforeEach) is named "R E" and must NOT show up even though it matches the query

        mvc.perform(get("/api/admin/users/parrains?q=Martin")
                .with(user(new CustomUserDetails(admin))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].email").value(org.hamcrest.Matchers.startsWith("p")));
    }

    @Test
    void patch_togglesIsParrain() throws Exception {
        String body = "{\"isParrain\": true}";
        mvc.perform(patch("/api/admin/users/" + regular.getId())
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isParrain").value(true));
    }

    @Test
    void putParrain_linksToParrain() throws Exception {
        User parrain = seedParrain("p@fm.com", "Papa", "Rain");
        String body = "{\"parrainId\": \"" + parrain.getId() + "\"}";
        mvc.perform(put("/api/admin/users/" + regular.getId() + "/parrain")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parrainId").value(parrain.getId().toString()))
            .andExpect(jsonPath("$.parrainFirstName").value("Papa"))
            .andExpect(jsonPath("$.parrainLastName").value("Rain"));
    }

    @Test
    void putParrain_nullUnlinks() throws Exception {
        User parrain = seedParrain("p@fm.com", "Papa", "Rain");
        regular.setParrain(parrain);
        userRepository.save(regular);

        mvc.perform(put("/api/admin/users/" + regular.getId() + "/parrain")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parrainId\": null}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parrainId").doesNotExist());
    }

    @Test
    void putParrain_selfLinkRejected() throws Exception {
        regular.setIsParrain(true); // make self-target a parrain so we test self-link, not non-parrain
        userRepository.save(regular);
        String body = "{\"parrainId\": \"" + regular.getId() + "\"}";
        mvc.perform(put("/api/admin/users/" + regular.getId() + "/parrain")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void putParrain_nonParrainTargetRejected() throws Exception {
        User other = seedParrain("other@fm.com", "Non", "Parrain");
        other.setIsParrain(false); // explicitly demote
        userRepository.save(other);
        String body = "{\"parrainId\": \"" + other.getId() + "\"}";
        mvc.perform(put("/api/admin/users/" + regular.getId() + "/parrain")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void demotingParrain_preservesFilleulsLink() throws Exception {
        User parrain = seedParrain("p@fm.com", "Papa", "Rain");
        regular.setParrain(parrain);
        userRepository.save(regular);

        // demote parrain
        String body = "{\"isParrain\": false}";
        mvc.perform(patch("/api/admin/users/" + parrain.getId())
                .with(user(new CustomUserDetails(admin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        // filleul still linked to the (now ex-) parrain
        User filleul = userRepository.findById(regular.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(parrain.getId(), filleul.getParrain().getId());

        // and the ex-parrain does not show up in searches anymore
        mvc.perform(get("/api/admin/users/parrains?q=Rain")
                .with(user(new CustomUserDetails(admin))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}

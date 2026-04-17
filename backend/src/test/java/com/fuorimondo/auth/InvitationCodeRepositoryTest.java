package com.fuorimondo.auth;

import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InvitationCodeRepositoryTest {

    @Autowired UserRepository userRepo;
    @Autowired InvitationCodeRepository codeRepo;

    @Test
    void findByCode() {
        User admin = user("admin@fm.com", UserRole.ADMIN);
        User pending = user("x@fm.com", UserRole.USER);
        userRepo.saveAll(java.util.List.of(admin, pending));

        InvitationCode ic = new InvitationCode();
        ic.setUser(pending);
        ic.setCode("ABC123");
        ic.setGeneratedAt(Instant.now());
        ic.setGeneratedBy(admin);
        ic.setExpiresAt(Instant.now().plusSeconds(3600));
        codeRepo.save(ic);

        Optional<InvitationCode> found = codeRepo.findByCode("ABC123");
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("x@fm.com");
    }

    private User user(String email, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("F"); u.setLastName("L");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("X");
        u.setStatus(UserStatus.ALLOCATAIRE_PENDING);
        u.setRole(role);
        u.setLocale(Locale.FR);
        return u;
    }
}

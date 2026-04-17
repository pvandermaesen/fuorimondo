package com.fuorimondo.auth;

import com.fuorimondo.auth.dto.*;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired InvitationCodeRepository codeRepository;
    @Autowired InvitationCodeService codeService;

    @Test
    void registerWaitingListCreatesUser() {
        RegisterWaitingListRequest req = new RegisterWaitingListRequest(
            "new@ex.com", "New", "User", "+33100000000", "France", "Paris",
            "Parrain: X", Locale.FR, "aVerySecurePass123!", true, true);
        User user = authService.registerWaitingList(req);
        assertThat(user.getStatus()).isEqualTo(UserStatus.WAITING_LIST);
        assertThat(user.getPasswordHash()).isNotNull();
    }

    @Test
    void duplicateEmailRejected() {
        RegisterWaitingListRequest req = new RegisterWaitingListRequest(
            "dup@ex.com", "A", "B", null, "FR", "X", null,
            Locale.FR, "aVerySecurePass123!", true, true);
        authService.registerWaitingList(req);
        assertThrows(AuthException.class, () -> authService.registerWaitingList(req));
    }

    @Test
    void activationWithValidCodeSucceeds() {
        User admin = createAdmin();
        User pending = createPending();
        InvitationCode ic = new InvitationCode();
        ic.setUser(pending);
        ic.setCode(codeService.generateCode());
        ic.setGeneratedAt(java.time.Instant.now());
        ic.setGeneratedBy(admin);
        ic.setExpiresAt(codeService.computeExpiration());
        codeRepository.save(ic);

        SetPasswordRequest req = new SetPasswordRequest(
            pending.getEmail(), ic.getCode(), "aVerySecurePass123!");
        User activated = authService.activateAccount(req);
        assertThat(activated.getStatus()).isEqualTo(UserStatus.ALLOCATAIRE);
        assertThat(activated.getPasswordHash()).isNotNull();
    }

    private User createAdmin() {
        User u = baseUser("admin@fm.com");
        u.setRole(UserRole.ADMIN);
        u.setStatus(UserStatus.ALLOCATAIRE);
        return userRepository.save(u);
    }

    private User createPending() {
        User u = baseUser("pending@fm.com");
        u.setStatus(UserStatus.ALLOCATAIRE_PENDING);
        return userRepository.save(u);
    }

    private User baseUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("F"); u.setLastName("L");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("X");
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setLocale(Locale.FR);
        return u;
    }
}

package com.fuorimondo.auth;

import com.fuorimondo.auth.dto.*;
import com.fuorimondo.email.EmailSender;
import com.fuorimondo.users.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final InvitationCodeRepository codeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final int resetExpirationHours;
    private final String frontendBaseUrl;
    private static final SecureRandom RNG = new SecureRandom();

    public AuthService(
        UserRepository userRepository,
        InvitationCodeRepository codeRepository,
        PasswordResetTokenRepository tokenRepository,
        PasswordEncoder passwordEncoder,
        EmailSender emailSender,
        @Value("${fuorimondo.password-reset.expiration-hours:1}") int resetExpirationHours,
        @Value("${fuorimondo.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.resetExpirationHours = resetExpirationHours;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public User registerWaitingList(RegisterWaitingListRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new AuthException(AuthException.Reason.EMAIL_ALREADY_USED, "email already used");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setPhone(req.phone());
        user.setCivility(Civility.NONE);
        user.setCountry(req.country());
        user.setCity(req.city());
        user.setReferrerInfo(req.referrerInfo());
        user.setLocale(req.locale());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setStatus(UserStatus.WAITING_LIST);
        user.setRole(UserRole.USER);
        userRepository.save(user);
        emailSender.sendWaitingListConfirmation(user.getEmail(), user.getFirstName(), user.getLocale());
        return user;
    }

    @Transactional(readOnly = true)
    public void verifyInvitationCode(ActivateRequest req) {
        InvitationCode ic = codeRepository.findByCode(req.code().toUpperCase())
            .orElseThrow(() -> new AuthException(AuthException.Reason.INVALID_CODE, "invalid code"));
        if (!ic.getUser().getEmail().equalsIgnoreCase(req.email())) {
            throw new AuthException(AuthException.Reason.INVALID_CODE, "email/code mismatch");
        }
        if (ic.getUsedAt() != null) {
            throw new AuthException(AuthException.Reason.CODE_ALREADY_USED, "already used");
        }
        if (Instant.now().isAfter(ic.getExpiresAt())) {
            throw new AuthException(AuthException.Reason.CODE_EXPIRED, "expired");
        }
    }

    @Transactional
    public User activateAccount(SetPasswordRequest req) {
        InvitationCode ic = codeRepository.findByCode(req.code().toUpperCase())
            .orElseThrow(() -> new AuthException(AuthException.Reason.INVALID_CODE, "invalid code"));
        if (!ic.getUser().getEmail().equalsIgnoreCase(req.email())) {
            throw new AuthException(AuthException.Reason.INVALID_CODE, "mismatch");
        }
        if (!ic.isValid(Instant.now())) {
            throw new AuthException(AuthException.Reason.CODE_EXPIRED, "invalid or expired");
        }
        User user = ic.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setStatus(UserStatus.ALLOCATAIRE);
        ic.setUsedAt(Instant.now());
        return user;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            byte[] raw = new byte[32];
            RNG.nextBytes(raw);
            String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            String hash = sha256Hex(plainToken);

            tokenRepository.deleteByUserId(user.getId());

            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hash);
            token.setExpiresAt(Instant.now().plus(resetExpirationHours, ChronoUnit.HOURS));
            tokenRepository.save(token);

            String url = frontendBaseUrl + "/reset-password?token=" + plainToken;
            emailSender.sendPasswordResetLink(user.getEmail(), url, user.getLocale());
        });
    }

    @Transactional
    public void confirmPasswordReset(String plainToken, String newPassword) {
        String hash = sha256Hex(plainToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new AuthException(AuthException.Reason.INVALID_TOKEN, "invalid"));
        if (!token.isValid(Instant.now())) {
            throw new AuthException(AuthException.Reason.TOKEN_EXPIRED, "invalid or expired");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(Instant.now());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

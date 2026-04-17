package com.fuorimondo.admin;

import com.fuorimondo.admin.dto.CreateAllocataireRequest;
import com.fuorimondo.admin.dto.UpdateUserByAdminRequest;
import com.fuorimondo.auth.*;
import com.fuorimondo.users.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final InvitationCodeRepository codeRepository;
    private final InvitationCodeService codeService;

    public AdminUserService(UserRepository userRepository,
                             InvitationCodeRepository codeRepository,
                             InvitationCodeService codeService) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.codeService = codeService;
    }

    @Transactional(readOnly = true)
    public Page<User> search(String status, String query, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return userRepository.findByEmailContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                query, query, pageable);
        }
        if (status != null && !status.isBlank()) {
            return userRepository.findByStatus(UserStatus.valueOf(status), pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow();
    }

    @Transactional
    public CreateAllocataireResult createAllocataire(CreateAllocataireRequest req, UUID adminId) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new AuthException(AuthException.Reason.EMAIL_ALREADY_USED, "email already used");
        }
        User admin = userRepository.findById(adminId).orElseThrow();
        User u = new User();
        u.setEmail(req.email());
        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setCivility(req.civility());
        u.setBirthDate(req.birthDate());
        u.setPhone(req.phone());
        u.setCountry(req.country());
        u.setCity(req.city());
        u.setTierCode(req.tierCode());
        u.setLocale(req.locale());
        u.setAdminNotes(req.adminNotes());
        u.setStatus(UserStatus.ALLOCATAIRE_PENDING);
        u.setRole(UserRole.USER);
        userRepository.save(u);

        InvitationCode ic = newCodeFor(u, admin);
        codeRepository.save(ic);
        return new CreateAllocataireResult(u, ic.getCode());
    }

    @Transactional
    public String regenerateCode(UUID userId, UUID adminId) {
        User u = userRepository.findById(userId).orElseThrow();
        User admin = userRepository.findById(adminId).orElseThrow();
        codeRepository.deleteByUserId(userId);
        codeRepository.flush();
        InvitationCode ic = newCodeFor(u, admin);
        codeRepository.save(ic);
        return ic.getCode();
    }

    @Transactional
    public User update(UUID userId, UpdateUserByAdminRequest req) {
        User u = userRepository.findById(userId).orElseThrow();
        if (req.status() != null) u.setStatus(req.status());
        if (req.tierCode() != null) u.setTierCode(req.tierCode());
        if (req.adminNotes() != null) u.setAdminNotes(req.adminNotes());
        return u;
    }

    private InvitationCode newCodeFor(User user, User admin) {
        InvitationCode ic = new InvitationCode();
        ic.setUser(user);
        ic.setCode(codeService.generateCode());
        ic.setGeneratedAt(Instant.now());
        ic.setGeneratedBy(admin);
        ic.setExpiresAt(codeService.computeExpiration());
        return ic;
    }

    public record CreateAllocataireResult(User user, String code) {}
}

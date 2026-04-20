package com.fuorimondo.admin.dto;

import com.fuorimondo.auth.InvitationCode;
import com.fuorimondo.users.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminUserResponse(
    UUID id, String email, String firstName, String lastName, Civility civility,
    LocalDate birthDate, String phone, String country, String city,
    UserStatus status, UserRole role, TierCode tierCode, Locale locale,
    String referrerInfo, String adminNotes, Instant createdAt,
    String invitationCode, Instant invitationCodeExpiresAt, Instant invitationCodeUsedAt,
    boolean isParrain, UUID parrainId, String parrainFirstName, String parrainLastName
) {
    public static AdminUserResponse from(User u) {
        return from(u, null);
    }

    public static AdminUserResponse from(User u, InvitationCode ic) {
        User p = u.getParrain();
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
            u.getCivility(), u.getBirthDate(), u.getPhone(), u.getCountry(), u.getCity(),
            u.getStatus(), u.getRole(), u.getTierCode(), u.getLocale(),
            u.getReferrerInfo(), u.getAdminNotes(), u.getCreatedAt(),
            ic != null ? ic.getCode() : null,
            ic != null ? ic.getExpiresAt() : null,
            ic != null ? ic.getUsedAt() : null,
            u.isParrain(),
            p != null ? p.getId() : null,
            p != null ? p.getFirstName() : null,
            p != null ? p.getLastName() : null);
    }
}

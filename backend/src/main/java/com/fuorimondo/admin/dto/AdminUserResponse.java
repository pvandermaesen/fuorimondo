package com.fuorimondo.admin.dto;

import com.fuorimondo.users.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminUserResponse(
    UUID id, String email, String firstName, String lastName, Civility civility,
    LocalDate birthDate, String phone, String country, String city,
    UserStatus status, UserRole role, TierCode tierCode, Locale locale,
    String referrerInfo, String adminNotes, Instant createdAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
            u.getCivility(), u.getBirthDate(), u.getPhone(), u.getCountry(), u.getCity(),
            u.getStatus(), u.getRole(), u.getTierCode(), u.getLocale(),
            u.getReferrerInfo(), u.getAdminNotes(), u.getCreatedAt());
    }
}

package com.fuorimondo.users.dto;

import com.fuorimondo.users.*;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    Civility civility,
    LocalDate birthDate,
    String phone,
    String country,
    String city,
    UserStatus status,
    UserRole role,
    TierCode tierCode,
    Locale locale
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
            u.getCivility(), u.getBirthDate(), u.getPhone(), u.getCountry(), u.getCity(),
            u.getStatus(), u.getRole(), u.getTierCode(), u.getLocale());
    }
}

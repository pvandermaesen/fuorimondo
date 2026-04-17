package com.fuorimondo.admin.dto;

import com.fuorimondo.users.Civility;
import com.fuorimondo.users.Locale;
import com.fuorimondo.users.TierCode;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateAllocataireRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotNull Civility civility,
    LocalDate birthDate,
    @Size(max = 30) String phone,
    @NotBlank @Size(max = 100) String country,
    @NotBlank @Size(max = 100) String city,
    @NotNull TierCode tierCode,
    @NotNull Locale locale,
    @Size(max = 2000) String adminNotes
) {}

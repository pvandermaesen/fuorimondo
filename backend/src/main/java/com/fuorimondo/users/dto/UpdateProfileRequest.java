package com.fuorimondo.users.dto;

import com.fuorimondo.users.Civility;
import com.fuorimondo.users.Locale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotNull Civility civility,
    LocalDate birthDate,
    @Size(max = 30) String phone,
    @NotBlank @Size(max = 100) String country,
    @NotBlank @Size(max = 100) String city,
    @NotNull Locale locale
) {}

package com.fuorimondo.auth.dto;

import com.fuorimondo.users.Locale;
import jakarta.validation.constraints.*;

public record RegisterWaitingListRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Size(max = 30) String phone,
    @NotBlank @Size(max = 100) String country,
    @NotBlank @Size(max = 100) String city,
    @Size(max = 2000) String referrerInfo,
    @NotNull Locale locale,
    @NotBlank @Size(min = 12, max = 200) String password,
    @AssertTrue(message = "must accept terms") boolean acceptTerms,
    @AssertTrue(message = "must accept privacy") boolean acceptPrivacy
) {}

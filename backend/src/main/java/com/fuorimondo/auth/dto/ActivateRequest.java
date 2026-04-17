package com.fuorimondo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 10) String code
) {}

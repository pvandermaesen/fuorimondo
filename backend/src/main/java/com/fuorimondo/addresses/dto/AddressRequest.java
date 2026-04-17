package com.fuorimondo.addresses.dto;

import com.fuorimondo.addresses.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @NotNull AddressType type,
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank String street,
    @Size(max = 255) String streetExtra,
    @NotBlank @Size(max = 20) String postalCode,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String country,
    boolean isDefault
) {}

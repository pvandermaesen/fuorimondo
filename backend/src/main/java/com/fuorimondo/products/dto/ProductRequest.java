package com.fuorimondo.products.dto;

import com.fuorimondo.users.TierCode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal priceEur,
        @NotNull Boolean delivery,
        @DecimalMin("0.000") BigDecimal weightKg,
        @NotEmpty Set<TierCode> tiers,
        @NotNull Instant saleStartAt,
        Instant saleEndAt,
        @PositiveOrZero Integer stock
) {}

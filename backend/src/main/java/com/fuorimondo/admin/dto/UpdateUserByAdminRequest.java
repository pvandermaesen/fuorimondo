package com.fuorimondo.admin.dto;

import com.fuorimondo.users.TierCode;
import com.fuorimondo.users.UserStatus;
import jakarta.validation.constraints.Size;

public record UpdateUserByAdminRequest(
    UserStatus status,
    TierCode tierCode,
    @Size(max = 2000) String adminNotes,
    Boolean isParrain
) {}

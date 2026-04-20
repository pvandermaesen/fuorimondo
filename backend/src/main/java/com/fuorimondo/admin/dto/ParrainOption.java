package com.fuorimondo.admin.dto;

import com.fuorimondo.users.User;
import java.util.UUID;

public record ParrainOption(UUID id, String firstName, String lastName, String email) {
    public static ParrainOption from(User u) {
        return new ParrainOption(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail());
    }
}

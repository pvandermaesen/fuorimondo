package com.fuorimondo.security;

import com.fuorimondo.users.User;
import com.fuorimondo.users.UserRole;
import com.fuorimondo.users.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Session-safe view of the authenticated principal. Holds only primitive
 * fields extracted from {@link User} at login time — never the JPA entity
 * itself — so that Spring Session serialization stays stable across
 * schema changes to the User entity.
 */
public class CustomUserDetails implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final UserStatus status;

    public CustomUserDetails(User user) {
        this(user.getId(), user.getEmail(), user.getPasswordHash(),
             user.getRole(), user.getStatus());
    }

    public CustomUserDetails(UUID userId, String email, String passwordHash,
                              UserRole role, UserStatus status) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public UUID getUserId() { return userId; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return passwordHash != null && status != UserStatus.ALLOCATAIRE_PENDING;
    }
}

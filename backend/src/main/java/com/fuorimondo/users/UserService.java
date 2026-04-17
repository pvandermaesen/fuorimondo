package com.fuorimondo.users;

import com.fuorimondo.users.dto.UpdateProfileRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow();
    }

    @Transactional
    public User updateProfile(UUID id, UpdateProfileRequest req) {
        User user = getById(id);
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setCivility(req.civility());
        user.setBirthDate(req.birthDate());
        user.setPhone(req.phone());
        user.setCountry(req.country());
        user.setCity(req.city());
        user.setLocale(req.locale());
        return user;
    }

    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        User user = getById(id);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid current password");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }
}

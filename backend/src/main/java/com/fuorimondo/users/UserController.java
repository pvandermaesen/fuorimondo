package com.fuorimondo.users;

import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.dto.ChangePasswordRequest;
import com.fuorimondo.users.dto.UpdateProfileRequest;
import com.fuorimondo.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getMe(@AuthenticationPrincipal CustomUserDetails principal) {
        return UserResponse.from(userService.getById(principal.getUserId()));
    }

    @PatchMapping
    public UserResponse updateMe(@AuthenticationPrincipal CustomUserDetails principal,
                                  @Valid @RequestBody UpdateProfileRequest req) {
        return UserResponse.from(userService.updateProfile(principal.getUserId(), req));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                                @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(principal.getUserId(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}

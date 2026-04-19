package com.fuorimondo.admin;

import com.fuorimondo.admin.dto.AdminUserResponse;
import com.fuorimondo.admin.dto.CreateAllocataireRequest;
import com.fuorimondo.admin.dto.UpdateUserByAdminRequest;
import com.fuorimondo.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) { this.service = service; }

    public record CreateAllocataireResponse(AdminUserResponse user, String code) {}
    public record RegenerateCodeResponse(String code) {}

    @GetMapping
    public Page<AdminUserResponse> list(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String q,
                                         Pageable pageable) {
        return service.search(status, q, pageable).map(AdminUserResponse::from);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable UUID id) {
        var detail = service.getDetail(id);
        return AdminUserResponse.from(detail.user(), detail.code());
    }

    @PostMapping
    public ResponseEntity<CreateAllocataireResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateAllocataireRequest req) {
        var result = service.createAllocataire(req, principal.getUserId());
        return ResponseEntity.status(201).body(
            new CreateAllocataireResponse(AdminUserResponse.from(result.user()), result.code()));
    }

    @PostMapping("/{id}/regenerate-code")
    public RegenerateCodeResponse regenerate(@AuthenticationPrincipal CustomUserDetails principal,
                                              @PathVariable UUID id) {
        return new RegenerateCodeResponse(service.regenerateCode(id, principal.getUserId()));
    }

    @PatchMapping("/{id}")
    public AdminUserResponse update(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateUserByAdminRequest req) {
        return AdminUserResponse.from(service.update(id, req));
    }
}

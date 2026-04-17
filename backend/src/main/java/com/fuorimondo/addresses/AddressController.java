package com.fuorimondo.addresses;

import com.fuorimondo.addresses.dto.AddressRequest;
import com.fuorimondo.addresses.dto.AddressResponse;
import com.fuorimondo.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/addresses")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) { this.service = service; }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return service.listForUser(principal.getUserId()).stream()
            .map(AddressResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @Valid @RequestBody AddressRequest req) {
        return ResponseEntity.status(201).body(AddressResponse.from(service.create(principal.getUserId(), req)));
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal CustomUserDetails principal,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody AddressRequest req) {
        return AddressResponse.from(service.update(principal.getUserId(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable UUID id) {
        service.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}

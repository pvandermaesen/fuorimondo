package com.fuorimondo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationCodeRepository extends JpaRepository<InvitationCode, UUID> {

    Optional<InvitationCode> findByCode(String code);

    Optional<InvitationCode> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}

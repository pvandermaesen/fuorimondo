package com.fuorimondo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class InvitationCodeService {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private final int length;
    private final int expirationDays;

    public InvitationCodeService(
        @Value("${fuorimondo.invitation-code.length:6}") int length,
        @Value("${fuorimondo.invitation-code.expiration-days:90}") int expirationDays
    ) {
        this.length = length;
        this.expirationDays = expirationDays;
    }

    public String generateCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public Instant computeExpiration() {
        return Instant.now().plus(expirationDays, ChronoUnit.DAYS);
    }
}

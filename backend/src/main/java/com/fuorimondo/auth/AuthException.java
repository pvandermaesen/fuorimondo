package com.fuorimondo.auth;

public class AuthException extends RuntimeException {
    public enum Reason {
        EMAIL_ALREADY_USED,
        INVALID_CODE,
        CODE_EXPIRED,
        CODE_ALREADY_USED,
        INVALID_TOKEN,
        TOKEN_EXPIRED
    }

    private final Reason reason;

    public AuthException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}

package com.fuorimondo.common;

import com.fuorimondo.auth.AuthException;
import com.fuorimondo.orders.OrderException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuth(AuthException ex) {
        HttpStatus status = switch (ex.getReason()) {
            case EMAIL_ALREADY_USED -> HttpStatus.CONFLICT;
            case INVALID_CODE, INVALID_TOKEN -> HttpStatus.BAD_REQUEST;
            case CODE_EXPIRED, TOKEN_EXPIRED, CODE_ALREADY_USED -> HttpStatus.GONE;
        };
        log.info("Auth rejected [{}]: {} -> {}", ex.getReason(), ex.getMessage(), status.value());
        return ResponseEntity.status(status).body(ApiError.of(ex.getReason().name().toLowerCase(), ex.getMessage()));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ApiError> handleOrder(OrderException ex) {
        HttpStatus status = switch (ex.getReason()) {
            case PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case TIER_MISMATCH -> HttpStatus.FORBIDDEN;
            case SALE_WINDOW_CLOSED, OUT_OF_STOCK -> HttpStatus.CONFLICT;
            case NO_SHIPPING_ADDRESS, INVALID_SHIPPING_ADDRESS -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PAYMENT_GATEWAY_ERROR -> HttpStatus.BAD_GATEWAY;
        };
        log.info("Order rejected [{}]: {} -> {}", ex.getReason(), ex.getMessage(), status.value());
        return ResponseEntity.status(status).body(ApiError.of(ex.getReason().name().toLowerCase(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        log.info("Validation rejected: {}", details);
        return ResponseEntity.badRequest().body(new ApiError("validation_error", "invalid request", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex) {
        log.info("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiError.of("constraint_violation", ex.getMessage()));
    }
}

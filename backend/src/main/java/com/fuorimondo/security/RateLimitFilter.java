package com.fuorimondo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitConfig> configs;

    public RateLimitFilter(
        @Value("${fuorimondo.rate-limit.login-attempts-per-15min:5}") int loginLimit,
        @Value("${fuorimondo.rate-limit.activation-attempts-per-15min:5}") int activationLimit,
        @Value("${fuorimondo.rate-limit.reset-attempts-per-15min:5}") int resetLimit
    ) {
        Duration window = Duration.ofMinutes(15);
        this.configs = Map.of(
            "/api/auth/login", new RateLimitConfig(loginLimit, window),
            "/api/auth/activate", new RateLimitConfig(activationLimit, window),
            "/api/auth/password-reset/request", new RateLimitConfig(resetLimit, window)
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        RateLimitConfig config = configs.get(path);
        if (config != null && "POST".equals(request.getMethod())) {
            String key = path + ":" + request.getRemoteAddr();
            if (!config.resolveBucket(key).tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"too_many_requests\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

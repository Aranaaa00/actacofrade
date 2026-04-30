package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.AuthResponse;
import com.actacofrade.backend.dto.LoginRequest;
import com.actacofrade.backend.dto.RegisterRequest;
import com.actacofrade.backend.security.LoginRateLimiter;
import com.actacofrade.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String clientKey = resolveClientKey(httpRequest, request.email());
        if (!loginRateLimiter.tryAcquire(clientKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos de inicio de sesi\u00f3n. Int\u00e9ntalo de nuevo m\u00e1s tarde.");
        }
        AuthResponse response = authService.login(request);
        loginRateLimiter.recordSuccess(clientKey);
        return ResponseEntity.ok(response);
    }

    private String resolveClientKey(HttpServletRequest request, String email) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        return ip + "|" + normalizedEmail;
    }
}

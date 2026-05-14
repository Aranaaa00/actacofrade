package com.actacofrade.backend.service;

import com.actacofrade.backend.entity.PasswordResetToken;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.PasswordResetTokenRepository;
import com.actacofrade.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int RAW_TOKEN_BYTES = 32;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long expirationMinutes;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${password-reset.token-expiration-minutes:10080}") long expirationMinutes) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Crea un nuevo token para el usuario indicado. Devuelve el secreto en claro una sola vez
     */
    public String issueTokenFor(User user, User issuedBy) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.invalidateActiveForUser(user.getId(), now);

        byte[] raw = new byte[RAW_TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(secret));
        token.setIssuedBy(issuedBy);
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusMinutes(expirationMinutes));
        tokenRepository.save(token);

        log.info("Token de reset emitido para userId={} (issuedBy={})",
                user.getId(), issuedBy == null ? "self" : issuedBy.getId());
        return secret;
    }

    /**
     * Consume el token, valida la expiración y aplica la nueva contraseña.
     * Lanza IllegalArgumentException si el token no es válido o ha caducado.
     */
    public void consumeAndResetPassword(String secret, String newPassword) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Token de restablecimiento no válido");
        }
        LocalDateTime now = LocalDateTime.now();
        Optional<PasswordResetToken> opt = tokenRepository.findByTokenHash(hash(secret));
        PasswordResetToken token = opt
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new IllegalArgumentException("Token de restablecimiento no válido o caducado"));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setConsumedAt(now);
        tokenRepository.save(token);

        log.info("Contraseña restablecida correctamente para userId={}", user.getId());
    }

    public long expirationMinutes() {
        return expirationMinutes;
    }

    private static String hash(String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}

package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén temporal en memoria para registros pendientes de verificación.
 * NO persiste usuarios en base de datos hasta que se confirma el email.
 *
 * Estrategia:
 *  - Por cada registro generamos un token aleatorio (32 bytes, base64url).
 *  - Guardamos solo el HASH SHA-256 del token (single-use) como clave.
 *  - Indexamos también por email para permitir reenvíos y bloquear duplicados.
 *  - Cada entrada caduca tras {@code TOKEN_EXPIRATION_TIME} minutos.
 */
@Component
public class PendingRegistrationStore {

    private static final Logger log = LoggerFactory.getLogger(PendingRegistrationStore.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final long expirationMinutes;

    /** clave = hash(token), valor = datos de registro pendiente */
    private final ConcurrentHashMap<String, PendingRegistration> byTokenHash = new ConcurrentHashMap<>();
    /** clave = email normalizado, valor = hash(token) más reciente */
    private final ConcurrentHashMap<String, String> tokenHashByEmail = new ConcurrentHashMap<>();

    public PendingRegistrationStore(
            @Value("${email.verification.token-expiration-minutes:30}") long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Crea una nueva entrada pendiente y devuelve el token plano (a enviar por email).
     * Si ya existía una entrada para ese email, se sustituye (invalida la anterior).
     */
    public String create(RegisterRequest request, String encodedPassword) {
        String token = generateToken();
        String tokenHash = sha256(token);

        PendingRegistration pending = new PendingRegistration(
                request.fullName().trim(),
                request.email().trim().toLowerCase(),
                encodedPassword,
                request.roleCode(),
                request.hermandadNombre().trim(),
                Instant.now().plus(Duration.ofMinutes(expirationMinutes))
        );

        String previousHash = tokenHashByEmail.put(pending.email(), tokenHash);
        if (previousHash != null) {
            byTokenHash.remove(previousHash);
        }
        byTokenHash.put(tokenHash, pending);
        return token;
    }

    /** Devuelve los datos pendientes asociados al email (sin consumir el token). */
    public Optional<PendingRegistration> findByEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        String hash = tokenHashByEmail.get(normalized);
        if (hash == null) {
            return Optional.empty();
        }
        PendingRegistration pending = byTokenHash.get(hash);
        if (pending == null || pending.isExpired()) {
            invalidateByEmail(normalized);
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    /**
     * Consume el token: si es válido y no ha expirado, devuelve los datos y
     * elimina la entrada (single-use). Si no, devuelve {@link Optional#empty()}.
     */
    public Optional<PendingRegistration> consume(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256(token);
        PendingRegistration pending = byTokenHash.remove(hash);
        if (pending == null) {
            return Optional.empty();
        }
        tokenHashByEmail.remove(pending.email(), hash);
        if (pending.isExpired()) {
            log.debug("Token de verificación caducado consumido y descartado");
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    /** Invalida la entrada asociada a un email (por ejemplo al reenviar). */
    public void invalidateByEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        String hash = tokenHashByEmail.remove(normalized);
        if (hash != null) {
            byTokenHash.remove(hash);
        }
    }

    /** Limpieza periódica de entradas caducadas (cada 5 minutos). */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void purgeExpired() {
        Instant now = Instant.now();
        byTokenHash.entrySet().removeIf(entry -> {
            if (entry.getValue().expiresAt().isBefore(now)) {
                tokenHashByEmail.remove(entry.getValue().email(), entry.getKey());
                return true;
            }
            return false;
        });
    }

    public long expirationMinutes() {
        return expirationMinutes;
    }

    private static String generateToken() {
        byte[] buffer = new byte[32];
        SECURE_RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    /**
     * Datos del registro pendiente. La contraseña ya viene cifrada con BCrypt.
     */
    public record PendingRegistration(
            String fullName,
            String email,
            String encodedPassword,
            String roleCode,
            String hermandadNombre,
            Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}

package com.actacofrade.backend.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "clave-secreta-de-prueba-suficientemente-larga-para-hs256");
        ReflectionTestUtils.setField(service, "expirationMs", 3_600_000L);
    }

    private UserDetails details(String email) {
        return new User(email, "pass", Collections.emptyList());
    }

    @Test
    void generateToken_returnsParseableToken() {
        String token = service.generateToken("u@e.com");
        assertThat(token).isNotBlank().contains(".");
        assertThat(service.extractEmail(token)).isEqualTo("u@e.com");
    }

    @Test
    void isTokenValid_trueForMatchingUser() {
        String token = service.generateToken("u@e.com");
        assertThat(service.isTokenValid(token, details("u@e.com"))).isTrue();
    }

    @Test
    void isTokenValid_falseForDifferentUser() {
        String token = service.generateToken("a@e.com");
        assertThat(service.isTokenValid(token, details("b@e.com"))).isFalse();
    }

    @Test
    void isTokenValid_falseForExpired() {
        ReflectionTestUtils.setField(service, "expirationMs", -1_000L);
        String token = service.generateToken("u@e.com");
        assertThatThrownBy(() -> service.isTokenValid(token, details("u@e.com")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractEmail_invalidTokenThrows() {
        assertThatThrownBy(() -> service.extractEmail("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void differentSecret_failsValidation() {
        String token = service.generateToken("u@e.com");
        ReflectionTestUtils.setField(service, "secret", "otra-clave-distinta-mas-de-32-caracteres-actacof");
        assertThatThrownBy(() -> service.extractEmail(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateConfiguration_acceptsLongSecret() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "secret", "clave-secreta-de-prueba-suficientemente-larga-para-hs256");
        ReflectionTestUtils.setField(svc, "expirationMs", 1000L);
        ReflectionTestUtils.invokeMethod(svc, "validateConfiguration");
    }

    @Test
    void validateConfiguration_rejectsNullSecret() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "secret", null);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(svc, "validateConfiguration"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void validateConfiguration_rejectsBlankSecret() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "secret", "   ");
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(svc, "validateConfiguration"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateConfiguration_rejectsShortSecret() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "secret", "corto");
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(svc, "validateConfiguration"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}

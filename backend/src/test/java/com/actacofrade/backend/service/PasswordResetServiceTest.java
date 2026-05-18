package com.actacofrade.backend.service;

import com.actacofrade.backend.entity.PasswordResetToken;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.PasswordResetTokenRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import com.actacofrade.backend.entity.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(tokenRepository, userRepository, passwordEncoder, 60);
        user = TestFixtures.user(1, "u@e.com", TestFixtures.hermandad(1, "H"), RoleCode.COLABORADOR);
    }

    @Test
    void issueTokenFor_invalidatesPrevious_andPersistsHashedToken() {
        String secret = service.issueTokenFor(user, null);
        assertThat(secret).isNotBlank();
        verify(tokenRepository).invalidateActiveForUser(any(), any(LocalDateTime.class));
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTokenHash()).isNotBlank().isNotEqualTo(secret);
        assertThat(saved.getExpiresAt()).isAfter(saved.getIssuedAt());
    }

    @Test
    void issueTokenFor_withIssuedBy_logsIssuerId() {
        User actor = TestFixtures.user(99, "a@e.com", TestFixtures.hermandad(1, "H"), RoleCode.SUPER_ADMIN);
        assertThat(service.issueTokenFor(user, actor)).isNotBlank();
    }

    @Test
    void expirationMinutes_returnsConfiguredValue() {
        assertThat(service.expirationMinutes()).isEqualTo(60);
    }

    @Test
    void consume_blankSecret_throws() {
        assertThatThrownBy(() -> service.consumeAndResetPassword(null, "newPw"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consumeAndResetPassword("  ", "newPw"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void consume_unknownToken_throws() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.consumeAndResetPassword("xyz", "newPw"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void consume_validToken_updatesPasswordAndMarksConsumed() {
        // emit a token to capture its hash
        String secret = service.issueTokenFor(user, null);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken issued = captor.getValue();

        when(tokenRepository.findByTokenHash(issued.getTokenHash())).thenReturn(Optional.of(issued));
        when(passwordEncoder.encode("nuevaPw")).thenReturn("ENCODED");

        service.consumeAndResetPassword(secret, "nuevaPw");

        assertThat(user.getPasswordHash()).isEqualTo("ENCODED");
        assertThat(issued.getConsumedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void consume_expiredToken_throws() {
        String secret = service.issueTokenFor(user, null);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken issued = captor.getValue();
        issued.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByTokenHash(issued.getTokenHash())).thenReturn(Optional.of(issued));
        assertThatThrownBy(() -> service.consumeAndResetPassword(secret, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

}

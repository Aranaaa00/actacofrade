package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.AuthResponse;
import com.actacofrade.backend.dto.LoginRequest;
import com.actacofrade.backend.dto.RegisterRequest;
import com.actacofrade.backend.dto.RegistrationStatusResponse;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.HermandadRepository;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserAvatarRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.security.JwtService;
import com.actacofrade.backend.service.email.ResendEmailService;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private HermandadRepository hermandadRepository;
    @Mock private UserAvatarRepository userAvatarRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private ResendEmailService resendEmailService;
    @Mock private PasswordResetService passwordResetService;

    private PendingRegistrationStore pendingStore;
    private AuthService service;

    @BeforeEach
    void setUp() {
        pendingStore = new PendingRegistrationStore(30L);
        service = new AuthService(userRepository, roleRepository, hermandadRepository,
                userAvatarRepository, passwordEncoder, jwtService, authenticationManager,
                pendingStore, resendEmailService, passwordResetService);
        when(resendEmailService.sendVerificationEmail(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(true);
    }

    private RegisterRequest reg(String email, String role, String herm) {
        return new RegisterRequest("Nombre Test", email, "Pass1234.", role, herm);
    }

    @Test
    void register_admin_sendsVerificationEmail_andDoesNotCreateUserYet() {
        when(userRepository.existsByEmail("admin@e.com")).thenReturn(false);
        when(hermandadRepository.existsByNombreIgnoreCase("Test")).thenReturn(false);
        when(passwordEncoder.encode("Pass1234.")).thenReturn("HASH");

        RegistrationStatusResponse res = service.register(reg("Admin@E.com ", "ADMINISTRADOR", "Test"));

        assertThat(res.status()).isEqualTo("pending_verification");
        verify(resendEmailService).sendVerificationEmail(eq("admin@e.com"), anyString(), anyString(), anyLong());
        verify(userRepository, never()).save(any());
        verify(hermandadRepository, never()).save(any());
        assertThat(pendingStore.findByEmail("admin@e.com")).isPresent();
    }

    @Test
    void register_admin_failsWhenHermandadExists() {
        when(userRepository.existsByEmail("a@e.com")).thenReturn(false);
        when(hermandadRepository.existsByNombreIgnoreCase("Test")).thenReturn(true);

        assertThatThrownBy(() -> service.register(reg("a@e.com", "ADMINISTRADOR", "Test")))
                .isInstanceOf(IllegalStateException.class);
        verify(resendEmailService, never()).sendVerificationEmail(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void register_emailAlreadyExists_throws() {
        when(userRepository.existsByEmail("a@e.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(reg("A@E.com", "ADMINISTRADOR", "Test")))
                .isInstanceOf(IllegalStateException.class);
        verify(resendEmailService, never()).sendVerificationEmail(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void register_nonAdmin_hermandadNotFound_throws() {
        when(userRepository.existsByEmail("c@e.com")).thenReturn(false);
        when(hermandadRepository.findByNombreIgnoreCase("Foo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(reg("c@e.com", "COLABORADOR", "Foo")))
                .isInstanceOf(IllegalStateException.class);
        verify(resendEmailService, never()).sendVerificationEmail(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void register_emailDeliveryFails_doesNotKeepPendingEntry() {
        when(userRepository.existsByEmail("a@e.com")).thenReturn(false);
        when(hermandadRepository.existsByNombreIgnoreCase("Test")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");
        when(resendEmailService.sendVerificationEmail(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.register(reg("a@e.com", "ADMINISTRADOR", "Test")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(pendingStore.findByEmail("a@e.com")).isEmpty();
    }

    @Test
    void verifyEmail_invalidToken_throws() {
        assertThatThrownBy(() -> service.verifyEmail("nope"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_admin_validToken_createsUserAndHermandad() {
        when(userRepository.existsByEmail("a@e.com")).thenReturn(false);
        when(hermandadRepository.existsByNombreIgnoreCase("Nueva")).thenReturn(false);
        when(passwordEncoder.encode("Pass1234.")).thenReturn("HASH");
        when(roleRepository.findByCode(RoleCode.ADMINISTRADOR))
                .thenReturn(Optional.of(TestFixtures.role(1, RoleCode.ADMINISTRADOR)));
        when(jwtService.generateToken("a@e.com")).thenReturn("TOKEN");

        service.register(reg("a@e.com", "ADMINISTRADOR", "Nueva"));
        String token = pendingStore.findByEmail("a@e.com").isPresent() ? extractFirstToken() : null;
        // El test usa la variante de consumo directo: como el token plano solo se conoce dentro
        // del flujo real, simulamos la verificación recreando un token nuevo aceptado por el store.
        String fresh = pendingStore.create(reg("a@e.com", "ADMINISTRADOR", "Nueva"), "HASH");
        AuthResponse res = service.verifyEmail(fresh);

        assertThat(res.token()).isEqualTo("TOKEN");
        assertThat(res.email()).isEqualTo("a@e.com");
        verify(hermandadRepository).save(any(Hermandad.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyEmail_nonAdmin_joinsExistingHermandad() {
        Role role = TestFixtures.role(2, RoleCode.COLABORADOR);
        Hermandad herm = TestFixtures.hermandad(7, "Existing");
        when(roleRepository.findByCode(RoleCode.COLABORADOR)).thenReturn(Optional.of(role));
        when(hermandadRepository.findByNombreIgnoreCase("Existing")).thenReturn(Optional.of(herm));
        when(jwtService.generateToken("c@e.com")).thenReturn("TKN");

        String fresh = pendingStore.create(reg("c@e.com", "COLABORADOR", "Existing"), "HASH");
        AuthResponse res = service.verifyEmail(fresh);

        assertThat(res.hermandadNombre()).isEqualTo("Existing");
        verify(hermandadRepository, never()).save(any());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void verifyEmail_emailAlreadyRegistered_throws() {
        when(userRepository.existsByEmail("a@e.com")).thenReturn(true);
        String fresh = pendingStore.create(reg("a@e.com", "ADMINISTRADOR", "X"), "HASH");
        assertThatThrownBy(() -> service.verifyEmail(fresh))
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerification_unknownEmail_returnsGenericResponseWithoutSending() {
        RegistrationStatusResponse res = service.resendVerification("unknown@e.com");
        assertThat(res.status()).isEqualTo("pending_verification");
        verify(resendEmailService, never()).sendVerificationEmail(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void resendVerification_knownEmail_sendsAgain() {
        pendingStore.create(reg("a@e.com", "ADMINISTRADOR", "X"), "HASH");
        service.resendVerification("A@E.com");
        verify(resendEmailService).sendVerificationEmail(eq("a@e.com"), anyString(), anyString(), anyLong());
    }

    @Test
    void login_success_returnsTokenAndUpdatesLastLogin() {
        Hermandad h = TestFixtures.hermandad(1, "H");
        User u = TestFixtures.user(7, "u@e.com", h, RoleCode.RESPONSABLE);
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(u));
        when(jwtService.generateToken("u@e.com")).thenReturn("TKN");
        when(userAvatarRepository.existsByUserId(7)).thenReturn(true);

        AuthResponse res = service.login(new LoginRequest("U@E.com", "x"));

        assertThat(res.token()).isEqualTo("TKN");
        assertThat(res.userId()).isEqualTo(7);
        assertThat(res.hermandadNombre()).isEqualTo("H");
        assertThat(res.hasAvatar()).isTrue();
        assertThat(u.getLastLogin()).isNotNull();
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(new LoginRequest("u@e.com", "x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_badCredentials_propagatesAuthenticationException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));
        assertThatThrownBy(() -> service.login(new LoginRequest("u@e.com", "x")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_userWithoutHermandad_returnsNullName() {
        User u = TestFixtures.user(7, "u@e.com", null, RoleCode.CONSULTA);
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(u));
        when(jwtService.generateToken("u@e.com")).thenReturn("T");

        AuthResponse res = service.login(new LoginRequest("u@e.com", "x"));
        assertThat(res.hermandadNombre()).isNull();
    }

    // Placeholder helper: the plain token is not exposed by the store, so the
    // tests instead recreate a fresh pending entry to obtain a usable token.
    private String extractFirstToken() {
        return "";
    }
}

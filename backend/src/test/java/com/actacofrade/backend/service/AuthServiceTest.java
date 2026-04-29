package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.AuthResponse;
import com.actacofrade.backend.dto.LoginRequest;
import com.actacofrade.backend.dto.RegisterRequest;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.HermandadRepository;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserAvatarRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.security.JwtService;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private HermandadRepository hermandadRepository;
    @Mock private UserAvatarRepository userAvatarRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, roleRepository, hermandadRepository,
                userAvatarRepository, passwordEncoder, jwtService, authenticationManager);
    }

    private RegisterRequest reg(String email, String role, String herm) {
        return new RegisterRequest("Nombre Test", email, "Pass1234.", role, herm);
    }

    @Test
    void register_admin_createsHermandadAndUser() {
        when(userRepository.existsByEmail("admin@e.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ADMINISTRADOR))
                .thenReturn(Optional.of(TestFixtures.role(1, RoleCode.ADMINISTRADOR)));
        when(hermandadRepository.existsByNombreIgnoreCase("Test")).thenReturn(false);
        when(passwordEncoder.encode("Pass1234.")).thenReturn("HASH");
        when(jwtService.generateToken("admin@e.com")).thenReturn("TOKEN");

        AuthResponse res = service.register(reg("Admin@E.com ", "ADMINISTRADOR", "Test"));

        assertThat(res.token()).isEqualTo("TOKEN");
        assertThat(res.email()).isEqualTo("admin@e.com");
        assertThat(res.roles()).containsExactly("ADMINISTRADOR");
        verify(hermandadRepository).save(any(Hermandad.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_admin_failsWhenHermandadExists() {
        when(userRepository.existsByEmail("a@e.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ADMINISTRADOR))
                .thenReturn(Optional.of(TestFixtures.role(1, RoleCode.ADMINISTRADOR)));
        when(hermandadRepository.existsByNombreIgnoreCase("Test")).thenReturn(true);

        assertThatThrownBy(() -> service.register(reg("a@e.com", "ADMINISTRADOR", "Test")))
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailAlreadyExists_throws() {
        when(userRepository.existsByEmail("a@e.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(reg("A@E.com", "ADMINISTRADOR", "Test")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void register_unknownRole_throws() {
        when(userRepository.existsByEmail("a@e.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.RESPONSABLE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.register(reg("a@e.com", "RESPONSABLE", "T")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_nonAdmin_joinsExistingHermandad() {
        Role role = TestFixtures.role(1, RoleCode.COLABORADOR);
        Hermandad herm = TestFixtures.hermandad(7, "Existing");
        when(userRepository.existsByEmail("c@e.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.COLABORADOR)).thenReturn(Optional.of(role));
        when(hermandadRepository.findByNombreIgnoreCase("Existing")).thenReturn(Optional.of(herm));
        when(passwordEncoder.encode("Pass1234.")).thenReturn("H");
        when(jwtService.generateToken("c@e.com")).thenReturn("TKN");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        AuthResponse res = service.register(reg("c@e.com", "COLABORADOR", "Existing"));

        verify(userRepository).save(captor.capture());
        verify(hermandadRepository, never()).save(any());
        assertThat(captor.getValue().getHermandad()).isEqualTo(herm);
        assertThat(res.hermandadNombre()).isEqualTo("Existing");
    }

    @Test
    void register_nonAdmin_hermandadNotFound_throws() {
        Role role = TestFixtures.role(1, RoleCode.COLABORADOR);
        when(userRepository.existsByEmail("c@e.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.COLABORADOR)).thenReturn(Optional.of(role));
        when(hermandadRepository.findByNombreIgnoreCase("Foo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(reg("c@e.com", "COLABORADOR", "Foo")))
                .isInstanceOf(IllegalStateException.class);
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
}

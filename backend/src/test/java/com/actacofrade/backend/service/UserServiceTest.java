package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.RoleStatsResponse;
import com.actacofrade.backend.dto.UserCreateRequest;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.dto.UserUpdateRequest;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserAvatarRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserAvatarRepository userAvatarRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService service;

    private Hermandad hermandad;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, roleRepository, userAvatarRepository, passwordEncoder);
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
    }

    @Test
    void create_collaborator_success() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmail("nuevo@e.com")).thenReturn(false);
        Role role = TestFixtures.role(2, RoleCode.COLABORADOR);
        when(roleRepository.findByCode(RoleCode.COLABORADOR)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Pass1234.")).thenReturn("HASH");

        UserCreateRequest req = new UserCreateRequest("Nombre", "nuevo@e.com", "Pass1234.", "COLABORADOR");
        UserResponse res = service.create(req, "admin@e.com");

        assertThat(res.email()).isEqualTo("nuevo@e.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_emailAlreadyExists_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmail("x@e.com")).thenReturn(true);
        UserCreateRequest req = new UserCreateRequest("N", "x@e.com", "p", "COLABORADOR");
        assertThatThrownBy(() -> service.create(req, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_administradorRole_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmail("x@e.com")).thenReturn(false);
        UserCreateRequest req = new UserCreateRequest("N", "x@e.com", "p", "ADMINISTRADOR");
        assertThatThrownBy(() -> service.create(req, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_userWithoutHermandad_throws() {
        User u = TestFixtures.user(99, "x@e.com", null, RoleCode.ADMINISTRADOR);
        when(userRepository.findByEmail("x@e.com")).thenReturn(Optional.of(u));
        UserCreateRequest req = new UserCreateRequest("N", "y@e.com", "p", "COLABORADOR");
        assertThatThrownBy(() -> service.create(req, "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_unknownEmail_throws() {
        when(userRepository.findByEmail("x@e.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(new UserCreateRequest("N", "y@e.com", "p", "COLABORADOR"), "x@e.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findAll_returnsResponses() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByHermandadId(1)).thenReturn(List.of(admin));
        assertThat(service.findAll("admin@e.com")).hasSize(1);
    }

    @Test
    void findAssignable_excludesConsulta() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findAssignableByHermandadId(1, RoleCode.CONSULTA)).thenReturn(List.of(admin));
        assertThat(service.findAssignable("admin@e.com")).hasSize(1);
    }

    @Test
    void findById_returnsUser() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(1, 1)).thenReturn(Optional.of(admin));
        assertThat(service.findById(1, "admin@e.com").id()).isEqualTo(1);
    }

    @Test
    void findById_outsideHermandad_denied() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(99, 1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99, "admin@e.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void countByRole_aggregates() {
        User resp = TestFixtures.user(2, "r@e.com", hermandad, RoleCode.RESPONSABLE);
        User col = TestFixtures.user(3, "c@e.com", hermandad, RoleCode.COLABORADOR);
        User cons = TestFixtures.user(4, "k@e.com", hermandad, RoleCode.CONSULTA);
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByHermandadId(1)).thenReturn(List.of(admin, resp, col, cons));

        RoleStatsResponse stats = service.countByRole("admin@e.com");
        assertThat(stats.administradores()).isEqualTo(1);
        assertThat(stats.responsables()).isEqualTo(1);
        assertThat(stats.colaboradores()).isEqualTo(1);
        assertThat(stats.consulta()).isEqualTo(1);
    }

    @Test
    void update_changesAllFields() {
        User target = TestFixtures.user(2, "old@e.com", hermandad, RoleCode.COLABORADOR);
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(2, 1)).thenReturn(Optional.of(target));
        Role role = TestFixtures.role(2, RoleCode.RESPONSABLE);
        when(roleRepository.findByCode(RoleCode.RESPONSABLE)).thenReturn(Optional.of(role));

        UserUpdateRequest req = new UserUpdateRequest("Nuevo Nombre", "Nuevo@E.com", "RESPONSABLE");
        UserResponse res = service.update(2, req, "admin@e.com");
        assertThat(res.email()).isEqualTo("nuevo@e.com");
        assertThat(res.fullName()).isEqualTo("Nuevo Nombre");
    }

    @Test
    void update_unknownRole_throws() {
        User target = TestFixtures.user(2, "x@e.com", hermandad, RoleCode.COLABORADOR);
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(2, 1)).thenReturn(Optional.of(target));
        when(roleRepository.findByCode(RoleCode.RESPONSABLE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(2, new UserUpdateRequest(null, null, "RESPONSABLE"), "admin@e.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_assignAdministrador_isBlocked() {
        User target = TestFixtures.user(2, "x@e.com", hermandad, RoleCode.COLABORADOR);
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(2, 1)).thenReturn(Optional.of(target));
        UserUpdateRequest req = new UserUpdateRequest(null, null, "ADMINISTRADOR");
        assertThatThrownBy(() -> service.update(2, req, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_changeRoleOfAdministrador_isBlocked() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(1, 1)).thenReturn(Optional.of(admin));
        UserUpdateRequest req = new UserUpdateRequest(null, null, "COLABORADOR");
        assertThatThrownBy(() -> service.update(1, req, "admin@e.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toggleActive_flipsValue() {
        User target = TestFixtures.user(2, "o@e.com", hermandad, RoleCode.COLABORADOR);
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(2, 1)).thenReturn(Optional.of(target));
        UserResponse res = service.toggleActive(2, "admin@e.com");
        assertThat(res.active()).isFalse();
    }

    @Test
    void toggleActive_self_denied() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(1, 1)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.toggleActive(1, "admin@e.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void delete_otherUser_succeeds() {
        User other = TestFixtures.user(2, "o@e.com", hermandad, RoleCode.COLABORADOR);
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(2, 1)).thenReturn(Optional.of(other));
        service.delete(2, "admin@e.com");
        verify(userRepository).delete(other);
    }

    @Test
    void delete_self_denied() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(1, 1)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.delete(1, "admin@e.com"))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).delete(any(User.class));
    }
}

package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.AdminChangeRequestApprove;
import com.actacofrade.backend.dto.AdminChangeRequestCreate;
import com.actacofrade.backend.dto.AdminChangeRequestResponse;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.entity.AdminChangeRequest;
import com.actacofrade.backend.entity.AdminChangeRequestStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.SupportRequestType;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.AdminChangeRequestRepository;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminChangeRequestServiceTest {

    @Mock private AdminChangeRequestRepository requestRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    private AdminChangeRequestService service;
    private Hermandad hermandad;
    private User requester;
    private User candidate;
    private User currentAdmin;

    @BeforeEach
    void setUp() {
        service = new AdminChangeRequestService(requestRepository, userRepository, roleRepository);
        hermandad = TestFixtures.hermandad(1, "H");
        requester = TestFixtures.user(10, "req@e.com", hermandad, RoleCode.COLABORADOR);
        candidate = TestFixtures.user(11, "cand@e.com", hermandad, RoleCode.COLABORADOR);
        currentAdmin = TestFixtures.user(12, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
    }

    private AdminChangeRequest pending(SupportRequestType type) {
        AdminChangeRequest r = new AdminChangeRequest();
        r.setId(100);
        r.setHermandad(hermandad);
        r.setRequester(requester);
        r.setType(type);
        r.setMessage("hola hola hola");
        r.setStatus(AdminChangeRequestStatus.PENDING);
        return r;
    }

    // create

    @Test
    void create_happyPath() {
        when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
        AdminChangeRequestResponse r = service.create(
                new AdminChangeRequestCreate(SupportRequestType.ADMIN_CHANGE, "mensaje suficientemente largo"),
                requester.getEmail());
        assertThat(r.status()).isEqualTo("PENDING");
        assertThat(r.type()).isEqualTo("ADMIN_CHANGE");
    }

    @Test
    void create_resolvedTypeDefaultsToAdminChange() {
        when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
        AdminChangeRequestResponse r = service.create(
                new AdminChangeRequestCreate(null, "mensaje suficientemente largo"),
                requester.getEmail());
        assertThat(r.type()).isEqualTo("ADMIN_CHANGE");
    }

    @Test
    void create_requesterWithoutHermandad_throws() {
        User noH = TestFixtures.user(20, "noh@e.com", null, RoleCode.COLABORADOR);
        when(userRepository.findByEmail(noH.getEmail())).thenReturn(Optional.of(noH));
        assertThatThrownBy(() -> service.create(
                new AdminChangeRequestCreate(SupportRequestType.CONTACT, "mensaje largo de prueba"),
                noH.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_superAdmin_throws() {
        User sa = TestFixtures.user(30, "sa@e.com", hermandad, RoleCode.SUPER_ADMIN);
        when(userRepository.findByEmail(sa.getEmail())).thenReturn(Optional.of(sa));
        assertThatThrownBy(() -> service.create(
                new AdminChangeRequestCreate(SupportRequestType.CONTACT, "mensaje largo de prueba"),
                sa.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_unknownRequester_throws() {
        when(userRepository.findByEmail("ghost@e.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(
                new AdminChangeRequestCreate(SupportRequestType.CONTACT, "mensaje largo"),
                "ghost@e.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // findAll / findById

    @Test
    void findAll_returnsMappedResponses() {
        when(requestRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pending(SupportRequestType.ADMIN_CHANGE)));
        List<AdminChangeRequestResponse> all = service.findAll();
        assertThat(all).hasSize(1);
    }

    @Test
    void findById_missing_throws() {
        when(requestRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findById_found_returns() {
        AdminChangeRequest req = pending(SupportRequestType.ADMIN_CHANGE);
        when(requestRepository.findById(100)).thenReturn(Optional.of(req));
        assertThat(service.findById(100).id()).isEqualTo(100);
    }

    // findCandidates

    @Test
    void findCandidates_nonAdminChange_returnsEmpty() {
        when(requestRepository.findById(100)).thenReturn(Optional.of(pending(SupportRequestType.CONTACT)));
        assertThat(service.findCandidates(100)).isEmpty();
    }

    @Test
    void findCandidates_filtersAdminAndSuperAdmin() {
        when(requestRepository.findById(100)).thenReturn(Optional.of(pending(SupportRequestType.ADMIN_CHANGE)));
        User superAdmin = TestFixtures.user(50, "sa@e.com", hermandad, RoleCode.SUPER_ADMIN);
        User inactive = TestFixtures.user(51, "in@e.com", hermandad, RoleCode.COLABORADOR);
        inactive.setActive(false);
        when(userRepository.findByHermandadId(hermandad.getId()))
                .thenReturn(List.of(candidate, currentAdmin, superAdmin, inactive));
        List<UserResponse> result = service.findCandidates(100);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(candidate.getId());
    }

    // approve

    @Test
    void approve_happyPath_setsAdministratorRole() {
        AdminChangeRequest req = pending(SupportRequestType.ADMIN_CHANGE);
        when(requestRepository.findById(100)).thenReturn(Optional.of(req));
        when(userRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
        Role adminRole = TestFixtures.role(99, RoleCode.ADMINISTRADOR);
        when(roleRepository.findByCode(RoleCode.ADMINISTRADOR)).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmail("super@e.com")).thenReturn(Optional.empty());

        AdminChangeRequestResponse r = service.approve(100,
                new AdminChangeRequestApprove(candidate.getId()), "super@e.com");
        assertThat(r.status()).isEqualTo("APPROVED");
        assertThat(candidate.getRoles()).extracting(Role::getCode).containsExactly(RoleCode.ADMINISTRADOR);
    }

    @Test
    void approve_notPending_throws() {
        AdminChangeRequest req = pending(SupportRequestType.ADMIN_CHANGE);
        req.setStatus(AdminChangeRequestStatus.APPROVED);
        when(requestRepository.findById(100)).thenReturn(Optional.of(req));
        assertThatThrownBy(() -> service.approve(100,
                new AdminChangeRequestApprove(candidate.getId()), "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approve_wrongType_throws() {
        when(requestRepository.findById(100)).thenReturn(Optional.of(pending(SupportRequestType.CONTACT)));
        assertThatThrownBy(() -> service.approve(100,
                new AdminChangeRequestApprove(candidate.getId()), "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approve_candidateDifferentHermandad_throws() {
        when(requestRepository.findById(100)).thenReturn(Optional.of(pending(SupportRequestType.ADMIN_CHANGE)));
        User other = TestFixtures.user(60, "o@e.com", TestFixtures.hermandad(2, "Otra"), RoleCode.COLABORADOR);
        when(userRepository.findById(60)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.approve(100,
                new AdminChangeRequestApprove(60), "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approve_inactiveCandidate_throws() {
        when(requestRepository.findById(100)).thenReturn(Optional.of(pending(SupportRequestType.ADMIN_CHANGE)));
        candidate.setActive(false);
        when(userRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
        assertThatThrownBy(() -> service.approve(100,
                new AdminChangeRequestApprove(candidate.getId()), "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approve_superAdminTarget_throws() {
        when(requestRepository.findById(100)).thenReturn(Optional.of(pending(SupportRequestType.ADMIN_CHANGE)));
        User sa = TestFixtures.user(70, "sa@e.com", hermandad, RoleCode.SUPER_ADMIN);
        when(userRepository.findById(70)).thenReturn(Optional.of(sa));
        assertThatThrownBy(() -> service.approve(100,
                new AdminChangeRequestApprove(70), "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approve_candidateNotFound_throws() {
        when(requestRepository.findById(100)).thenReturn(Optional.of(pending(SupportRequestType.ADMIN_CHANGE)));
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.approve(100,
                new AdminChangeRequestApprove(999), "x@e.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // reject

    @Test
    void reject_setsStatusRejected() {
        AdminChangeRequest req = pending(SupportRequestType.CONTACT);
        when(requestRepository.findById(100)).thenReturn(Optional.of(req));
        when(userRepository.findByEmail("sa@e.com")).thenReturn(Optional.empty());
        AdminChangeRequestResponse r = service.reject(100, "sa@e.com");
        assertThat(r.status()).isEqualTo("REJECTED");
    }

    // resolve

    @Test
    void resolve_nonAdminChange_setsApproved() {
        AdminChangeRequest req = pending(SupportRequestType.VERIFICATION);
        when(requestRepository.findById(100)).thenReturn(Optional.of(req));
        when(userRepository.findByEmail("sa@e.com")).thenReturn(Optional.empty());
        AdminChangeRequestResponse r = service.resolve(100, "sa@e.com");
        assertThat(r.status()).isEqualTo("APPROVED");
    }

    @Test
    void resolve_adminChange_throws() {
        when(requestRepository.findById(100)).thenReturn(Optional.of(pending(SupportRequestType.ADMIN_CHANGE)));
        assertThatThrownBy(() -> service.resolve(100, "sa@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }
}

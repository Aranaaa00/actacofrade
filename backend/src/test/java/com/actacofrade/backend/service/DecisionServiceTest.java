package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateDecisionRequest;
import com.actacofrade.backend.dto.DecisionResponse;
import com.actacofrade.backend.dto.UpdateDecisionRequest;
import com.actacofrade.backend.entity.Decision;
import com.actacofrade.backend.entity.DecisionStatus;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.DecisionRepository;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import com.actacofrade.backend.util.AuthorizationHelper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock private DecisionRepository decisionRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private final AuthorizationHelper authorizationHelper = new AuthorizationHelper();

    private DecisionService service;

    private Hermandad hermandad;
    private User admin, responsable, colaborador, consultor;
    private Event event;
    private Decision decision;

    @BeforeEach
    void setUp() {
        service = new DecisionService(decisionRepository, eventRepository, userRepository, auditLogService, authorizationHelper);
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
        responsable = TestFixtures.user(2, "resp@e.com", hermandad, RoleCode.RESPONSABLE);
        colaborador = TestFixtures.user(3, "col@e.com", hermandad, RoleCode.COLABORADOR);
        consultor = TestFixtures.user(4, "cons@e.com", hermandad, RoleCode.CONSULTA);
        event = TestFixtures.event(10, hermandad, responsable);
        decision = TestFixtures.decision(30, event, responsable, DecisionStatus.PENDING);
    }

    private void mockUser(User u) {
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    private void mockEvent() {
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
    }

    private void mockDecision() {
        when(decisionRepository.findById(30)).thenReturn(Optional.of(decision));
    }

    @Test
    void findByEventId_returnsList() {
        mockUser(admin);
        mockEvent();
        when(decisionRepository.findByEventId(10)).thenReturn(List.of(decision));
        assertThat(service.findByEventId(10, admin.getEmail())).hasSize(1);
    }

    @Test
    void findById_returnsResponse() {
        mockUser(admin);
        mockEvent();
        mockDecision();
        DecisionResponse res = service.findById(10, 30, admin.getEmail());
        assertThat(res.id()).isEqualTo(30);
    }

    @Test
    void findById_wrongEvent_throws() {
        Event other = TestFixtures.event(99, hermandad, admin);
        Decision d = TestFixtures.decision(30, other, admin, DecisionStatus.PENDING);
        mockUser(admin);
        mockEvent();
        when(decisionRepository.findById(30)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.findById(10, 30, admin.getEmail()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_admin_setsReviewer() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(2)).thenReturn(Optional.of(responsable));
        CreateDecisionRequest req = new CreateDecisionRequest("SECRETARIA", "Titulo", 2);
        DecisionResponse res = service.create(10, req, admin.getEmail());
        assertThat(res.reviewedById()).isEqualTo(2);
    }

    @Test
    void create_collaborator_isForcedAsReviewer() {
        mockUser(colaborador);
        mockEvent();
        CreateDecisionRequest req = new CreateDecisionRequest("SECRETARIA", "T", 999);
        DecisionResponse res = service.create(10, req, colaborador.getEmail());
        assertThat(res.reviewedById()).isEqualTo(colaborador.getId());
    }

    @Test
    void create_assignToConsultor_throws() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(4)).thenReturn(Optional.of(consultor));
        CreateDecisionRequest req = new CreateDecisionRequest("SECRETARIA", "T", 4);
        assertThatThrownBy(() -> service.create(10, req, admin.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_eventClosed_throws() {
        event.setStatus(EventStatus.CLOSED);
        mockUser(admin);
        mockEvent();
        CreateDecisionRequest req = new CreateDecisionRequest("SECRETARIA", "T", null);
        assertThatThrownBy(() -> service.create(10, req, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_admin_changesAllFields() {
        mockUser(admin);
        mockEvent();
        mockDecision();
        when(userRepository.findById(2)).thenReturn(Optional.of(responsable));
        UpdateDecisionRequest req = new UpdateDecisionRequest("PRIOSTIA", "Nuevo", 2);
        DecisionResponse res = service.update(10, 30, req, admin.getEmail());
        assertThat(res.title()).isEqualTo("Nuevo");
        assertThat(res.area()).isEqualTo("PRIOSTIA");
    }

    @Test
    void update_collaborator_denied() {
        mockUser(colaborador);
        mockEvent();
        mockDecision();
        UpdateDecisionRequest req = new UpdateDecisionRequest(null, "x", null);
        assertThatThrownBy(() -> service.update(10, 30, req, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void delete_admin_deletes() {
        mockUser(admin);
        mockEvent();
        mockDecision();
        service.delete(10, 30, admin.getEmail());
        verify(decisionRepository).delete(decision);
    }

    @Test
    void delete_collaborator_denied() {
        mockUser(colaborador);
        mockEvent();
        mockDecision();
        assertThatThrownBy(() -> service.delete(10, 30, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void accept_admin() {
        mockUser(admin);
        mockEvent();
        mockDecision();
        DecisionResponse res = service.accept(10, 30, admin.getEmail());
        assertThat(res.status()).isEqualTo("ACCEPTED");
    }

    @Test
    void accept_alreadyAccepted_throws() {
        decision.setStatus(DecisionStatus.ACCEPTED);
        mockUser(admin);
        mockEvent();
        mockDecision();
        assertThatThrownBy(() -> service.accept(10, 30, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reject_admin() {
        mockUser(admin);
        mockEvent();
        mockDecision();
        DecisionResponse res = service.reject(10, 30, admin.getEmail());
        assertThat(res.status()).isEqualTo("REJECTED");
    }

    @Test
    void reject_alreadyDecided_throws() {
        decision.setStatus(DecisionStatus.REJECTED);
        mockUser(admin);
        mockEvent();
        mockDecision();
        assertThatThrownBy(() -> service.reject(10, 30, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void accept_collaborator_denied() {
        mockUser(colaborador);
        mockEvent();
        mockDecision();
        assertThatThrownBy(() -> service.accept(10, 30, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }
}

package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateIncidentRequest;
import com.actacofrade.backend.dto.IncidentResponse;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Incident;
import com.actacofrade.backend.entity.IncidentStatus;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.IncidentRepository;
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
class IncidentServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private final AuthorizationHelper authorizationHelper = new AuthorizationHelper();

    private IncidentService service;

    private Hermandad hermandad;
    private User admin, responsable, colaborador, consultor;
    private Event event;
    private Incident incident;

    @BeforeEach
    void setUp() {
        service = new IncidentService(incidentRepository, eventRepository, userRepository, auditLogService, authorizationHelper);
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
        responsable = TestFixtures.user(2, "resp@e.com", hermandad, RoleCode.RESPONSABLE);
        colaborador = TestFixtures.user(3, "col@e.com", hermandad, RoleCode.COLABORADOR);
        consultor = TestFixtures.user(4, "cons@e.com", hermandad, RoleCode.CONSULTA);
        event = TestFixtures.event(10, hermandad, responsable);
        incident = TestFixtures.incident(40, event, colaborador, IncidentStatus.OPEN);
    }

    private void mockUser(User u) {
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    private void mockEvent() {
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
    }

    private void mockIncident() {
        when(incidentRepository.findById(40)).thenReturn(Optional.of(incident));
    }

    @Test
    void findByEventId_returnsList() {
        mockUser(admin);
        mockEvent();
        when(incidentRepository.findByEventId(10)).thenReturn(List.of(incident));
        assertThat(service.findByEventId(10, admin.getEmail())).hasSize(1);
    }

    @Test
    void findById_returnsResponse() {
        mockUser(admin);
        mockEvent();
        mockIncident();
        IncidentResponse res = service.findById(10, 40, admin.getEmail());
        assertThat(res.id()).isEqualTo(40);
    }

    @Test
    void create_admin_setsReporter() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(3)).thenReturn(Optional.of(colaborador));
        IncidentResponse res = service.create(10, new CreateIncidentRequest("desc", 3), admin.getEmail());
        assertThat(res.reportedById()).isEqualTo(3);
    }

    @Test
    void create_collaborator_isForcedAsReporter() {
        mockUser(colaborador);
        mockEvent();
        IncidentResponse res = service.create(10, new CreateIncidentRequest("desc", 999), colaborador.getEmail());
        assertThat(res.reportedById()).isEqualTo(colaborador.getId());
    }

    @Test
    void create_assignToConsultor_throws() {
        mockUser(admin);
        mockEvent();
        when(userRepository.findById(4)).thenReturn(Optional.of(consultor));
        assertThatThrownBy(() -> service.create(10, new CreateIncidentRequest("d", 4), admin.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_eventClosed_throws() {
        event.setStatus(EventStatus.CLOSED);
        mockUser(admin);
        mockEvent();
        assertThatThrownBy(() -> service.create(10, new CreateIncidentRequest("d", null), admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_admin_deletes() {
        mockUser(admin);
        mockEvent();
        mockIncident();
        service.delete(10, 40, admin.getEmail());
        verify(incidentRepository).delete(incident);
    }

    @Test
    void delete_collaborator_denied() {
        mockUser(colaborador);
        mockEvent();
        mockIncident();
        assertThatThrownBy(() -> service.delete(10, 40, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolve_admin_resolves() {
        mockUser(admin);
        mockEvent();
        mockIncident();
        IncidentResponse res = service.resolve(10, 40, admin.getEmail());
        assertThat(res.status()).isEqualTo("RESUELTA");
    }

    @Test
    void resolve_alreadyResolved_throws() {
        incident.setStatus(IncidentStatus.RESOLVED);
        mockUser(admin);
        mockEvent();
        mockIncident();
        assertThatThrownBy(() -> service.resolve(10, 40, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolve_collaborator_denied() {
        mockUser(colaborador);
        mockEvent();
        mockIncident();
        assertThatThrownBy(() -> service.resolve(10, 40, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void reopen_admin_reopens() {
        incident.setStatus(IncidentStatus.RESOLVED);
        mockUser(admin);
        mockEvent();
        mockIncident();
        IncidentResponse res = service.reopen(10, 40, admin.getEmail());
        assertThat(res.status()).isEqualTo("ABIERTA");
    }

    @Test
    void reopen_alreadyOpen_throws() {
        mockUser(admin);
        mockEvent();
        mockIncident();
        assertThatThrownBy(() -> service.reopen(10, 40, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reopen_collaborator_denied() {
        incident.setStatus(IncidentStatus.RESOLVED);
        mockUser(colaborador);
        mockEvent();
        mockIncident();
        assertThatThrownBy(() -> service.reopen(10, 40, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolve_setsResolverAndTimestamp() {
        mockUser(admin);
        mockEvent();
        mockIncident();
        service.resolve(10, 40, admin.getEmail());
        assertThat(incident.getResolvedBy()).isEqualTo(admin);
        assertThat(incident.getResolvedAt()).isNotNull();
    }
}


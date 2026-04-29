package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateEventRequest;
import com.actacofrade.backend.dto.EventResponse;
import com.actacofrade.backend.dto.UpdateEventRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import com.actacofrade.backend.util.AuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private final AuthorizationHelper authorizationHelper = new AuthorizationHelper();

    private EventService service;

    private Hermandad hermandad;
    private User admin;
    private User responsable;
    private User colaborador;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, userRepository, auditLogService, authorizationHelper);
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
        responsable = TestFixtures.user(2, "resp@e.com", hermandad, RoleCode.RESPONSABLE);
        colaborador = TestFixtures.user(3, "col@e.com", hermandad, RoleCode.COLABORADOR);
        event = TestFixtures.event(10, hermandad, responsable);
    }

    private void mockUser(User u) {
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    @Test
    void findAll_filtersByHermandad() {
        mockUser(admin);
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event));

        List<EventResponse> result = service.findAll(admin.getEmail());
        assertThat(result).hasSize(1);
    }

    @Test
    void findFiltered_returnsPage() {
        mockUser(admin);
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        Page<EventResponse> page = service.findFiltered("CABILDO", "PLANIFICACION",
                LocalDate.now(), "x", PageRequest.of(0, 10), admin.getEmail());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findById_returnsEvent() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));

        EventResponse res = service.findById(10, admin.getEmail());
        assertThat(res.id()).isEqualTo(10);
    }

    @Test
    void findById_notFound_accessDenied() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(99, 1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99, admin.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveHermandadId_userNotFound_throws() {
        when(userRepository.findByEmail("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findAll("x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveHermandadId_userWithoutHermandad_throws() {
        User noH = TestFixtures.user(99, "n@e.com", null, RoleCode.ADMINISTRADOR);
        mockUser(noH);
        assertThatThrownBy(() -> service.findAll(noH.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_admin_setsResponsibleFromRequest() {
        mockUser(admin);
        when(userRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(eventRepository.findMaxReferenceNumberByYearPrefix(anyString())).thenReturn(0);

        CreateEventRequest req = new CreateEventRequest("Titulo", "CABILDO",
                LocalDate.now().plusDays(1), "Loc", 2, "obs");
        EventResponse res = service.create(req, admin.getEmail());

        assertThat(res.title()).isEqualTo("Titulo");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_collaborator_isForcedAsResponsible() {
        mockUser(colaborador);
        when(eventRepository.findMaxReferenceNumberByYearPrefix(anyString())).thenReturn(5);

        CreateEventRequest req = new CreateEventRequest("Titulo", "CABILDO",
                LocalDate.now().plusDays(1), null, 999, null);
        EventResponse res = service.create(req, colaborador.getEmail());

        assertThat(res.responsibleName()).isEqualTo(colaborador.getFullName());
        verify(userRepository, never()).findById(999);
    }

    @Test
    void create_userWithoutHermandad_throws() {
        User u = TestFixtures.user(50, "x@e.com", null, RoleCode.ADMINISTRADOR);
        mockUser(u);
        CreateEventRequest req = new CreateEventRequest("T", "CABILDO", LocalDate.now(), null, null, null);
        assertThatThrownBy(() -> service.create(req, u.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_collaboratorNotResponsible_isDenied() {
        mockUser(colaborador);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));

        UpdateEventRequest req = new UpdateEventRequest("Nuevo", null, null, null, null, null);
        assertThatThrownBy(() -> service.update(10, req, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_responsable_canEditOwnEvent() {
        mockUser(responsable);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));

        UpdateEventRequest req = new UpdateEventRequest("Nuevo", "CULTOS",
                LocalDate.now().plusDays(2), "L", null, "obs");
        EventResponse res = service.update(10, req, responsable.getEmail());
        assertThat(res.title()).isEqualTo("Nuevo");
        assertThat(res.eventType()).isEqualTo("CULTOS");
    }

    @Test
    void delete_removesEvent() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        service.delete(10, admin.getEmail());
        verify(eventRepository).delete(event);
    }

    @Test
    void advanceStatus_planificationToPreparation() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        EventResponse res = service.advanceStatus(10, admin.getEmail());
        assertThat(res.status()).isEqualTo("PREPARACION");
    }

    @Test
    void advanceStatus_walksAllStates() {
        mockUser(admin);
        event.setStatus(EventStatus.PREPARACION);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        assertThat(service.advanceStatus(10, admin.getEmail()).status()).isEqualTo("CONFIRMACION");

        event.setStatus(EventStatus.CONFIRMACION);
        assertThat(service.advanceStatus(10, admin.getEmail()).status()).isEqualTo("CIERRE");
    }

    @Test
    void advanceStatus_fromCierreThrows() {
        mockUser(admin);
        event.setStatus(EventStatus.CIERRE);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        assertThatThrownBy(() -> service.advanceStatus(10, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void advanceStatus_fromCerradoThrows() {
        mockUser(admin);
        event.setStatus(EventStatus.CERRADO);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        assertThatThrownBy(() -> service.advanceStatus(10, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void close_succeedsWhenNothingPending() {
        mockUser(admin);
        event.setStatus(EventStatus.CIERRE);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        when(eventRepository.countPendingTasksByEventId(10)).thenReturn(0L);
        when(eventRepository.countOpenIncidentsByEventId(10)).thenReturn(0L);
        when(eventRepository.countPendingDecisionsByEventId(10)).thenReturn(0L);

        EventResponse res = service.close(10, admin.getEmail());
        assertThat(res.status()).isEqualTo("CERRADO");
        verify(auditLogService).log(eq(event), eq("EVENT"), eq(10), eq("EVENT_CLOSED"), eq(admin), any());
    }

    @Test
    void close_failsWhenPendingTasks() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        when(eventRepository.countPendingTasksByEventId(10)).thenReturn(2L);
        when(eventRepository.countOpenIncidentsByEventId(10)).thenReturn(0L);
        when(eventRepository.countPendingDecisionsByEventId(10)).thenReturn(0L);

        assertThatThrownBy(() -> service.close(10, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void close_failsWhenOpenIncidents() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        when(eventRepository.countPendingTasksByEventId(10)).thenReturn(0L);
        when(eventRepository.countOpenIncidentsByEventId(10)).thenReturn(1L);
        when(eventRepository.countPendingDecisionsByEventId(10)).thenReturn(0L);

        assertThatThrownBy(() -> service.close(10, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void close_failsWhenPendingDecisions() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        when(eventRepository.countPendingTasksByEventId(10)).thenReturn(0L);
        when(eventRepository.countOpenIncidentsByEventId(10)).thenReturn(0L);
        when(eventRepository.countPendingDecisionsByEventId(10)).thenReturn(3L);

        assertThatThrownBy(() -> service.close(10, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void close_alreadyClosed_throws() {
        mockUser(admin);
        event.setStatus(EventStatus.CERRADO);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        assertThatThrownBy(() -> service.close(10, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void close_collaboratorDenied() {
        mockUser(colaborador);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        assertThatThrownBy(() -> service.close(10, colaborador.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toggleLockForClosing_flipsValue() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        EventResponse res = service.toggleLockForClosing(10, admin.getEmail());
        assertThat(res.isLockedForClosing()).isTrue();
        EventResponse res2 = service.toggleLockForClosing(10, admin.getEmail());
        assertThat(res2.isLockedForClosing()).isFalse();
    }

    @Test
    void toggleLockForClosing_closedEventThrows() {
        mockUser(admin);
        event.setStatus(EventStatus.CERRADO);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        assertThatThrownBy(() -> service.toggleLockForClosing(10, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void clone_createsNewEventInPlanification() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        when(eventRepository.findMaxReferenceNumberByYearPrefix(anyString())).thenReturn(null);

        EventResponse res = service.clone(10, admin.getEmail());
        assertThat(res.status()).isEqualTo("PLANIFICACION");
        verify(eventRepository).save(any(Event.class));
        verify(eventRepository).insertCloneRelation(eq(10), any());
    }

    @Test
    void getAvailableDates_returnsStringDates() {
        mockUser(admin);
        when(eventRepository.findDistinctEventDatesByHermandadId(1))
                .thenReturn(List.of(LocalDate.of(2026, 1, 1)));
        assertThat(service.getAvailableDates(admin.getEmail())).containsExactly("2026-01-01");
    }

    @Test
    void findHistory_paginatesAndSorts() {
        mockUser(admin);
        Event closed = TestFixtures.event(20, hermandad, admin);
        closed.setStatus(EventStatus.CERRADO);
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event, closed));

        Page<EventResponse> page = service.findHistory(null, null, null, null, null, 0, 10, admin.getEmail());
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).status()).isEqualTo("PLANIFICACION");
    }

    @Test
    void update_partialFields_keepsExisting() {
        mockUser(admin);
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        UpdateEventRequest req = new UpdateEventRequest(null, null, null, null, null, null);
        EventResponse res = service.update(10, req, admin.getEmail());
        assertThat(res.title()).isEqualTo("Acto 10");
    }
}

package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.DashboardResponse;
import com.actacofrade.backend.dto.EventResponse;
import com.actacofrade.backend.entity.DecisionStatus;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.IncidentStatus;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.DecisionRepository;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.IncidentRepository;
import com.actacofrade.backend.repository.TaskRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private DecisionRepository decisionRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventService eventService;

    private DashboardService service;

    private Hermandad hermandad;
    private User admin;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new DashboardService(eventRepository, taskRepository, incidentRepository,
                decisionRepository, userRepository, eventService);
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
        event = TestFixtures.event(10, hermandad, admin);
    }

    @Test
    void getDashboard_unknownUser_throws() {
        when(userRepository.findByEmail("nope@e.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDashboard("nope@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getDashboard_userWithoutHermandad_throws() {
        User u = TestFixtures.user(2, "x@e.com", null, RoleCode.ADMINISTRADOR);
        when(userRepository.findByEmail("x@e.com")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.getDashboard("x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getDashboard_aggregatesData() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));

        Page<Event> page = new PageImpl<>(List.of(event));
        when(eventRepository.<Event>findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        EventResponse er = new EventResponse(10, "2026/0001", "Acto", "CABILDO",
                LocalDate.now(), null, null, "PLANIFICACION", 1, "User 1", false,
                0, 0, 0, 0, null, null);
        when(eventService.toResponse(event)).thenReturn(er);

        when(taskRepository.findByAssignedToIdAndStatusInAndEventHermandadIdOrderByCreatedAtDesc(eq(1), any(), eq(1)))
                .thenReturn(List.of());
        when(incidentRepository.findByReportedByIdAndStatusAndEventHermandadIdOrderByCreatedAtDesc(eq(1), eq(IncidentStatus.ABIERTA), eq(1)))
                .thenReturn(List.of());
        when(decisionRepository.findByReviewedByIdAndStatusAndEventHermandadIdOrderByCreatedAtDesc(eq(1), eq(DecisionStatus.PENDING), eq(1)))
                .thenReturn(List.of());
        when(taskRepository.countByAssignedToIdAndStatusInAndEventHermandadId(eq(1), any(), eq(1))).thenReturn(4L);
        when(eventRepository.count(any(Specification.class))).thenReturn(2L);

        DashboardResponse res = service.getDashboard("admin@e.com");
        assertThat(res.recentEvents()).hasSize(1);
        assertThat(res.alerts()).isEmpty();
        assertThat(res.pendingTasksCount()).isEqualTo(4);
        assertThat(res.readyToCloseCount()).isEqualTo(2);
    }

    @Test
    void getDashboard_excludesClosedEventsFromRecentSpecification() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        Event closed = TestFixtures.event(11, hermandad, admin);
        closed.setStatus(EventStatus.CERRADO);
        Page<Event> page = new PageImpl<>(List.of(event));
        when(eventRepository.<Event>findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(eventService.toResponse(any(Event.class))).thenReturn(
                new EventResponse(10, "r", "t", "CABILDO", LocalDate.now(), null, null, "PLANIFICACION",
                        1, "U", false, 0, 0, 0, 0, null, null));
        when(taskRepository.findByAssignedToIdAndStatusInAndEventHermandadIdOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(incidentRepository.findByReportedByIdAndStatusAndEventHermandadIdOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(decisionRepository.findByReviewedByIdAndStatusAndEventHermandadIdOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of());

        DashboardResponse res = service.getDashboard("admin@e.com");
        assertThat(res.recentEvents()).hasSize(1);
        assertThat(res.recentEvents().get(0).status()).isEqualTo("PLANIFICACION");
    }
}

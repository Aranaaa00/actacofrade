package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.ExportRequest;
import com.actacofrade.backend.entity.DecisionStatus;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.IncidentStatus;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.TaskStatus;
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
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventExportServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private DecisionRepository decisionRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private UserRepository userRepository;

    private EventExportService service;

    private Hermandad hermandad;
    private User admin;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new EventExportService(eventRepository, taskRepository, decisionRepository, incidentRepository, userRepository);
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
        event = TestFixtures.event(10, hermandad, admin);
        event.setObservations("Observaciones del acto");
    }

    private void mockUserAndEvent() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
    }

    @Test
    void export_userWithoutHermandad_throws() {
        User u = TestFixtures.user(2, "x@e.com", null, RoleCode.ADMINISTRADOR);
        when(userRepository.findByEmail("x@e.com")).thenReturn(Optional.of(u));
        ExportRequest req = new ExportRequest("PDF", List.of("OBSERVATIONS"));
        assertThatThrownBy(() -> service.export(10, req, "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void export_eventNotInHermandad_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findByIdAndHermandadId(99, 1)).thenReturn(Optional.empty());
        ExportRequest req = new ExportRequest("PDF", List.of("OBSERVATIONS"));
        assertThatThrownBy(() -> service.export(99, req, "admin@e.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void export_unknownUser_throws() {
        when(userRepository.findByEmail("nope@e.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.export(10, new ExportRequest("PDF", List.of("OBSERVATIONS")), "nope@e.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exportPdf_allSections_returnsNonEmptyBytes() {
        mockUserAndEvent();
        when(taskRepository.findByEventId(10)).thenReturn(List.of(TestFixtures.task(20, event, admin, TaskStatus.PLANNED)));
        when(decisionRepository.findByEventId(10)).thenReturn(List.of(TestFixtures.decision(30, event, admin, DecisionStatus.PENDING)));
        when(incidentRepository.findByEventId(10)).thenReturn(List.of(TestFixtures.incident(40, event, admin, IncidentStatus.ABIERTA)));

        byte[] bytes = service.export(10, new ExportRequest("PDF", List.of("OBSERVATIONS", "TASKS", "DECISIONS", "INCIDENTS")), "admin@e.com");
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void exportPdf_emptyData_renderEmptyRow() {
        mockUserAndEvent();
        when(taskRepository.findByEventId(10)).thenReturn(List.of());
        when(decisionRepository.findByEventId(10)).thenReturn(List.of());
        when(incidentRepository.findByEventId(10)).thenReturn(List.of());
        byte[] bytes = service.export(10, new ExportRequest("PDF", List.of("TASKS", "DECISIONS", "INCIDENTS")), "admin@e.com");
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void exportCsv_startsWithBom() {
        mockUserAndEvent();
        when(taskRepository.findByEventId(10)).thenReturn(List.of());
        byte[] bytes = service.export(10, new ExportRequest("CSV", List.of("TASKS")), "admin@e.com");
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);
    }

    @Test
    void exportCsv_includesAllSelectedSections() {
        mockUserAndEvent();
        when(taskRepository.findByEventId(10)).thenReturn(List.of(TestFixtures.task(20, event, admin, TaskStatus.PLANNED)));
        when(decisionRepository.findByEventId(10)).thenReturn(List.of(TestFixtures.decision(30, event, admin, DecisionStatus.ACCEPTED)));
        when(incidentRepository.findByEventId(10)).thenReturn(List.of(TestFixtures.incident(40, event, admin, IncidentStatus.RESUELTA)));

        byte[] bytes = service.export(10, new ExportRequest("CSV", List.of("OBSERVATIONS", "TASKS", "DECISIONS", "INCIDENTS")), "admin@e.com");
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(csv).contains("OBSERVACIONES");
        assertThat(csv).contains("TAREAS");
        assertThat(csv).contains("DECISIONES");
        assertThat(csv).contains("INCIDENCIAS");
        assertThat(csv).contains("Tarea 20");
        assertThat(csv).contains("Decision 30");
        assertThat(csv).contains("Incidencia 40");
        assertThat(csv).contains("Observaciones del acto");
    }

    @Test
    void exportCsv_emptyData_writesPlaceholder() {
        mockUserAndEvent();
        when(taskRepository.findByEventId(10)).thenReturn(List.of());
        when(decisionRepository.findByEventId(10)).thenReturn(List.of());
        when(incidentRepository.findByEventId(10)).thenReturn(List.of());
        byte[] bytes = service.export(10, new ExportRequest("CSV", List.of("TASKS", "DECISIONS", "INCIDENTS")), "admin@e.com");
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(csv).contains("Sin registros para esta sección.");
    }

    @Test
    void exportCsv_blankObservations_usesPlaceholder() {
        event.setObservations(null);
        mockUserAndEvent();
        byte[] bytes = service.export(10, new ExportRequest("CSV", List.of("OBSERVATIONS")), "admin@e.com");
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(csv).contains("Sin observaciones registradas.");
    }

    @Test
    void export_normalizesSectionsCaseAndTrim() {
        mockUserAndEvent();
        when(taskRepository.findByEventId(10)).thenReturn(List.of());
        byte[] bytes = service.export(10, new ExportRequest("CSV", List.of("  tasks  ")), "admin@e.com");
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(csv).contains("TAREAS");
    }

    @Test
    void export_unknownSectionIgnored() {
        mockUserAndEvent();
        byte[] bytes = service.export(10, new ExportRequest("CSV", List.of("UNKNOWN_X")), "admin@e.com");
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(csv).doesNotContain("TAREAS");
        assertThat(csv).doesNotContain("DECISIONES");
        assertThat(csv).doesNotContain("INCIDENCIAS");
    }
}

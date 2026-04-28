package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.DashboardAlertResponse;
import com.actacofrade.backend.dto.DashboardResponse;
import com.actacofrade.backend.dto.EventResponse;
import com.actacofrade.backend.entity.Decision;
import com.actacofrade.backend.entity.DecisionStatus;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.Incident;
import com.actacofrade.backend.entity.IncidentStatus;
import com.actacofrade.backend.entity.Task;
import com.actacofrade.backend.entity.TaskStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.DecisionRepository;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.EventSpecification;
import com.actacofrade.backend.repository.IncidentRepository;
import com.actacofrade.backend.repository.TaskRepository;
import com.actacofrade.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECENT_EVENTS_LIMIT = 5;
    private static final int ALERTS_LIMIT = 5;
    private static final String ALERT_TYPE_TASK = "TASK";
    private static final String ALERT_TYPE_INCIDENT = "INCIDENT";
    private static final String ALERT_TYPE_DECISION = "DECISION";
    private static final Collection<TaskStatus> ACTIVE_TASK_STATUSES = Set.of(
            TaskStatus.PLANNED, TaskStatus.ACCEPTED, TaskStatus.IN_PREPARATION, TaskStatus.CONFIRMED);

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final IncidentRepository incidentRepository;
    private final DecisionRepository decisionRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    public DashboardService(EventRepository eventRepository,
                            TaskRepository taskRepository,
                            IncidentRepository incidentRepository,
                            DecisionRepository decisionRepository,
                            UserRepository userRepository,
                            EventService eventService) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.incidentRepository = incidentRepository;
        this.decisionRepository = decisionRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    public DashboardResponse getDashboard(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Integer hermandadId = user.getHermandad().getId();
        Integer userId = user.getId();

        List<EventResponse> recentEvents = loadRecentEvents(hermandadId);
        List<DashboardAlertResponse> alerts = loadAlerts(userId, hermandadId);
        long pendingTasksCount = taskRepository.countByAssignedToIdAndStatusInAndEventHermandadId(
                userId, ACTIVE_TASK_STATUSES, hermandadId);
        long readyToCloseCount = countReadyToCloseEvents(hermandadId);

        return new DashboardResponse(recentEvents, alerts, pendingTasksCount, readyToCloseCount);
    }

    private List<EventResponse> loadRecentEvents(Integer hermandadId) {
        Specification<Event> spec = Specification.where(EventSpecification.hasHermandad(hermandadId))
                .and(EventSpecification.isNotClosed());
        return eventRepository.findAll(
                spec,
                PageRequest.of(0, RECENT_EVENTS_LIMIT, Sort.by(Sort.Direction.DESC, "eventDate"))
        ).stream()
                .map(eventService::toResponse)
                .toList();
    }

    private List<DashboardAlertResponse> loadAlerts(Integer userId, Integer hermandadId) {
        List<Task> tasks = taskRepository.findByAssignedToIdAndStatusInAndEventHermandadIdOrderByCreatedAtDesc(
                userId, ACTIVE_TASK_STATUSES, hermandadId);
        List<Incident> incidents = incidentRepository.findByReportedByIdAndStatusAndEventHermandadIdOrderByCreatedAtDesc(
                userId, IncidentStatus.ABIERTA, hermandadId);
        List<Decision> decisions = decisionRepository.findByReviewedByIdAndStatusAndEventHermandadIdOrderByCreatedAtDesc(
                userId, DecisionStatus.PENDING, hermandadId);

        List<AlertEntry> entries = new ArrayList<>();
        tasks.forEach(t -> entries.add(new AlertEntry(
                ALERT_TYPE_TASK, t.getTitle(), t.getEvent().getId(),
                t.getEvent().getEventDate().atStartOfDay(), t.getId(), t.getCreatedAt())));
        incidents.forEach(i -> entries.add(new AlertEntry(
                ALERT_TYPE_INCIDENT, i.getDescription(), i.getEvent().getId(),
                i.getEvent().getEventDate().atStartOfDay(), i.getId(), i.getCreatedAt())));
        decisions.forEach(d -> entries.add(new AlertEntry(
                ALERT_TYPE_DECISION, d.getTitle(), d.getEvent().getId(),
                d.getEvent().getEventDate().atStartOfDay(), d.getId(), d.getCreatedAt())));

        return entries.stream()
                .sorted(Comparator.comparing(AlertEntry::createdAt).reversed())
                .limit(ALERTS_LIMIT)
                .map(e -> new DashboardAlertResponse(
                        e.type(), e.description(), e.eventId(), e.eventDateTime().toLocalDate(), e.entityId()))
                .toList();
    }

    private long countReadyToCloseEvents(Integer hermandadId) {
        Specification<Event> spec = Specification
                .where(EventSpecification.hasHermandad(hermandadId))
                .and((root, query, cb) -> cb.equal(root.get("status"), EventStatus.CIERRE));
        return eventRepository.count(spec);
    }

    private record AlertEntry(String type, String description, Integer eventId,
                              LocalDateTime eventDateTime, Integer entityId, LocalDateTime createdAt) {}
}

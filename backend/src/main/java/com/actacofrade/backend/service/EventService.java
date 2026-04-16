package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateEventRequest;
import com.actacofrade.backend.dto.EventResponse;
import com.actacofrade.backend.dto.UpdateEventRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.EventType;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.EventSpecification;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.SanitizationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public EventService(EventRepository eventRepository, UserRepository userRepository,
                        AuditLogService auditLogService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<EventResponse> findAll(String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Specification<Event> spec = Specification.where(EventSpecification.hasHermandad(hermandadId));
        return eventRepository.findAll(spec).stream()
                .map(this::toResponse)
                .sorted(Comparator
                        .comparing((EventResponse e) -> "CERRADO".equals(e.status()) ? 1 : 0)
                        .thenComparing(EventResponse::eventDate))
                .toList();
    }

    public Page<EventResponse> findFiltered(String eventType, String status, LocalDate eventDate,
                                            String search, Pageable pageable, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        EventType typeFilter = (eventType != null) ? EventType.valueOf(eventType) : null;
        EventStatus statusFilter = (status != null) ? EventStatus.valueOf(status) : null;

        Specification<Event> spec = Specification
                .where(EventSpecification.hasHermandad(hermandadId))
                .and(EventSpecification.hasEventType(typeFilter))
                .and(EventSpecification.hasStatus(statusFilter))
                .and(EventSpecification.hasEventDate(eventDate))
                .and(EventSpecification.searchByText(search));

        return eventRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public EventResponse findById(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(id, hermandadId);
        return toResponse(event);
    }

    public EventResponse create(CreateEventRequest request, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        if (currentUser.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        EventType eventType = EventType.valueOf(request.eventType());
        User responsible = resolveResponsible(request.responsibleId());
        String reference = generateReference();

        Event event = new Event();
        event.setReference(reference);
        event.setTitle(SanitizationUtils.sanitize(request.title()));
        event.setEventType(eventType);
        event.setEventDate(request.eventDate());
        event.setLocation(SanitizationUtils.sanitizeNullable(request.location()));
        event.setObservations(SanitizationUtils.sanitizeNullable(request.observations()));
        event.setResponsible(responsible);
        event.setHermandad(currentUser.getHermandad());

        eventRepository.save(event);
        return toResponse(event);
    }

    public EventResponse update(Integer id, UpdateEventRequest request, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(id, hermandadId);

        if (request.title() != null) {
            event.setTitle(SanitizationUtils.sanitize(request.title()));
        }
        if (request.eventType() != null) {
            event.setEventType(EventType.valueOf(request.eventType()));
        }
        if (request.eventDate() != null) {
            event.setEventDate(request.eventDate());
        }
        if (request.location() != null) {
            event.setLocation(SanitizationUtils.sanitize(request.location()));
        }
        if (request.observations() != null) {
            event.setObservations(SanitizationUtils.sanitize(request.observations()));
        }
        if (request.responsibleId() != null) {
            event.setResponsible(resolveResponsible(request.responsibleId()));
        }

        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return toResponse(event);
    }

    public void delete(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(id, hermandadId);
        eventRepository.delete(event);
    }

    public EventResponse advanceStatus(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(id, hermandadId);
        EventStatus currentStatus = event.getStatus();

        if (currentStatus == EventStatus.CERRADO) {
            throw new IllegalStateException("El acto ya se encuentra cerrado y no puede avanzar de fase");
        }
        if (currentStatus == EventStatus.CIERRE) {
            throw new IllegalStateException("El acto esta en fase de cierre. Use la accion de cerrar acto");
        }

        EventStatus nextStatus = getNextStatus(currentStatus);
        event.setStatus(nextStatus);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return toResponse(event);
    }

    public EventResponse close(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(id, hermandadId);
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);

        if (event.getStatus() == EventStatus.CERRADO) {
            throw new IllegalStateException("El acto ya se encuentra cerrado");
        }

        long pendingTasks = eventRepository.countPendingTasksByEventId(id);
        long openIncidents = eventRepository.countOpenIncidentsByEventId(id);

        if (pendingTasks > 0 || openIncidents > 0) {
            throw new IllegalStateException(
                    "No se puede cerrar el acto: quedan " + pendingTasks
                            + " tareas sin completar y " + openIncidents + " incidencias abiertas");
        }

        event.setStatus(EventStatus.CERRADO);
        event.setIsLockedForClosing(true);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        auditLogService.log(event, "EVENT", event.getId(), "EVENT_CLOSED", currentUser, event.getTitle());
        return toResponse(event);
    }

    public EventResponse toggleLockForClosing(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(id, hermandadId);

        if (event.getStatus() == EventStatus.CERRADO) {
            throw new IllegalStateException("El acto ya esta cerrado y no puede modificarse");
        }

        event.setIsLockedForClosing(!event.getIsLockedForClosing());
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return toResponse(event);
    }

    public EventResponse clone(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Event original = findEventForHermandadOrThrow(id, hermandadId);
        String reference = generateReference();

        Event cloned = new Event();
        cloned.setReference(reference);
        cloned.setTitle(original.getTitle());
        cloned.setEventType(original.getEventType());
        cloned.setEventDate(LocalDate.now());
        cloned.setLocation(original.getLocation());
        cloned.setObservations(original.getObservations());
        cloned.setResponsible(original.getResponsible());
        cloned.setStatus(EventStatus.PLANIFICACION);
        cloned.setHermandad(original.getHermandad());

        eventRepository.save(cloned);
        eventRepository.insertCloneRelation(original.getId(), cloned.getId());
        return toResponse(cloned);
    }

    private EventStatus getNextStatus(EventStatus current) {
        return switch (current) {
            case PLANIFICACION -> EventStatus.PREPARACION;
            case PREPARACION -> EventStatus.CONFIRMACION;
            case CONFIRMACION -> EventStatus.CIERRE;
            default -> throw new IllegalStateException("No se puede avanzar desde el estado: " + current.name());
        };
    }

    private Event findEventForHermandadOrThrow(Integer id, Integer hermandadId) {
        return eventRepository.findByIdAndHermandadId(id, hermandadId)
                .orElseThrow(() -> new AccessDeniedException("Acto no encontrado o no pertenece a tu hermandad"));
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
    }

    private Integer resolveHermandadId(String authenticatedEmail) {
        User user = findUserByEmailOrThrow(authenticatedEmail);
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        return user.getHermandad().getId();
    }

    private User resolveResponsible(Integer responsibleId) {
        User responsible = null;
        if (responsibleId != null) {
            responsible = userRepository.findById(responsibleId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsable no encontrado con id: " + responsibleId));
        }
        return responsible;
    }

    private String generateReference() {
        int year = LocalDate.now().getYear();
        String prefix = year + "/%";
        Integer maxNumber = eventRepository.findMaxReferenceNumberByYearPrefix(prefix);
        int nextNumber = (maxNumber == null) ? 1 : maxNumber + 1;
        return String.format("%d/%04d", year, nextNumber);
    }

    private EventResponse toResponse(Event event) {
        Integer responsibleId = null;
        String responsibleName = null;
        if (event.getResponsible() != null) {
            responsibleId = event.getResponsible().getId();
            responsibleName = event.getResponsible().getFullName();
        }

        long pendingTasks = eventRepository.countPendingTasksByEventId(event.getId());
        long openIncidents = eventRepository.countOpenIncidentsByEventId(event.getId());
        long totalTasks = eventRepository.countTotalTasksByEventId(event.getId());
        long completedTasks = eventRepository.countTasksWithCompletedStatus(event.getId());

        return new EventResponse(
                event.getId(),
                event.getReference(),
                event.getTitle(),
                event.getEventType().name(),
                event.getEventDate(),
                event.getLocation(),
                event.getObservations(),
                event.getStatus().name(),
                responsibleId,
                responsibleName,
                event.getIsLockedForClosing(),
                pendingTasks,
                openIncidents,
                totalTasks,
                completedTasks,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}

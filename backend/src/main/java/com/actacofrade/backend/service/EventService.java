package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateEventRequest;
import com.actacofrade.backend.dto.EventResponse;
import com.actacofrade.backend.dto.UpdateEventRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.EventType;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.EventSpecification;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.AuthorizationHelper;
import com.actacofrade.backend.util.SanitizationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private final AuthorizationHelper authorizationHelper;

    public EventService(EventRepository eventRepository, UserRepository userRepository,
                        AuditLogService auditLogService, AuthorizationHelper authorizationHelper) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.authorizationHelper = authorizationHelper;
    }

    public List<EventResponse> findAll(String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Specification<Event> spec = Specification.where(EventSpecification.hasHermandad(hermandadId));
        return eventRepository.findAll(spec).stream()
                .map(this::toResponse)
                .sorted(Comparator
                        .comparing((EventResponse e) -> "CLOSED".equals(e.status()) ? 1 : 0)
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
                .and(EventSpecification.isNotClosed())
                .and(EventSpecification.hasEventType(typeFilter))
                .and(EventSpecification.hasStatus(statusFilter))
                .and(EventSpecification.hasEventDate(eventDate))
                .and(EventSpecification.searchByText(search));

        return eventRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public Page<EventResponse> findHistory(String eventType, Integer responsibleId,
                                           LocalDate dateFrom, LocalDate dateTo,
                                           String search, int page, int size, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        EventType typeFilter = (eventType != null) ? EventType.valueOf(eventType) : null;

        Specification<Event> spec = Specification
                .where(EventSpecification.hasHermandad(hermandadId))
                .and(EventSpecification.hasEventType(typeFilter))
                .and(EventSpecification.hasResponsible(responsibleId))
                .and(EventSpecification.hasDateFrom(dateFrom))
                .and(EventSpecification.hasDateTo(dateTo))
                .and(EventSpecification.searchByText(search));

        List<EventResponse> sorted = eventRepository.findAll(spec).stream()
                .map(this::toResponse)
                .sorted(Comparator
                        .comparingInt((EventResponse e) -> "CLOSED".equals(e.status()) ? 1 : 0)
                        .thenComparing((a, b) -> {
                            boolean aClosed = "CLOSED".equals(a.status());
                            boolean bClosed = "CLOSED".equals(b.status());
                            if (!aClosed && !bClosed) {
                                return b.eventDate().compareTo(a.eventDate());
                            }
                            if (aClosed && bClosed) {
                                return b.updatedAt().compareTo(a.updatedAt());
                            }
                            return 0;
                        }))
                .toList();

        int total = sorted.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<EventResponse> content = fromIndex >= total ? List.of() : sorted.subList(fromIndex, toIndex);

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    public List<String> getAvailableDates(String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        return eventRepository.findDistinctEventDatesByHermandadId(hermandadId)
                .stream()
                .map(LocalDate::toString)
                .toList();
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
        User responsible = resolveResponsibleForUser(currentUser, request.responsibleId());
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
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        if (currentUser.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Event event = findEventForHermandadOrThrow(id, currentUser.getHermandad().getId());
        authorizationHelper.requireEventManager(event, currentUser);

        AuditLogService.ChangeSetBuilder diff = new AuditLogService.ChangeSetBuilder();

        if (request.title() != null) {
            String newTitle = SanitizationUtils.sanitize(request.title());
            diff.track("title", event.getTitle(), newTitle);
            event.setTitle(newTitle);
        }
        if (request.eventType() != null) {
            EventType newType = EventType.valueOf(request.eventType());
            diff.track("eventType", event.getEventType(), newType);
            event.setEventType(newType);
        }
        if (request.eventDate() != null) {
            diff.track("eventDate", event.getEventDate(), request.eventDate());
            event.setEventDate(request.eventDate());
        }
        if (request.location() != null) {
            String newLocation = SanitizationUtils.sanitize(request.location());
            diff.track("location", event.getLocation(), newLocation);
            event.setLocation(newLocation);
        }
        if (request.observations() != null) {
            String newObs = SanitizationUtils.sanitize(request.observations());
            diff.track("observations", event.getObservations(), newObs);
            event.setObservations(newObs);
        }
        if (request.responsibleId() != null) {
            User newResponsible = resolveResponsibleForUser(currentUser, request.responsibleId());
            Integer oldId = event.getResponsible() != null ? event.getResponsible().getId() : null;
            Integer newId = newResponsible != null ? newResponsible.getId() : null;
            diff.track("responsibleId", oldId, newId);
            event.setResponsible(newResponsible);
        }

        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        if (!diff.isEmpty()) {
            auditLogService.log(event, "EVENT", event.getId(), "EVENT_UPDATED", currentUser, event.getTitle(), diff.toJson());
        }
        return toResponse(event);
    }

    public void delete(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        Event event = findEventForHermandadOrThrow(id, hermandadId);
        eventRepository.delete(event);
    }

    public EventResponse advanceStatus(Integer id, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        if (currentUser.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Event event = findEventForHermandadOrThrow(id, currentUser.getHermandad().getId());
        authorizationHelper.requireEventManager(event, currentUser);
        EventStatus currentStatus = event.getStatus();

        if (currentStatus == EventStatus.CLOSED) {
            throw new IllegalStateException("El acto ya se encuentra cerrado y no puede avanzar de fase");
        }
        if (currentStatus == EventStatus.CLOSING) {
            throw new IllegalStateException("El acto esta en fase de cierre. Use la accion de cerrar acto");
        }

        EventStatus nextStatus = getNextStatus(currentStatus);
        event.setStatus(nextStatus);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return toResponse(event);
    }

    public EventResponse close(Integer id, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        if (currentUser.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Event event = findEventForHermandadOrThrow(id, currentUser.getHermandad().getId());
        authorizationHelper.requireEventManager(event, currentUser);

        if (event.getStatus() == EventStatus.CLOSED) {
            throw new IllegalStateException("El acto ya se encuentra cerrado");
        }

        long pendingTasks = eventRepository.countPendingTasksByEventId(id);
        long openIncidents = eventRepository.countOpenIncidentsByEventId(id);
        long pendingDecisions = eventRepository.countPendingDecisionsByEventId(id);

        if (pendingTasks > 0 || openIncidents > 0 || pendingDecisions > 0) {
            throw new IllegalStateException(
                    "No se puede cerrar el acto: quedan " + pendingTasks
                            + " tareas sin completar, " + pendingDecisions
                            + " decisiones pendientes y " + openIncidents + " incidencias abiertas");
        }

        event.setStatus(EventStatus.CLOSED);
        event.setIsLockedForClosing(true);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        auditLogService.log(event, "EVENT", event.getId(), "EVENT_CLOSED", currentUser, event.getTitle());
        return toResponse(event);
    }

    public EventResponse toggleLockForClosing(Integer id, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        if (currentUser.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Event event = findEventForHermandadOrThrow(id, currentUser.getHermandad().getId());
        authorizationHelper.requireEventManager(event, currentUser);

        if (event.getStatus() == EventStatus.CLOSED) {
            throw new IllegalStateException("El acto ya esta cerrado y no puede modificarse");
        }

        event.setIsLockedForClosing(!event.getIsLockedForClosing());
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return toResponse(event);
    }

    public EventResponse clone(Integer id, String authenticatedEmail) {
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        if (currentUser.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Event original = findEventForHermandadOrThrow(id, currentUser.getHermandad().getId());
        authorizationHelper.requireEventManager(original, currentUser);
        String reference = generateReference();

        Event cloned = new Event();
        cloned.setReference(reference);
        cloned.setTitle(original.getTitle());
        cloned.setEventType(original.getEventType());
        cloned.setEventDate(LocalDate.now());
        cloned.setLocation(original.getLocation());
        cloned.setObservations(original.getObservations());
        cloned.setResponsible(currentUser);
        cloned.setStatus(EventStatus.PLANNING);
        cloned.setHermandad(original.getHermandad());

        eventRepository.save(cloned);
        eventRepository.insertCloneRelation(original.getId(), cloned.getId());
        return toResponse(cloned);
    }

    private EventStatus getNextStatus(EventStatus current) {
        return switch (current) {
            case PLANNING -> EventStatus.PREPARATION;
            case PREPARATION -> EventStatus.CONFIRMATION;
            case CONFIRMATION -> EventStatus.CLOSING;
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
            boolean hasValidRole = responsible.getRoles().stream()
                    .anyMatch(r -> r.getCode() == RoleCode.ADMINISTRADOR || r.getCode() == RoleCode.RESPONSABLE);
            if (!hasValidRole) {
                throw new IllegalArgumentException("El responsable debe tener rol ADMINISTRADOR o RESPONSABLE");
            }
        }
        return responsible;
    }

    private User resolveResponsibleForUser(User currentUser, Integer requestedResponsibleId) {
        if (!authorizationHelper.isAdmin(currentUser)) {
            return currentUser;
        }
        return resolveResponsible(requestedResponsibleId);
    }

    private String generateReference() {
        int year = LocalDate.now().getYear();
        String prefix = year + "/%";
        Integer maxNumber = eventRepository.findMaxReferenceNumberByYearPrefix(prefix);
        int nextNumber = (maxNumber == null) ? 1 : maxNumber + 1;
        return String.format("%d/%04d", year, nextNumber);
    }

    public EventResponse toResponse(Event event) {
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

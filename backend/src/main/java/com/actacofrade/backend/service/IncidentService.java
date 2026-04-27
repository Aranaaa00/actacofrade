package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateIncidentRequest;
import com.actacofrade.backend.dto.IncidentResponse;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.Incident;
import com.actacofrade.backend.entity.IncidentStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.IncidentRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.AuthorizationHelper;
import com.actacofrade.backend.util.SanitizationUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AuthorizationHelper authorizationHelper;

    public IncidentService(IncidentRepository incidentRepository, EventRepository eventRepository,
                           UserRepository userRepository, AuditLogService auditLogService,
                           AuthorizationHelper authorizationHelper) {
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.authorizationHelper = authorizationHelper;
    }

    public List<IncidentResponse> findByEventId(Integer eventId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        return incidentRepository.findByEventId(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    public IncidentResponse findById(Integer eventId, Integer incidentId, String authenticatedEmail) {
        findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        Incident incident = findIncidentOrThrow(incidentId, eventId);
        return toResponse(incident);
    }

    public IncidentResponse create(Integer eventId, CreateIncidentRequest request, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        User reportedBy = authorizationHelper.isColaboradorOnly(currentUser)
                ? currentUser
                : resolveUser(request.reportedById());

        Incident incident = new Incident();
        incident.setEvent(event);
        incident.setDescription(SanitizationUtils.sanitize(request.description()));
        incident.setReportedBy(reportedBy);

        incidentRepository.save(incident);
        auditLogService.log(event, "INCIDENT", incident.getId(), "INCIDENT_CREATED", currentUser, incident.getDescription());
        return toResponse(incident);
    }

    public void delete(Integer eventId, Integer incidentId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Incident incident = findIncidentOrThrow(incidentId, eventId);
        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, currentUser);
        incidentRepository.delete(incident);
    }

    public IncidentResponse resolve(Integer eventId, Integer incidentId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Incident incident = findIncidentOrThrow(incidentId, eventId);

        if (incident.getStatus() == IncidentStatus.RESUELTA) {
            throw new IllegalStateException("La incidencia ya esta resuelta");
        }

        User resolver = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, resolver);

        incident.setStatus(IncidentStatus.RESUELTA);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolvedBy(resolver);
        incidentRepository.save(incident);
        auditLogService.log(event, "INCIDENT", incident.getId(), "INCIDENT_RESOLVED", resolver, incident.getDescription());
        return toResponse(incident);
    }

    public IncidentResponse reopen(Integer eventId, Integer incidentId, String authenticatedEmail) {
        Event event = findEventForHermandadOrThrow(eventId, resolveHermandadId(authenticatedEmail));
        validateEventNotClosed(event);
        Incident incident = findIncidentOrThrow(incidentId, eventId);

        if (incident.getStatus() == IncidentStatus.ABIERTA) {
            throw new IllegalStateException("La incidencia ya esta abierta");
        }

        User currentUser = findUserByEmailOrThrow(authenticatedEmail);
        authorizationHelper.requireEventManager(event, currentUser);
        incident.setStatus(IncidentStatus.ABIERTA);
        incident.setResolvedAt(null);
        incident.setResolvedBy(null);
        incidentRepository.save(incident);
        return toResponse(incident);
    }

    private Event findEventForHermandadOrThrow(Integer eventId, Integer hermandadId) {
        return eventRepository.findByIdAndHermandadId(eventId, hermandadId)
                .orElseThrow(() -> new AccessDeniedException("Acto no encontrado o no pertenece a tu hermandad"));
    }

    private void validateEventNotClosed(Event event) {
        if (event.getStatus() == EventStatus.CERRADO) {
            throw new IllegalStateException("El acto esta cerrado y no permite modificaciones");
        }
    }

    private Integer resolveHermandadId(String authenticatedEmail) {
        User user = findUserByEmailOrThrow(authenticatedEmail);
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        return user.getHermandad().getId();
    }

    private Incident findIncidentOrThrow(Integer incidentId, Integer eventId) {
        return incidentRepository.findById(incidentId)
                .filter(incident -> incident.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada con id: " + incidentId + " en el acto: " + eventId));
    }

    private User resolveUser(Integer userId) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));
        }
        return user;
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado: " + email));
    }

    private IncidentResponse toResponse(Incident incident) {
        Integer reportedById = null;
        String reportedByName = null;
        if (incident.getReportedBy() != null) {
            reportedById = incident.getReportedBy().getId();
            reportedByName = incident.getReportedBy().getFullName();
        }

        Integer resolvedById = null;
        String resolvedByName = null;
        if (incident.getResolvedBy() != null) {
            resolvedById = incident.getResolvedBy().getId();
            resolvedByName = incident.getResolvedBy().getFullName();
        }

        return new IncidentResponse(
                incident.getId(),
                incident.getEvent().getId(),
                incident.getDescription(),
                incident.getStatus().name(),
                reportedById,
                reportedByName,
                resolvedById,
                resolvedByName,
                incident.getCreatedAt(),
                incident.getResolvedAt()
        );
    }
}

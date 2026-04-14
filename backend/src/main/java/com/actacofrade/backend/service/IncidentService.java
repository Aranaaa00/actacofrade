package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.CreateIncidentRequest;
import com.actacofrade.backend.dto.IncidentResponse;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.Incident;
import com.actacofrade.backend.entity.IncidentStatus;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.IncidentRepository;
import com.actacofrade.backend.repository.UserRepository;
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

    public IncidentService(IncidentRepository incidentRepository, EventRepository eventRepository,
                           UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public List<IncidentResponse> findByEventId(Integer eventId) {
        findEventOrThrow(eventId);
        return incidentRepository.findByEventId(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    public IncidentResponse findById(Integer eventId, Integer incidentId) {
        findEventOrThrow(eventId);
        Incident incident = findIncidentOrThrow(incidentId, eventId);
        return toResponse(incident);
    }

    public IncidentResponse create(Integer eventId, CreateIncidentRequest request) {
        Event event = findEventOrThrow(eventId);
        User reportedBy = resolveUser(request.reportedById());

        Incident incident = new Incident();
        incident.setEvent(event);
        incident.setDescription(request.description());
        incident.setReportedBy(reportedBy);

        incidentRepository.save(incident);
        return toResponse(incident);
    }

    public void delete(Integer eventId, Integer incidentId) {
        findEventOrThrow(eventId);
        Incident incident = findIncidentOrThrow(incidentId, eventId);
        incidentRepository.delete(incident);
    }

    public IncidentResponse resolve(Integer eventId, Integer incidentId, String authenticatedEmail) {
        findEventOrThrow(eventId);
        Incident incident = findIncidentOrThrow(incidentId, eventId);

        if (incident.getStatus() == IncidentStatus.RESUELTA) {
            throw new IllegalStateException("La incidencia ya esta resuelta");
        }

        User resolver = findUserByEmailOrThrow(authenticatedEmail);

        incident.setStatus(IncidentStatus.RESUELTA);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolvedBy(resolver);
        incidentRepository.save(incident);
        return toResponse(incident);
    }

    public IncidentResponse reopen(Integer eventId, Integer incidentId, String authenticatedEmail) {
        findEventOrThrow(eventId);
        Incident incident = findIncidentOrThrow(incidentId, eventId);

        if (incident.getStatus() == IncidentStatus.ABIERTA) {
            throw new IllegalStateException("La incidencia ya esta abierta");
        }

        incident.setStatus(IncidentStatus.ABIERTA);
        incident.setResolvedAt(null);
        incident.setResolvedBy(null);
        incidentRepository.save(incident);
        return toResponse(incident);
    }

    private Event findEventOrThrow(Integer eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Acto no encontrado con id: " + eventId));
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

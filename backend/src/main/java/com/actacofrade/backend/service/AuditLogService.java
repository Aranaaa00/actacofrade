package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.AuditLogResponse;
import com.actacofrade.backend.entity.AuditLog;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.AuditLogRepository;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           EventRepository eventRepository,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByEventId(Integer eventId, Pageable pageable, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        eventRepository.findByIdAndHermandadId(eventId, hermandadId)
                .orElseThrow(() -> new AccessDeniedException("Acto no encontrado o no pertenece a tu hermandad"));
        return auditLogRepository.findByEventIdOrderByPerformedAtDesc(eventId, pageable)
                .map(this::toResponse);
    }

    public void log(Event event, String entityType, Integer entityId, String action, User performedBy, String details) {
        AuditLog entry = new AuditLog();
        entry.setEvent(event);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setPerformedBy(performedBy);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    private Integer resolveHermandadId(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + authenticatedEmail));
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        return user.getHermandad().getId();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        Integer performedById = null;
        String performedByName = null;
        if (log.getPerformedBy() != null) {
            performedById = log.getPerformedBy().getId();
            performedByName = log.getPerformedBy().getFullName();
        }
        return new AuditLogResponse(
                log.getId(),
                log.getEvent().getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                performedById,
                performedByName,
                log.getPerformedAt(),
                log.getDetails()
        );
    }
}

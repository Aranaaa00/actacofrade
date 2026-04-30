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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
        log(event, entityType, entityId, action, performedBy, details, null);
    }

    public void log(Event event, String entityType, Integer entityId, String action, User performedBy, String details, String changes) {
        AuditLog entry = new AuditLog();
        entry.setEvent(event);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setPerformedBy(performedBy);
        entry.setDetails(details);
        entry.setChanges(changes);
        auditLogRepository.save(entry);
    }

    public static class ChangeSetBuilder {
        private final Map<String, Map<String, String>> entries = new LinkedHashMap<>();

        public ChangeSetBuilder track(String field, Object oldValue, Object newValue) {
            String oldStr = oldValue == null ? null : oldValue.toString();
            String newStr = newValue == null ? null : newValue.toString();
            if (!Objects.equals(oldStr, newStr)) {
                Map<String, String> diff = new LinkedHashMap<>();
                diff.put("oldValue", oldStr);
                diff.put("newValue", newStr);
                entries.put(field, diff);
            }
            return this;
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public String toJson() {
            if (entries.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Map<String, String>> entry : entries.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(escape(entry.getKey())).append("\":{");
                sb.append("\"oldValue\":").append(jsonValue(entry.getValue().get("oldValue")));
                sb.append(",\"newValue\":").append(jsonValue(entry.getValue().get("newValue")));
                sb.append('}');
            }
            sb.append('}');
            return sb.toString();
        }

        private static String jsonValue(String s) {
            if (s == null) {
                return "null";
            }
            return "\"" + escape(s) + "\"";
        }

        private static String escape(String s) {
            StringBuilder out = new StringBuilder(s.length() + 2);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            out.append(String.format("\\u%04x", (int) c));
                        } else {
                            out.append(c);
                        }
                    }
                }
            }
            return out.toString();
        }
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
                log.getDetails(),
                log.getChanges()
        );
    }
}

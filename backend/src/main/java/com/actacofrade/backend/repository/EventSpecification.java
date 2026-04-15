package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.EventStatus;
import com.actacofrade.backend.entity.EventType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> hasHermandad(Integer hermandadId) {
        return (root, query, cb) -> hermandadId == null
                ? cb.conjunction()
                : cb.equal(root.get("hermandad").get("id"), hermandadId);
    }

    public static Specification<Event> hasEventType(EventType eventType) {
        return (root, query, cb) -> eventType == null
                ? cb.conjunction()
                : cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Event> hasEventDate(LocalDate eventDate) {
        return (root, query, cb) -> eventDate == null
                ? cb.conjunction()
                : cb.equal(root.get("eventDate"), eventDate);
    }

    public static Specification<Event> searchByText(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("reference")), pattern),
                    cb.like(cb.lower(root.get("location")), pattern)
            );
        };
    }
}

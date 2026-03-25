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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public List<EventResponse> findAll() {
        return eventRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<EventResponse> findFiltered(String eventType, String status, LocalDate eventDate,
                                            String search, Pageable pageable) {
        EventType typeFilter = (eventType != null) ? EventType.valueOf(eventType) : null;
        EventStatus statusFilter = (status != null) ? EventStatus.valueOf(status) : null;

        Specification<Event> spec = Specification
                .where(EventSpecification.hasEventType(typeFilter))
                .and(EventSpecification.hasStatus(statusFilter))
                .and(EventSpecification.hasEventDate(eventDate))
                .and(EventSpecification.searchByText(search));

        return eventRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public EventResponse findById(Integer id) {
        Event event = findEventOrThrow(id);
        return toResponse(event);
    }

    public EventResponse create(CreateEventRequest request) {
        EventType eventType = EventType.valueOf(request.eventType());
        User responsible = resolveResponsible(request.responsibleId());
        String reference = generateReference();

        Event event = new Event();
        event.setReference(reference);
        event.setTitle(request.title());
        event.setEventType(eventType);
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        event.setObservations(request.observations());
        event.setResponsible(responsible);

        eventRepository.save(event);
        return toResponse(event);
    }

    public EventResponse update(Integer id, UpdateEventRequest request) {
        Event event = findEventOrThrow(id);

        if (request.title() != null) {
            event.setTitle(request.title());
        }
        if (request.eventType() != null) {
            event.setEventType(EventType.valueOf(request.eventType()));
        }
        if (request.eventDate() != null) {
            event.setEventDate(request.eventDate());
        }
        if (request.location() != null) {
            event.setLocation(request.location());
        }
        if (request.observations() != null) {
            event.setObservations(request.observations());
        }
        if (request.responsibleId() != null) {
            event.setResponsible(resolveResponsible(request.responsibleId()));
        }

        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return toResponse(event);
    }

    public void delete(Integer id) {
        Event event = findEventOrThrow(id);
        eventRepository.delete(event);
    }

    private Event findEventOrThrow(Integer id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Acto no encontrado con id: " + id));
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
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}

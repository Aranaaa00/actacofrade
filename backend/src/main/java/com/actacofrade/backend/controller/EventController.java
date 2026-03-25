package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.CreateEventRequest;
import com.actacofrade.backend.dto.EventResponse;
import com.actacofrade.backend.dto.UpdateEventRequest;
import com.actacofrade.backend.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> findAll() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<EventResponse>> findFiltered(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "eventDate") Pageable pageable) {
        return ResponseEntity.ok(eventService.findFiltered(eventType, status, eventDate, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(eventService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(@PathVariable Integer id,
                                                @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

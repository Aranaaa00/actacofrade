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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    public ResponseEntity<List<EventResponse>> findAll(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.findAll(userDetails.getUsername()));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<EventResponse>> findFiltered(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "eventDate") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.findFiltered(eventType, status, eventDate, search, pageable, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(@PathVariable Integer id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.findById(id, userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> update(@PathVariable Integer id,
                                                @Valid @RequestBody UpdateEventRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.update(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        eventService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/advance-status")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> advanceStatus(@PathVariable Integer id,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.advanceStatus(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> close(@PathVariable Integer id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.close(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/toggle-lock")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<EventResponse> toggleLock(@PathVariable Integer id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.toggleLockForClosing(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<EventResponse> cloneEvent(@PathVariable Integer id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.clone(id, userDetails.getUsername()));
    }
}

package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.CreateIncidentRequest;
import com.actacofrade.backend.dto.IncidentResponse;
import com.actacofrade.backend.service.IncidentService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> findByEventId(@PathVariable Integer eventId,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(incidentService.findByEventId(eventId, userDetails.getUsername()));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> findById(@PathVariable Integer eventId,
                                                     @PathVariable Integer incidentId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(incidentService.findById(eventId, incidentId, userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<IncidentResponse> create(@PathVariable Integer eventId,
                                                   @Valid @RequestBody CreateIncidentRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(eventId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{incidentId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<Void> delete(@PathVariable Integer eventId,
                                       @PathVariable Integer incidentId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        incidentService.delete(eventId, incidentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{incidentId}/resolve")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<IncidentResponse> resolve(@PathVariable Integer eventId,
                                                    @PathVariable Integer incidentId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(incidentService.resolve(eventId, incidentId, userDetails.getUsername()));
    }

    @PatchMapping("/{incidentId}/reopen")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<IncidentResponse> reopen(@PathVariable Integer eventId,
                                                   @PathVariable Integer incidentId,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(incidentService.reopen(eventId, incidentId, userDetails.getUsername()));
    }
}

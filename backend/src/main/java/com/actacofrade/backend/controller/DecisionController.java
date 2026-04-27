package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.CreateDecisionRequest;
import com.actacofrade.backend.dto.DecisionResponse;
import com.actacofrade.backend.dto.UpdateDecisionRequest;
import com.actacofrade.backend.service.DecisionService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/decisions")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @GetMapping
    public ResponseEntity<List<DecisionResponse>> findByEventId(@PathVariable Integer eventId,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(decisionService.findByEventId(eventId, userDetails.getUsername()));
    }

    @GetMapping("/{decisionId}")
    public ResponseEntity<DecisionResponse> findById(@PathVariable Integer eventId,
                                                     @PathVariable Integer decisionId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(decisionService.findById(eventId, decisionId, userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<DecisionResponse> create(@PathVariable Integer eventId,
                                                   @Valid @RequestBody CreateDecisionRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionService.create(eventId, request, userDetails.getUsername()));
    }

    @PutMapping("/{decisionId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<DecisionResponse> update(@PathVariable Integer eventId,
                                                   @PathVariable Integer decisionId,
                                                   @Valid @RequestBody UpdateDecisionRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(decisionService.update(eventId, decisionId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{decisionId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<Void> delete(@PathVariable Integer eventId,
                                       @PathVariable Integer decisionId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        decisionService.delete(eventId, decisionId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{decisionId}/accept")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<DecisionResponse> accept(@PathVariable Integer eventId,
                                                   @PathVariable Integer decisionId,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(decisionService.accept(eventId, decisionId, userDetails.getUsername()));
    }

    @PatchMapping("/{decisionId}/reject")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<DecisionResponse> reject(@PathVariable Integer eventId,
                                                   @PathVariable Integer decisionId,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(decisionService.reject(eventId, decisionId, userDetails.getUsername()));
    }
}

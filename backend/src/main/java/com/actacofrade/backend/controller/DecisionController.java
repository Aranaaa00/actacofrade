package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.CreateDecisionRequest;
import com.actacofrade.backend.dto.DecisionResponse;
import com.actacofrade.backend.dto.UpdateDecisionRequest;
import com.actacofrade.backend.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<DecisionResponse>> findByEventId(@PathVariable Integer eventId) {
        return ResponseEntity.ok(decisionService.findByEventId(eventId));
    }

    @GetMapping("/{decisionId}")
    public ResponseEntity<DecisionResponse> findById(@PathVariable Integer eventId,
                                                     @PathVariable Integer decisionId) {
        return ResponseEntity.ok(decisionService.findById(eventId, decisionId));
    }

    @PostMapping
    public ResponseEntity<DecisionResponse> create(@PathVariable Integer eventId,
                                                   @Valid @RequestBody CreateDecisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionService.create(eventId, request));
    }

    @PutMapping("/{decisionId}")
    public ResponseEntity<DecisionResponse> update(@PathVariable Integer eventId,
                                                   @PathVariable Integer decisionId,
                                                   @Valid @RequestBody UpdateDecisionRequest request) {
        return ResponseEntity.ok(decisionService.update(eventId, decisionId, request));
    }

    @DeleteMapping("/{decisionId}")
    public ResponseEntity<Void> delete(@PathVariable Integer eventId,
                                       @PathVariable Integer decisionId) {
        decisionService.delete(eventId, decisionId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{decisionId}/ready")
    public ResponseEntity<DecisionResponse> markAsReady(@PathVariable Integer eventId,
                                                        @PathVariable Integer decisionId) {
        return ResponseEntity.ok(decisionService.markAsReady(eventId, decisionId));
    }

    @PatchMapping("/{decisionId}/reject")
    public ResponseEntity<DecisionResponse> reject(@PathVariable Integer eventId,
                                                   @PathVariable Integer decisionId) {
        return ResponseEntity.ok(decisionService.reject(eventId, decisionId));
    }

    @PatchMapping("/{decisionId}/reset")
    public ResponseEntity<DecisionResponse> resetToPending(@PathVariable Integer eventId,
                                                           @PathVariable Integer decisionId) {
        return ResponseEntity.ok(decisionService.resetToPending(eventId, decisionId));
    }
}

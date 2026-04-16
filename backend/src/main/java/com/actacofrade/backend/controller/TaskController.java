package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.CreateTaskRequest;
import com.actacofrade.backend.dto.RejectTaskRequest;
import com.actacofrade.backend.dto.TaskResponse;
import com.actacofrade.backend.dto.UpdateTaskRequest;
import com.actacofrade.backend.service.TaskService;
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
@RequestMapping("/api/events/{eventId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> findByEventId(@PathVariable Integer eventId,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.findByEventId(eventId, userDetails.getUsername()));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Integer eventId,
                                                 @PathVariable Integer taskId,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.findById(eventId, taskId, userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<TaskResponse> create(@PathVariable Integer eventId,
                                               @Valid @RequestBody CreateTaskRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(eventId, request, userDetails.getUsername()));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<TaskResponse> update(@PathVariable Integer eventId,
                                               @PathVariable Integer taskId,
                                               @Valid @RequestBody UpdateTaskRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.update(eventId, taskId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<Void> delete(@PathVariable Integer eventId,
                                       @PathVariable Integer taskId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        taskService.delete(eventId, taskId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/accept")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<TaskResponse> accept(@PathVariable Integer eventId,
                                               @PathVariable Integer taskId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.accept(eventId, taskId, userDetails.getUsername()));
    }

    @PatchMapping("/{taskId}/confirm")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<TaskResponse> confirm(@PathVariable Integer eventId,
                                                @PathVariable Integer taskId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.confirm(eventId, taskId, userDetails.getUsername()));
    }

    @PatchMapping("/{taskId}/complete")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<TaskResponse> complete(@PathVariable Integer eventId,
                                                 @PathVariable Integer taskId,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.complete(eventId, taskId, userDetails.getUsername()));
    }

    @PatchMapping("/{taskId}/reject")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<TaskResponse> reject(@PathVariable Integer eventId,
                                               @PathVariable Integer taskId,
                                               @Valid @RequestBody RejectTaskRequest body,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.reject(eventId, taskId, body.rejectionReason(), userDetails.getUsername()));
    }

    @PatchMapping("/{taskId}/reset")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<TaskResponse> resetToPlanned(@PathVariable Integer eventId,
                                                       @PathVariable Integer taskId,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.resetToPlanned(eventId, taskId, userDetails.getUsername()));
    }
}

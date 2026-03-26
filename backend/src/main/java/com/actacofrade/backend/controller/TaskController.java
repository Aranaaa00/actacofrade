package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.CreateTaskRequest;
import com.actacofrade.backend.dto.TaskResponse;
import com.actacofrade.backend.dto.UpdateTaskRequest;
import com.actacofrade.backend.service.TaskService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/events/{eventId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> findByEventId(@PathVariable Integer eventId) {
        return ResponseEntity.ok(taskService.findByEventId(eventId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Integer eventId,
                                                 @PathVariable Integer taskId) {
        return ResponseEntity.ok(taskService.findById(eventId, taskId));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@PathVariable Integer eventId,
                                               @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(eventId, request));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> update(@PathVariable Integer eventId,
                                               @PathVariable Integer taskId,
                                               @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.update(eventId, taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Integer eventId,
                                       @PathVariable Integer taskId) {
        taskService.delete(eventId, taskId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/confirm")
    public ResponseEntity<TaskResponse> confirm(@PathVariable Integer eventId,
                                                @PathVariable Integer taskId) {
        return ResponseEntity.ok(taskService.confirm(eventId, taskId));
    }

    @PatchMapping("/{taskId}/reject")
    public ResponseEntity<TaskResponse> reject(@PathVariable Integer eventId,
                                               @PathVariable Integer taskId,
                                               @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(taskService.reject(eventId, taskId, body.get("rejectionReason")));
    }

    @PatchMapping("/{taskId}/reset")
    public ResponseEntity<TaskResponse> resetToPending(@PathVariable Integer eventId,
                                                       @PathVariable Integer taskId) {
        return ResponseEntity.ok(taskService.resetToPending(eventId, taskId));
    }
}

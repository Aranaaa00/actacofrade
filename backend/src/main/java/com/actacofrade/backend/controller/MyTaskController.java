package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.MyTaskResponse;
import com.actacofrade.backend.dto.MyTaskStatsResponse;
import com.actacofrade.backend.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-tasks")
public class MyTaskController {

    private final TaskService taskService;

    public MyTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<Page<MyTaskResponse>> findMyTasks(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String statusGroup,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(taskService.findMyTasks(userDetails.getUsername(), eventType, statusGroup, search, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<MyTaskStatsResponse> getMyTaskStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.getMyTaskStats(userDetails.getUsername()));
    }
}

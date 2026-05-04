package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.DashboardResponse;
import com.actacofrade.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Dashboard", description = "Métricas agregadas para la hermandad")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getDashboard(userDetails.getUsername()));
    }
}

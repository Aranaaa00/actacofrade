package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.AdminChangeRequestApprove;
import com.actacofrade.backend.dto.AdminChangeRequestCreate;
import com.actacofrade.backend.dto.AdminChangeRequestResponse;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.service.AdminChangeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-change-requests")
@io.swagger.v3.oas.annotations.tags.Tag(
        name = "Admin Change Requests",
        description = "Solicitudes de cambio de administrador entre hermandades"
)
public class AdminChangeRequestController {

    private final AdminChangeRequestService service;

    public AdminChangeRequestController(AdminChangeRequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and !hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminChangeRequestResponse> create(
            @Valid @RequestBody AdminChangeRequestCreate request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AdminChangeRequestResponse response = service.create(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AdminChangeRequestResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminChangeRequestResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/{id}/candidates")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> findCandidates(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findCandidates(id));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminChangeRequestResponse> approve(
            @PathVariable Integer id,
            @Valid @RequestBody AdminChangeRequestApprove payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.approve(id, payload, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminChangeRequestResponse> reject(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.reject(id, userDetails.getUsername()));
    }
}

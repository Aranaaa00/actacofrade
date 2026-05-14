package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.InterventionLogEntry;
import com.actacofrade.backend.dto.PageResponse;
import com.actacofrade.backend.dto.SuperAdminRoleRequest;
import com.actacofrade.backend.dto.SuperAdminStatusRequest;
import com.actacofrade.backend.dto.SuperAdminUserResponse;
import com.actacofrade.backend.service.SuperAdminUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super-admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "SuperAdmin", description = "Centro de intervención del super administrador")
public class SuperAdminUserController {

    private final SuperAdminUserService service;

    public SuperAdminUserController(SuperAdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SuperAdminUserResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(query, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuperAdminUserResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SuperAdminUserResponse> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody SuperAdminStatusRequest request,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(service.updateStatus(id, request, actor.getUsername()));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<SuperAdminUserResponse> verify(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(service.setManualVerification(id, true, actor.getUsername()));
    }

    @PostMapping("/{id}/unverify")
    public ResponseEntity<SuperAdminUserResponse> unverify(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(service.setManualVerification(id, false, actor.getUsername()));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<SuperAdminUserResponse> overrideRole(
            @PathVariable Integer id,
            @Valid @RequestBody SuperAdminRoleRequest request,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(service.overrideRole(id, request, actor.getUsername()));
    }

    @PostMapping("/{id}/password-reset")
    public ResponseEntity<Void> triggerPasswordReset(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails actor) {
        service.triggerPasswordReset(id, actor.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<PageResponse<InterventionLogEntry>> userLogs(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.findUserLogs(id, page, size));
    }

    @GetMapping("/logs")
    public ResponseEntity<PageResponse<InterventionLogEntry>> allLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.findAllLogs(page, size));
    }
}

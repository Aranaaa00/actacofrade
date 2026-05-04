package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.RoleStatsResponse;
import com.actacofrade.backend.dto.UserCreateRequest;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.dto.UserUpdateRequest;
import com.actacofrade.backend.service.UserService;
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
@RequestMapping("/api/users")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Users", description = "Gestión de usuarios de la hermandad")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UserResponse>> findAll(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.findAll(userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.create(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/assignable")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')")
    public ResponseEntity<List<UserResponse>> findAssignable(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.findAssignable(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> findById(@PathVariable Integer id,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.findById(id, userDetails.getUsername()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RoleStatsResponse> getStats(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.countByRole(userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> update(@PathVariable Integer id,
                                               @Valid @RequestBody UserUpdateRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.update(id, request, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> toggleActive(@PathVariable Integer id,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.toggleActive(id, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        userService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

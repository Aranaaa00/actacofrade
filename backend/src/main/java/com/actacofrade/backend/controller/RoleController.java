package com.actacofrade.backend.controller;

import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Roles", description = "Catálogo de roles disponibles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR')")
    public ResponseEntity<List<Role>> findAll() {
        return ResponseEntity.ok(roleRepository.findAll());
    }
}

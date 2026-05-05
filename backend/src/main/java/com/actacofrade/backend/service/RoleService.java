package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.RoleResponse;
import com.actacofrade.backend.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de catálogo de roles.
 * Centraliza el mapeo entidad → DTO y evita exponer la entidad JPA en el controlador.
 */
@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .map(RoleResponse::from)
                .toList();
    }
}

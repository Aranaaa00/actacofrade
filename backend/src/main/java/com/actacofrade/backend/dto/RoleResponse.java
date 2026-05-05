package com.actacofrade.backend.dto;

import com.actacofrade.backend.entity.Role;

/**
 * DTO público para exponer roles del catálogo sin filtrar la entidad JPA.
 */
public record RoleResponse(
        Integer id,
        String code,
        String description
) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode() != null ? role.getCode().name() : null,
                role.getDescription()
        );
    }
}

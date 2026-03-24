package com.actacofrade.backend.dto;

public record RoleStatsResponse(
        long administradores,
        long responsables,
        long colaboradores,
        long consulta
) {}

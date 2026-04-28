package com.actacofrade.backend.dto;

import java.time.LocalDateTime;

public record HermandadResponse(
        Integer id,
        String nombre,
        String descripcion,
        Integer anioFundacion,
        String localidad,
        String direccionSede,
        String emailContacto,
        String telefonoContacto,
        String sitioWeb,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long usersCount,
        long eventsCount
) {}

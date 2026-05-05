package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request body to approve a request, with the user who becomes administrator. */
public record AdminChangeRequestApprove(
        @NotNull(message = "El nuevo administrador es obligatorio")
        @Positive(message = "Identificador de usuario invalido")
        Integer newAdminUserId
) {}

package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminChangeRequestApprove(
        @NotNull(message = "El nuevo administrador es obligatorio")
        @Positive(message = "Identificador de usuario invalido")
        Integer newAdminUserId
) {}

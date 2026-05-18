package com.actacofrade.backend.dto;

import com.actacofrade.backend.entity.SupportRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body to create a new support request (admin change or any other tracked flow). */
public record AdminChangeRequestCreate(
        SupportRequestType type,

        @NotBlank(message = "El mensaje es obligatorio")
        @Size(min = 10, max = 2000, message = "El mensaje debe tener entre 10 y 2000 caracteres")
        String message
) {
    public SupportRequestType resolvedType() {
        return type != null ? type : SupportRequestType.ADMIN_CHANGE;
    }
}

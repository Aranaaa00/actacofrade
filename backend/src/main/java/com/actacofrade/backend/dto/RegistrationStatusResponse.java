package com.actacofrade.backend.dto;

/**
 * Respuesta genérica para los flujos previos a la verificación de correo.
 * No expone si el email existe o no para evitar enumeración de usuarios.
 */
public record RegistrationStatusResponse(
        String status,
        String message
) {

    public static RegistrationStatusResponse pendingVerification() {
        return new RegistrationStatusResponse(
                "pending_verification",
                "Hemos enviado un correo de verificación. Revisa tu bandeja de entrada para continuar."
        );
    }
}

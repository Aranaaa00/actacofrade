package com.actacofrade.backend.exception;

public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException(String reason) {
        super(reason == null || reason.isBlank()
                ? "Tu cuenta está suspendida. Contacta con el administrador."
                : "Tu cuenta está suspendida: " + reason);
    }
}

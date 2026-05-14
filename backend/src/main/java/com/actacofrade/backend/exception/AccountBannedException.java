package com.actacofrade.backend.exception;

public class AccountBannedException extends RuntimeException {

    public AccountBannedException(String reason) {
        super(reason == null || reason.isBlank()
                ? "Tu cuenta ha sido bloqueada permanentemente."
                : "Tu cuenta ha sido bloqueada permanentemente: " + reason);
    }
}

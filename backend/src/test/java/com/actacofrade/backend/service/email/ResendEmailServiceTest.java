package com.actacofrade.backend.service.email;

import com.actacofrade.backend.entity.AccountStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura de las ramas que no requieren conexión real con Resend.
 */
class ResendEmailServiceTest {

    @Test
    void isConfigured_returnsTrueOnlyWhenAllValuesProvided() {
        assertThat(new ResendEmailService("", "", "").isConfigured()).isFalse();
        assertThat(new ResendEmailService(null, null, null).isConfigured()).isFalse();
        assertThat(new ResendEmailService("k", "", "u").isConfigured()).isFalse();
        assertThat(new ResendEmailService("k", "f@x", "").isConfigured()).isFalse();
        assertThat(new ResendEmailService("k", "f@x", "https://app").isConfigured()).isTrue();
    }

    @Test
    void sendVerificationEmail_notConfigured_returnsFalse() {
        ResendEmailService svc = new ResendEmailService("", "", "");
        assertThat(svc.sendVerificationEmail("to@x", "Nombre", "tok", 10)).isFalse();
    }

    @Test
    void sendPasswordResetEmail_notConfigured_returnsFalse() {
        ResendEmailService svc = new ResendEmailService(null, "f@x", "https://app");
        assertThat(svc.sendPasswordResetEmail("to@x", "Nombre", "tok", 10)).isFalse();
    }

    @Test
    void sendAccountStatusEmail_notConfigured_returnsFalse() {
        ResendEmailService svc = new ResendEmailService("k", "f@x", "");
        assertThat(svc.sendAccountStatusEmail("to@x", "N", AccountStatus.SUSPENDED, "r")).isFalse();
    }

    @Test
    void sendAccountStatusEmail_activeStatus_returnsFalse() {
        ResendEmailService svc = new ResendEmailService("k", "f@x", "https://app");
        assertThat(svc.sendAccountStatusEmail("to@x", "N", AccountStatus.ACTIVE, "r")).isFalse();
    }
}

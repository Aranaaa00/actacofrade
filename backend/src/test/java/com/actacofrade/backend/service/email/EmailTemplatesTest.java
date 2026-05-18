package com.actacofrade.backend.service.email;

import com.actacofrade.backend.entity.AccountStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura de las plantillas de correo HTML/texto.
 */
class EmailTemplatesTest {

    // VerificationEmailTemplate

    @Test
    void verificationHtml_includesNameUrlAndMinutes() {
        String html = VerificationEmailTemplate.buildHtml("Juan", "https://app.test/verify?token=abc", 30);
        assertThat(html).contains("Hola, Juan.")
                .contains("https://app.test/verify?token=abc")
                .contains("30 minutos");
    }

    @Test
    void verificationHtml_escapesUnsafeName() {
        String html = VerificationEmailTemplate.buildHtml("<b>X</b>", "https://app.test", 5);
        assertThat(html).contains("&lt;b&gt;X&lt;/b&gt;");
    }

    @Test
    void verificationHtml_blankNameUsesPlainGreeting() {
        String html = VerificationEmailTemplate.buildHtml("   ", "https://app.test", 5);
        assertThat(html).contains("Hola,");
    }

    @Test
    void verificationHtml_nullNameUsesPlainGreeting() {
        String html = VerificationEmailTemplate.buildHtml(null, "https://app.test", 5);
        assertThat(html).contains("Hola,");
    }

    @Test
    void verificationHtml_clampsExpirationToOneMinute() {
        String html = VerificationEmailTemplate.buildHtml("X", "https://app.test", 0);
        assertThat(html).contains("1 minutos");
    }

    @Test
    void verificationText_includesUrlAndMinutes() {
        String text = VerificationEmailTemplate.buildText("https://app.test/verify", 10);
        assertThat(text).contains("https://app.test/verify").contains("10 minutos");
    }

    @Test
    void verificationText_clampsExpiration() {
        String text = VerificationEmailTemplate.buildText("https://app.test", -1);
        assertThat(text).contains("1 minutos");
    }

    // PasswordResetEmailTemplate

    @Test
    void resetHtml_humanizesDaysHoursMinutes() {
        assertThat(PasswordResetEmailTemplate.buildHtml("Ana", "https://r", 60)).contains("1 hora");
        assertThat(PasswordResetEmailTemplate.buildHtml("Ana", "https://r", 120)).contains("2 horas");
        assertThat(PasswordResetEmailTemplate.buildHtml("Ana", "https://r", 1440)).contains("1 día");
        assertThat(PasswordResetEmailTemplate.buildHtml("Ana", "https://r", 2880)).contains("2 días");
        assertThat(PasswordResetEmailTemplate.buildHtml("Ana", "https://r", 45)).contains("45 minutos");
    }

    @Test
    void resetHtml_nullAndBlankNameUsePlainGreeting() {
        assertThat(PasswordResetEmailTemplate.buildHtml(null, "https://r", 10)).contains("Hola,");
        assertThat(PasswordResetEmailTemplate.buildHtml("  ", "https://r", 10)).contains("Hola,");
    }

    @Test
    void resetHtml_includesNameAndEscapesUrlAndName() {
        String html = PasswordResetEmailTemplate.buildHtml("Pe<dro>", "https://r?t=<x>", 10);
        assertThat(html).contains("Hola, Pe&lt;dro&gt;.").contains("https://r?t=&lt;x&gt;");
    }

    @Test
    void resetText_includesUrlAndDuration() {
        String text = PasswordResetEmailTemplate.buildText("https://app.test/reset", 60);
        assertThat(text).contains("https://app.test/reset").contains("1 hora");
    }

    @Test
    void resetHtml_clampsZeroToOneMinute() {
        assertThat(PasswordResetEmailTemplate.buildHtml("X", "u", 0)).contains("1 minutos");
    }

    // AccountStatusEmailTemplate

    @Test
    void accountStatus_subjectVaries() {
        assertThat(AccountStatusEmailTemplate.subject(AccountStatus.SUSPENDED)).contains("suspendida");
        assertThat(AccountStatusEmailTemplate.subject(AccountStatus.BANNED)).contains("bloqueada");
    }

    @Test
    void accountStatus_htmlForSuspended() {
        String html = AccountStatusEmailTemplate.buildHtml("Marta", AccountStatus.SUSPENDED, "Motivo X");
        assertThat(html).contains("suspendida").contains("Hola, Marta.").contains("Motivo X");
    }

    @Test
    void accountStatus_htmlForBanned() {
        String html = AccountStatusEmailTemplate.buildHtml("", AccountStatus.BANNED, "Spam");
        assertThat(html).contains("bloqueada").contains("Hola,").contains("Spam");
    }

    @Test
    void accountStatus_htmlEscapesReason() {
        String html = AccountStatusEmailTemplate.buildHtml("Ana", AccountStatus.SUSPENDED, "<b>r</b>");
        assertThat(html).contains("&lt;b&gt;r&lt;/b&gt;");
    }

    @Test
    void accountStatus_textForSuspendedAndBanned() {
        assertThat(AccountStatusEmailTemplate.buildText(AccountStatus.SUSPENDED, "X"))
                .contains("suspendida").contains("Motivo: X");
        assertThat(AccountStatusEmailTemplate.buildText(AccountStatus.BANNED, null))
                .contains("bloqueada");
    }

    @Test
    void accountStatus_nullName() {
        String html = AccountStatusEmailTemplate.buildHtml(null, AccountStatus.SUSPENDED, "x");
        assertThat(html).contains("Hola,");
    }
}

package com.actacofrade.backend.service.email;

import com.actacofrade.backend.entity.AccountStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptador para enviar correos a través de Resend (https://resend.com).
 * Toda la configuración sensible (API key, dirección remitente, URL base
 * de la aplicación) llega exclusivamente por variables de entorno.
 */
@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;
    private final String appBaseUrl;

    public ResendEmailService(
            @Value("${email.resend.api-key:}") String apiKey,
            @Value("${email.from-address:}") String fromAddress,
            @Value("${app.base-url:}") String appBaseUrl) {
        this.restClient = RestClient.builder().build();
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * Envía el correo de verificación. Devuelve {@code true} si Resend lo aceptó.
     * Nunca lanza al llamador para no filtrar información de configuración.
     */
    public boolean sendVerificationEmail(String toEmail, String fullName, String token, long expirationMinutes) {
        if (!isConfigured()) {
            log.error("Resend no está configurado: revisa RESEND_API_KEY, EMAIL_FROM_ADDRESS y APP_BASE_URL");
            return false;
        }

        String verificationUrl = buildVerificationUrl(token);
        String html = VerificationEmailTemplate.buildHtml(fullName, verificationUrl, expirationMinutes);
        String text = VerificationEmailTemplate.buildText(verificationUrl, expirationMinutes);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", fromAddress);
        payload.put("to", new String[] { toEmail });
        payload.put("subject", "Confirma tu correo en ActaCofrade");
        payload.put("html", html);
        payload.put("text", text);

        try {
            HttpStatusCode status = restClient.post()
                    .uri(RESEND_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> { /* no-op, manejamos abajo */ })
                    .toBodilessEntity()
                    .getStatusCode();

            if (status.is2xxSuccessful()) {
                return true;
            }
            log.warn("Resend devolvió un estado no exitoso al enviar verificación: {}", status.value());
            return false;
        } catch (RestClientException ex) {
            log.error("Fallo al contactar con Resend: {}", ex.getMessage());
            return false;
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && fromAddress != null && !fromAddress.isBlank()
                && appBaseUrl != null && !appBaseUrl.isBlank();
    }

    /**
     * Envía el correo de restablecimiento de contraseña con un enlace
     * firmado y de un solo uso. Reutiliza el mismo transporte Resend.
     */
    public boolean sendPasswordResetEmail(String toEmail, String fullName, String token, long expirationMinutes) {
        if (!isConfigured()) {
            log.error("Resend no está configurado: no se puede enviar el correo de reset");
            return false;
        }
        String resetUrl = buildResetUrl(token);
        String html = PasswordResetEmailTemplate.buildHtml(fullName, resetUrl, expirationMinutes);
        String text = PasswordResetEmailTemplate.buildText(resetUrl, expirationMinutes);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", fromAddress);
        payload.put("to", new String[] { toEmail });
        payload.put("subject", "Restablece tu contraseña en ActaCofrade");
        payload.put("html", html);
        payload.put("text", text);

        try {
            HttpStatusCode status = restClient.post()
                    .uri(RESEND_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> { /* no-op, manejamos abajo */ })
                    .toBodilessEntity()
                    .getStatusCode();

            if (status.is2xxSuccessful()) {
                return true;
            }
            log.warn("Resend devolvió un estado no exitoso al enviar reset: {}", status.value());
            return false;
        } catch (RestClientException ex) {
            log.error("Fallo al contactar con Resend (reset): {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Notifica al usuario que su cuenta ha sido suspendida o bloqueada por el
     * SuperAdmin. El motivo es obligatorio aguas arriba; aquí solo lo
     * transportamos al cuerpo del correo.
     */
    public boolean sendAccountStatusEmail(String toEmail, String fullName, AccountStatus accountStatus, String reason) {
        if (!isConfigured()) {
            log.error("Resend no está configurado: no se puede enviar el aviso de cambio de estado");
            return false;
        }
        if (accountStatus != AccountStatus.SUSPENDED && accountStatus != AccountStatus.BANNED) {
            return false;
        }

        String html = AccountStatusEmailTemplate.buildHtml(fullName, accountStatus, reason);
        String text = AccountStatusEmailTemplate.buildText(accountStatus, reason);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", fromAddress);
        payload.put("to", new String[] { toEmail });
        payload.put("subject", AccountStatusEmailTemplate.subject(accountStatus));
        payload.put("html", html);
        payload.put("text", text);

        try {
            HttpStatusCode httpStatus = restClient.post()
                    .uri(RESEND_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> { /* no-op, manejamos abajo */ })
                    .toBodilessEntity()
                    .getStatusCode();

            if (httpStatus.is2xxSuccessful()) {
                return true;
            }
            log.warn("Resend devolvió un estado no exitoso al enviar aviso de cuenta: {}", httpStatus.value());
            return false;
        } catch (RestClientException ex) {
            log.error("Fallo al contactar con Resend (estado de cuenta): {}", ex.getMessage());
            return false;
        }
    }

    private String buildVerificationUrl(String token) {
        String base = stripTrailingSlash(appBaseUrl);
        return base + "/auth/verify-email?token=" + token;
    }

    private String buildResetUrl(String token) {
        String base = stripTrailingSlash(appBaseUrl);
        return base + "/auth/reset-password?token=" + token;
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

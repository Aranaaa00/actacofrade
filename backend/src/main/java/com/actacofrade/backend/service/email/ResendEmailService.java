package com.actacofrade.backend.service.email;

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

    private String buildVerificationUrl(String token) {
        String base = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return base + "/auth/verify-email?token=" + token;
    }
}

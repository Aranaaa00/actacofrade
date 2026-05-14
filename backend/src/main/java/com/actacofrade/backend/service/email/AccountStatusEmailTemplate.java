package com.actacofrade.backend.service.email;

import com.actacofrade.backend.entity.AccountStatus;

public final class AccountStatusEmailTemplate {

    private AccountStatusEmailTemplate() {}

    public static String subject(AccountStatus status) {
        return status == AccountStatus.SUSPENDED
                ? "Tu cuenta de ActaCofrade ha sido suspendida"
                : "Tu cuenta de ActaCofrade ha sido bloqueada";
    }

    public static String buildHtml(String fullName, AccountStatus status, String reason) {
        String safeName = escape(fullName == null ? "" : fullName.trim());
        String greeting = safeName.isEmpty() ? "Hola," : "Hola, " + safeName + ".";
        String headline = status == AccountStatus.SUSPENDED
                ? "Tu cuenta ha sido suspendida"
                : "Tu cuenta ha sido bloqueada";
        String body = status == AccountStatus.SUSPENDED
                ? "El equipo de administración ha suspendido temporalmente el acceso a tu cuenta."
                : "El equipo de administración ha bloqueado el acceso a tu cuenta de forma indefinida.";
        String safeReason = escape(reason == null ? "" : reason.trim());

        return """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="color-scheme" content="light only">
              <title>%s</title>
              <style>
                @media (max-width: 480px) {
                  .ac-card { padding: 24px !important; }
                  .ac-title { font-size: 22px !important; }
                }
              </style>
            </head>
            <body style="margin:0;padding:0;background:#f5f3f0;font-family:'Inter',Arial,Helvetica,sans-serif;color:#0d0d0d;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f5f3f0;padding:32px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:560px;width:100%%;">
                    <tr><td align="center" style="padding-bottom:24px;">
                      <div style="font-family:'EB Garamond',Georgia,serif;font-size:14px;letter-spacing:0.32em;text-transform:uppercase;color:#7a6428;">ActaCofrade</div>
                      <div style="height:1px;width:48px;background:#c9a961;margin:12px auto 0;"></div>
                    </td></tr>
                    <tr><td class="ac-card" style="background:#ffffff;border:1px solid #e8e8e8;padding:40px;">
                      <h1 class="ac-title" style="margin:0 0 16px;font-family:'EB Garamond',Georgia,serif;font-weight:600;font-size:26px;line-height:1.25;color:#0d0d0d;">%s</h1>
                      <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#0d0d0d;">%s</p>
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#736856;">%s</p>
                      <div style="margin:8px 0 20px;padding:16px;background:#faf7f0;border-left:3px solid #c9a961;font-size:14px;line-height:1.6;color:#0d0d0d;">
                        <strong style="display:block;font-size:12px;letter-spacing:0.08em;text-transform:uppercase;color:#7a6428;margin-bottom:6px;">Motivo</strong>
                        %s
                      </div>
                      <p style="margin:0;font-size:12px;line-height:1.6;color:#a0957d;">
                        Si crees que se trata de un error, ponte en contacto con el equipo de administración respondiendo a este correo.
                      </p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(subject(status), headline, greeting, body, safeReason);
    }

    public static String buildText(AccountStatus status, String reason) {
        String headline = status == AccountStatus.SUSPENDED
                ? "Tu cuenta ha sido suspendida"
                : "Tu cuenta ha sido bloqueada";
        return """
                ActaCofrade — %s

                %s

                Motivo: %s

                Si crees que se trata de un error, ponte en contacto con el equipo de administración.
                """.formatted(headline, headline, reason == null ? "" : reason.trim());
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

package com.actacofrade.backend.service.email;

public final class PasswordResetEmailTemplate {

    private PasswordResetEmailTemplate() {}

    public static String buildHtml(String fullName, String resetUrl, long expirationMinutes) {
        String safeName = escape(fullName == null ? "" : fullName.trim());
        String greeting = safeName.isEmpty() ? "Hola," : "Hola, " + safeName + ".";
        String safeUrl = escape(resetUrl);
        String duration = humanizeDuration(expirationMinutes);

        return """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="color-scheme" content="light only">
              <title>Restablece tu contraseña en ActaCofrade</title>
              <style>
                @media (max-width: 480px) {
                  .ac-card { padding: 24px !important; }
                  .ac-title { font-size: 22px !important; }
                  .ac-cta a { display: block !important; width: 100%% !important; box-sizing: border-box; }
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
                      <h1 class="ac-title" style="margin:0 0 16px;font-family:'EB Garamond',Georgia,serif;font-weight:600;font-size:26px;line-height:1.25;color:#0d0d0d;">Restablece tu contraseña</h1>
                      <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#0d0d0d;">%s</p>
                      <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#736856;">
                        El equipo de administración de ActaCofrade ha generado un enlace para que
                        establezcas una nueva contraseña. Pulsa el botón para continuar.
                      </p>
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="ac-cta" style="margin:8px 0 28px;">
                        <tr><td align="center" bgcolor="#5c4033" style="background:#5c4033;">
                          <a href="%s" style="display:inline-block;padding:14px 28px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:15px;font-weight:600;letter-spacing:0.02em;color:#ffffff;text-decoration:none;border:1px solid #5c4033;">Establecer nueva contraseña</a>
                        </td></tr>
                      </table>
                      <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#736856;">Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                      <p style="margin:0 0 24px;font-size:13px;line-height:1.6;word-break:break-all;"><a href="%s" style="color:#7a6428;text-decoration:underline;">%s</a></p>
                      <div style="height:1px;background:#e8e8e8;margin:8px 0 20px;"></div>
                      <p style="margin:0;font-size:12px;line-height:1.6;color:#a0957d;">
                        El enlace caduca en %s y solo puede usarse una vez. Si no has solicitado el cambio, ignora este mensaje y tu contraseña actual seguirá siendo válida.
                      </p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(greeting, safeUrl, safeUrl, safeUrl, duration);
    }

    public static String buildText(String resetUrl, long expirationMinutes) {
        String duration = humanizeDuration(expirationMinutes);
        return """
                ActaCofrade — Restablece tu contraseña

                Abre este enlace en tu navegador para establecer una nueva contraseña:
                %s

                El enlace caduca en %s y solo puede usarse una vez.
                Si no has solicitado el cambio, ignora este mensaje.
                """.formatted(resetUrl, duration);
    }

    private static String humanizeDuration(long expirationMinutes) {
        long minutes = Math.max(1, expirationMinutes);
        if (minutes % 1440 == 0) {
            long days = minutes / 1440;
            return days == 1 ? "1 día" : days + " días";
        }
        if (minutes % 60 == 0) {
            long hours = minutes / 60;
            return hours == 1 ? "1 hora" : hours + " horas";
        }
        return minutes + " minutos";
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

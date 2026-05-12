package com.actacofrade.backend.service.email;

/**
 * Construye el HTML del correo de verificación de ActaCofrade.
 *
 * Diseño propio (no derivado de plantilla pública): cabecera con escudo
 * tipográfico, tarjeta crema sobre fondo cofrade y CTA en marrón "wood".
 * Estructura basada en tabla para compatibilidad con clientes de correo,
 * con media-query para móvil y enlace de fallback explícito.
 */
public final class VerificationEmailTemplate {

    private VerificationEmailTemplate() {}

    public static String buildHtml(String fullName, String verificationUrl, long expirationMinutes) {
        String safeName = escape(fullName == null ? "" : fullName.trim());
        String greeting = safeName.isEmpty() ? "Hola," : "Hola, " + safeName + ".";
        String safeUrl = escape(verificationUrl);
        long minutes = Math.max(1, expirationMinutes);

        return """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="color-scheme" content="light only">
              <title>Confirma tu correo en ActaCofrade</title>
              <style>
                @media (max-width: 480px) {
                  .ac-card { padding: 24px !important; }
                  .ac-title { font-size: 22px !important; }
                  .ac-cta a { display: block !important; width: 100%% !important; box-sizing: border-box; }
                }
              </style>
            </head>
            <body style="margin:0;padding:0;background:#f5f3f0;font-family:'Inter',Arial,Helvetica,sans-serif;color:#0d0d0d;">
              <span style="display:none!important;opacity:0;color:transparent;height:0;width:0;overflow:hidden;">
                Confirma tu correo para activar tu cuenta de ActaCofrade.
              </span>
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f5f3f0;padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:560px;width:100%%;">
                      <tr>
                        <td align="center" style="padding-bottom:24px;">
                          <div style="font-family:'EB Garamond',Georgia,serif;font-size:14px;letter-spacing:0.32em;text-transform:uppercase;color:#7a6428;">
                            ActaCofrade
                          </div>
                          <div style="height:1px;width:48px;background:#c9a961;margin:12px auto 0;"></div>
                        </td>
                      </tr>
                      <tr>
                        <td class="ac-card" style="background:#ffffff;border:1px solid #e8e8e8;padding:40px;">
                          <h1 class="ac-title" style="margin:0 0 16px;font-family:'EB Garamond',Georgia,serif;font-weight:600;font-size:26px;line-height:1.25;color:#0d0d0d;">
                            Confirma tu correo para entrar
                          </h1>
                          <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#0d0d0d;">
                            %s
                          </p>
                          <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#736856;">
                            Antes de activar tu cuenta necesitamos asegurarnos de que este correo
                            es tuyo. Pulsa el botón para confirmarlo y completar el registro de tu
                            hermandad en ActaCofrade.
                          </p>
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="ac-cta" style="margin:8px 0 28px;">
                            <tr>
                              <td align="center" bgcolor="#5c4033" style="background:#5c4033;">
                                <a href="%s"
                                   style="display:inline-block;padding:14px 28px;font-family:'Inter',Arial,Helvetica,sans-serif;font-size:15px;font-weight:600;letter-spacing:0.02em;color:#ffffff;text-decoration:none;border:1px solid #5c4033;">
                                  Confirmar mi correo
                                </a>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#736856;">
                            Si el botón no funciona, copia y pega este enlace en tu navegador:
                          </p>
                          <p style="margin:0 0 24px;font-size:13px;line-height:1.6;word-break:break-all;">
                            <a href="%s" style="color:#7a6428;text-decoration:underline;">%s</a>
                          </p>
                          <div style="height:1px;background:#e8e8e8;margin:8px 0 20px;"></div>
                          <p style="margin:0;font-size:12px;line-height:1.6;color:#a0957d;">
                            El enlace caduca en %d minutos y solo puede usarse una vez. Si no has
                            solicitado esta cuenta puedes ignorar este mensaje: no se creará ningún
                            usuario hasta que confirmes el correo.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:24px 8px 0;font-size:12px;color:#a0957d;font-family:'Inter',Arial,Helvetica,sans-serif;">
                          ActaCofrade · Gestión de actos y decisiones cofrades<br>
                          Este correo se envía únicamente con fines de verificación.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(greeting, safeUrl, safeUrl, safeUrl, minutes);
    }

    public static String buildText(String verificationUrl, long expirationMinutes) {
        long minutes = Math.max(1, expirationMinutes);
        return """
                ActaCofrade — Confirma tu correo

                Para activar tu cuenta abre el siguiente enlace en tu navegador:
                %s

                El enlace caduca en %d minutos y solo puede usarse una vez.
                Si no has solicitado esta cuenta puedes ignorar este mensaje:
                no se creará ningún usuario hasta que confirmes el correo.
                """.formatted(verificationUrl, minutes);
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

-- Las solicitudes de recuperación de contraseña ya no se gestionan en el
-- Centro de Intervención: el usuario las dispara directamente y el backend
-- envía el correo en el momento. Eliminamos cualquier solicitud previa de
-- ese tipo para que el frontend ya no las muestre.
DELETE FROM admin_change_requests WHERE type = 'PASSWORD_RESET';

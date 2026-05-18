-- Añade el tipo de solicitud para diferenciar peticiones de soporte (cambio de admin,
-- recuperación de contraseña, verificación manual y contacto general).

CREATE TYPE support_request_type AS ENUM ('ADMIN_CHANGE', 'PASSWORD_RESET', 'VERIFICATION', 'CONTACT');

ALTER TABLE admin_change_requests
    ADD COLUMN type support_request_type NOT NULL DEFAULT 'ADMIN_CHANGE';

CREATE INDEX idx_admin_change_requests_type ON admin_change_requests(type);

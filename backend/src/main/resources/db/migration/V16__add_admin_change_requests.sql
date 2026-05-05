-- Crea tabla de solicitudes de cambio de administrador y siembra el rol SUPER_ADMIN.

INSERT INTO roles (code, description) VALUES
    ('SUPER_ADMIN', 'Acceso global para gestionar solicitudes entre hermandades')
ON CONFLICT (code) DO NOTHING;

CREATE TYPE admin_change_request_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE admin_change_requests (
    id SERIAL PRIMARY KEY,
    hermandad_id INTEGER NOT NULL REFERENCES hermandades(id) ON DELETE CASCADE,
    requester_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    status admin_change_request_status NOT NULL DEFAULT 'PENDING',
    new_admin_user_id INTEGER REFERENCES users(id),
    resolved_by_user_id INTEGER REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_change_requests_status ON admin_change_requests(status);
CREATE INDEX idx_admin_change_requests_hermandad ON admin_change_requests(hermandad_id);

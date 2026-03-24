-- ACTACOFRADE - V2: Insert default roles

INSERT INTO roles (code, description) VALUES
    ('ADMINISTRADOR', 'Full access to all system features'),
    ('RESPONSABLE', 'Can manage events and assign tasks'),
    ('COLABORADOR', 'Can view and confirm assigned tasks'),
    ('CONSULTA', 'Read-only access to events and decisions');

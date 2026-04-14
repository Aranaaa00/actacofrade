-- ACTACOFRADE - V5: Seed demo data for development

-- === USERS ===
INSERT INTO users (id, full_name, email, password_hash, active, created_at)
VALUES
    (1, 'Manuel Arana',    'admin@actacofrade.es',      crypt('Cofrade2026!', gen_salt('bf', 10)), TRUE, NOW()),
    (2, 'M. López',        'lopez@actacofrade.es',      crypt('Cofrade2026!', gen_salt('bf', 10)), TRUE, NOW()),
    (3, 'J. Riva',         'riva@actacofrade.es',       crypt('Cofrade2026!', gen_salt('bf', 10)), TRUE, NOW()),
    (4, 'F. García',       'garcia@actacofrade.es',     crypt('Cofrade2026!', gen_salt('bf', 10)), TRUE, NOW()),
    (5, 'Mayordomía',      'mayordomia@actacofrade.es', crypt('Cofrade2026!', gen_salt('bf', 10)), TRUE, NOW());

-- Reset sequence
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- === USER ROLES ===
INSERT INTO user_roles (user_id, role_id) VALUES
    (1, (SELECT id FROM roles WHERE code = 'ADMINISTRADOR')),
    (2, (SELECT id FROM roles WHERE code = 'RESPONSABLE')),
    (3, (SELECT id FROM roles WHERE code = 'COLABORADOR')),
    (4, (SELECT id FROM roles WHERE code = 'COLABORADOR')),
    (5, (SELECT id FROM roles WHERE code = 'RESPONSABLE'));

-- === EVENTS ===
INSERT INTO events (id, reference, title, event_type, event_date, location, observations, status, responsible_id, is_locked_for_closing, created_at, updated_at)
VALUES
    (1, '2026/0001', 'Cabildo General Ordinario',        'CABILDO',   '2026-03-15', 'Casa Hermandad',       'Convocatoria anual de cabildo general',                        'PREPARACION',  5, FALSE, NOW(), NOW()),
    (2, '2026/0002', 'Quinario al Santísimo Cristo',      'CULTOS',    '2026-04-06', 'Iglesia de San Román', 'Quinario previo a la Semana Santa',                            'PLANIFICACION', 2, FALSE, NOW(), NOW()),
    (3, '2026/0003', 'Ensayo General de Costaleros',      'ENSAYO',    '2026-04-10', 'Nave de Hermandad',    'Ensayo previo a la salida procesional',                        'CONFIRMACION', 3, FALSE, NOW(), NOW()),
    (4, '2026/0004', 'Salida Procesional Viernes Santo',  'PROCESION', '2026-04-17', 'Recorrido oficial',    'Salida procesional del Viernes Santo por el recorrido oficial', 'PLANIFICACION', 1, FALSE, NOW(), NOW());

-- Reset sequence
SELECT setval('events_id_seq', (SELECT MAX(id) FROM events));

-- === TASKS (for event 1 - Cabildo General Ordinario) ===
INSERT INTO tasks (id, event_id, title, description, assigned_to, status, deadline, rejection_reason, confirmed_by, confirmed_at, created_at, updated_at)
VALUES
    (1, 1, 'Preparación de Censo Electoral',  'Revisión de altas y bajas del último trimestre', 2, 'PENDIENTE',  '2026-03-12', NULL, NULL, NULL, NOW(), NOW()),
    (2, 1, 'Reserva de Casa Hermandad',       'Confirmar disponibilidad del salón principal',    3, 'CONFIRMADA', '2026-03-01', NULL, 1,    NOW(), NOW(), NOW()),
    (3, 1, 'Contratación de Catering',         'Motivo: No se realizará catering este año',       4, 'RECHAZADA',  '2026-03-05', 'No se realizará catering por decisión de mayordomía', NULL, NULL, NOW(), NOW());

-- Tasks for event 3 (Ensayo General)
INSERT INTO tasks (id, event_id, title, description, assigned_to, status, deadline, created_at, updated_at)
VALUES
    (4, 3, 'Montaje del paso de misterio',   'Coordinar cuadrilla para el montaje completo',    3, 'CONFIRMADA', '2026-04-08', NOW(), NOW()),
    (5, 3, 'Verificación de túnica y enseres', 'Revisar estado de túnicas y cirios de costaleros', 4, 'PENDIENTE',  '2026-04-09', NOW(), NOW());

-- Reset sequence
SELECT setval('tasks_id_seq', (SELECT MAX(id) FROM tasks));

-- === DECISIONS (for event 1 - Cabildo General Ordinario) ===
INSERT INTO decisions (id, event_id, area, title, status, reviewed_by, created_at, updated_at)
VALUES
    (1, 1, 'TESORERIA',  'Aprobación de presupuesto anual 2026',               'LISTA',     1, NOW(), NOW()),
    (2, 1, 'PRIOSTIA',   'Cambio de horario del quinario',                      'PENDIENTE', 2, NOW(), NOW()),
    (3, 1, 'MAYORDOMIA', 'Renovación del contrato de limpieza de Casa Hermandad', 'PENDIENTE', 5, NOW(), NOW());

-- Reset sequence
SELECT setval('decisions_id_seq', (SELECT MAX(id) FROM decisions));

-- === INCIDENTS (for event 1 - Cabildo General Ordinario) ===
INSERT INTO incidents (id, event_id, description, status, reported_by, resolved_by, created_at, resolved_at)
VALUES
    (1, 1, 'Avería en sistema de megafonía de la Casa Hermandad', 'ABIERTA',  4, NULL, NOW(), NULL),
    (2, 1, 'Fallo en iluminación del salón de actos',             'RESUELTA', 2, 1,    NOW(), NOW());

-- Reset sequence
SELECT setval('incidents_id_seq', (SELECT MAX(id) FROM incidents));

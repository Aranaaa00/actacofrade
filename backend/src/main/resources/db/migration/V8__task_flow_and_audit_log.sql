-- ACTACOFRADE - V8: New task flow states + audit log table

-- === 1. MIGRATE task_status ENUM ===

CREATE TYPE task_status_new AS ENUM ('PLANNED', 'IN_PREPARATION', 'CONFIRMED', 'COMPLETED', 'REJECTED');

ALTER TABLE tasks ALTER COLUMN status DROP DEFAULT;

ALTER TABLE tasks ALTER COLUMN status TYPE VARCHAR(30);

UPDATE tasks SET status = 'PLANNED' WHERE status = 'PENDIENTE';
UPDATE tasks SET status = 'CONFIRMED' WHERE status = 'CONFIRMADA';
UPDATE tasks SET status = 'REJECTED' WHERE status = 'RECHAZADA';

DROP TYPE task_status;

ALTER TYPE task_status_new RENAME TO task_status;

ALTER TABLE tasks ALTER COLUMN status TYPE task_status USING status::task_status;

ALTER TABLE tasks ALTER COLUMN status SET DEFAULT 'PLANNED';

-- === 2. ADD completed_at TO tasks ===

ALTER TABLE tasks ADD COLUMN completed_at TIMESTAMP;

-- === 3. AUDIT LOG TABLE ===

CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    event_id INTEGER REFERENCES events(id) ON DELETE CASCADE,
    entity_type VARCHAR(20) NOT NULL,
    entity_id INTEGER NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by INTEGER REFERENCES users(id),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

CREATE INDEX idx_audit_log_event_id ON audit_log(event_id);
CREATE INDEX idx_audit_log_performed_at ON audit_log(performed_at DESC);

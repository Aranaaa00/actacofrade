-- ACTACOFRADE - V4: Add confirmation tracking (who confirmed/resolved and when)

-- Tasks: record who confirmed and when
ALTER TABLE tasks
    ADD COLUMN confirmed_by INTEGER REFERENCES users(id),
    ADD COLUMN confirmed_at TIMESTAMP;

-- Incidents: record who resolved
ALTER TABLE incidents
    ADD COLUMN resolved_by INTEGER REFERENCES users(id);

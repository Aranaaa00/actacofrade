CREATE INDEX IF NOT EXISTS idx_events_responsible_id ON events(responsible_id);
CREATE INDEX IF NOT EXISTS idx_events_hermandad_id ON events(hermandad_id);
CREATE INDEX IF NOT EXISTS idx_tasks_event_id ON tasks(event_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to ON tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_users_hermandad_id ON users(hermandad_id);

-- Enables safe cascade deletion for hermandades and users.
-- Hermandad removal cascades to users and events; user removal nullifies operational references.

-- users.hermandad_id -> hermandades(id) ON DELETE CASCADE
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_hermandad_id_fkey;
ALTER TABLE users
    ADD CONSTRAINT users_hermandad_id_fkey
        FOREIGN KEY (hermandad_id) REFERENCES hermandades(id) ON DELETE CASCADE;

-- events.hermandad_id -> hermandades(id) ON DELETE CASCADE
ALTER TABLE events DROP CONSTRAINT IF EXISTS events_hermandad_id_fkey;
ALTER TABLE events
    ADD CONSTRAINT events_hermandad_id_fkey
        FOREIGN KEY (hermandad_id) REFERENCES hermandades(id) ON DELETE CASCADE;

-- events.responsible_id -> users(id) ON DELETE SET NULL
ALTER TABLE events DROP CONSTRAINT IF EXISTS events_responsible_id_fkey;
ALTER TABLE events
    ADD CONSTRAINT events_responsible_id_fkey
        FOREIGN KEY (responsible_id) REFERENCES users(id) ON DELETE SET NULL;

-- tasks.assigned_to -> users(id) ON DELETE SET NULL
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_assigned_to_fkey;
ALTER TABLE tasks
    ADD CONSTRAINT tasks_assigned_to_fkey
        FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;

-- tasks.created_by -> users(id) ON DELETE SET NULL
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_created_by_fkey;
ALTER TABLE tasks
    ADD CONSTRAINT tasks_created_by_fkey
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- tasks.confirmed_by -> users(id) ON DELETE SET NULL
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_confirmed_by_fkey;
ALTER TABLE tasks
    ADD CONSTRAINT tasks_confirmed_by_fkey
        FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL;

-- incidents.reported_by -> users(id) ON DELETE SET NULL
ALTER TABLE incidents DROP CONSTRAINT IF EXISTS incidents_reported_by_fkey;
ALTER TABLE incidents
    ADD CONSTRAINT incidents_reported_by_fkey
        FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE SET NULL;

-- incidents.resolved_by -> users(id) ON DELETE SET NULL
ALTER TABLE incidents DROP CONSTRAINT IF EXISTS incidents_resolved_by_fkey;
ALTER TABLE incidents
    ADD CONSTRAINT incidents_resolved_by_fkey
        FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL;

-- decisions.reviewed_by -> users(id) ON DELETE SET NULL
ALTER TABLE decisions DROP CONSTRAINT IF EXISTS decisions_reviewed_by_fkey;
ALTER TABLE decisions
    ADD CONSTRAINT decisions_reviewed_by_fkey
        FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL;

-- audit_log.performed_by -> users(id) ON DELETE SET NULL
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_performed_by_fkey;
ALTER TABLE audit_log
    ADD CONSTRAINT audit_log_performed_by_fkey
        FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL;

-- admin_change_requests.new_admin_user_id -> users(id) ON DELETE SET NULL
ALTER TABLE admin_change_requests DROP CONSTRAINT IF EXISTS admin_change_requests_new_admin_user_id_fkey;
ALTER TABLE admin_change_requests
    ADD CONSTRAINT admin_change_requests_new_admin_user_id_fkey
        FOREIGN KEY (new_admin_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- admin_change_requests.resolved_by_user_id -> users(id) ON DELETE SET NULL
ALTER TABLE admin_change_requests DROP CONSTRAINT IF EXISTS admin_change_requests_resolved_by_user_id_fkey;
ALTER TABLE admin_change_requests
    ADD CONSTRAINT admin_change_requests_resolved_by_user_id_fkey
        FOREIGN KEY (resolved_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

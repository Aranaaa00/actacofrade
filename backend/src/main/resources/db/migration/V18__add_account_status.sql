ALTER TABLE users
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN status_reason VARCHAR(500),
    ADD COLUMN status_changed_at TIMESTAMP,
    ADD COLUMN status_changed_by INTEGER;

ALTER TABLE users
    ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BANNED'));

ALTER TABLE users
    ADD CONSTRAINT fk_users_status_changed_by
        FOREIGN KEY (status_changed_by) REFERENCES users(id) ON DELETE SET NULL;

UPDATE users SET status = 'SUSPENDED', status_changed_at = NOW()
    WHERE active = FALSE;

CREATE INDEX idx_users_status ON users(status);

ALTER TABLE users
    ADD COLUMN manually_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN manually_verified_at TIMESTAMP,
    ADD COLUMN manually_verified_by INTEGER;

ALTER TABLE users
    ADD CONSTRAINT fk_users_manually_verified_by
        FOREIGN KEY (manually_verified_by) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_users_manually_verified ON users(manually_verified);

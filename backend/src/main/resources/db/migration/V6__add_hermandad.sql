-- Note: V5 was intentionally dropped during early development before any
-- environment had executed it. The change it covered was folded into this
-- migration. Do not reintroduce a V5 file: Flyway's `out-of-order=false`
-- policy would reject it on databases that already applied V6..V21.

CREATE TABLE hermandades (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL UNIQUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ADD COLUMN hermandad_id INTEGER REFERENCES hermandades(id);

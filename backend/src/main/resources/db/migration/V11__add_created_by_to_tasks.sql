-- ACTACOFRADE - V11: Add created_by to tasks for COLABORADOR ownership validation

ALTER TABLE tasks ADD COLUMN created_by INTEGER REFERENCES users(id);

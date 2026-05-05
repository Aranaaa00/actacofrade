-- Agrega el valor SUPER_ADMIN al tipo enum role_code.
-- ALTER TYPE ... ADD VALUE no se puede combinar con uso del nuevo valor en la
-- misma transaccion, por lo que se aplica en una migracion separada.
ALTER TYPE role_code ADD VALUE IF NOT EXISTS 'SUPER_ADMIN';

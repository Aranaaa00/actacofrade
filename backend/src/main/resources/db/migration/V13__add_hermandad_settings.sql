ALTER TABLE hermandades
    ADD COLUMN descripcion         VARCHAR(500),
    ADD COLUMN anio_fundacion      INTEGER,
    ADD COLUMN localidad           VARCHAR(120),
    ADD COLUMN direccion_sede      VARCHAR(200),
    ADD COLUMN email_contacto      VARCHAR(150),
    ADD COLUMN telefono_contacto   VARCHAR(20),
    ADD COLUMN sitio_web           VARCHAR(200),
    ADD COLUMN updated_at          TIMESTAMP;

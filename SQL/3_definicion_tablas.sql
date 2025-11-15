CREATE DATABASE IF NOT EXISTS TP_Integrador_Programacion_II;
use TP_Integrador_Programacion_II;

DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS credencial_acceso;

CREATE TABLE credencial_acceso (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    eliminado TINYINT(1) NOT NULL DEFAULT 0, -- 0: no eliminado. 1: eliminado.
    hashPassword VARCHAR(255) NOT NULL,
    salt VARCHAR(64),
    ultimoCambio DATETIME DEFAULT CURRENT_TIMESTAMP,
    requireReset TINYINT(1) NOT NULL DEFAULT 0 -- 0: no requiere. 1: requiere.
);

CREATE TABLE usuarios (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
    eliminado TINYINT(1) NOT NULL DEFAULT 0, -- 0: no eliminado. 1: eliminado.
    username VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    activo TINYINT(1) NOT NULL DEFAULT 0, -- 0: activo. 1: inactivo.
    fechaRegistro DATETIME DEFAULT CURRENT_TIMESTAMP,
    credencial_id BIGINT NOT NULL UNIQUE,
    FOREIGN KEY (credencial_id) REFERENCES credencial_acceso(id) ON DELETE CASCADE ON UPDATE CASCADE
);

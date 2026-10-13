DROP DATABASE IF EXISTS dbkinalllaves;
CREATE DATABASE dbkinalllaves CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dbkinalllaves;
CREATE TABLE IF NOT EXISTS roles (
 id_rol INT PRIMARY KEY AUTO_INCREMENT, codigo VARCHAR(25) NOT NULL UNIQUE,
 nombre VARCHAR(80) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS permisos (
 id_permiso INT PRIMARY KEY AUTO_INCREMENT, codigo VARCHAR(45) NOT NULL UNIQUE, descripcion VARCHAR(180) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS rol_permiso (
 id_rol INT NOT NULL,id_permiso INT NOT NULL,PRIMARY KEY(id_rol,id_permiso),
 FOREIGN KEY(id_rol) REFERENCES roles(id_rol), FOREIGN KEY(id_permiso) REFERENCES permisos(id_permiso)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS usuarios (
 id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(40) NOT NULL UNIQUE,
 nombre_completo VARCHAR(125) NOT NULL, carnet VARCHAR(40) UNIQUE, password_hash VARCHAR(180) NOT NULL,
 id_rol INT NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE,
 creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(id_rol) REFERENCES roles(id_rol)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS empleados (
 id_empleado BIGINT AUTO_INCREMENT PRIMARY KEY, cui CHAR(13) UNIQUE, carnet VARCHAR(40) UNIQUE,
 nombres VARCHAR(100) NOT NULL, apellidos VARCHAR(100) NOT NULL,
 puesto VARCHAR(80) NOT NULL, email VARCHAR(130), uid_rfid VARCHAR(80) UNIQUE,
 activo BOOLEAN NOT NULL DEFAULT TRUE, creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_empleados_nombre(apellidos,nombres), INDEX idx_empleados_puesto(puesto)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS salones (
 id_salon BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(20) NOT NULL UNIQUE,
 edificio VARCHAR(30) NOT NULL, nivel VARCHAR(30) NOT NULL,
 nombre VARCHAR(120) NOT NULL, capacidad INT NULL,
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT chk_capacidad CHECK (capacidad IS NULL OR capacidad >= 0),
 INDEX idx_salon_edificio_nivel(edificio,nivel)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS llaves (
 id_llave BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE,
 id_salon BIGINT NOT NULL UNIQUE, descripcion VARCHAR(180),
 estado ENUM('DISPONIBLE','ENTREGADA','EXTRAVIADA','DANADA','FUERA_SERVICIO') NOT NULL DEFAULT 'DISPONIBLE',
 activo BOOLEAN NOT NULL DEFAULT TRUE, FOREIGN KEY(id_salon) REFERENCES salones(id_salon),
 INDEX idx_llave_estado(estado,activo)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS series_reserva (
 id_serie BIGINT AUTO_INCREMENT PRIMARY KEY, frecuencia VARCHAR(20) NOT NULL DEFAULT 'SEMANAL',
 creado_en DATETIME DEFAULT CURRENT_TIMESTAMP, id_usuario BIGINT NOT NULL,
 FOREIGN KEY(id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS reservas (
 id_reserva BIGINT AUTO_INCREMENT PRIMARY KEY,id_salon BIGINT NOT NULL,
 id_empleado BIGINT NOT NULL,id_usuario BIGINT NOT NULL,id_serie BIGINT NULL,
 inicio DATETIME NOT NULL,fin DATETIME NOT NULL,
 estado ENUM('ACTIVA','CANCELADA','CERRADA') NOT NULL DEFAULT 'ACTIVA',
 observacion VARCHAR(240), creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_reserva_horario CHECK (fin > inicio),
 FOREIGN KEY(id_salon) REFERENCES salones(id_salon),
 FOREIGN KEY(id_empleado) REFERENCES empleados(id_empleado),
 FOREIGN KEY(id_usuario) REFERENCES usuarios(id_usuario),
 FOREIGN KEY(id_serie) REFERENCES series_reserva(id_serie),
 INDEX idx_reserva_salon_fecha(id_salon,inicio,fin,estado),
 INDEX idx_reserva_estado_inicio(estado,inicio)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS entregas (
 id_entrega BIGINT AUTO_INCREMENT PRIMARY KEY,id_llave BIGINT NOT NULL,id_empleado BIGINT NOT NULL,
 id_usuario_entrega BIGINT NOT NULL,id_reserva BIGINT NULL,
 identificacion_dejada VARCHAR(80), fecha_entrega DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 fecha_prevista DATETIME NULL,
 estado ENUM('ACTIVA','CERRADA') NOT NULL DEFAULT 'ACTIVA',
 llave_activa BIGINT GENERATED ALWAYS AS (CASE WHEN estado = 'ACTIVA' THEN id_llave ELSE NULL END) STORED,
 UNIQUE KEY uq_entrega_abierta(llave_activa), INDEX idx_entrega_empleado(id_empleado,estado),
 INDEX idx_entrega_fecha(fecha_entrega), INDEX idx_entrega_estado_prevista(estado,fecha_prevista),
 FOREIGN KEY(id_llave) REFERENCES llaves(id_llave),FOREIGN KEY(id_empleado) REFERENCES empleados(id_empleado),
 FOREIGN KEY(id_usuario_entrega) REFERENCES usuarios(id_usuario),FOREIGN KEY(id_reserva) REFERENCES reservas(id_reserva)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS devoluciones (
 id_devolucion BIGINT AUTO_INCREMENT PRIMARY KEY,id_entrega BIGINT NOT NULL UNIQUE,
 id_empleado_devuelve BIGINT NOT NULL,id_usuario_recibe BIGINT NOT NULL,
 fecha_devolucion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 documento_retirado BOOLEAN NOT NULL DEFAULT FALSE, observacion VARCHAR(250),
 FOREIGN KEY(id_entrega) REFERENCES entregas(id_entrega),
 FOREIGN KEY(id_empleado_devuelve) REFERENCES empleados(id_empleado),
 FOREIGN KEY(id_usuario_recibe) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS incidencias (
 id_incidencia BIGINT AUTO_INCREMENT PRIMARY KEY,
 id_entrega BIGINT NULL,id_llave BIGINT NULL,id_usuario_registra BIGINT NOT NULL,
 tipo ENUM('RETRASO','EXTRAVIO','DANO','DOCUMENTO_OLVIDADO','OTRA') NOT NULL,
 descripcion TEXT NOT NULL,estado ENUM('ABIERTA','EN_REVISION','CERRADA') NOT NULL DEFAULT 'ABIERTA',
 fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 fecha_cierre DATETIME NULL, id_usuario_cierra BIGINT NULL,
 FOREIGN KEY(id_entrega) REFERENCES entregas(id_entrega),FOREIGN KEY(id_llave) REFERENCES llaves(id_llave),
 FOREIGN KEY(id_usuario_registra) REFERENCES usuarios(id_usuario),
 FOREIGN KEY(id_usuario_cierra) REFERENCES usuarios(id_usuario),
 INDEX idx_incidencia_estado_fecha(estado,fecha_registro), INDEX idx_incidencia_llave(id_llave),
 CONSTRAINT chk_incidencia_referencia CHECK (id_entrega IS NOT NULL OR id_llave IS NOT NULL)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS comentarios_reporte (
 id_comentario BIGINT AUTO_INCREMENT PRIMARY KEY,
 id_usuario BIGINT NOT NULL,
 tipo_reporte VARCHAR(45) NOT NULL,
 fecha_desde DATE NOT NULL,
 fecha_hasta DATE NOT NULL,
 comentario VARCHAR(500) NOT NULL,
 fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(id_usuario) REFERENCES usuarios(id_usuario),
 INDEX idx_comentarios_tipo_fecha(tipo_reporte,fecha_registro)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS auditoria (
 id_auditoria BIGINT AUTO_INCREMENT PRIMARY KEY,id_usuario BIGINT NOT NULL,
 evento VARCHAR(75) NOT NULL,entidad VARCHAR(60) NOT NULL, id_entidad BIGINT,
 detalle VARCHAR(600),fecha_evento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(id_usuario) REFERENCES usuarios(id_usuario),INDEX idx_auditoria_fecha(fecha_evento),
 INDEX idx_auditoria_usuario_fecha(id_usuario,fecha_evento)
) ENGINE=InnoDB;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_aplicar_indice_si_falta$$
CREATE PROCEDURE sp_aplicar_indice_si_falta(IN p_tabla VARCHAR(64),IN p_nombre VARCHAR(64),IN p_columnas VARCHAR(150))
BEGIN
 DECLARE existe INT DEFAULT 0;
 SELECT COUNT(*) INTO existe FROM information_schema.statistics
 WHERE table_schema=DATABASE() AND table_name=p_tabla AND index_name=p_nombre;
 IF existe=0 THEN
  SET @idx_sql=CONCAT('CREATE INDEX `',p_nombre,'` ON `',p_tabla,'` (',p_columnas,')');
  PREPARE sentencia FROM @idx_sql;
  EXECUTE sentencia;
  DEALLOCATE PREPARE sentencia;
 END IF;
END$$
DELIMITER ;
CALL sp_aplicar_indice_si_falta('empleados','idx_empleados_nombre','`apellidos`,`nombres`');
CALL sp_aplicar_indice_si_falta('empleados','idx_empleados_puesto','`puesto`');
CALL sp_aplicar_indice_si_falta('salones','idx_salon_edificio_nivel','`edificio`,`nivel`');
CALL sp_aplicar_indice_si_falta('llaves','idx_llave_estado','`estado`,`activo`');
CALL sp_aplicar_indice_si_falta('entregas','idx_entrega_fecha','`fecha_entrega`');
CALL sp_aplicar_indice_si_falta('entregas','idx_entrega_estado_prevista','`estado`,`fecha_prevista`');
CALL sp_aplicar_indice_si_falta('incidencias','idx_incidencia_estado_fecha','`estado`,`fecha_registro`');
CALL sp_aplicar_indice_si_falta('incidencias','idx_incidencia_llave','`id_llave`');
CALL sp_aplicar_indice_si_falta('auditoria','idx_auditoria_usuario_fecha','`id_usuario`,`fecha_evento`');
CALL sp_aplicar_indice_si_falta('reservas','idx_reserva_estado_inicio','`estado`,`inicio`');
DROP PROCEDURE IF EXISTS sp_aplicar_indice_si_falta;

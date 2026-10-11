USE dbkinalllaves;

SET @sql_detalle_auditoria = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE auditoria ADD COLUMN detalle VARCHAR(600) NULL',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auditoria'
      AND COLUMN_NAME = 'detalle'
);
PREPARE stmt_detalle_auditoria FROM @sql_detalle_auditoria;
EXECUTE stmt_detalle_auditoria;
DEALLOCATE PREPARE stmt_detalle_auditoria;

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

SET @sql_carnet_usuario = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE usuarios ADD COLUMN carnet VARCHAR(40) NULL UNIQUE',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'carnet'
);
PREPARE stmt_carnet_usuario FROM @sql_carnet_usuario;
EXECUTE stmt_carnet_usuario;
DEALLOCATE PREPARE stmt_carnet_usuario;

SET @sql_carnet_empleado = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE empleados ADD COLUMN carnet VARCHAR(40) NULL UNIQUE',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'empleados' AND COLUMN_NAME = 'carnet'
);
PREPARE stmt_carnet_empleado FROM @sql_carnet_empleado;
EXECUTE stmt_carnet_empleado;
DEALLOCATE PREPARE stmt_carnet_empleado;

CREATE OR REPLACE VIEW vw_estado_llaves AS
 SELECT l.id_llave,l.codigo AS llave,s.codigo AS salon,s.nombre AS nombre_salon,
 CASE WHEN l.estado='DISPONIBLE' AND EXISTS(
  SELECT 1 FROM reservas r WHERE r.id_salon=s.id_salon AND r.estado='ACTIVA'
  AND NOW()>=r.inicio AND NOW()<r.fin) THEN 'RESERVADA'
 ELSE l.estado END AS estado_operativo,
 CONCAT_WS(' ',e.nombres,e.apellidos) AS responsable, t.id_entrega,
 t.fecha_entrega,t.fecha_prevista
 FROM llaves l JOIN salones s ON l.id_salon=s.id_salon
 LEFT JOIN entregas t ON t.id_llave=l.id_llave AND t.estado='ACTIVA'
 LEFT JOIN empleados e ON t.id_empleado=e.id_empleado WHERE l.activo=1 AND s.activo=1;
CREATE OR REPLACE VIEW vw_historial_entregas AS
 SELECT t.id_entrega,l.codigo AS llave,s.codigo AS salon,
 CONCAT_WS(' ',e.nombres,e.apellidos) AS empleado, e.carnet, u.username AS secretario,
 t.fecha_entrega,t.fecha_prevista,t.estado,t.identificacion_dejada,
 d.fecha_devolucion, TIMESTAMPDIFF(MINUTE, t.fecha_entrega, COALESCE(d.fecha_devolucion, NOW())) AS minutos_uso
 FROM entregas t JOIN llaves l ON t.id_llave=l.id_llave JOIN salones s ON l.id_salon=s.id_salon
 JOIN empleados e ON t.id_empleado=e.id_empleado
 JOIN usuarios u ON t.id_usuario_entrega=u.id_usuario
 LEFT JOIN devoluciones d ON d.id_entrega=t.id_entrega;
CREATE OR REPLACE VIEW vw_historial_devoluciones AS
 SELECT d.id_devolucion,d.id_entrega,l.codigo AS llave,s.codigo AS salon,
 CONCAT_WS(' ',e.nombres,e.apellidos) AS quien_devuelve, e.carnet AS carnet_devuelve,
 CONCAT_WS(' ',ep.nombres,ep.apellidos) AS quien_recibio_llave, ep.carnet AS carnet_receptor,
 t.fecha_entrega, d.fecha_devolucion,
 TIMESTAMPDIFF(MINUTE, t.fecha_entrega, d.fecha_devolucion) AS minutos_uso,
 u.username AS quien_recibe, d.documento_retirado,d.observacion
 FROM devoluciones d JOIN entregas t ON t.id_entrega=d.id_entrega
 JOIN llaves l ON l.id_llave=t.id_llave JOIN salones s ON s.id_salon=l.id_salon
 JOIN empleados e ON d.id_empleado_devuelve=e.id_empleado
 JOIN empleados ep ON ep.id_empleado=t.id_empleado
 JOIN usuarios u ON d.id_usuario_recibe=u.id_usuario;
CREATE OR REPLACE VIEW vw_uso_salones AS
 SELECT s.codigo AS salon,DATE(t.fecha_entrega) AS fecha,COUNT(*) AS usos
 FROM entregas t JOIN llaves l ON t.id_llave=l.id_llave JOIN salones s ON l.id_salon=s.id_salon
 GROUP BY s.codigo,DATE(t.fecha_entrega);
CREATE OR REPLACE VIEW vw_indicadores AS
 SELECT (SELECT COUNT(*) FROM empleados WHERE activo=1) AS empleados,
 (SELECT COUNT(*) FROM salones WHERE activo=1) AS salones,
 (SELECT COUNT(*) FROM llaves l JOIN salones s ON s.id_salon=l.id_salon
  WHERE l.activo=1 AND s.activo=1 AND l.estado='DISPONIBLE'
  AND NOT EXISTS(SELECT 1 FROM reservas r WHERE r.id_salon=s.id_salon AND r.estado='ACTIVA' AND NOW()>=r.inicio AND NOW()<r.fin)) AS disponibles,
 (SELECT COUNT(*) FROM entregas WHERE estado='ACTIVA') AS entregadas,
 (SELECT COUNT(*) FROM reservas WHERE estado='ACTIVA' AND inicio>=NOW()) AS reservas,
 (SELECT COUNT(*) FROM incidencias WHERE estado<>'CERRADA') AS incidencias;

CREATE OR REPLACE VIEW vw_reservas_detalle AS
 SELECT r.id_reserva,r.id_salon,s.codigo AS salon,s.nombre AS nombre_salon,
 r.id_empleado,CONCAT_WS(' ',e.nombres,e.apellidos) AS empleado,
 r.id_usuario,u.username AS registrado_por,r.id_serie,r.inicio,r.fin,
 r.estado,r.observacion,r.creado_en
 FROM reservas r JOIN salones s ON s.id_salon=r.id_salon
 JOIN empleados e ON e.id_empleado=r.id_empleado
 JOIN usuarios u ON u.id_usuario=r.id_usuario;

CREATE OR REPLACE VIEW vw_entregas_activas AS
 SELECT t.id_entrega,l.codigo AS llave,s.codigo AS salon,
 CONCAT_WS(' ',e.nombres,e.apellidos) AS responsable, e.carnet,
 u.username AS registrado_por,t.fecha_entrega,t.fecha_prevista,
 t.identificacion_dejada,
 IF(t.fecha_prevista IS NOT NULL AND t.fecha_prevista<NOW(),'ATRASADA','EN_CURSO') AS situacion
 FROM entregas t JOIN llaves l ON l.id_llave=t.id_llave
 JOIN salones s ON s.id_salon=l.id_salon
 JOIN empleados e ON e.id_empleado=t.id_empleado
 JOIN usuarios u ON u.id_usuario=t.id_usuario_entrega
 WHERE t.estado='ACTIVA';

SELECT 'Empleados' entidad,COUNT(*) total FROM empleados
UNION ALL SELECT 'Salones',COUNT(*) FROM salones
UNION ALL SELECT 'Llaves',COUNT(*) FROM llaves
UNION ALL SELECT 'Usuarios',COUNT(*) FROM usuarios
UNION ALL SELECT 'Entregas',COUNT(*) FROM entregas
UNION ALL SELECT 'Devoluciones',COUNT(*) FROM devoluciones
UNION ALL SELECT 'Reservas',COUNT(*) FROM reservas
UNION ALL SELECT 'Incidencias',COUNT(*) FROM incidencias
UNION ALL SELECT 'Auditoría',COUNT(*) FROM auditoria;
SELECT id_llave,COUNT(*) cantidad FROM entregas WHERE estado='ACTIVA' GROUP BY id_llave HAVING COUNT(*)>1;
SELECT l.codigo,l.estado,COUNT(t.id_entrega) activas FROM llaves l
LEFT JOIN entregas t ON t.id_llave=l.id_llave AND t.estado='ACTIVA'
GROUP BY l.id_llave,l.codigo,l.estado
HAVING (l.estado='ENTREGADA' AND activas<>1) OR (l.estado='DISPONIBLE' AND activas<>0);
SELECT a.id_reserva reserva_a,b.id_reserva reserva_b,s.codigo salon
FROM reservas a JOIN reservas b ON a.id_salon=b.id_salon AND a.id_reserva<b.id_reserva
JOIN salones s ON s.id_salon=a.id_salon
WHERE a.estado='ACTIVA' AND b.estado='ACTIVA' AND a.inicio<b.fin AND a.fin>b.inicio;
SELECT d.id_devolucion,t.id_entrega FROM devoluciones d JOIN entregas t ON t.id_entrega=d.id_entrega WHERE t.estado<>'CERRADA';
SELECT * FROM vw_indicadores;
SELECT * FROM vw_estado_llaves ORDER BY salon LIMIT 50;
SELECT * FROM vw_historial_entregas ORDER BY id_entrega DESC LIMIT 20;

SELECT r.codigo AS rol, COUNT(rp.id_permiso) AS permisos_asignados
FROM roles r LEFT JOIN rol_permiso rp ON rp.id_rol=r.id_rol GROUP BY r.id_rol,r.codigo;
SELECT COUNT(*) AS administradores_activos FROM usuarios u
JOIN roles r ON r.id_rol=u.id_rol WHERE r.codigo='ADMIN' AND u.activo=1;
SELECT e.id_entrega,l.codigo,l.estado FROM entregas e JOIN llaves l ON e.id_llave=l.id_llave
WHERE e.estado='ACTIVA' AND l.estado<>'ENTREGADA';
SELECT l.id_llave,l.codigo FROM llaves l WHERE l.estado='ENTREGADA'
AND NOT EXISTS(SELECT 1 FROM entregas e WHERE e.id_llave=l.id_llave AND e.estado='ACTIVA');
SELECT i.id_incidencia,i.id_llave,i.id_entrega FROM incidencias i
JOIN entregas e ON i.id_entrega=e.id_entrega WHERE i.id_llave IS NOT NULL AND i.id_llave<>e.id_llave;
SELECT id_salon,COUNT(*) AS llaves FROM llaves GROUP BY id_salon HAVING COUNT(*)>1;
SELECT l.codigo,s.codigo FROM llaves l JOIN salones s ON s.id_salon=l.id_salon
WHERE l.activo=1 AND s.activo=0;
SELECT r.id_reserva,e.nombres,e.apellidos FROM reservas r JOIN empleados e ON e.id_empleado=r.id_empleado
WHERE r.estado='ACTIVA' AND r.fin>NOW() AND e.activo=0;
SELECT r.id_reserva,s.codigo FROM reservas r JOIN salones s ON s.id_salon=r.id_salon
LEFT JOIN llaves l ON l.id_salon=s.id_salon AND l.activo=1
WHERE r.estado='ACTIVA' AND r.fin>NOW() AND (s.activo=0 OR l.id_llave IS NULL OR l.estado IN ('EXTRAVIADA','DANADA','FUERA_SERVICIO'));
SELECT id_llave,COUNT(*) AS cantidad FROM entregas WHERE estado='ACTIVA'
GROUP BY id_llave HAVING COUNT(*)>1;
SELECT * FROM vw_indicadores;
SELECT * FROM vw_estado_llaves ORDER BY salon;

SELECT r.codigo,p.codigo AS permiso FROM rol_permiso rp JOIN roles r ON r.id_rol=rp.id_rol
 JOIN permisos p ON p.id_permiso=rp.id_permiso ORDER BY r.codigo,p.codigo;
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME;

SHOW CREATE PROCEDURE sp_entregar_llave;
SHOW CREATE PROCEDURE sp_devolver_llave;
SELECT * FROM vw_estado_llaves;
SELECT * FROM vw_historial_entregas ORDER BY id_entrega DESC;
SELECT * FROM vw_historial_devoluciones ORDER BY id_devolucion DESC;
SELECT * FROM incidencias ORDER BY id_incidencia DESC;
SELECT * FROM auditoria ORDER BY id_auditoria DESC;

SHOW CREATE PROCEDURE sp_registrar_reserva;
SELECT id_reserva,id_salon,id_empleado,inicio,fin,estado FROM reservas ORDER BY id_reserva DESC;
SELECT * FROM vw_uso_salones ORDER BY fecha DESC;
SELECT * FROM vw_indicadores;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_verificar_instalacion$$
CREATE PROCEDURE sp_verificar_instalacion()
BEGIN
 DECLARE v_tablas INT DEFAULT 0;
 DECLARE v_vistas INT DEFAULT 0;
 DECLARE v_rutinas INT DEFAULT 0;
 DECLARE v_roles INT DEFAULT 0;
 DECLARE v_permisos INT DEFAULT 0;
 SELECT COUNT(*) INTO v_tablas FROM information_schema.tables
 WHERE table_schema=DATABASE() AND table_type='BASE TABLE'
 AND table_name IN ('roles','permisos','rol_permiso','usuarios','empleados','salones',
 'llaves','series_reserva','reservas','entregas','devoluciones','incidencias','auditoria','comentarios_reporte');
 SELECT COUNT(*) INTO v_vistas FROM information_schema.views
 WHERE table_schema=DATABASE() AND table_name IN
 ('vw_estado_llaves','vw_historial_entregas','vw_historial_devoluciones',
 'vw_uso_salones','vw_indicadores','vw_reservas_detalle','vw_entregas_activas');
 SELECT COUNT(*) INTO v_rutinas FROM information_schema.routines
 WHERE routine_schema=DATABASE() AND routine_type='PROCEDURE'
 AND routine_name IN ('sp_entregar_llave','sp_devolver_llave',
 'sp_registrar_reserva','sp_cargar_movimientos_demo','sp_verificar_instalacion');
 SELECT COUNT(*) INTO v_roles FROM roles WHERE codigo IN ('ADMIN','JEFE','SECRETARIO');
 SELECT COUNT(*) INTO v_permisos FROM permisos;
 SELECT DATABASE() AS esquema,
 v_tablas AS tablas_de_14,v_vistas AS vistas_de_7,
 v_rutinas AS procedimientos_de_5,v_roles AS roles_de_3,
 v_permisos AS permisos,
 (SELECT COUNT(*) FROM empleados) AS empleados,
 (SELECT COUNT(*) FROM salones) AS salones,
 (SELECT COUNT(*) FROM llaves) AS llaves,
 CASE WHEN v_tablas=14 AND v_vistas=7 AND v_rutinas=5 AND v_roles=3 AND v_permisos>=12
 THEN 'ESTRUCTURA COMPLETA' ELSE 'REVISAR INSTALACION' END AS diagnostico;
 IF v_tablas<>14 OR v_vistas<>7 OR v_rutinas<>5 OR v_roles<>3 OR v_permisos<12 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Instalacion incompleta: verifica scripts 01 a 04';
 END IF;
END$$
DELIMITER ;
CALL sp_verificar_instalacion();

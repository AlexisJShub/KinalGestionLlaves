USE dbkinalllaves;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_entregar_llave$$
CREATE PROCEDURE sp_entregar_llave(
 IN p_llave BIGINT,IN p_empleado BIGINT,IN p_usuario BIGINT,
 IN p_reserva BIGINT,IN p_documento VARCHAR(80),IN p_prevista DATETIME)
BEGIN
 DECLARE v_estado VARCHAR(20);
 DECLARE v_existe INT DEFAULT 0;
 DECLARE v_id BIGINT;
 DECLARE v_reserva_asignada BIGINT DEFAULT NULL;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 SELECT l.estado INTO v_estado FROM llaves l
 JOIN salones s ON s.id_salon=l.id_salon AND s.activo=1
 WHERE l.id_llave=p_llave AND l.activo=1 FOR UPDATE;
 IF v_estado IS NULL OR v_estado <> 'DISPONIBLE' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Llave o salon no disponible para entrega'; END IF;
 IF p_prevista IS NOT NULL AND p_prevista < NOW() THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La fecha prevista de devolucion no puede ser pasada'; END IF;
 SELECT COUNT(*) INTO v_existe FROM reservas r JOIN llaves l ON l.id_salon=r.id_salon
 WHERE l.id_llave=p_llave AND r.estado='ACTIVA' AND NOW()>=r.inicio AND NOW()<r.fin
 AND r.id_empleado<>p_empleado;
 IF v_existe>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reserva actual pertenece a otro empleado'; END IF;
 SELECT COUNT(*) INTO v_existe FROM empleados WHERE id_empleado=p_empleado AND activo=1;
 IF v_existe <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Empleado no activo'; END IF;
 SELECT COUNT(*) INTO v_existe FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol
 WHERE u.id_usuario=p_usuario AND u.activo=1 AND r.codigo IN('ADMIN','SECRETARIO','JEFE');
 IF v_existe <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Usuario no autorizado'; END IF;
 IF p_reserva IS NOT NULL THEN
  SELECT COUNT(*) INTO v_existe FROM reservas r JOIN llaves l ON l.id_salon=r.id_salon
  WHERE r.id_reserva=p_reserva AND r.estado='ACTIVA' AND l.id_llave=p_llave
    AND r.id_empleado=p_empleado AND NOW()>=r.inicio AND NOW()<r.fin;
  IF v_existe<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reserva no vigente o no corresponde a la entrega'; END IF;
 END IF;
 SET v_reserva_asignada = p_reserva;
 IF v_reserva_asignada IS NULL THEN
  SET v_reserva_asignada = (
   SELECT r.id_reserva FROM reservas r JOIN llaves l ON l.id_salon=r.id_salon
   WHERE l.id_llave=p_llave AND r.id_empleado=p_empleado
     AND r.estado='ACTIVA' AND NOW() >= r.inicio AND NOW() < r.fin
   ORDER BY r.inicio, r.id_reserva LIMIT 1
  );
 END IF;
 INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,id_reserva,identificacion_dejada,fecha_prevista)
 VALUES(p_llave,p_empleado,p_usuario,v_reserva_asignada,p_documento,p_prevista);
 SET v_id=LAST_INSERT_ID();
 UPDATE llaves SET estado='ENTREGADA' WHERE id_llave=p_llave;
 INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
 VALUES(p_usuario,'ENTREGA','entregas',v_id,CONCAT('Llave ',p_llave,' empleado ',p_empleado));
 COMMIT;
 SELECT v_id AS id_entrega;
END$$
DROP PROCEDURE IF EXISTS sp_devolver_llave$$
CREATE PROCEDURE sp_devolver_llave(
 IN p_entrega BIGINT, IN p_devuelve BIGINT,IN p_usuario BIGINT,
 IN p_documento_retirado BOOLEAN,IN p_observacion VARCHAR(250))
BEGIN
 DECLARE v_llave BIGINT DEFAULT NULL;
 DECLARE v_estado VARCHAR(15);
 DECLARE v_documento VARCHAR(80) DEFAULT NULL;
 DECLARE v_incidencia BIGINT DEFAULT NULL;
 DECLARE v_existe INT DEFAULT 0;
 DECLARE v_id BIGINT;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 SELECT id_llave,estado,identificacion_dejada INTO v_llave,v_estado,v_documento
 FROM entregas WHERE id_entrega=p_entrega FOR UPDATE;
 IF v_llave IS NULL OR v_estado <> 'ACTIVA' THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La entrega no está activa'; END IF;
 SELECT COUNT(*) INTO v_existe FROM empleados WHERE id_empleado=p_devuelve AND activo=1;
 IF v_existe<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Empleado que devuelve no válido'; END IF;
 SELECT COUNT(*) INTO v_existe FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol
 WHERE u.id_usuario=p_usuario AND u.activo=1 AND r.codigo IN('ADMIN','SECRETARIO','JEFE');
 IF v_existe<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Usuario no autorizado'; END IF;
 INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,documento_retirado,observacion)
 VALUES(p_entrega,p_devuelve,p_usuario,p_documento_retirado,p_observacion);
 SET v_id=LAST_INSERT_ID();
 UPDATE entregas SET estado='CERRADA' WHERE id_entrega=p_entrega;
 UPDATE llaves SET estado=CASE
  WHEN estado IN ('DANADA','FUERA_SERVICIO') THEN estado
  ELSE 'DISPONIBLE' END WHERE id_llave=v_llave;
 INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
 VALUES(p_usuario,'DEVOLUCION','devoluciones',v_id,CONCAT('Entrega ',p_entrega,' devolvio ',p_devuelve));
 IF COALESCE(p_documento_retirado,0)=0 AND v_documento IS NOT NULL AND TRIM(v_documento)<>'' THEN
  INSERT INTO incidencias(id_entrega,id_llave,id_usuario_registra,tipo,descripcion)
  VALUES(p_entrega,v_llave,p_usuario,'DOCUMENTO_OLVIDADO',
    CONCAT('Documento pendiente de retiro tras devolucion #',p_entrega));
  SET v_incidencia=LAST_INSERT_ID();
  INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
  VALUES(p_usuario,'INCIDENCIA','incidencias',v_incidencia,'Documento pendiente de retiro');
 END IF;
 COMMIT;
 SELECT v_id AS id_devolucion;
END$$
DROP PROCEDURE IF EXISTS sp_registrar_reserva$$
CREATE PROCEDURE sp_registrar_reserva(
 IN p_salon BIGINT,IN p_empleado BIGINT,IN p_usuario BIGINT,
 IN p_inicio DATETIME,IN p_fin DATETIME,IN p_serie BIGINT, IN p_obs VARCHAR(240))
BEGIN
 DECLARE v_id BIGINT;
 DECLARE v_cuenta INT DEFAULT 0;
 DECLARE v_salon BIGINT;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 IF p_inicio IS NULL OR p_fin IS NULL OR p_fin<=p_inicio THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Horario de reserva invalido';END IF;
 IF p_inicio < NOW() THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No se permiten reservas en el pasado';END IF;
 START TRANSACTION;
 SELECT id_salon INTO v_salon FROM salones WHERE id_salon=p_salon AND activo=1 FOR UPDATE;
 IF v_salon IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Salón no disponible';END IF;
 SELECT COUNT(*) INTO v_cuenta FROM llaves WHERE id_salon=p_salon AND activo=1
    AND estado NOT IN ('EXTRAVIADA','DANADA','FUERA_SERVICIO');
 IF v_cuenta<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El salón no tiene llave operativa'; END IF;
 SELECT COUNT(*) INTO v_cuenta FROM usuarios u JOIN roles ro ON ro.id_rol=u.id_rol
 WHERE u.id_usuario=p_usuario AND u.activo=1
 AND ro.codigo IN ('ADMIN','JEFE','SECRETARIO');
 IF v_cuenta<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Usuario no autorizado'; END IF;
 SELECT COUNT(*) INTO v_cuenta FROM empleados WHERE id_empleado=p_empleado AND activo=1;
 IF v_cuenta<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Empleado no disponible';END IF;
 IF p_serie IS NOT NULL THEN
  SELECT COUNT(*) INTO v_cuenta FROM series_reserva WHERE id_serie=p_serie AND id_usuario=p_usuario;
  IF v_cuenta<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Serie de reserva no valida'; END IF;
 END IF;
 SELECT COUNT(*) INTO v_cuenta FROM reservas
 WHERE id_salon=p_salon AND estado='ACTIVA' AND inicio<p_fin AND fin>p_inicio;
 IF v_cuenta>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicto de horario';END IF;
 INSERT INTO reservas(id_salon,id_empleado,id_usuario,id_serie,inicio,fin,observacion)
 VALUES(p_salon,p_empleado,p_usuario,p_serie,p_inicio,p_fin,p_obs);
 SET v_id=LAST_INSERT_ID();
 INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
 VALUES(p_usuario,'RESERVA','reservas',v_id,CONCAT('Salón ',p_salon));
 COMMIT;
 SELECT v_id AS id_reserva;
END$$
DELIMITER ;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_cargar_movimientos_demo$$
CREATE PROCEDURE sp_cargar_movimientos_demo()
BEGIN
 DECLARE v_admin BIGINT DEFAULT NULL;
 DECLARE v_llave BIGINT DEFAULT NULL;
 DECLARE v_empleado BIGINT DEFAULT NULL;
 DECLARE v_salon BIGINT DEFAULT NULL;
 DECLARE v_entrega BIGINT DEFAULT NULL;
 DECLARE v_dev BIGINT DEFAULT NULL;
 DECLARE v_i INT DEFAULT 0;
 DECLARE v_ref VARCHAR(80);
 DECLARE v_clave VARCHAR(20);
 DECLARE v_dia DATETIME;
 DECLARE v_inicio DATETIME;
 DECLARE v_fin DATETIME;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 SELECT u.id_usuario INTO v_admin FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol
 WHERE r.codigo='ADMIN' AND u.activo=1 ORDER BY u.id_usuario LIMIT 1;
 IF v_admin IS NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Primero crea el administrador inicial desde la pantalla de Login';
 END IF;
 START TRANSACTION;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-A11' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo01@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-001') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-001',DATE_SUB(NOW(),INTERVAL 7 DAY),DATE_SUB(NOW(),INTERVAL 7 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 7 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 7 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 7 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-A12' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo02@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-002') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-002',DATE_SUB(NOW(),INTERVAL 9 DAY),DATE_SUB(NOW(),INTERVAL 9 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 9 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 9 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 9 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-A13' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo03@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-003') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-003',DATE_SUB(NOW(),INTERVAL 11 DAY),DATE_SUB(NOW(),INTERVAL 11 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 11 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 11 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 11 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-A14' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo04@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-004') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-004',DATE_SUB(NOW(),INTERVAL 13 DAY),DATE_SUB(NOW(),INTERVAL 13 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 13 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 13 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 13 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-A21' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo05@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-005') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-005',DATE_SUB(NOW(),INTERVAL 15 DAY),DATE_SUB(NOW(),INTERVAL 15 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 15 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 15 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 15 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-A22' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo06@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-006') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-006',DATE_SUB(NOW(),INTERVAL 17 DAY),DATE_SUB(NOW(),INTERVAL 17 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 17 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 17 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 17 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-A23' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo07@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-007') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-007',DATE_SUB(NOW(),INTERVAL 19 DAY),DATE_SUB(NOW(),INTERVAL 19 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 19 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 19 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 19 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-A24' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo08@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-008') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-008',DATE_SUB(NOW(),INTERVAL 21 DAY),DATE_SUB(NOW(),INTERVAL 21 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 21 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 21 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 21 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-B11' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo09@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-009') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-009',DATE_SUB(NOW(),INTERVAL 23 DAY),DATE_SUB(NOW(),INTERVAL 23 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 23 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 23 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 23 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-B12' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo10@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-010') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-010',DATE_SUB(NOW(),INTERVAL 25 DAY),DATE_SUB(NOW(),INTERVAL 25 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 25 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 25 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 25 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-B13' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo11@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-011') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-011',DATE_SUB(NOW(),INTERVAL 27 DAY),DATE_SUB(NOW(),INTERVAL 27 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 27 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 27 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 27 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-B14' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo12@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-012') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-012',DATE_SUB(NOW(),INTERVAL 29 DAY),DATE_SUB(NOW(),INTERVAL 29 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 29 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 29 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 29 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-B21' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo13@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-013') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-013',DATE_SUB(NOW(),INTERVAL 31 DAY),DATE_SUB(NOW(),INTERVAL 31 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 31 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 31 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 31 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-B22' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo14@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-014') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-014',DATE_SUB(NOW(),INTERVAL 33 DAY),DATE_SUB(NOW(),INTERVAL 33 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 33 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 33 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 33 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-B23' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo15@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-015') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-015',DATE_SUB(NOW(),INTERVAL 35 DAY),DATE_SUB(NOW(),INTERVAL 35 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 35 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 35 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 35 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT l.id_llave FROM llaves l WHERE l.codigo='LL-B24' LIMIT 1);
 SET v_empleado=(SELECT e.id_empleado FROM empleados e WHERE e.email='demo16@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-HIST-016') THEN
   INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
   VALUES(v_llave,v_empleado,v_admin,'DEMO-HIST-016',DATE_SUB(NOW(),INTERVAL 37 DAY),DATE_SUB(NOW(),INTERVAL 37 DAY)+INTERVAL 4 HOUR,'CERRADA');
   SET v_entrega=LAST_INSERT_ID();
   INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion,documento_retirado,observacion)
   VALUES(v_entrega,v_empleado,v_admin,DATE_SUB(NOW(),INTERVAL 37 DAY)+INTERVAL 4 HOUR,TRUE,'Devolución demostrativa');
   SET v_dev=LAST_INSERT_ID();
   INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle,fecha_evento)
   VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 37 DAY)),
         (v_admin,'DEVOLUCION','devoluciones',v_dev,'Movimiento de demostración',DATE_SUB(NOW(),INTERVAL 37 DAY)+INTERVAL 4 HOUR);
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C11' AND activo=1 AND estado='DISPONIBLE' LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo21@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-ACT-001')
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE id_llave=v_llave AND estado='ACTIVA') THEN
    INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
    VALUES(v_llave,v_empleado,v_admin,'DEMO-ACT-001',DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 5 HOUR),'ACTIVA');
    SET v_entrega=LAST_INSERT_ID();
    UPDATE llaves SET estado='ENTREGADA' WHERE id_llave=v_llave;
    INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
    VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Entrega demostrativa en curso');
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C12' AND activo=1 AND estado='DISPONIBLE' LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo22@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-ACT-002')
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE id_llave=v_llave AND estado='ACTIVA') THEN
    INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
    VALUES(v_llave,v_empleado,v_admin,'DEMO-ACT-002',DATE_SUB(NOW(),INTERVAL 2 HOUR),DATE_ADD(NOW(),INTERVAL 6 HOUR),'ACTIVA');
    SET v_entrega=LAST_INSERT_ID();
    UPDATE llaves SET estado='ENTREGADA' WHERE id_llave=v_llave;
    INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
    VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Entrega demostrativa en curso');
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C13' AND activo=1 AND estado='DISPONIBLE' LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo23@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-ACT-003')
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE id_llave=v_llave AND estado='ACTIVA') THEN
    INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
    VALUES(v_llave,v_empleado,v_admin,'DEMO-ACT-003',DATE_SUB(NOW(),INTERVAL 3 HOUR),DATE_ADD(NOW(),INTERVAL 7 HOUR),'ACTIVA');
    SET v_entrega=LAST_INSERT_ID();
    UPDATE llaves SET estado='ENTREGADA' WHERE id_llave=v_llave;
    INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
    VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Entrega demostrativa en curso');
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C14' AND activo=1 AND estado='DISPONIBLE' LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo24@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-ACT-004')
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE id_llave=v_llave AND estado='ACTIVA') THEN
    INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
    VALUES(v_llave,v_empleado,v_admin,'DEMO-ACT-004',DATE_SUB(NOW(),INTERVAL 4 HOUR),DATE_ADD(NOW(),INTERVAL 8 HOUR),'ACTIVA');
    SET v_entrega=LAST_INSERT_ID();
    UPDATE llaves SET estado='ENTREGADA' WHERE id_llave=v_llave;
    INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
    VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Entrega demostrativa en curso');
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C21' AND activo=1 AND estado='DISPONIBLE' LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo25@ejemplo.invalid' LIMIT 1);
 IF v_llave IS NOT NULL AND v_empleado IS NOT NULL
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE identificacion_dejada='DEMO-ACT-005')
   AND NOT EXISTS(SELECT 1 FROM entregas WHERE id_llave=v_llave AND estado='ACTIVA') THEN
    INSERT INTO entregas(id_llave,id_empleado,id_usuario_entrega,identificacion_dejada,fecha_entrega,fecha_prevista,estado)
    VALUES(v_llave,v_empleado,v_admin,'DEMO-ACT-005',DATE_SUB(NOW(),INTERVAL 5 HOUR),DATE_ADD(NOW(),INTERVAL 9 HOUR),'ACTIVA');
    SET v_entrega=LAST_INSERT_ID();
    UPDATE llaves SET estado='ENTREGADA' WHERE id_llave=v_llave;
    INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle)
    VALUES(v_admin,'ENTREGA','entregas',v_entrega,'Entrega demostrativa en curso');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='A21' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo01@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 2 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-001')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-001');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='A22' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo02@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 3 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-002')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-002');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='A23' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo03@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 4 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-003')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-003');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='A24' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo04@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 5 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-004')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-004');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='B11' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo05@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 6 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-005')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-005');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='B12' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo06@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 7 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-006')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-006');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='B13' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo07@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 8 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-007')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-007');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='B14' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo08@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 9 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-008')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-008');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='B21' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo09@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 10 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-009')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-009');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='B22' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo10@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 11 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-010')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-010');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='B23' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo11@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 12 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-011')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-011');
 END IF;
 SET v_salon=(SELECT id_salon FROM salones WHERE codigo='B24' AND activo=1 LIMIT 1);
 SET v_empleado=(SELECT id_empleado FROM empleados WHERE email='demo12@ejemplo.invalid' LIMIT 1);
 SET v_inicio=DATE_ADD(DATE_ADD(CURDATE(),INTERVAL 13 DAY),INTERVAL 9 HOUR);
 SET v_fin=DATE_ADD(v_inicio,INTERVAL 2 HOUR);
 IF v_salon IS NOT NULL AND v_empleado IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE observacion='DEMO-RES-012')
    AND NOT EXISTS(SELECT 1 FROM reservas WHERE id_salon=v_salon AND estado='ACTIVA' AND inicio<v_fin AND fin>v_inicio) THEN
     INSERT INTO reservas(id_salon,id_empleado,id_usuario,inicio,fin,estado,observacion)
     VALUES(v_salon,v_empleado,v_admin,v_inicio,v_fin,'ACTIVA','DEMO-RES-012');
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C11' LIMIT 1);
 IF v_llave IS NOT NULL AND NOT EXISTS(SELECT 1 FROM incidencias WHERE descripcion='DEMO-INC-001') THEN
   INSERT INTO incidencias(id_llave,id_usuario_registra,tipo,descripcion,estado,fecha_registro)
   VALUES(v_llave,v_admin,'RETRASO','DEMO-INC-001','ABIERTA',DATE_SUB(NOW(),INTERVAL 2 HOUR));
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C12' LIMIT 1);
 IF v_llave IS NOT NULL AND NOT EXISTS(SELECT 1 FROM incidencias WHERE descripcion='DEMO-INC-002') THEN
   INSERT INTO incidencias(id_llave,id_usuario_registra,tipo,descripcion,estado,fecha_registro)
   VALUES(v_llave,v_admin,'DOCUMENTO_OLVIDADO','DEMO-INC-002','ABIERTA',DATE_SUB(NOW(),INTERVAL 4 HOUR));
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C13' LIMIT 1);
 IF v_llave IS NOT NULL AND NOT EXISTS(SELECT 1 FROM incidencias WHERE descripcion='DEMO-INC-003') THEN
   INSERT INTO incidencias(id_llave,id_usuario_registra,tipo,descripcion,estado,fecha_registro)
   VALUES(v_llave,v_admin,'DANO','DEMO-INC-003','EN_REVISION',DATE_SUB(NOW(),INTERVAL 6 HOUR));
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C14' LIMIT 1);
 IF v_llave IS NOT NULL AND NOT EXISTS(SELECT 1 FROM incidencias WHERE descripcion='DEMO-INC-004') THEN
   INSERT INTO incidencias(id_llave,id_usuario_registra,tipo,descripcion,estado,fecha_registro)
   VALUES(v_llave,v_admin,'OTRA','DEMO-INC-004','ABIERTA',DATE_SUB(NOW(),INTERVAL 8 HOUR));
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-C21' LIMIT 1);
 IF v_llave IS NOT NULL AND NOT EXISTS(SELECT 1 FROM incidencias WHERE descripcion='DEMO-INC-005') THEN
   INSERT INTO incidencias(id_llave,id_usuario_registra,tipo,descripcion,estado,fecha_registro)
   VALUES(v_llave,v_admin,'RETRASO','DEMO-INC-005','ABIERTA',DATE_SUB(NOW(),INTERVAL 10 HOUR));
 END IF;
 SET v_llave=(SELECT id_llave FROM llaves WHERE codigo='LL-A11' LIMIT 1);
 IF v_llave IS NOT NULL AND NOT EXISTS(SELECT 1 FROM incidencias WHERE descripcion='DEMO-INC-006') THEN
   INSERT INTO incidencias(id_llave,id_usuario_registra,tipo,descripcion,estado,fecha_registro)
   VALUES(v_llave,v_admin,'OTRA','DEMO-INC-006','CERRADA',DATE_SUB(NOW(),INTERVAL 12 HOUR));
 END IF;
 COMMIT;
END$$
DELIMITER ;

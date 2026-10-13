USE dbkinalllaves;

INSERT INTO roles(codigo,nombre) VALUES
 ('ADMIN','Administrador'),('SECRETARIO','Secretario'),('JEFE','Jefe de Secretaría')
 ON DUPLICATE KEY UPDATE nombre=VALUES(nombre);
INSERT INTO permisos(codigo,descripcion) VALUES
 ('USUARIOS','Administrar usuarios'),('ESTADOS','Consultar estado de llaves'),('EMPLEADOS','Administrar empleados'),
 ('SALONES','Administrar salones'),('LLAVES','Administrar llaves'),
 ('ENTREGAS','Registrar entrega'),('DEVOLUCIONES','Registrar devolución'),
 ('RESERVAS','Administrar reservas'),('INCIDENCIAS','Registrar incidencias'),
 ('GESTION_INCIDENCIAS','Gestionar estado de incidencias'),
 ('AUDITORIA','Consultar auditoría'),('REPORTES','Consultar y exportar reportes')
 ON DUPLICATE KEY UPDATE descripcion=VALUES(descripcion);
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
 SELECT r.id_rol,p.id_permiso FROM roles r JOIN permisos p
 WHERE r.codigo='ADMIN';
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
 SELECT r.id_rol,p.id_permiso FROM roles r JOIN permisos p
 WHERE r.codigo='JEFE' AND p.codigo IN
 ('EMPLEADOS','SALONES','LLAVES','ESTADOS','ENTREGAS','DEVOLUCIONES','RESERVAS','INCIDENCIAS','GESTION_INCIDENCIAS','AUDITORIA','REPORTES');
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
 SELECT r.id_rol,p.id_permiso FROM roles r JOIN permisos p
 WHERE r.codigo='SECRETARIO' AND p.codigo IN
 ('ESTADOS','ENTREGAS','DEVOLUCIONES','RESERVAS','INCIDENCIAS','REPORTES');
INSERT INTO usuarios (username,nombre_completo,password_hash,id_rol,activo)
SELECT 'admin1','Administrador de Demostración',
       'pbkdf2$210000$gakTg5sND/kjSRNUkAehYw==$eBBLPnDR/RPUvIkj4kl9f3NMwTsKZVSpolI9FBRzJB0=',r.id_rol,1
FROM roles r WHERE r.codigo='ADMIN'
AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.username='admin1');

INSERT INTO usuarios (username,nombre_completo,password_hash,id_rol,activo)
SELECT 'jefe1','Jefe de Secretaría Demo',
       'pbkdf2$210000$0F0MYWI8ybFEBfbX/xOaxg==$Ihw22v/xaKc7GeElU3y/G68QwnJokO17wqV77EWQ+bw=',r.id_rol,1
FROM roles r WHERE r.codigo='JEFE'
AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.username='jefe1');

INSERT INTO usuarios (username,nombre_completo,password_hash,id_rol,activo)
SELECT 'secretario1','Secretario de Demostración',
       'pbkdf2$210000$c+UB4QfxspIHGSp6YRrKrQ==$vaj8aY8cTwAxCpSVWwq3flJDgLiF++BdxxirxKQwEQM=',r.id_rol,1
FROM roles r WHERE r.codigo='SECRETARIO'
AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.username='secretario1');

START TRANSACTION;
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 01', 'Personal de Prueba', 'Docente de Informática', 'demo01@ejemplo.invalid', 'DEMO-RFID-001'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo01@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 02', 'Personal de Prueba', 'Docente de Electricidad', 'demo02@ejemplo.invalid', 'DEMO-RFID-002'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo02@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 03', 'Personal de Prueba', 'Docente de Mecánica', 'demo03@ejemplo.invalid', 'DEMO-RFID-003'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo03@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 04', 'Personal de Prueba', 'Docente de Matemáticas', 'demo04@ejemplo.invalid', 'DEMO-RFID-004'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo04@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 05', 'Personal de Prueba', 'Coordinación Académica', 'demo05@ejemplo.invalid', 'DEMO-RFID-005'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo05@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 06', 'Personal de Prueba', 'Docente de Electrónica', 'demo06@ejemplo.invalid', 'DEMO-RFID-006'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo06@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 07', 'Personal de Prueba', 'Asistencia Académica', 'demo07@ejemplo.invalid', 'DEMO-RFID-007'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo07@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 08', 'Personal de Prueba', 'Dirección Técnica', 'demo08@ejemplo.invalid', 'DEMO-RFID-008'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo08@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 09', 'Personal de Prueba', 'Docente de Informática', 'demo09@ejemplo.invalid', 'DEMO-RFID-009'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo09@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 10', 'Personal de Prueba', 'Docente de Electricidad', 'demo10@ejemplo.invalid', 'DEMO-RFID-010'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo10@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 11', 'Personal de Prueba', 'Docente de Mecánica', 'demo11@ejemplo.invalid', 'DEMO-RFID-011'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo11@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 12', 'Personal de Prueba', 'Docente de Matemáticas', 'demo12@ejemplo.invalid', 'DEMO-RFID-012'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo12@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 13', 'Personal de Prueba', 'Coordinación Académica', 'demo13@ejemplo.invalid', 'DEMO-RFID-013'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo13@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 14', 'Personal de Prueba', 'Docente de Electrónica', 'demo14@ejemplo.invalid', 'DEMO-RFID-014'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo14@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 15', 'Personal de Prueba', 'Asistencia Académica', 'demo15@ejemplo.invalid', 'DEMO-RFID-015'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo15@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 16', 'Personal de Prueba', 'Dirección Técnica', 'demo16@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo16@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 17', 'Personal de Prueba', 'Docente de Informática', 'demo17@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo17@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 18', 'Personal de Prueba', 'Docente de Electricidad', 'demo18@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo18@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 19', 'Personal de Prueba', 'Docente de Mecánica', 'demo19@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo19@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 20', 'Personal de Prueba', 'Docente de Matemáticas', 'demo20@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo20@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 21', 'Personal de Prueba', 'Coordinación Académica', 'demo21@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo21@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 22', 'Personal de Prueba', 'Docente de Electrónica', 'demo22@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo22@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 23', 'Personal de Prueba', 'Asistencia Académica', 'demo23@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo23@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 24', 'Personal de Prueba', 'Dirección Técnica', 'demo24@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo24@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 25', 'Personal de Prueba', 'Docente de Informática', 'demo25@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo25@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 26', 'Personal de Prueba', 'Docente de Electricidad', 'demo26@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo26@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 27', 'Personal de Prueba', 'Docente de Mecánica', 'demo27@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo27@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 28', 'Personal de Prueba', 'Docente de Matemáticas', 'demo28@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo28@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 29', 'Personal de Prueba', 'Coordinación Académica', 'demo29@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo29@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 30', 'Personal de Prueba', 'Docente de Electrónica', 'demo30@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo30@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 31', 'Personal de Prueba', 'Asistencia Académica', 'demo31@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo31@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 32', 'Personal de Prueba', 'Dirección Técnica', 'demo32@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo32@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 33', 'Personal de Prueba', 'Docente de Informática', 'demo33@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo33@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 34', 'Personal de Prueba', 'Docente de Electricidad', 'demo34@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo34@ejemplo.invalid');
INSERT INTO empleados(nombres,apellidos,puesto,email,uid_rfid)
SELECT 'Empleado Demo 35', 'Personal de Prueba', 'Docente de Mecánica', 'demo35@ejemplo.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email='demo35@ejemplo.invalid');
INSERT INTO salones(codigo,edificio,nivel,nombre,capacidad) VALUES
('A11','A','1','Laboratorio de aprendizaje A11',30),
('A12','A','1','Laboratorio de aprendizaje A12',35),
('A13','A','1','Laboratorio de aprendizaje A13',40),
('A14','A','1','Laboratorio de aprendizaje A14',25),
('A21','A','2','Laboratorio de aprendizaje A21',30),
('A22','A','2','Laboratorio de aprendizaje A22',35),
('A23','A','2','Laboratorio de aprendizaje A23',40),
('A24','A','2','Laboratorio de aprendizaje A24',25),
('B11','B','1','Laboratorio de aprendizaje B11',30),
('B12','B','1','Laboratorio de aprendizaje B12',35),
('B13','B','1','Laboratorio de aprendizaje B13',40),
('B14','B','1','Laboratorio de aprendizaje B14',25),
('B21','B','2','Laboratorio de aprendizaje B21',30),
('B22','B','2','Laboratorio de aprendizaje B22',35),
('B23','B','2','Laboratorio de aprendizaje B23',40),
('B24','B','2','Laboratorio de aprendizaje B24',25),
('C11','C','1','Laboratorio de aprendizaje C11',30),
('C12','C','1','Laboratorio de aprendizaje C12',35),
('C13','C','1','Laboratorio de aprendizaje C13',40),
('C14','C','1','Laboratorio de aprendizaje C14',25),
('C21','C','2','Laboratorio de aprendizaje C21',30),
('C22','C','2','Laboratorio de aprendizaje C22',35),
('C23','C','2','Laboratorio de aprendizaje C23',40),
('C24','C','2','Laboratorio de aprendizaje C24',25)
ON DUPLICATE KEY UPDATE codigo=VALUES(codigo);
INSERT INTO llaves(codigo,id_salon,descripcion,estado,activo)
SELECT CONCAT('LL-',s.codigo),s.id_salon,CONCAT('Llave del salón ',s.codigo),'DISPONIBLE',1
FROM salones s WHERE s.codigo IN ('A11','A12','A13','A14','A21','A22','A23','A24','B11','B12','B13','B14','B21','B22','B23','B24','C11','C12','C13','C14','C21','C22','C23','C24')
AND NOT EXISTS (SELECT 1 FROM llaves l WHERE l.id_salon=s.id_salon OR l.codigo=CONCAT('LL-',s.codigo));
COMMIT;
UPDATE empleados
SET carnet = CONCAT('CARNE-', SUBSTRING(email, 5, 2))
WHERE email REGEXP '^demo[0-9][0-9]@ejemplo[.]invalid$'
  AND carnet IS NULL;
UPDATE usuarios
SET carnet = CASE username
    WHEN 'admin1' THEN 'USR-ADMIN1'
    WHEN 'jefe1' THEN 'USR-JEFE1'
    WHEN 'secretario1' THEN 'USR-SECRETARIO1'
END
WHERE username IN ('admin1', 'jefe1', 'secretario1') AND carnet IS NULL;
SELECT 'Empleados' ,COUNT(*) FROM empleados UNION ALL
SELECT 'Salones',COUNT(*) FROM salones UNION ALL
SELECT 'Llaves',COUNT(*) FROM llaves;

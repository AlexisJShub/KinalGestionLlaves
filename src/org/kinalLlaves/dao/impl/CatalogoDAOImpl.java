package org.kinalllaves.dao.impl;

import org.kinalllaves.dao.CatalogoDAO;
import java.sql.*;
import java.util.*;
import org.kinalllaves.util.*;
import org.kinalllaves.model.Opcion;


public class CatalogoDAOImpl implements CatalogoDAO {

    public List<Map<String, Object>> listar(String modulo, String filtro) throws SQLException {
        Permisos.exigir(modulo);
        Def d = MODULOS.get(modulo);
        if (d == null) {
            throw new IllegalArgumentException("Módulo no reconocido");
        }
        String sql = "SELECT " + d.pk() + "," + String.join(",", d.columnas()) + " FROM " + d.tabla() + " ORDER BY " + d.pk() + " DESC LIMIT 400";
        List<Map<String, Object>> resultado = new ArrayList<>();
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put(d.pk(), r.getObject(d.pk()));
                for (String f : d.columnas()) {
                    row.put(f, f.equals("password_hash") ? "••••••••" : r.getObject(f));
                }
                if (filtro == null || filtro.isBlank() || row.values().toString().toLowerCase().contains(filtro.toLowerCase())) {
                    resultado.add(row);
                }
            }
        }
        return resultado;
    }

    public void guardar(String modulo, long id, Map<String, String> datos) throws SQLException {
        Permisos.exigir(modulo);
        Def d = MODULOS.get(modulo);
        if (d == null) {
            throw new SQLException("Catálogo inválido");
        }
        if (modulo.equals("USUARIOS") && id > 0 && id == Sesion.id()) {
            
            try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement query = c.prepareStatement("SELECT id_rol FROM usuarios WHERE id_usuario=?")) {
                query.setLong(1, id);
                try (ResultSet r = query.executeQuery()) {
                    if (!r.next() || !Objects.equals(String.valueOf(r.getLong(1)), datos.get("id_rol"))) {
                        throw new SQLException("No puedes cambiar tu propio rol");
                    }
                }
            }
        }
        if (modulo.equals("USUARIOS") && id > 0) {
            try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(
                    "SELECT r.codigo FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol WHERE u.id_usuario=?")) {
                p.setLong(1, id);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next() && "ADMIN".equals(r.getString(1))) {
                        long nuevo = Long.parseLong(datos.getOrDefault("id_rol", "0"));
                        try (PreparedStatement rp = c.prepareStatement("SELECT codigo FROM roles WHERE id_rol=?")) {
                            rp.setLong(1, nuevo);
                            try (ResultSet r2 = rp.executeQuery()) {
                                if (r2.next() && !"ADMIN".equals(r2.getString(1))) {
                                    try (PreparedStatement count = c.prepareStatement("SELECT COUNT(*) FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol WHERE r.codigo='ADMIN' AND u.activo=1")) {
                                        try (ResultSet total = count.executeQuery()) {
                                            total.next();
                                            if (total.getInt(1) <= 1) {
                                                throw new SQLException("Debe quedar al menos un administrador activo");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (modulo.equals("EMPLEADOS") || modulo.equals("USUARIOS")) {
            String carnet = datos.getOrDefault("carnet", "").trim();
            if (carnet.isEmpty()) {
                throw new SQLException("Falta el número de carné.");
            }
            if (!carnet.matches("[A-Za-z0-9-]{3,40}")) {
                throw new SQLException("Carné: usa de 3 a 40 letras, números o guiones.");
            }
        }
        if (modulo.equals("EMPLEADOS")) {
            String cui = datos.getOrDefault("cui", "");
            if (!cui.isEmpty() && !cui.matches("[0-9]{13}")) {
                throw new SQLException("CUI: exactamente 13 dígitos, o vacío");
            }
            if (datos.getOrDefault("nombres", "").isBlank() || datos.getOrDefault("apellidos", "").isBlank()) {
                throw new SQLException("Nombres y apellidos requeridos");
            }
        }
        if (modulo.equals("USUARIOS")) {
            if (datos.getOrDefault("username", "").isBlank() || datos.getOrDefault("nombre_completo", "").isBlank()) {
                throw new SQLException("Usuario y nombre completo son obligatorios");
            }
            if (id == 0 && datos.getOrDefault("password_hash", "").isBlank()) {
                throw new SQLException("Define una contraseña para el usuario nuevo");
            }
        }
        if (modulo.equals("EMPLEADOS")) {
            String correo = datos.getOrDefault("email", "");
            if (!correo.isBlank() && !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new SQLException("Correo electrónico inválido");
            }
        }
        if (modulo.equals("SALONES") && datos.getOrDefault("codigo", "").isBlank()) {
            throw new SQLException("Código de salón obligatorio");
        }
        if (modulo.equals("LLAVES") && datos.getOrDefault("codigo", "").isBlank()) {
            throw new SQLException("Código de llave obligatorio");
        }
        if (modulo.equals("LLAVES") && !"DISPONIBLE".equals(datos.get("estado"))) {
            throw new SQLException("Las llaves nuevas o editadas deben mantenerse DISPONIBLES; usa operaciones para cambiar su estado");
        }
        if (modulo.equals("LLAVES")) {
            if (datos.getOrDefault("id_salon", "").isBlank()) {
                throw new SQLException("Selecciona el salón de la llave");
            }
        }
        if (modulo.equals("USUARIOS")) {
            if (datos.getOrDefault("id_rol", "").isBlank()) {
                throw new SQLException("Selecciona el rol del usuario");
            }
        }
        if (modulo.equals("SALONES")) {
            if (datos.getOrDefault("edificio", "").isBlank() || datos.getOrDefault("nivel", "").isBlank()
                    || datos.getOrDefault("nombre", "").isBlank()) {
                throw new SQLException("Edificio, nivel y nombre son obligatorios");
            }
            String capacidad = datos.getOrDefault("capacidad", "");
            if (!capacidad.isBlank()) {
                try {
                    if (Integer.parseInt(capacidad) < 0) {
                        throw new SQLException("La capacidad no puede ser negativa");
                    }
                } catch (NumberFormatException ex) {
                    throw new SQLException("Capacidad: introduce un número entero válido", ex);
                }
            }
        }
        if (modulo.equals("LLAVES") && id > 0) {
            try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement("SELECT estado FROM llaves WHERE id_llave=?")) {
                p.setLong(1, id);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) {
                        throw new SQLException("La llave ya no existe");
                    }
                    if (!"DISPONIBLE".equals(r.getString(1))) {
                        throw new SQLException("No puedes modificar una llave entregada o fuera de servicio");
                    }
                }
            }
        }
        List<String> cols = new ArrayList<>(Arrays.asList(d.columnas()));
        if (modulo.equals("USUARIOS")) {
            if (id > 0 && datos.getOrDefault("password_hash", "").isBlank()) {
                cols.remove("password_hash");
            } else {
                String password = datos.get("password_hash");
                if (password == null || password.length() < 8) {
                    throw new SQLException("La contraseña debe tener al menos 8 caracteres.");
                }
                datos.put("password_hash", Seguridad.hash(password));
            }
        }
        if (modulo.equals("LLAVES")) {
            String estado = datos.get("estado");
            if (id > 0 && !"DISPONIBLE".equals(estado)) {
                throw new SQLException("El estado de la llave se modifica desde operaciones o incidencias");
            }
        }
        String sql = id == 0 ? "INSERT INTO " + d.tabla() + "(" + String.join(",", cols) + ") VALUES (" + "?,".repeat(cols.size()).replaceAll(",$", "") + ")"
                : "UPDATE " + d.tabla() + " SET " + String.join("=?,", cols) + "=? WHERE " + d.pk() + "=?";
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try {
                long auditId = id;
                try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    int j = 1;
                    for (String col : cols) {
                        String val = datos.get(col);
                        if (val == null || val.isBlank()) {
                            if (Set.of("id_rol", "id_salon", "codigo", "username", "nombre_completo", "nombres", "apellidos", "puesto", "edificio", "nivel", "nombre", "password_hash").contains(col)) {
                                throw new SQLException("El campo " + col + " es obligatorio");
                            }
                            p.setNull(j++, Set.of("capacidad", "id_rol", "id_salon").contains(col) ? Types.INTEGER : Types.VARCHAR);
                            continue;
                        }
                        if (col.equals("activo")) {
                            p.setBoolean(j++, Boolean.parseBoolean(val));
                        } else if (Set.of("capacidad", "id_rol", "id_salon").contains(col)) {
                            p.setLong(j++, Long.parseLong(val));
                        } else {
                            p.setString(j++, val);
                        }
                    }
                    if (id > 0) {
                        p.setLong(j, id);
                    }
                    int afectadas = p.executeUpdate();
                    if (afectadas != 1) {
                        throw new SQLException("No se pudo guardar: el registro ya no existe");
                    }
                    if (id == 0)try (ResultSet r = p.getGeneratedKeys()) {
                        if (r.next()) {
                            auditId = r.getLong(1);
                        }
                    }
                }
                auditar(c, id == 0 ? "CREAR" : "EDITAR", modulo, auditId);
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                if (ex instanceof SQLException e) {
                    throw e;
                }
                throw new SQLException("Datos inválidos: " + ex.getMessage(), ex);
            }
        }
    }

    public void desactivar(String modulo, long id, String motivo) throws SQLException {
        if (motivo == null || motivo.isBlank()) {
            throw new SQLException("Escribe el motivo para desactivar el registro.");
        }
        if (motivo.trim().length() > 500) {
            throw new SQLException("El motivo debe tener máximo 500 caracteres.");
        }
        Permisos.exigir(modulo);
        Def d = MODULOS.get(modulo);
        if (d == null) {
            throw new SQLException("Módulo inválido");
        }
        if (modulo.equals("USUARIOS") && id == Sesion.id()) {
            throw new SQLException("No puedes desactivar tu usuario actual");
        }
        if (id <= 0) {
            throw new SQLException("Selecciona un registro antes de desactivar");
        }
        if (modulo.equals("USUARIOS")) {
            try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(
                    "SELECT r.codigo FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol WHERE u.id_usuario=? AND u.activo=1")) {
                p.setLong(1, id);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next() && "ADMIN".equals(r.getString(1))) {
                        try (PreparedStatement count = c.prepareStatement("SELECT COUNT(*) FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol WHERE u.activo=1 AND r.codigo='ADMIN'")) {
                            try (ResultSet all = count.executeQuery()) {
                                all.next();
                                if (all.getInt(1) <= 1) {
                                    throw new SQLException("No puedes desactivar el último administrador activo");
                                }
                            }
                        }
                    }
                }
            }
        }
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try {
                String sqlRestriccion = switch (modulo) {
                    case "LLAVES" ->
                        "SELECT COUNT(*) FROM entregas WHERE id_llave=? AND estado='ACTIVA'";
                    case "SALONES" ->
                        "SELECT COUNT(*) FROM llaves WHERE id_salon=? AND activo=1";
                    case "EMPLEADOS" ->
                        "SELECT COUNT(*) FROM entregas WHERE id_empleado=? AND estado='ACTIVA'";
                    default ->
                        null;
                };
                if (sqlRestriccion != null) {
                    try (PreparedStatement chk = c.prepareStatement(sqlRestriccion)) {
                        chk.setLong(1, id);
                        try (ResultSet r = chk.executeQuery()) {
                            r.next();
                            if (r.getLong(1) > 0) {
                                throw new SQLException("No puedes desactivar un registro utilizado en una operación activa");
                            }
                        }
                    }
                }
                String reservaSql = switch (modulo) {
                    case "SALONES" ->
                        "SELECT COUNT(*) FROM reservas WHERE id_salon=? AND estado='ACTIVA' AND fin>NOW()";
                    case "EMPLEADOS" ->
                        "SELECT COUNT(*) FROM reservas WHERE id_empleado=? AND estado='ACTIVA' AND fin>NOW()";
                    case "LLAVES" ->
                        "SELECT COUNT(*) FROM reservas r JOIN llaves l ON l.id_salon=r.id_salon WHERE l.id_llave=? AND r.estado='ACTIVA' AND r.fin>NOW()";
                    default ->
                        null;
                };
                if (reservaSql != null) {
                    try (PreparedStatement chk = c.prepareStatement(reservaSql)) {
                        chk.setLong(1, id);
                        try (ResultSet r = chk.executeQuery()) {
                            r.next();
                            if (r.getLong(1) > 0) {
                                throw new SQLException("Hay reservas futuras o vigentes. Cancélalas antes de desactivar");
                            }
                        }
                    }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE " + d.tabla() + " SET activo=0 WHERE " + d.pk() + "=?")) {
                    p.setLong(1, id);
                    if (p.executeUpdate() != 1) {
                        throw new SQLException("Registro inexistente");
                    }
                }
                auditar(c, "DESACTIVAR", modulo, id, motivo.trim());
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public void auditar(String evento, String entidad, long id) throws SQLException {
        try (Connection c = Conexion.getInstancia().conectar()) {
            auditar(c, evento, entidad, id);
        }
    }

    private void auditar(Connection c, String evento, String entidad, long id) throws SQLException {
        auditar(c, evento, entidad, id, null);
    }

    private void auditar(Connection c, String evento, String entidad, long id, String detalle) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle) VALUES(?,?,?,?,?)")) {
            p.setLong(1, Sesion.id());
            p.setString(2, evento);
            p.setString(3, entidad);
            if (id == 0) {
                p.setNull(4, Types.BIGINT);
            } else {
                p.setLong(4, id);
            }
            if (detalle == null || detalle.isBlank()) {
                p.setNull(5, Types.VARCHAR);
            } else {
                p.setString(5, detalle);
            }
            p.executeUpdate();
        }
    }

    public List<Opcion> opciones(String tabla) throws SQLException {
        if (Sesion.actual() == null) {
            throw new SecurityException("Sesión requerida");
        }
        String sql = switch (tabla) {
            case "EMPLEADOS" ->
                "SELECT id_empleado,CONCAT(nombres,' ',apellidos) FROM empleados WHERE activo=1 ORDER BY nombres";
            case "SALONES" ->
                "SELECT id_salon,CONCAT(codigo,' - ',nombre) FROM salones WHERE activo=1 ORDER BY codigo";
            case "LLAVES" ->
                "SELECT l.id_llave,CONCAT(l.codigo,' - ',s.codigo) FROM llaves l JOIN salones s ON s.id_salon=l.id_salon WHERE l.activo=1 AND s.activo=1 AND l.estado='DISPONIBLE' ORDER BY l.codigo";
            case "TODAS_LLAVES" ->
                "SELECT l.id_llave,CONCAT(l.codigo,' - ',s.codigo,' (',l.estado,')') FROM llaves l JOIN salones s ON s.id_salon=l.id_salon WHERE l.activo=1 ORDER BY l.codigo";
            case "ROLES" ->
                "SELECT id_rol,nombre FROM roles ORDER BY id_rol";
            case "ENTREGAS" ->
                "SELECT t.id_entrega,CONCAT('#',t.id_entrega,' - ',l.codigo,' - ',e.nombres,' ',e.apellidos) FROM entregas t JOIN llaves l ON l.id_llave=t.id_llave JOIN empleados e ON e.id_empleado=t.id_empleado WHERE t.estado='ACTIVA' ORDER BY t.id_entrega DESC";
            default ->
                throw new SQLException("Opción inválida");
        };
        List<Opcion> list = new ArrayList<>();
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                list.add(new Opcion(r.getLong(1), r.getString(2)));
            }
        }
        return list;
    }
}

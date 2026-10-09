package org.kinalllaves.dao.impl;

import org.kinalllaves.dao.OperacionDAO;
import java.sql.*;
import java.util.*;
import org.kinalllaves.util.*;

public class OperacionDAOImpl implements OperacionDAO {

    public long entregar(long llave, long empleado, String documento, Timestamp prevista) throws SQLException {
        Permisos.exigir("ENTREGAS");
        try (Connection c = Conexion.getInstancia().conectar(); CallableStatement p = c.prepareCall("{CALL sp_entregar_llave(?,?,?,?,?,?)}")) {
            p.setLong(1, llave);
            p.setLong(2, empleado);
            p.setLong(3, Sesion.id());
            p.setNull(4, Types.BIGINT);
            p.setString(5, documento);
            if (prevista == null) {
                p.setNull(6, Types.TIMESTAMP);
            } else {
                p.setTimestamp(6, prevista);
            }
            return resultadoId(p, "id_entrega");
        }
    }

    public long devolver(long entrega, long devuelve, boolean documentoRetirado, String observacion) throws SQLException {
        Permisos.exigir("DEVOLUCIONES");
        try (Connection c = Conexion.getInstancia().conectar(); CallableStatement p = c.prepareCall("{CALL sp_devolver_llave(?,?,?,?,?)}")) {
            p.setLong(1, entrega);
            p.setLong(2, devuelve);
            p.setLong(3, Sesion.id());
            p.setBoolean(4, documentoRetirado);
            p.setString(5, observacion);
            return resultadoId(p, "id_devolucion");
        }
    }

    private long resultadoId(CallableStatement p, String campo) throws SQLException {
        boolean hayResultados = p.execute();
        while (true) {
            if (hayResultados) {
                try (ResultSet r = p.getResultSet()) {
                    if (r != null && r.next()) {
                        return r.getLong(campo);
                    }
                }
            }
            if (p.getUpdateCount() == -1 && !hayResultados) {
                break;
            }
            hayResultados = p.getMoreResults();
        }
        throw new SQLException("La operación no devolvió su número de comprobante.");
    }

    public List<Map<String, Object>> prestamosActivosDe(long empleado) throws SQLException {
        Permisos.exigir("DEVOLUCIONES");
        String sql = "SELECT t.id_entrega,l.codigo llave,s.codigo salon,CONCAT(e.nombres,' ',e.apellidos) empleado,"
                + "COALESCE(e.carnet,e.uid_rfid) carnet,t.fecha_entrega,"
                + "TIMESTAMPDIFF(MINUTE,t.fecha_entrega,NOW()) minutos_uso FROM entregas t "
                + "JOIN llaves l ON l.id_llave=t.id_llave JOIN salones s ON s.id_salon=l.id_salon "
                + "JOIN empleados e ON e.id_empleado=t.id_empleado "
                + "WHERE t.estado='ACTIVA' AND t.id_empleado=? ORDER BY t.fecha_entrega DESC";
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, empleado);
            try (ResultSet r = p.executeQuery()) {
                return leerFilas(r);
            }
        }
    }

    public Map<String, Object> comprobante(String tipo, long id) throws SQLException {
        String sql = switch (tipo) {
            case "ENTREGA" ->
                "SELECT * FROM vw_historial_entregas WHERE id_entrega=?";
            case "DEVOLUCION" ->
                "SELECT * FROM vw_historial_devoluciones WHERE id_devolucion=?";
            default ->
                throw new SQLException("Tipo de comprobante desconocido");
        };
        Permisos.exigir(tipo.equals("ENTREGA") ? "ENTREGAS" : "DEVOLUCIONES");
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> filas = leerFilas(r);
                if (filas.isEmpty()) {
                    throw new SQLException("No se encontró el movimiento registrado");
                }
                return filas.get(0);
            }
        }
    }

    private List<Map<String, Object>> leerFilas(ResultSet r) throws SQLException {
        List<Map<String, Object>> filas = new ArrayList<>();
        ResultSetMetaData md = r.getMetaData();
        while (r.next()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                fila.put(md.getColumnLabel(i), r.getObject(i));
            }
            filas.add(fila);
        }
        return filas;
    }

    public List<Map<String, Object>> tabla(String vista) throws SQLException {
        String permiso = switch (vista) {
            case "AUDITORIA" ->
                "AUDITORIA";
            case "ESTADOS" ->
                "ESTADOS";
            case "ENTREGAS" ->
                "ENTREGAS";
            case "DEVOLUCIONES" ->
                "DEVOLUCIONES";
            default ->
                throw new SQLException("Vista inválida");
        };
        Permisos.exigir(permiso);
        String sql = switch (vista) {
            case "ESTADOS" ->
                "SELECT * FROM vw_estado_llaves ORDER BY salon";
            case "ENTREGAS" ->
                "SELECT * FROM vw_historial_entregas ORDER BY id_entrega DESC LIMIT 350";
            case "DEVOLUCIONES" ->
                "SELECT * FROM vw_historial_devoluciones ORDER BY id_devolucion DESC LIMIT 350";
            case "AUDITORIA" ->
                "SELECT a.id_auditoria,u.username,a.evento,a.entidad,a.id_entidad,a.detalle,a.fecha_evento FROM auditoria a JOIN usuarios u ON a.id_usuario=u.id_usuario ORDER BY a.id_auditoria DESC LIMIT 400";
            default ->
                throw new SQLException("Vista inválida");
        };
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            ResultSetMetaData md = r.getMetaData();
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    row.put(md.getColumnLabel(i), r.getObject(i));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public Map<String, Integer> indicadores() throws SQLException {
        if (Sesion.actual() == null) {
            throw new SecurityException("Inicia sesión");
        }
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM vw_indicadores"); ResultSet r = p.executeQuery()) {
            Map<String, Integer> ret = new LinkedHashMap<>();
            if (r.next()) {
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
                    ret.put(r.getMetaData().getColumnLabel(i), r.getInt(i));
                }
            }
            return ret;
        }
    }
}

package org.kinalllaves.dao.impl;

import org.kinalllaves.dao.ReporteDAO;
import java.sql.*;
import java.util.*;
import java.time.*;
import org.kinalllaves.util.*;

public class ReporteDAOImpl implements ReporteDAO {

    public List<Map<String, Object>> consultar(String tipo, LocalDate desde, LocalDate hasta, String filtro) throws SQLException {
        Permisos.exigir("REPORTES");
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new SQLException("Rango de fechas inválido");
        }
        String sql = switch (tipo) {
            case "Disponibilidad" ->
                "SELECT llave,salon,nombre_salon,estado_operativo,responsable FROM vw_estado_llaves";
            case "Entregas" ->
                "SELECT * FROM vw_historial_entregas WHERE fecha_entrega>=? AND fecha_entrega<?";
            case "Devoluciones" ->
                "SELECT * FROM vw_historial_devoluciones WHERE fecha_devolucion>=? AND fecha_devolucion<?";
            case "Uso por fecha" ->
                "SELECT * FROM vw_uso_salones WHERE fecha>=? AND fecha<=?";
            default ->
                throw new SQLException("Reporte desconocido");
        };
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            if (!tipo.equals("Disponibilidad")) {
                p.setDate(1, java.sql.Date.valueOf(desde));
                p.setDate(2, java.sql.Date.valueOf(tipo.equals("Uso por fecha") ? hasta : hasta.plusDays(1)));
            }
            try (ResultSet r = p.executeQuery()) {
                ResultSetMetaData md = r.getMetaData();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        row.put(md.getColumnLabel(i), r.getObject(i));
                    }
                    if (filtro == null || filtro.isBlank() || row.values().toString().toLowerCase().contains(filtro.toLowerCase())) {
                        rows.add(row);
                    }
                }
            }
        }
        return rows;
    }

    public void guardarComentario(String tipo, LocalDate desde, LocalDate hasta, String comentario) throws SQLException {
        Permisos.exigir("REPORTES");
        if (tipo == null || tipo.isBlank()) {
            throw new SQLException("Selecciona el tipo de reporte.");
        }
        if (comentario == null || comentario.isBlank()) {
            throw new SQLException("Escribe el comentario antes de guardarlo.");
        }
        if (comentario.length() > 500) {
            throw new SQLException("El comentario no puede superar 500 caracteres.");
        }
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new SQLException("Revisa las fechas del reporte.");
        }
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO comentarios_reporte(id_usuario,tipo_reporte,fecha_desde,fecha_hasta,comentario) VALUES(?,?,?,?,?)"); PreparedStatement a = c.prepareStatement("INSERT INTO auditoria(id_usuario,evento,entidad,detalle) VALUES(?,'COMENTARIO','REPORTES',?)")) {
                p.setLong(1, Sesion.id());
                p.setString(2, tipo);
                p.setDate(3, java.sql.Date.valueOf(desde));
                p.setDate(4, java.sql.Date.valueOf(hasta));
                p.setString(5, comentario.trim());
                p.executeUpdate();
                a.setLong(1, Sesion.id());
                a.setString(2, "Reporte: " + tipo + " - " + comentario.trim());
                a.executeUpdate();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public List<String> comentarios(String tipo) throws SQLException {
        Permisos.exigir("REPORTES");
        List<String> resultado = new ArrayList<>();
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(
                "SELECT cr.comentario,cr.fecha_registro,u.username,cr.fecha_desde,cr.fecha_hasta FROM comentarios_reporte cr "
                + "JOIN usuarios u ON u.id_usuario=cr.id_usuario WHERE cr.tipo_reporte=? ORDER BY cr.id_comentario DESC LIMIT 30")) {
            p.setString(1, tipo);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    resultado.add(r.getTimestamp(2) + "  |  " + r.getString(3) + "  |  " + r.getDate(4) + " a " + r.getDate(5) + ": " + r.getString(1));
                }
            }
        }
        return resultado;
    }

}

package org.kinalllaves.dao.impl;

import org.kinalllaves.dao.IncidenciaDAO;
import java.sql.*;
import java.util.*;
import org.kinalllaves.util.*;

public class IncidenciaDAOImpl implements IncidenciaDAO {

    public void registrar(String tipo, String descripcion, Long entrega, Long llave) throws SQLException {
        Permisos.exigir("INCIDENCIAS");
        if (descripcion == null || descripcion.isBlank()) {
            throw new SQLException("Describe la incidencia");
        }
        if (!Set.of("RETRASO", "EXTRAVIO", "DANO", "DOCUMENTO_OLVIDADO", "OTRA").contains(tipo)) {
            throw new SQLException("Tipo de incidencia no válido");
        }
        if (entrega == null && llave == null) {
            throw new SQLException("Selecciona una entrega o llave para relacionar la incidencia");
        }
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try {
                if (entrega != null) {
                    try (PreparedStatement lookup = c.prepareStatement("SELECT id_llave FROM entregas WHERE id_entrega=?")) {
                        lookup.setLong(1, entrega);
                        try (ResultSet r = lookup.executeQuery()) {
                            if (!r.next()) {
                                throw new SQLException("Entrega no existente");
                            }
                            long vinculada = r.getLong(1);
                            if (llave != null && llave.longValue() != vinculada) {
                                throw new SQLException("La llave no corresponde a la entrega");
                            }
                            llave = vinculada;
                        }
                    }
                }
                long id;
                try (PreparedStatement p = c.prepareStatement("INSERT INTO incidencias(tipo,descripcion,id_entrega,id_llave,id_usuario_registra) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, tipo);
                    p.setString(2, descripcion);
                    if (entrega == null) {
                        p.setNull(3, Types.BIGINT);
                    } else {
                        p.setLong(3, entrega);
                    }
                    if (llave == null) {
                        p.setNull(4, Types.BIGINT);
                    } else {
                        p.setLong(4, llave);
                    }
                    p.setLong(5, Sesion.id());
                    p.executeUpdate();
                    try (ResultSet r = p.getGeneratedKeys()) {
                        r.next();
                        id = r.getLong(1);
                    }
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad) VALUES(?,'INCIDENCIA','incidencias',?)")) {
                    p.setLong(1, Sesion.id());
                    p.setLong(2, id);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public void estado(long id, String estado) throws SQLException {
        Permisos.exigir("GESTION_INCIDENCIAS");
        if (!Set.of("ABIERTA", "EN_REVISION", "CERRADA").contains(estado)) {
            throw new SQLException("Estado no válido");
        }
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("UPDATE incidencias SET estado=?,fecha_cierre=IF(?='CERRADA',NOW(),NULL),id_usuario_cierra=IF(?='CERRADA',?,NULL) WHERE id_incidencia=?")) {
                    p.setString(1, estado);
                    p.setString(2, estado);
                    p.setString(3, estado);
                    p.setLong(4, Sesion.id());
                    p.setLong(5, id);
                    if (p.executeUpdate() != 1) {
                        throw new SQLException("Incidencia inexistente");
                    }
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad,detalle) VALUES(?,'CAMBIO_ESTADO','incidencias',?,?)")) {
                    p.setLong(1, Sesion.id());
                    p.setLong(2, id);
                    p.setString(3, estado);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<Map<String, Object>> listar() throws SQLException {
        Permisos.exigir("INCIDENCIAS");
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement("SELECT id_incidencia,tipo,LEFT(descripcion,110) descripcion,estado,fecha_registro,id_entrega,id_llave FROM incidencias ORDER BY id_incidencia DESC LIMIT 350"); ResultSet r = p.executeQuery()) {
            ResultSetMetaData md = r.getMetaData();
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    row.put(md.getColumnLabel(i), r.getObject(i));
                }
                lista.add(row);
            }
        }
        return lista;
    }
}

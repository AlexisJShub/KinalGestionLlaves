package org.kinalllaves.dao.impl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.kinalllaves.dao.DevolucionDAO;
import org.kinalllaves.model.Devolucion;
import org.kinalllaves.util.Conexion;
import org.kinalllaves.util.Sesion;

public final class DevolucionDAOImpl implements DevolucionDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO devoluciones(id_entrega,id_empleado_devuelve,id_usuario_recibe,fecha_devolucion) VALUES(?,?,?,?)";
    private static final String SQL_LIBERAR_LLAVE =
            "UPDATE llaves l JOIN entregas t ON l.id_llave=t.id_llave SET l.estado=CASE "
          + "WHEN l.estado IN ('DANADA','EXTRAVIADA','FUERA_SERVICIO') THEN l.estado ELSE 'DISPONIBLE' END "
          + "WHERE t.id_entrega=?";
    private static final String SQL_SELECT =
            "SELECT id_devolucion,id_entrega,id_empleado_devuelve,fecha_devolucion FROM devoluciones ";

    @Override
    public boolean registrarDevolucion(Devolucion devolucion) throws SQLException {
        if (devolucion == null) {
            throw new SQLException("La devolución no puede ser nula");
        }
        if (devolucion.entregaId() <= 0) {
            throw new SQLException("La devolución debe relacionarse con una entrega");
        }
        if (devolucion.quienDevuelve() <= 0) {
            throw new SQLException("Indica quién devuelve la llave");
        }
        if (devolucion.fecha() == null) {
            throw new SQLException("La fecha de devolución no puede ser nula");
        }
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ins = c.prepareStatement(SQL_INSERTAR)) {
                    ins.setLong(1, devolucion.entregaId());
                    ins.setLong(2, devolucion.quienDevuelve());
                    ins.setLong(3, Sesion.id());
                    ins.setTimestamp(4, Timestamp.valueOf(devolucion.fecha()));
                    ins.executeUpdate();
                }
                try (PreparedStatement lib = c.prepareStatement(SQL_LIBERAR_LLAVE)) {
                    lib.setLong(1, devolucion.entregaId());
                    lib.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
        return true;
    }

    @Override
    public Devolucion obtenerPorIdEntrega(long idEntrega) throws SQLException {
        try (Connection c = Conexion.getInstancia().conectar();
             PreparedStatement p = c.prepareStatement(SQL_SELECT + "WHERE id_entrega=?")) {
            p.setLong(1, idEntrega);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? mapear(r) : null;
            }
        }
    }

    @Override
    public List<Devolucion> listarDevoluciones() throws SQLException {
        List<Devolucion> lista = new ArrayList<>();
        try (Connection c = Conexion.getInstancia().conectar();
             PreparedStatement p = c.prepareStatement(SQL_SELECT + "ORDER BY fecha_devolucion DESC LIMIT 350");
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                lista.add(mapear(r));
            }
        }
        return lista;
    }

    private Devolucion mapear(ResultSet r) throws SQLException {
        Timestamp fecha = r.getTimestamp("fecha_devolucion");
        return new Devolucion(
                r.getLong("id_devolucion"),
                r.getLong("id_entrega"),
                r.getLong("id_empleado_devuelve"),
                fecha == null ? null : fecha.toLocalDateTime());
    }
}
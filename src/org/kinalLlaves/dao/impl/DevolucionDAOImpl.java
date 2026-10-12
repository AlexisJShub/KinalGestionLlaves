package org.kinalllaves.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.kinalllaves.dao.DevolucionDAO;
import org.kinalllaves.model.Devolucion;
import org.kinalllaves.util.Conexion;

public class DevolucionDAOImpl implements DevolucionDAO {

    @Override
    public boolean registrarDevolucion(Devolucion devolucion) {
        boolean exito = false;
        String sqlDev = "INSERT INTO Devolucion (id_entrega, fecha_hora_devolucion, id_empleado_devuelve, id_secretario, observaciones) VALUES (?, ?, ?, ?, ?)";
        String sqlActualizarLlave = "UPDATE Llave l JOIN Entrega e ON l.id_llave = e.id_llave SET l.estado = 'DISPONIBLE' WHERE e.id_entrega = ?";

        Connection con = null;
        PreparedStatement psDev = null;
        PreparedStatement psLlave = null;

        try {
            con = Conexion.getInstancia().getConexion();
            con.setAutoCommit(false);

            // 1. Insertar el registro de la devolución
            psDev = con.prepareStatement(sqlDev);
            psDev.setInt(1, devolucion.getIdEntrega());
            psDev.setTimestamp(2, java.sql.Timestamp.valueOf(devolucion.getFechaHoraDevolucion()));
            psDev.setInt(3, devolucion.getIdEmpleadoDevuelve());
            psDev.setInt(4, devolucion.getIdSecretario());
            psDev.setString(5, devolucion.getObservaciones());

            int filasDev = psDev.executeUpdate();

            // 2. Actualizar el estado de la llave a disponible
            psLlave = con.prepareStatement(sqlActualizarLlave);
            psLlave.setInt(1, devolucion.getIdEntrega());
            int filasLlave = psLlave.executeUpdate();

            if (filasDev > 0) {
                con.commit();
                exito = true;
            } else {
                con.rollback();
            }

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en Rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error al registrar devolución: " + e.getMessage());
        } finally {
            try {
                if (psDev != null) {
                    psDev.close();
                }
                if (psLlave != null) {
                    psLlave.close();
                }
                if (con != null) {
                    con.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar recursos: " + e.getMessage());
            }
        }

        return exito;
    }

    @Override
    public Devolucion obtenerPorIdEntrega(int idEntrega) {
        String sql = "SELECT * FROM Devolucion WHERE id_entrega = ?";
        Devolucion devolucion = null;

        try (Connection con = Conexion.getInstancia().getConexion(); 
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idEntrega);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    devolucion = new Devolucion();
                    devolucion.setIdDevolucion(rs.getInt("id_devolucion"));
                    devolucion.setIdEntrega(rs.getInt("id_entrega"));
                    devolucion.setFechaHoraDevolucion(rs.getTimestamp("fecha_hora_devolucion").toLocalDateTime());
                    devolucion.setIdEmpleadoDevuelve(rs.getInt("id_empleado_devuelve"));
                    devolucion.setIdSecretario(rs.getInt("id_secretario"));
                    devolucion.setObservaciones(rs.getString("observaciones"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener devolución: " + e.getMessage());
        }

        return devolucion;
    }

    @Override
    public List<Devolucion> listarDevoluciones() {
        String sql = "SELECT * FROM Devolucion ORDER BY fecha_hora_devolucion DESC";
        List<Devolucion> lista = new ArrayList<>();

        try (Connection con = Conexion.getInstancia().getConexion(); 
             PreparedStatement ps = con.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Devolucion dev = new Devolucion();
                dev.setIdDevolucion(rs.getInt("id_devolucion"));
                dev.setIdEntrega(rs.getInt("id_entrega"));
                dev.setFechaHoraDevolucion(rs.getTimestamp("fecha_hora_devolucion").toLocalDateTime());
                dev.setIdEmpleadoDevuelve(rs.getInt("id_empleado_devuelve"));
                dev.setIdSecretario(rs.getInt("id_secretario"));
                dev.setObservaciones(rs.getString("observaciones"));
                lista.add(dev);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar devoluciones: " + e.getMessage());
        }

        return lista;
    }
}
package org.kinalllaves.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.kinalllaves.dao.EmpleadoDAO;
import org.kinalllaves.model.EmpleadoIdentificado;
import org.kinalllaves.util.Conexion;
import org.kinalllaves.util.Permisos;

public class EmpleadoDAOImpl implements EmpleadoDAO {

    @Override
    public Long buscarPorRfid(String uid) throws SQLException {
        EmpleadoIdentificado empleado = identificarPorCarnet(uid);
        return empleado == null ? null : empleado.id();
    }

    @Override
    public Long buscarPorCarnet(String carnet) throws SQLException {
        EmpleadoIdentificado empleado = identificarPorCarnet(carnet);
        return empleado == null ? null : empleado.id();
    }

    @Override
    public EmpleadoIdentificado identificarPorCarnet(String codigo) throws SQLException {
        Permisos.exigir("ENTREGAS");
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        String sql = "SELECT id_empleado, nombres, apellidos, carnet, cui, puesto "
                + "FROM empleados WHERE activo = 1 AND (carnet = ? OR uid_rfid = ?)";
        try (Connection conexion = Conexion.getInstancia().conectar();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, codigo.trim());
            consulta.setString(2, codigo.trim());
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                EmpleadoIdentificado encontrado = new EmpleadoIdentificado(
                        resultado.getLong("id_empleado"),
                        resultado.getString("nombres"),
                        resultado.getString("apellidos"),
                        resultado.getString("carnet"),
                        resultado.getString("cui"),
                        resultado.getString("puesto"));
                if (resultado.next()) {
                    throw new SQLException("Este código corresponde a más de una persona. Revisa los carnés registrados.");
                }
                return encontrado;
            }
        }
    }
}

package org.kinalllaves.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.kinalllaves.dao.RolDAO;
import org.kinalllaves.util.Conexion;

public final class RolDAOImpl implements RolDAO {
    @Override
    public Set<String> obtenerPermisos(long idUsuario) throws SQLException {
        String sql = "SELECT p.codigo FROM usuarios u "
                + "JOIN roles r ON r.id_rol = u.id_rol "
                + "JOIN rol_permiso rp ON rp.id_rol = r.id_rol "
                + "JOIN permisos p ON p.id_permiso = rp.id_permiso "
                + "WHERE u.id_usuario = ? AND u.activo = 1";
        Set<String> permisos = new HashSet<>();
        try (Connection conexion = Conexion.getInstancia().conectar();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, idUsuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    permisos.add(resultado.getString("codigo"));
                }
            }
        }
        return Collections.unmodifiableSet(permisos);
    }
}
package org.kinalllaves.dao.impl;

import org.kinalllaves.dao.UsuarioDAO;
import java.sql.*;
import org.kinalllaves.model.Usuario;
import org.kinalllaves.util.*;

public class UsuarioDAOImpl implements UsuarioDAO {

    public boolean hayUsuarios() throws SQLException {
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM usuarios"); ResultSet r = p.executeQuery()) {
            r.next();
            return r.getInt(1) > 0;
        }
    }

    public Usuario autenticar(String usuario, String clave) throws SQLException {
        String sql = "SELECT u.id_usuario,u.username,u.nombre_completo,u.password_hash,u.activo,r.codigo FROM usuarios u JOIN roles r ON u.id_rol=r.id_rol WHERE u.username=?";
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, usuario);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    return null;
                }
                String almacenado = r.getString(4);
                boolean valida = Seguridad.verificar(clave, almacenado);
                return valida && r.getBoolean(5) ? new Usuario(r.getLong(1), r.getString(2), r.getString(3), r.getString(6), true) : null;
            }
        }
    }

    public void primerAdmin(String username, String nombre, String password) throws SQLException {
        String hash = Seguridad.hash(password);
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement lock = c.prepareStatement("SELECT id_rol FROM roles WHERE codigo='ADMIN' FOR UPDATE"); ResultSet r = lock.executeQuery()) {
                if (!r.next()) {
                    throw new SQLException("Ejecuta primero los scripts 01 y 02");
                }
                long role = r.getLong(1);
                try (PreparedStatement check = c.prepareStatement("SELECT COUNT(*) FROM usuarios"); ResultSet exists = check.executeQuery()) {
                    exists.next();
                    if (exists.getLong(1) != 0) {
                        throw new SQLException("El administrador inicial ya existe");
                    }
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO usuarios(username,nombre_completo,password_hash,id_rol) VALUES(?,?,?,?)")) {
                    p.setString(1, username);
                    p.setString(2, nombre);
                    p.setString(3, hash);
                    p.setLong(4, role);
                    p.executeUpdate();
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                if (ex instanceof SQLException e) {
                    throw e;
                }
                throw new SQLException("Error creando administrador", ex);
            }
        }
    }

    public void cambiarClave(long id, String actual, String nueva) throws SQLException {
        if (Sesion.actual() == null || Sesion.id() != id) {
            throw new SecurityException("Cuenta no autorizada");
        }
        String nuevoHash = Seguridad.hash(nueva);
        try (Connection c = Conexion.getInstancia().conectar()) {
            c.setAutoCommit(false);
            try {
                String guardado;
                try (PreparedStatement p = c.prepareStatement("SELECT password_hash FROM usuarios WHERE id_usuario=? AND activo=1 FOR UPDATE")) {
                    p.setLong(1, id);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) {
                            throw new SQLException("La cuenta no está activa");
                        }
                        guardado = r.getString(1);
                    }
                }
                if (!Seguridad.verificar(actual, guardado)) {
                    throw new SQLException("Contraseña actual incorrecta");
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE usuarios SET password_hash=? WHERE id_usuario=?")) {
                    p.setString(1, nuevoHash);
                    p.setLong(2, id);
                    if (p.executeUpdate() != 1) {
                        throw new SQLException("No se actualizó la contraseña");
                    }
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO auditoria(id_usuario,evento,entidad,id_entidad) VALUES(?,'CAMBIAR_CLAVE','usuarios',?)")) {
                    p.setLong(1, id);
                    p.setLong(2, id);
                    p.executeUpdate();
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                if (ex instanceof SQLException sql) {
                    throw sql;
                }
                throw new SQLException("No se pudo cambiar la contraseña", ex);
            }
        }
    }
}

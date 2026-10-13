package org.kinalllaves.util;

import java.sql.SQLException;
import java.util.Set;
import org.kinalllaves.dao.RolDAO;
import org.kinalllaves.dao.impl.RolDAOImpl;
import org.kinalllaves.model.Usuario;

public final class Sesion {
    private static Usuario actual;
    private static Set<String> permisos = Set.of();
    private Sesion() {}

    public static Usuario actual() { return actual; }

    public static void iniciar(Usuario usuario) throws SQLException {
        if (usuario == null) throw new IllegalArgumentException("Usuario no válido");
        RolDAO dao = new RolDAOImpl();
        Set<String> autorizados = dao.obtenerPermisos(usuario.id());
        if (autorizados.isEmpty()) {
            throw new SQLException("Este usuario no tiene permisos. Revisa los roles del script DML.");
        }
        permisos = autorizados;
        actual = usuario;
    }

    public static Set<String> permisos() { return permisos; }

    public static void cerrar() {
        actual = null;
        permisos = Set.of();
    }

    public static long id() {
        if (actual == null) throw new SecurityException("Inicia sesión nuevamente.");
        return actual.id();
    }
}

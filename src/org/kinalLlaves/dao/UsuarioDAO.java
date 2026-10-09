package org.kinalllaves.dao;

public interface UsuarioDAO {

    boolean hayUsuarios() throws java.sql.SQLException;

    org.kinalllaves.model.Usuario autenticar(String usuario, String clave) throws java.sql.SQLException;

    void primerAdmin(String username, String nombre, String password) throws java.sql.SQLException;

    void cambiarClave(long id, String actual, String nueva) throws java.sql.SQLException;
}

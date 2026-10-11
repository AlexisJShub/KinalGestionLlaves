package org.kinalllaves.dao;

import java.sql.SQLException;
import java.util.Set;

public interface RolDAO {
    Set<String> obtenerPermisos(long idUsuario) throws SQLException;
}


package org.kinalllaves.dao;

import java.sql.SQLException;

public interface LlaveEstadoDAO {

    void cambiarEstado(long idLlave, String estado) throws SQLException;
}

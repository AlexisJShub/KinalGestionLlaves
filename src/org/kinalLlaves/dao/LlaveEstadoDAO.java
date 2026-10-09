package org.kinalllaves.dao;

import java.sql.SQLException;

public interface LlaveEstadoDAO {

    /**
     * Transición excepcional supervisada, sin entrega activa y auditada.
     */
    void cambiarEstado(long idLlave, String estado) throws SQLException;
}
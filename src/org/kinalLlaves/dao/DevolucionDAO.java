package org.kinalllaves.dao;

import java.sql.SQLException;
import java.util.List;
import org.kinalllaves.model.Devolucion;

public interface DevolucionDAO {

    boolean registrarDevolucion(Devolucion devolucion) throws SQLException;

    Devolucion obtenerPorIdEntrega(long idEntrega) throws SQLException;

    List<Devolucion> listarDevoluciones() throws SQLException;
}
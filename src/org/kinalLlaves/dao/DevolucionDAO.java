package org.kinalllaves.dao;

import java.util.List;
import org.kinalllaves.model.Devolucion;

public interface DevolucionDAO {

    boolean registrarDevolucion(Devolucion devolucion);

    Devolucion obtenerPorIdEntrega(int idEntrega);

    List<Devolucion> listarDevoluciones();
}

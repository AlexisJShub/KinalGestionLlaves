package org.kinalLlaves.dao;

public interface ReservaDAO {

    void reservar(long salon, long empleado, java.time.LocalDateTime inicio, java.time.LocalDateTime fin, int semanas, String obs) throws java.sql.SQLException;

    void cambiarEstado(long id, String estado) throws java.sql.SQLException;

    java.util.List<java.util.Map<String, Object>> listar() throws java.sql.SQLException;
}
 
package org.kinalllaves.dao;

public interface IncidenciaDAO {

    void registrar(String tipo, String descripcion, Long entrega, Long llave) throws java.sql.SQLException;

    void estado(long id, String estado) throws java.sql.SQLException;

    java.util.List<java.util.Map<String, Object>> listar() throws java.sql.SQLException;
}

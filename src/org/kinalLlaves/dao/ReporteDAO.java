package org.kinalllaves.dao;

public interface ReporteDAO {

    void guardarComentario(String tipo, java.time.LocalDate desde, java.time.LocalDate hasta, String comentario) throws java.sql.SQLException;

    java.util.List<String> comentarios(String tipo) throws java.sql.SQLException;

    java.util.List<java.util.Map<String, Object>> consultar(String tipo, java.time.LocalDate desde, java.time.LocalDate hasta, String filtro) throws java.sql.SQLException;
}

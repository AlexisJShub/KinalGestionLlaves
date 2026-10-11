package org.kinalllaves.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReporteDAO {

    List<Map<String, Object>> consultar(String tipo, LocalDate desde, LocalDate hasta, String filtro) throws SQLException;

    void guardarComentario(String tipo, LocalDate desde, LocalDate hasta, String comentario) throws SQLException;

    List<String> comentarios(String tipo) throws SQLException;
}
package org.kinalllaves.dao;

public interface OperacionDAO {

    long entregar(long llave, long empleado, String documento, java.sql.Timestamp prevista) throws java.sql.SQLException;

    long devolver(long entrega, long devuelve, boolean doc, String obs) throws java.sql.SQLException;

    java.util.List<java.util.Map<String, Object>> tabla(String vista) throws java.sql.SQLException;

    java.util.List<java.util.Map<String, Object>> prestamosActivosDe(long empleado) throws java.sql.SQLException;

    java.util.Map<String, Object> comprobante(String tipo, long id) throws java.sql.SQLException;

    java.util.Map<String, Integer> indicadores() throws java.sql.SQLException;
}

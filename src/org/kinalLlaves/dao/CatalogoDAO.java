package org.kinalllaves.dao;

import java.util.*;

public interface CatalogoDAO {

    public record Def(String tabla, String pk, String[] columnas, String titulo) {

    }
    public static final Map<String, Def> MODULOS = Map.of(
            "USUARIOS", new Def("usuarios", "id_usuario", new String[]{"username", "nombre_completo", "carnet", "password_hash", "id_rol", "activo"}, "Usuarios"),
            "EMPLEADOS", new Def("empleados", "id_empleado", new String[]{"cui", "carnet", "nombres", "apellidos", "puesto", "email", "uid_rfid", "activo"}, "Empleados"),
            "SALONES", new Def("salones", "id_salon", new String[]{"codigo", "edificio", "nivel", "nombre", "capacidad", "activo"}, "Salones"),
            "LLAVES", new Def("llaves", "id_llave", new String[]{"codigo", "id_salon", "descripcion", "estado", "activo"}, "Llaves"));

    java.util.List<java.util.Map<String, Object>> listar(String modulo, String filtro) throws java.sql.SQLException;

    void guardar(String modulo, long id, java.util.Map<String, String> datos) throws java.sql.SQLException;

    void desactivar(String modulo, long id, String motivo) throws java.sql.SQLException;

    default void desactivar(String modulo, long id) throws java.sql.SQLException {
        desactivar(modulo, id, "Desactivación administrativa");
    }

    void auditar(String evento, String entidad, long id) throws java.sql.SQLException;

    java.util.List<org.kinalllaves.model.Opcion> opciones(String tabla) throws java.sql.SQLException;
}

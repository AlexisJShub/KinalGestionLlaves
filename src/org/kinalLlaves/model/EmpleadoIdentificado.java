package org.kinalllaves.model;

/**
 * Datos necesarios para identificar al empleado mediante su carné.
 */
public record EmpleadoIdentificado(long id, String nombres, String apellidos,
        String carnet, String cui, String puesto) {

    public String nombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }
}

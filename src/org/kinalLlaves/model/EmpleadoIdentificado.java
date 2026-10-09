package org.kinalLlaves.model;

public record EmpleadoIdentificado(long id, String nombres, String apellidos,
        String carnet, String cui, String puesto) {

    public String nombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }
}

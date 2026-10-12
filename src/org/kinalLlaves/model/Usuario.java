package org.kinalllaves.model;

public record Usuario(long id, String username, String nombre, String rol, boolean activo) {

    @Override
    public String toString() {
        return nombre + " (" + rol + ")";
    }
}

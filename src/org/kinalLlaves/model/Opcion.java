package org.kinalllaves.model;

public record Opcion(long id, String nombre) {

    @Override
    public String toString() {
        return nombre;
    }
}

package org.kinalllaves.model;

public record Empleado(long id,
        String cui,
        String nombres,
        String apellidos,
        String puesto,
        String email,
        String rfid,
        boolean activo) {}



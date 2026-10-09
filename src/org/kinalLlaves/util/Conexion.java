package org.kinalllaves.util;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;


public final class Conexion {

    private static Conexion instancia;
    private final String url, user, password;

    private Conexion() {
        Properties p = new Properties();
        Path archivo = Paths.get("db.properties");
        try (InputStream in = Files.exists(archivo) ? Files.newInputStream(archivo)
                : Conexion.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException("Copia db.properties.example como db.properties y configura MySQL.");
            }
            p.load(in);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar db.properties", ex);
        }
        url = requerir(p, "db.url");
        user = requerir(p, "db.user");
        password = requerir(p, "db.password");
    
}

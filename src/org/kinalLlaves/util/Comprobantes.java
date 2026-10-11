package org.kinalllaves.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class Comprobantes {

    private Comprobantes() {
    }

    public static Path generar(String tipo, long id, Map<String, Object> movimiento) throws IOException {
        Path carpeta = Path.of(System.getProperty("user.home"), "KinalLlaves", "comprobantes");
        Files.createDirectories(carpeta);
        Path destino = carpeta.resolve(tipo + "_" + String.format("%06d", id) + ".pdf");
        Exportaciones.pdf(destino, "Comprobante de " + tipo.toLowerCase(), List.of(movimiento));
        return destino;
    }
}

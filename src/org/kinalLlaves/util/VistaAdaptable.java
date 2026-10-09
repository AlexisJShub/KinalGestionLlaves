package org.kinalLlaves.util;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
 
/**
 * Mantiene la interfaz completa visible en un unico escenario, sin barras de
 * desplazamiento para toda la pagina. Ajusta proporcionalmente la vista a la
 * resolucion disponible, incluyendo cuando cambia el tamano de la ventana.
 *
 * Los controles, tablas y acciones conservan sus coordenadas y son
 * interactivos.
 */
public final class VistaAdaptable extends Pane {

    private static final double ANCHO_DISENO = 1360;
    private static final double ALTO_BASE = 830;
    private final Region contenido;
    private final Scale escala = new Scale(1, 1, 0, 0);

    public VistaAdaptable(Parent raiz) {
        if (!(raiz instanceof Region region)) {
            throw new IllegalArgumentException("La vista debe tener un panel raiz de JavaFX");
        }
        contenido = region;
        contenido.getTransforms().add(escala);
        getChildren().add(contenido);
        setMinSize(0, 0);
        setPrefSize(ANCHO_DISENO, ALTO_BASE);
    }

    @Override
    protected void layoutChildren() {
        double anchoVentana = Math.max(1, getWidth());
        double altoVentana = Math.max(1, getHeight());

        // El alto preferido puede cambiar al leer un carne y pasar del estado
        // de espera al resumen, o al construir el formulario desde el DAO.
        double altoRequerido = contenido.prefHeight(ANCHO_DISENO);
        if (!Double.isFinite(altoRequerido) || altoRequerido < 1) {
            altoRequerido = ALTO_BASE;
        }
        double altoDiseno = Math.max(ALTO_BASE, altoRequerido + 12);
        double factor = Math.min(anchoVentana / ANCHO_DISENO, altoVentana / altoDiseno);
        factor = Math.max(0.1, factor);

        escala.setX(factor);
        escala.setY(factor);
        contenido.resizeRelocate(
                (anchoVentana - ANCHO_DISENO * factor) / 2,
                (altoVentana - altoDiseno * factor) / 2,
                ANCHO_DISENO,
                altoDiseno
        );
    }
}

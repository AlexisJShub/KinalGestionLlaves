package org.kinalllaves.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import org.kinalllaves.dao.LlaveEstadoDAO;
import org.kinalllaves.dao.impl.LlaveEstadoDAOImpl;
import org.kinalllaves.util.MensajesUI;

/**
 * Inventario con mantenimiento excepcional del estado de una llave.
 */
public final class LlavesController extends CatalogoController {

    @FXML
    private ComboBox<String> nuevoEstado;
    private final LlaveEstadoDAO estados = new LlaveEstadoDAOImpl();

    @FXML
    public void initialize() {
        configurar("LLAVES");
        nuevoEstado.getItems().setAll("DISPONIBLE", "DANADA", "EXTRAVIADA", "FUERA_SERVICIO");
        nuevoEstado.setValue("DISPONIBLE");
    }

    @FXML
    private void cambiarEstado() {
        try {
            if (nuevoEstado.getValue() == null) {
                throw new IllegalArgumentException("Selecciona el nuevo estado");
            }
            if (!MensajesUI.confirmar("¿Cambiar el estado de la llave seleccionada a " + nuevoEstado.getValue() + "?")) {
                return;
            }
            estados.cambiarEstado(idSeleccionado(), nuevoEstado.getValue());
            MensajesUI.info("Estado actualizado y auditado");
            limpiar();
            recargar();
        } catch (Exception ex) {
            MensajesUI.error("No se pudo actualizar el estado", ex);
        }
    }
}

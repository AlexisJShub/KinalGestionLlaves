package org.kinalllaves.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.*;
import org.kinalllaves.dao.*;
import org.kinalllaves.dao.impl.*;
import org.kinalllaves.model.Opcion;
import org.kinalllaves.system.Main;
import org.kinalllaves.util.*;

public class IncidenciasController {

    @FXML
    private ComboBox<String> tipo, estado;
    @FXML
    private TextArea descripcion;
    @FXML
    private ComboBox<Opcion> entrega, llave;
    @FXML
    private TableView<Map<String, Object>> tabla;
    @FXML
    private Button btnGestion;
    private final IncidenciaDAO dao = new IncidenciaDAOImpl();
    private final CatalogoDAO catalogo = new CatalogoDAOImpl();

    @FXML
    public void initialize() {
        if (!Permisos.puede("INCIDENCIAS")) {
            javafx.application.Platform.runLater(Main::dashboard);
            return;
        }
        tipo.getItems().setAll("RETRASO", "EXTRAVIO", "DANO", "DOCUMENTO_OLVIDADO", "OTRA");
        tipo.setValue("OTRA");
        estado.getItems().setAll("ABIERTA", "EN_REVISION", "CERRADA");
        estado.setValue("EN_REVISION");
        btnGestion.setDisable(!Permisos.puede("GESTION_INCIDENCIAS"));
        recargar();
    }

    @FXML
    private void recargar() {
        try {
            entrega.getItems().setAll(catalogo.opciones("ENTREGAS"));
            llave.getItems().setAll(catalogo.opciones("TODAS_LLAVES"));
            Tablas.mostrar(tabla, dao.listar(), "", "id_incidencia", "tipo", "descripcion", "estado", "fecha_registro");
        } catch (Exception e) {
            MensajesUI.error("No se pudieron leer incidencias", e);
        }
    }

    @FXML
    private void guardar() {
        try {
            Long id = entrega.getValue() == null ? null : entrega.getValue().id();
            Long idLlave = llave.getValue() == null ? null : llave.getValue().id();
            if (id == null && idLlave == null) {
                throw new IllegalArgumentException("Selecciona una entrega o una llave");
            }
            dao.registrar(tipo.getValue(), descripcion.getText().trim(), id, idLlave);
            descripcion.clear();
            entrega.setValue(null);
            llave.setValue(null);
            MensajesUI.info("Incidencia registrada");
            recargar();
        } catch (Exception e) {
            MensajesUI.error("Error al registrar incidencia", e);
        }
    }

    @FXML
    private void cambiar() {
        try {
            var r = tabla.getSelectionModel().getSelectedItem();
            if (r == null) {
                throw new IllegalArgumentException("Selecciona una incidencia");
            }
            dao.estado(((Number) r.get("id_incidencia")).longValue(), estado.getValue());
            recargar();
        } catch (Exception e) {
            MensajesUI.error("No se pudo modificar", e);
        }
    }

    @FXML
    private void volver() {
        Main.dashboard();
    }
}

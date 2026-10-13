package org.kinalllaves.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kinalllaves.dao.OperacionDAO;
import org.kinalllaves.dao.impl.OperacionDAOImpl;
import org.kinalllaves.util.*;
import org.kinalllaves.system.Main;

public class EstadosController {

    @FXML
    private TextField filtro;
    @FXML
    private TableView<java.util.Map<String, Object>> tabla;
    private final OperacionDAO dao = new OperacionDAOImpl();

    @FXML
    public void initialize() {
        if (!Permisos.puede("ESTADOS")) {
            javafx.application.Platform.runLater(Main::dashboard);
            return;
        }
        recargar();
    }

    @FXML
    private void recargar() {
        try {
            Tablas.mostrar(tabla, dao.tabla("ESTADOS"), filtro.getText(), "id_llave", "llave", "salon", "estado_operativo", "responsable");
        } catch (Exception e) {
            MensajesUI.error("Consulta de llaves", e);
        }
    }

    @FXML
    private void volver() {
        Main.dashboard();
    }
}

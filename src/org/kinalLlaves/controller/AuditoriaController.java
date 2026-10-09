package org.kinalLlaves.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.*;
import org.kinalllaves.dao.OperacionDAO;
import org.kinalllaves.dao.impl.OperacionDAOImpl;
import org.kinalllaves.system.Main;
import org.kinalllaves.util.*;

public class AuditoriaController {

    @FXML
    private TableView<Map<String, Object>> tabla;
    @FXML
    private TextField filtro;
    private final OperacionDAO dao = new OperacionDAOImpl();

    @FXML
    public void initialize() {
        if (!Permisos.puede("AUDITORIA")) {
            javafx.application.Platform.runLater(Main::dashboard);
            return;
        }
        recargar();
    }

    @FXML
    private void recargar() {
        try {
            Tablas.mostrar(tabla, dao.tabla("AUDITORIA"), filtro.getText(), "id_auditoria", "username", "evento", "entidad", "id_entidad", "detalle", "fecha_evento");
        } catch (Exception e) {
            MensajesUI.error("No se pudo cargar auditoría", e);
        }
    }

    @FXML
    private void volver() {
        Main.dashboard();
    }
}

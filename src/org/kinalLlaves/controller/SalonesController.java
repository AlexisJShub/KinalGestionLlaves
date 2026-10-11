package org.kinalllaves.controller;

import javafx.fxml.FXML;

/**
 * Controlador dedicado del módulo Salones de aprendizaje, con lógica compartida
 * en CatalogoController.
 */
public final class SalonesController extends CatalogoController {

    @FXML
    public void initialize() {
        configurar("SALONES");
    }
}
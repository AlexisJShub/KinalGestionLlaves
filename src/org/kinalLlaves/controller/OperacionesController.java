package org.kinalllaves.controller;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.kinalllaves.dao.CatalogoDAO;
import org.kinalllaves.dao.EmpleadoDAO;
import org.kinalllaves.dao.OperacionDAO;
import org.kinalllaves.dao.impl.CatalogoDAOImpl;
import org.kinalllaves.dao.impl.EmpleadoDAOImpl;
import org.kinalllaves.dao.impl.OperacionDAOImpl;
import org.kinalllaves.model.EmpleadoIdentificado;
import org.kinalllaves.model.Opcion;
import org.kinalllaves.system.Main;
import org.kinalllaves.util.Comprobantes;
import org.kinalllaves.util.Permisos;


public class OperacionesController {

    @FXML
    private VBox prestamoInicio, prestamoEspera, prestamoResumen;
    @FXML
    private VBox devolucionInicio, devolucionEspera, devolucionResumen;
    @FXML
    private VBox bloqueTitular;
    @FXML
    private TabPane pestanas;
    @FXML
    private Tab pestanaDevolver;
    @FXML
    private ComboBox<Opcion> llave, entrega;
    @FXML
    private TextField carnetPrestamo, carnetDevolucion, carnetTitular, carnetOtraPersona;
    @FXML
    private TextField documento, filtro;
    @FXML
    private TextArea observacion;
    @FXML
    private CheckBox documentoRetirado;
    @FXML
    private Label estadoPrestamo, estadoPrestamoEspera, estadoDevolucion, estadoDevolucionEspera;
    @FXML
    private Label nombrePrestamo, apellidosPrestamo, carnetPrestamoDato, cuiPrestamo, puestoPrestamo;
    @FXML
    private Label nombreDevolucion, apellidosDevolucion, carnetDevolucionDato, cuiDevolucion;
    @FXML
    private Label quienDevuelve, datosPrestamoActivo;
    @FXML
    private Button btnConfirmarPrestamo, btnConfirmarDevolucion;
    @FXML
    private TableView<Map<String, Object>> tabla;

    private final EmpleadoDAO empleados = new EmpleadoDAOImpl();
    private final CatalogoDAO catalogos = new CatalogoDAOImpl();
    private final OperacionDAO operaciones = new OperacionDAOImpl();
    private EmpleadoIdentificado titularPrestamo;
    private EmpleadoIdentificado titularDevolucion;
    private EmpleadoIdentificado personaDevuelve;
    private List<Map<String, Object>> prestamosPendientes = List.of();
    private boolean guardando;

    @FXML
    public void initialize() {
        if (!Permisos.puede("ENTREGAS")) {
            Platform.runLater(Main::dashboard);
            return;
        }
        mostrarPrestamo(prestamoInicio);
        mostrarDevolucion(devolucionInicio);
        bloqueTitular.setVisible(false);
        bloqueTitular.setManaged(false);
        entrega.valueProperty().addListener((obs, anterior, actual) -> mostrarDetalleActivo());
        carnetOtraPersona.textProperty().addListener((obs, antes, nuevo) -> {
            if (personaDevuelve != null && titularDevolucion != null
                    && personaDevuelve.id() != titularDevolucion.id()) {
                personaDevuelve = titularDevolucion;
                quienDevuelve.setText(titularDevolucion.nombreCompleto());
            }
        });
        recargar();
    }

    private static void mostrar(VBox visible, VBox... paneles) {
        for (VBox panel : paneles) {
            panel.setVisible(panel == visible);
            panel.setManaged(panel == visible);
        }
    }

    private void mostrarPrestamo(VBox actual) {
        mostrar(actual, prestamoInicio, prestamoEspera, prestamoResumen);
    }

    private void mostrarDevolucion(VBox actual) {
        mostrar(actual, devolucionInicio, devolucionEspera, devolucionResumen);
    }

    @FXML
    private void iniciarPrestamo() {
        titularPrestamo = null;
        llave.setValue(null);
        carnetPrestamo.clear();
        documento.clear();
        btnConfirmarPrestamo.setDisable(false);
        mensajePrestamo("Acerca tu carné al lector para continuar.", false);
        mostrarPrestamo(prestamoEspera);
        Platform.runLater(carnetPrestamo::requestFocus);
    }

    @FXML
    private void identificarCarnetPrestamo() {
        String codigo = carnetPrestamo.getText().trim();
        if (codigo.isEmpty()) {
            mensajePrestamo("Primero lee o escribe tu número de carné.", true);
            return;
        }
        try {
            EmpleadoIdentificado encontrado = empleados.identificarPorCarnet(codigo);
            if (encontrado == null) {
                mensajePrestamo("Este carné no está registrado o está inactivo. Revisa Empleados.", true);
                carnetPrestamo.selectAll();
                return;
            }
            titularPrestamo = encontrado;
            nombrePrestamo.setText(valor(encontrado.nombres()));
            apellidosPrestamo.setText(valor(encontrado.apellidos()));
            carnetPrestamoDato.setText(valor(encontrado.carnet()));
            cuiPrestamo.setText(valor(encontrado.cui()));
            puestoPrestamo.setText(valor(encontrado.puesto()));
            actualizarLlaves();
            mensajePrestamo("Carné identificado. Elige la llave y confirma el préstamo.", false);
            mostrarPrestamo(prestamoResumen);
        } catch (Exception ex) {
            mensajePrestamo("No se pudo leer el carné: " + textoError(ex), true);
        }
    }

    @FXML
    private void confirmarPrestamo() {
        if (guardando) {
            return;
        }
        if (titularPrestamo == null) {
            mensajePrestamo("Primero identifica tu carné.", true);
            mostrarPrestamo(prestamoEspera);
            return;
        }
        if (llave.getValue() == null) {
            mensajePrestamo("Falta seleccionar la llave que vas a retirar.", true);
            llave.requestFocus();
            return;
        }
        guardando = true;
        btnConfirmarPrestamo.setDisable(true);
        try {
            long movimiento = operaciones.entregar(llave.getValue().id(), titularPrestamo.id(),
                    documento.getText().trim(), null);
            String llaveCodigo = llave.getValue().toString();
            String resumen = "Préstamo #" + movimiento + " registrado. "
                    + titularPrestamo.nombreCompleto() + " recibió " + llaveCodigo + ".";
            String comprobante = generarComprobante("ENTREGA", movimiento);
            recargar();
            mensajePrestamo(resumen + comprobante + " Puedes iniciar otro préstamo.", false);
            btnConfirmarPrestamo.setDisable(true);
        } catch (Exception ex) {
            mensajePrestamo("No se registró el préstamo: " + textoError(ex), true);
            btnConfirmarPrestamo.setDisable(false);
        } finally {
            guardando = false;
        }
    }

    @FXML
    private void iniciarDevolucion() {
        titularDevolucion = null;
        personaDevuelve = null;
        prestamosPendientes = List.of();
        entrega.getItems().clear();
        entrega.setValue(null);
        carnetDevolucion.clear();
        carnetTitular.clear();
        carnetOtraPersona.clear();
        observacion.clear();
        documentoRetirado.setSelected(false);
        bloqueTitular.setVisible(false);
        bloqueTitular.setManaged(false);
        btnConfirmarDevolucion.setDisable(false);
        mensajeDevolucion("Acerca el carné de quien recibió la llave.", false);
        mostrarDevolucion(devolucionEspera);
        Platform.runLater(carnetDevolucion::requestFocus);
    }

    @FXML
    private void identificarCarnetDevolucion() {
        String codigo = carnetDevolucion.getText().trim();
        if (codigo.isEmpty()) {
            mensajeDevolucion("Lee el carné para buscar el préstamo pendiente.", true);
            return;
        }
        try {
            EmpleadoIdentificado identificado = empleados.identificarPorCarnet(codigo);
            if (identificado == null) {
                mensajeDevolucion("No se encontró el carné. Revisa Empleados.", true);
                return;
            }
            personaDevuelve = identificado;
            titularDevolucion = identificado;
            localizarPrestamos(identificado);
        } catch (Exception ex) {
            mensajeDevolucion("No se pudo consultar el carné: " + textoError(ex), true);
        }
    }

    @FXML
    private void identificarTitular() {
        String codigo = carnetTitular.getText().trim();
        if (codigo.isEmpty()) {
            mensajeDevolucion("Lee el carné de quien recibió la llave originalmente.", true);
            return;
        }
        try {
            EmpleadoIdentificado identificado = empleados.identificarPorCarnet(codigo);
            if (identificado == null) {
                mensajeDevolucion("Ese carné no aparece entre los empleados activos.", true);
                return;
            }
            titularDevolucion = identificado;
            localizarPrestamos(identificado);
        } catch (Exception ex) {
            mensajeDevolucion("No se pudo consultar el préstamo: " + textoError(ex), true);
        }
    }

    private void localizarPrestamos(EmpleadoIdentificado identificado) throws Exception {
        prestamosPendientes = operaciones.prestamosActivosDe(identificado.id());
        if (prestamosPendientes.isEmpty()) {
            mensajeDevolucion("No hay préstamos pendientes con este carné. Si devuelves por otra persona, busca el carné de quien recibió la llave.", true);
            bloqueTitular.setVisible(true);
            bloqueTitular.setManaged(true);
            carnetTitular.requestFocus();
            return;
        }
        nombreDevolucion.setText(valor(identificado.nombres()));
        apellidosDevolucion.setText(valor(identificado.apellidos()));
        carnetDevolucionDato.setText(valor(identificado.carnet()));
        cuiDevolucion.setText(valor(identificado.cui()));
        quienDevuelve.setText(personaDevuelve == null ? identificado.nombreCompleto() : personaDevuelve.nombreCompleto());
        entrega.getItems().clear();
        for (Map<String, Object> movimiento : prestamosPendientes) {
            long id = ((Number) movimiento.get("id_entrega")).longValue();
            String titulo = "#" + id + " | " + movimiento.get("llave") + " | " + movimiento.get("salon");
            entrega.getItems().add(new Opcion(id, titulo));
        }
        if (!entrega.getItems().isEmpty()) {
            entrega.getSelectionModel().selectFirst();
        }
        mostrarDetalleActivo();
        carnetOtraPersona.clear();
        mensajeDevolucion("Préstamo localizado. Revisa la llave y confirma la devolución.", false);
        mostrarDevolucion(devolucionResumen);
    }

    @FXML
    private void identificarQuienDevuelve() {
        if (titularDevolucion == null) {
            return;
        }
        String codigo = carnetOtraPersona.getText().trim();
        if (codigo.isBlank()) {
            personaDevuelve = titularDevolucion;
            quienDevuelve.setText(titularDevolucion.nombreCompleto());
            mensajeDevolucion("Devolverá la llave quien la solicitó.", false);
            return;
        }
        try {
            EmpleadoIdentificado encontrado = empleados.identificarPorCarnet(codigo);
            if (encontrado == null) {
                mensajeDevolucion("El carné de la persona que devuelve no está registrado.", true);
                return;
            }
            personaDevuelve = encontrado;
            quienDevuelve.setText(encontrado.nombreCompleto() + " (otra persona)");
            mensajeDevolucion("Persona que devuelve identificada. Ya puedes confirmar.", false);
        } catch (Exception ex) {
            mensajeDevolucion("No se pudo identificar a quien devuelve: " + textoError(ex), true);
        }
    }

    private void mostrarDetalleActivo() {
        if (entrega.getValue() == null) {
            datosPrestamoActivo.setText("No hay un préstamo seleccionado.");
            return;
        }
        Map<String, Object> dato = prestamosPendientes.stream()
                .filter(r -> r.get("id_entrega") instanceof Number n && n.longValue() == entrega.getValue().id())
                .findFirst().orElse(null);
        if (dato == null) {
            datosPrestamoActivo.setText("Este préstamo ya no está pendiente. Actualiza la pantalla.");
            return;
        }
        datosPrestamoActivo.setText("Llave: " + dato.get("llave") + "   •   Salón: " + dato.get("salon")
                + "\nSe prestó: " + dato.get("fecha_entrega")
                + "\nTiempo transcurrido: " + dato.get("minutos_uso") + " minutos");
    }

    @FXML
    private void confirmarDevolucion() {
        if (guardando) {
            return;
        }
        if (titularDevolucion == null || personaDevuelve == null || entrega.getValue() == null) {
            mensajeDevolucion("Falta identificar a la persona y seleccionar su préstamo.", true);
            return;
        }
        if (!carnetOtraPersona.getText().isBlank() && personaDevuelve.id() == titularDevolucion.id()
                && !carnetOtraPersona.getText().trim().equals(titularDevolucion.carnet())) {
            mensajeDevolucion("Presiona Verificar carné para identificar a la otra persona.", true);
            return;
        }
        guardando = true;
        btnConfirmarDevolucion.setDisable(true);
        try {
            long movimiento = operaciones.devolver(entrega.getValue().id(), personaDevuelve.id(),
                    documentoRetirado.isSelected(), observacion.getText().trim());
            String resumen = "Devolución #" + movimiento + " registrada. "
                    + personaDevuelve.nombreCompleto() + " devolvió la llave.";
            String comprobante = generarComprobante("DEVOLUCION", movimiento);
            recargar();
            mensajeDevolucion(resumen + comprobante + " Puedes registrar otra devolución.", false);
            btnConfirmarDevolucion.setDisable(true);
        } catch (Exception ex) {
            mensajeDevolucion("No se pudo registrar la devolución: " + textoError(ex), true);
            btnConfirmarDevolucion.setDisable(false);
        } finally {
            guardando = false;
        }
    }

    private String generarComprobante(String tipo, long id) {
        try {
            Map<String, Object> datos = operaciones.comprobante(tipo, id);
            Path pdf = Comprobantes.generar(tipo, id, datos);
            return " Comprobante: " + pdf.toAbsolutePath();
        } catch (Exception ex) {
            return " Movimiento guardado, pero el PDF no se generó: " + textoError(ex);
        }
    }

    private void actualizarLlaves() throws Exception {
        Long elegida = llave.getValue() == null ? null : llave.getValue().id();
        llave.getItems().setAll(catalogos.opciones("LLAVES"));
        if (elegida != null) {
            llave.getItems().stream().filter(o -> o.id() == elegida)
                    .findFirst().ifPresent(llave::setValue);
        }
    }

    @FXML
    private void recargar() {
        try {
            actualizarLlaves();
            Tablas.mostrar(tabla, operaciones.tabla("ENTREGAS"), filtro.getText(),
                    "id_entrega", "llave", "salon", "empleado", "carnet", "estado", "fecha_entrega", "fecha_devolucion");
        } catch (Exception ex) {
            tabla.getItems().clear();
            mensajePrestamo("No se pudo actualizar la información: " + textoError(ex), true);
        }
    }

    @FXML
    private void volver() {
        Main.dashboard();
    }

    private void mensajePrestamo(String texto, boolean error) {
        mensaje(estadoPrestamo, texto, error);
        mensaje(estadoPrestamoEspera, texto, error);
    }

    private void mensajeDevolucion(String texto, boolean error) {
        mensaje(estadoDevolucion, texto, error);
        mensaje(estadoDevolucionEspera, texto, error);
    }

    private static String valor(String dato) {
        return dato == null || dato.isBlank() ? "No registrado" : dato;
    }

    private static String textoError(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private static void mensaje(Label destino, String texto, boolean error) {
        if (destino == null) {
            return;
        }
        destino.setText(texto);
        destino.getStyleClass().removeAll("inline-error", "inline-success");
        destino.getStyleClass().add(error ? "inline-error" : "inline-success");
    }
}

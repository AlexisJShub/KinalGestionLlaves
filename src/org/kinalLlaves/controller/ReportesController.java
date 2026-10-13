package org.kinalllaves.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.kinalllaves.dao.ReporteDAO;
import org.kinalllaves.dao.impl.ReporteDAOImpl;
import org.kinalllaves.system.Main;
import org.kinalllaves.util.*;

public class ReportesController {

    @FXML
    private ComboBox<String> tipo;
    @FXML
    private DatePicker desde, hasta;
    @FXML
    private TextField filtro;
    @FXML
    private TableView<Map<String, Object>> tabla;
    @FXML
    private Label total, estadoFiltros, estadoComentario, contadorComentario;
    @FXML
    private TextArea comentario;
    @FXML
    private ListView<String> listaComentarios;
    @FXML
    private Button btnPdf, btnExcel, btnGuardarComentario;

    private final ReporteDAO dao = new ReporteDAOImpl();
    private List<Map<String, Object>> rows = List.of();
    private String notaGuardada = "";
    private final PauseTransition espera = new PauseTransition(Duration.millis(350));

    @FXML
    public void initialize() {
        if (!Permisos.puede("REPORTES")) {
            javafx.application.Platform.runLater(Main::dashboard);
            return;
        }
        tipo.getItems().setAll("Disponibilidad", "Entregas", "Devoluciones", "Uso por fecha");
        tipo.setValue("Disponibilidad");
        desde.setValue(LocalDate.now().minusMonths(1));
        hasta.setValue(LocalDate.now());
        espera.setOnFinished(e -> consultar());
        tipo.valueProperty().addListener((obs, anterior, nuevo) -> {
            notaGuardada = "";
            cargarComentarios();
            programarConsulta();
        });
        desde.valueProperty().addListener((obs, a, n) -> programarConsulta());
        hasta.valueProperty().addListener((obs, a, n) -> programarConsulta());
        filtro.textProperty().addListener((obs, a, n) -> programarConsulta());
        comentario.textProperty().addListener((obs, a, n) -> actualizarComentario());
        listaComentarios.getSelectionModel().selectedItemProperty().addListener((obs, a, n) -> {
            if (n != null && !n.isBlank()) {
                estadoComentario.setText("Comentario seleccionado: " + n);
            }
        });
        actualizarComentario();
        consultar();
        cargarComentarios();
    }

    private void programarConsulta() {
        if (estadoFiltros != null) {
            estadoFiltros.setText("Actualizando resultados...");
        }
        espera.playFromStart();
    }

    private boolean filtrosValidos() {
        if (tipo.getValue() == null) {
            mostrarError("Selecciona el reporte que necesitas.");
            return false;
        }
        if (desde.getValue() == null || hasta.getValue() == null) {
            mostrarError("Selecciona la fecha inicial y la final.");
            return false;
        }
        if (hasta.getValue().isBefore(desde.getValue())) {
            mostrarError("La fecha final no puede ser anterior a la inicial.");
            return false;
        }
        return true;
    }

    @FXML
    private void consultar() {
        if (!filtrosValidos()) {
            return;
        }
        try {
            rows = dao.consultar(tipo.getValue(), desde.getValue(), hasta.getValue(), filtro.getText());
            mostrarFilas();
            total.setText(rows.isEmpty() ? "Sin registros" : "Resultados: " + rows.size());
            estadoFiltros.setText(rows.isEmpty()
                    ? "No hay resultados para esta búsqueda. Prueba otras fechas o palabras."
                    : "Información actualizada. Puedes guardar el reporte o escribir un comentario.");
            cambiarExportacion();
        } catch (Exception ex) {
            rows = List.of();
            mostrarFilas();
            total.setText("Sin resultados");
            mostrarError("No se pudo consultar: " + MensajesUI.explicar(ex));
            cambiarExportacion();
        }
    }

    private void mostrarFilas() {
        String[] cols = switch (tipo.getValue() == null ? "" : tipo.getValue()) {
            case "Entregas" ->
                new String[]{"id_entrega", "llave", "salon", "empleado", "fecha_entrega", "estado"};
            case "Devoluciones" ->
                new String[]{"id_devolucion", "llave", "salon", "quien_devuelve", "fecha_devolucion"};
            case "Uso por fecha" ->
                new String[]{"salon", "fecha", "usos"};
            default ->
                new String[]{"llave", "salon", "estado_operativo", "responsable"};
        };
        Tablas.mostrar(tabla, rows, "", cols);
    }

    private void mostrarError(String texto) {
        if (estadoFiltros != null) {
            estadoFiltros.setText(texto);
        }
    }

    private void cambiarExportacion() {
        boolean vacio = rows.isEmpty();
        if (btnPdf != null) {
            btnPdf.setDisable(vacio);
        }
        if (btnExcel != null) {
            btnExcel.setDisable(vacio);
        }
    }

    private void actualizarComentario() {
        String texto = comentario.getText() == null ? "" : comentario.getText().trim();
        if (contadorComentario != null) {
            contadorComentario.setText(texto.length() + " / 500 caracteres");
        }
        if (btnGuardarComentario != null) {
            btnGuardarComentario.setDisable(texto.isEmpty() || texto.length() > 500);
        }
        if (texto.length() > 500) {
            estadoComentario.setText("El comentario supera el máximo de 500 caracteres.");
        }
    }

    @FXML
    private void guardarComentario() {
        String texto = comentario.getText() == null ? "" : comentario.getText().trim();
        if (texto.isBlank()) {
            estadoComentario.setText("Primero escribe un comentario.");
            return;
        }
        if (texto.length() > 500) {
            estadoComentario.setText("Escribe máximo 500 caracteres.");
            return;
        }
        if (!filtrosValidos()) {
            return;
        }
        try {
            dao.guardarComentario(tipo.getValue(), desde.getValue(), hasta.getValue(), texto);
            notaGuardada = texto;
            comentario.clear();
            cargarComentarios();
            estadoComentario.setText("Comentario guardado con el usuario y la fecha. Se incluirá en la próxima exportación.");
        } catch (Exception ex) {
            estadoComentario.setText("No se pudo guardar: " + MensajesUI.explicar(ex));
        }
    }

    private void cargarComentarios() {
        if (listaComentarios == null || tipo.getValue() == null) {
            return;
        }
        try {
            listaComentarios.getItems().setAll(dao.comentarios(tipo.getValue()));
            listaComentarios.setPlaceholder(new Label("Este reporte todavía no tiene comentarios."));
        } catch (Exception ex) {
            estadoComentario.setText("No se pudieron cargar comentarios: " + MensajesUI.explicar(ex));
        }
    }

    private List<Map<String, Object>> datosParaExportar() {
        String nota = comentario.getText() == null ? "" : comentario.getText().trim();
        if (nota.isBlank()) {
            nota = notaGuardada;
        }
        if (nota.isBlank()) {
            return rows;
        }
        List<Map<String, Object>> copia = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> fila = new LinkedHashMap<>(r);
            fila.put("comentario", nota);
            copia.add(fila);
        }
        return copia;
    }

    private Path elegir(String extension) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar " + extension.toUpperCase());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension.toUpperCase(), "*." + extension));
        fc.setInitialFileName("KinalLlaves_" + tipo.getValue().replace(' ', '_') + "." + extension);
        var archivo = fc.showSaveDialog(tabla.getScene().getWindow());
        return archivo == null ? null : archivo.toPath();
    }

    @FXML
    private void excel() {
        if (rows.isEmpty()) {
            mostrarError("Primero consulta un reporte con datos.");
            return;
        }
        try {
            Path archivo = elegir("xlsx");
            if (archivo != null) {
                Exportaciones.xlsx(archivo, datosParaExportar());
                estadoFiltros.setText("Excel guardado: " + archivo.getFileName());
                MensajesUI.info("Reporte Excel guardado correctamente.");
            }
        } catch (Exception ex) {
            mostrarError("No se pudo exportar Excel: " + MensajesUI.explicar(ex));
        }
    }

    @FXML
    private void pdf() {
        if (rows.isEmpty()) {
            mostrarError("Primero consulta un reporte con datos.");
            return;
        }
        try {
            Path archivo = elegir("pdf");
            if (archivo != null) {
                Exportaciones.pdf(archivo, "Reporte de " + tipo.getValue(), datosParaExportar());
                estadoFiltros.setText("PDF guardado: " + archivo.getFileName());
                MensajesUI.info("Reporte PDF guardado correctamente.");
            }
        } catch (Exception ex) {
            mostrarError("No se pudo exportar PDF: " + MensajesUI.explicar(ex));
        }
    }

    @FXML
    private void volver() {
        Main.dashboard();
    }
}

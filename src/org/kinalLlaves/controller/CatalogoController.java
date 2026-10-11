package org.kinalllaves.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.*;
import org.kinalllaves.dao.*;
import org.kinalllaves.dao.impl.*;
import org.kinalllaves.model.Opcion;
import org.kinalllaves.system.Main;
import org.kinalllaves.util.*;

/**
 * Base compartida para los cuatro catálogos; cada vista tiene su propio
 * controlador.
 */
public class CatalogoController {

    @FXML
    private Label lblAyuda;
    @FXML
    private TextField buscar;
    @FXML
    private TableView<Map<String, Object>> tabla;
    @FXML
    private GridPane formulario;
    @FXML
    private Button btnGuardar, btnDesactivar;
    @FXML
    private Label lblEstadoLista;
    private Label mensajeFormulario;
    private Label progresoFormulario;
    private final List<String> pasosObligatorios = new ArrayList<>();
    private final List<String> ordenCampos = new ArrayList<>();
    private final CatalogoDAO dao = new CatalogoDAOImpl();
    private String modulo;
    private long seleccionado;
    private List<Map<String, Object>> filasCargadas = List.of();
    private final Map<String, TextField> campos = new LinkedHashMap<>();
    private final Map<String, ComboBox<Opcion>> relaciones = new LinkedHashMap<>();
    private final Map<String, ComboBox<String>> estados = new LinkedHashMap<>();
    private final Map<String, CheckBox> banderas = new LinkedHashMap<>();

    public final void configurar(String identificador) {
        if (!CatalogoDAO.MODULOS.containsKey(identificador)) {
            throw new IllegalArgumentException("Catálogo desconocido");
        }
        modulo = identificador;
        lblAyuda.setText(identificador.equals("USUARIOS")
                ? "Aquí registras cuentas del sistema. El carné de profesores se registra en Empleados."
                : identificador.equals("EMPLEADOS")
                ? "Registra el número de carné para identificar al profesor con el lector."
                : "Selecciona un registro para editarlo o desactivarlo sin borrar el historial.");
        construir();
        if (btnDesactivar != null) {
            btnDesactivar.setDisable(true);
        }
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, item) -> {
            if (item != null) {
                seleccionar(item);
            }
        });
        if (buscar != null) {
            buscar.textProperty().addListener((obs, antes, ahora) -> filtrarTabla());
        }
        recargar();
    }

    @FXML
    protected void volver() {
        Main.dashboard();
    }

    @FXML
    protected void buscar() {
        recargar();
    }

    @FXML
    protected void limpiar() {
        seleccionado = 0;
        campos.values().forEach(TextField::clear);
        relaciones.values().forEach(c -> c.getSelectionModel().clearSelection());
        banderas.values().forEach(c -> c.setSelected(true));
        estados.values().forEach(c -> c.setValue("DISPONIBLE"));
        tabla.getSelectionModel().clearSelection();
        actualizarProgreso();
        if (btnDesactivar != null) {
            btnDesactivar.setDisable(true);
        }
        notificar("Completa los campos en orden para registrar un nuevo dato.", false);
    }

    private void construir() {
        formulario.getChildren().clear();
        campos.clear();
        relaciones.clear();
        estados.clear();
        banderas.clear();
        var def = CatalogoDAO.MODULOS.get(modulo);
        pasosObligatorios.clear();
        ordenCampos.clear();
        List<String> orden = switch (modulo) {
            case "USUARIOS" ->
                List.of("username", "nombre_completo", "carnet", "password_hash", "id_rol", "activo");
            case "EMPLEADOS" ->
                List.of("nombres", "apellidos", "carnet", "puesto", "cui", "email", "uid_rfid", "activo");
            case "SALONES" ->
                List.of("codigo", "edificio", "nivel", "nombre", "capacidad", "activo");
            case "LLAVES" ->
                List.of("codigo", "id_salon", "descripcion", "estado", "activo");
            default ->
                Arrays.asList(def.columnas());
        };
        ordenCampos.addAll(orden);
        for (String campo : orden) {
            if (Set.of("username", "nombre_completo", "carnet", "password_hash", "id_rol", "nombres",
                    "apellidos", "puesto", "codigo", "id_salon", "edificio", "nivel", "nombre").contains(campo)) {
                pasosObligatorios.add(campo);
            }
        }
        progresoFormulario = new Label("Completa los datos para continuar.");
        progresoFormulario.setWrapText(true);
        progresoFormulario.getStyleClass().add("inline-status");
        formulario.getColumnConstraints().clear();
        for (double porcentaje : new double[]{14, 36, 14, 36}) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(porcentaje);
            columna.setHgrow(Priority.ALWAYS);
            formulario.getColumnConstraints().add(columna);
        }
        formulario.setMinHeight(0);
        formulario.add(progresoFormulario, 0, 0, 4, 1);
        int indice = 0;
        for (String campo : orden) {
            int columnaEtiqueta = (indice % 2) * 2;
            int columnaCampo = columnaEtiqueta + 1;
            int fila = 1 + indice / 2;
            Label etiqueta = new Label(etiqueta(campo));
            etiqueta.getStyleClass().add("field-label");
            etiqueta.setWrapText(true);
            formulario.add(etiqueta, columnaEtiqueta, fila);
            if (campo.equals("id_rol") || campo.equals("id_salon")) {
                ComboBox<Opcion> combo = new ComboBox<>();
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setPromptText("Selecciona una opción");
                try {
                    combo.getItems().setAll(dao.opciones(campo.equals("id_rol") ? "ROLES" : "SALONES"));
                } catch (Exception ex) {
                    combo.setPromptText("Sin opciones: revisa MySQL");
                }
                relaciones.put(campo, combo);
                formulario.add(combo, columnaCampo, fila);
                combo.valueProperty().addListener((obs, antes, ahora) -> actualizarProgreso());
            } else if (campo.equals("activo")) {
                CheckBox box = new CheckBox("Registro activo");
                box.setSelected(true);
                banderas.put(campo, box);
                formulario.add(box, columnaCampo, fila);
            } else if (campo.equals("estado")) {
                ComboBox<String> estado = new ComboBox<>();
                estado.setMaxWidth(Double.MAX_VALUE);
                estado.getItems().setAll("DISPONIBLE");
                estado.setValue("DISPONIBLE");
                estados.put(campo, estado);
                formulario.add(estado, columnaCampo, fila);
            } else {
                TextField field = campo.equals("password_hash") ? new PasswordField() : new TextField();
                field.setPromptText(campo.equals("password_hash") ? "Mínimo 8 caracteres; vacío para conservar" : etiqueta(campo));
                field.setMaxWidth(Double.MAX_VALUE);
                campos.put(campo, field);
                formulario.add(field, columnaCampo, fila);
                field.textProperty().addListener((obs, antes, ahora) -> actualizarProgreso());
            }
            indice++;
        }
        int filaMensaje = 1 + (indice + 1) / 2;
        mensajeFormulario = new Label("Selecciona un registro o llena el formulario.");
        mensajeFormulario.setWrapText(true);
        mensajeFormulario.getStyleClass().add("inline-status");
        formulario.add(mensajeFormulario, 0, filaMensaje, 4, 1);
        actualizarProgreso();
    }

    private boolean completado(String paso) {
        if (paso.equals("password_hash") && seleccionado > 0) {
            return true;
        }
        if (relaciones.containsKey(paso)) {
            return relaciones.get(paso).getValue() != null;
        }
        TextField campo = campos.get(paso);
        if (campo == null || campo.getText() == null || campo.getText().isBlank()) {
            return false;
        }
        String valor = campo.getText().trim();
        if (paso.equals("carnet")) {
            return valor.matches("[A-Za-z0-9-]{3,40}");
        }
        if (paso.equals("password_hash")) {
            return campo.getText().length() >= 8;
        }
        if (paso.equals("username")) {
            return valor.matches("[A-Za-z0-9._-]{3,40}");
        }
        if (paso.equals("nombre_completo")) {
            return valor.length() >= 3 && valor.length() <= 125;
        }
        if (paso.equals("codigo")) {
            return valor.length() <= 30;
        }
        return true;
    }

    private void actualizarProgreso() {
        if (progresoFormulario == null) {
            return;
        }
        boolean habilitar = true;
        List<String> pasos = new ArrayList<>();
        for (String campo : ordenCampos) {
            if (campos.containsKey(campo)) {
                campos.get(campo).setDisable(!habilitar);
            }
            if (relaciones.containsKey(campo)) {
                relaciones.get(campo).setDisable(!habilitar);
            }
            if (estados.containsKey(campo)) {
                estados.get(campo).setDisable(!habilitar);
            }
            if (banderas.containsKey(campo)) {
                banderas.get(campo).setDisable(!habilitar);
            }
            if (pasosObligatorios.contains(campo)) {
                boolean listo = completado(campo);
                String estado = listo ? "completo ✓" : habilitar ? "faltante" : "pendiente";
                if (campo.equals("carnet") && listo) {
                    estado = seleccionado == 0 ? "capturado, pendiente de guardar" : "registrado ✓";
                }
                pasos.add(etiqueta(campo) + ": " + estado);
                if (!listo) {
                    habilitar = false;
                }
            }
        }
        String opcional = errorOpcional();
        if (opcional != null && habilitar) {
            pasos.add(opcional);
        }
        boolean puedeGuardar = habilitar && opcional == null;
        if (btnGuardar != null) {
            btnGuardar.setDisable(!puedeGuardar);
            String falta = primerPendiente();
            btnGuardar.setTooltip(new Tooltip(puedeGuardar
                    ? "Los datos están listos para guardar." : falta != null
                            ? "Primero completa: " + etiqueta(falta) : opcional));
        }
        progresoFormulario.setText(String.join("  |  ", pasos));
        progresoFormulario.getStyleClass().removeAll("inline-error", "inline-success");
        progresoFormulario.getStyleClass().add(puedeGuardar ? "inline-success" : "inline-status");
    }

    private String errorOpcional() {
        TextField correo = campos.get("email");
        if (correo != null && !correo.getText().isBlank()
                && !correo.getText().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "Correo electrónico: formato incorrecto";
        }
        TextField cui = campos.get("cui");
        if (cui != null && !cui.getText().isBlank() && !cui.getText().trim().matches("[0-9]{13}")) {
            return "CUI: deben ser 13 números";
        }
        TextField capacidad = campos.get("capacidad");
        if (capacidad != null && !capacidad.getText().isBlank()) {
            try {
                if (Integer.parseInt(capacidad.getText().trim()) < 0) {
                    return "Capacidad: no puede ser negativa";
                }
            } catch (NumberFormatException ex) {
                return "Capacidad: debe ser un número entero";
            }
        }
        return null;
    }

    private String primerPendiente() {
        for (String paso : pasosObligatorios) {
            if (!completado(paso)) {
                return paso;
            }
        }
        return null;
    }

    private String etiqueta(String name) {
        return switch (name) {
            case "nombres" ->
                "Nombres";
            case "apellidos" ->
                "Apellidos";
            case "nombre" ->
                "Nombre";
            case "puesto" ->
                "Puesto";
            case "codigo" ->
                "Código";
            case "activo" ->
                "Activo";
            case "edificio" ->
                "Edificio";
            case "nivel" ->
                "Nivel";
            case "capacidad" ->
                "Capacidad";
            case "descripcion" ->
                "Descripción";
            case "estado" ->
                "Estado";
            case "username" ->
                "Usuario";
            case "nombre_completo" ->
                "Nombre completo";
            case "password_hash" ->
                "Contraseña";
            case "carnet" ->
                "Número de carné";
            case "id_rol" ->
                "Rol";
            case "id_salon" ->
                "Salón asignado";
            case "uid_rfid" ->
                "Identificador RFID";
            case "cui" ->
                "CUI (13 dígitos)";
            case "email" ->
                "Correo electrónico";
            default ->
                name.replace('_', ' ');
        };
    }

    @FXML
    protected void guardar() {
        try {
            String falta = primerPendiente();
            if (falta != null) {
                notificar("Falta completar: " + etiqueta(falta) + ".", true);
                if (campos.containsKey(falta)) {
                    campos.get(falta).requestFocus();
                } else if (relaciones.containsKey(falta)) {
                    relaciones.get(falta).requestFocus();
                }
                return;
            }
            String errorOpcional = errorOpcional();
            if (errorOpcional != null) {
                notificar(errorOpcional, true);
                return;
            }
            if ((modulo.equals("USUARIOS") || modulo.equals("EMPLEADOS") || modulo.equals("LLAVES"))
                    && !(modulo.equals("USUARIOS") && seleccionado == Sesion.id())
                    && !CarnetConfirmacion.operador("guardar " + modulo.toLowerCase(Locale.ROOT))) {
                notificar("Registro cancelado. No se confirmó el carné del operador.", true);
                return;
            }
            Map<String, String> data = new LinkedHashMap<>();
            campos.forEach((k, v) -> data.put(k, v.getText() == null ? "" : v.getText().trim()));
            for (var entry : relaciones.entrySet()) {
                Opcion value = entry.getValue().getValue();
                if (value == null) {
                    throw new IllegalArgumentException("Falta seleccionar: " + etiqueta(entry.getKey()));
                }
                data.put(entry.getKey(), String.valueOf(value.id()));
            }
            banderas.forEach((k, v) -> data.put(k, String.valueOf(v.isSelected())));
            estados.forEach((k, v) -> data.put(k, v.getValue()));
            dao.guardar(modulo, seleccionado, data);
            String operacion = seleccionado == 0 ? "registrado" : "actualizado";
            limpiar();
            recargar();
            notificar("Registro " + operacion + " correctamente. La lista ya está actualizada.", false);
            MensajesUI.info("Registro " + operacion + " correctamente.");
        } catch (Exception ex) {
            String aviso = MensajesUI.explicar(ex);
            notificar(aviso, true);
            MensajesUI.error("No se pudo guardar el registro", ex);
        }
    }

    @FXML
    protected void desactivar() {
        if (seleccionado <= 0) {
            notificar("Selecciona primero un registro de la tabla.", true);
            return;
        }
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Motivo de desactivación");
        dialogo.setHeaderText("¿Por qué vas a desactivar este registro?");
        dialogo.setContentText("Motivo:");
        var texto = dialogo.showAndWait();
        if (texto.isEmpty()) {
            return;
        }
        String motivo = texto.get().trim();
        if (motivo.isBlank()) {
            notificar("Falta escribir el motivo de desactivación.", true);
            return;
        }
        if (motivo.length() > 500) {
            notificar("El motivo no puede superar 500 caracteres.", true);
            return;
        }
        try {
            if (!CarnetConfirmacion.operador("desactivar " + modulo.toLowerCase(Locale.ROOT))) {
                return;
            }
            if (!MensajesUI.confirmar("¿Desactivar el registro seleccionado?\nMotivo: " + motivo)) {
                return;
            }
            dao.desactivar(modulo, seleccionado, motivo);
            limpiar();
            recargar();
            notificar("Registro desactivado. Motivo guardado en Auditoría.", false);
        } catch (Exception ex) {
            notificar(MensajesUI.explicar(ex), true);
            MensajesUI.error("No se pudo desactivar", ex);
        }
    }

    protected long idSeleccionado() {
        return seleccionado;
    }

    protected void recargar() {
        if (modulo == null) {
            return;
        }
        prepararColumnas();
        try {
            for (var entry : relaciones.entrySet()) {
                Opcion elegida = entry.getValue().getValue();
                entry.getValue().getItems().setAll(dao.opciones(entry.getKey().equals("id_rol") ? "ROLES" : "SALONES"));
                if (elegida != null) {
                    entry.getValue().getItems().stream()
                            .filter(op -> op.id() == elegida.id()).findFirst().ifPresent(entry.getValue()::setValue);
                }
            }
            filasCargadas = dao.listar(modulo, "");
            filtrarTabla();
        } catch (Exception ex) {
            filasCargadas = List.of();
            tabla.setItems(javafx.collections.FXCollections.observableArrayList());
            tabla.setPlaceholder(new Label("No se pudieron cargar los registros. Revisa MySQL y presiona Actualizar."));
            if (lblEstadoLista != null) {
                lblEstadoLista.setText("Error al cargar los datos");
            }
            notificar("Error al consultar datos: " + MensajesUI.explicar(ex), true);
        }
    }

    private void prepararColumnas() {
        if (!tabla.getColumns().isEmpty()) {
            return;
        }
        var def = CatalogoDAO.MODULOS.get(modulo);
        List<String> headers = new ArrayList<>();
        headers.add(def.pk());
        for (String campo : def.columnas()) {
            if (!campo.equals("password_hash")) {
                headers.add(campo);
            }
        }
        for (String key : headers) {
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(etiqueta(key));
            col.setPrefWidth(Set.of("nombre_completo", "nombres", "apellidos", "nombre").contains(key) ? 180 : 125);
            col.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(
                    Objects.toString(v.getValue().get(key), "")));
            tabla.getColumns().add(col);
        }
    }

    private void filtrarTabla() {
        if (modulo == null || tabla == null) {
            return;
        }
        String texto = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> visibles = new ArrayList<>();
        for (Map<String, Object> fila : filasCargadas) {
            if (texto.isEmpty() || fila.values().toString().toLowerCase(Locale.ROOT).contains(texto)) {
                visibles.add(fila);
            }
        }
        tabla.setItems(javafx.collections.FXCollections.observableArrayList(visibles));
        tabla.setPlaceholder(new Label(texto.isEmpty()
                ? "Todavía no hay registros. Completa el formulario para agregar uno."
                : "No hay resultados. Prueba con otro nombre, carné o código."));
        if (lblEstadoLista != null) {
            lblEstadoLista.setText(visibles.size() + " de " + filasCargadas.size() + " registros");
        }
    }

    private void notificar(String texto, boolean error) {
        if (mensajeFormulario == null) {
            return;
        }
        mensajeFormulario.setText(texto);
        mensajeFormulario.getStyleClass().removeAll("inline-error", "inline-success");
        mensajeFormulario.getStyleClass().add(error ? "inline-error" : "inline-success");
    }

    private void seleccionar(Map<String, Object> item) {
        var def = CatalogoDAO.MODULOS.get(modulo);
        seleccionado = ((Number) item.get(def.pk())).longValue();
        campos.forEach((campo, field) -> {
            if (campo.equals("password_hash")) {
                field.clear();
            } else {
                field.setText(Objects.toString(item.get(campo), ""));
            }
        });
        banderas.forEach((campo, field) -> {
            Object value = item.get(campo);
            field.setSelected(Boolean.TRUE.equals(value) || (value instanceof Number n && n.intValue() != 0));
        });
        estados.forEach((campo, field) -> field.setValue(Objects.toString(item.get(campo), "DISPONIBLE")));
        relaciones.forEach((campo, field) -> {
            Object selected = item.get(campo);
            if (selected instanceof Number n) {
                for (Opcion option : field.getItems()) {
                    if (option.id() == n.longValue()) {
                        field.setValue(option);
                        break;
                    }
                }
            }
        });
        actualizarProgreso();
        if (btnDesactivar != null) {
            btnDesactivar.setDisable(false);
        }
        notificar("Editando el registro #" + seleccionado + ". Puedes guardar cambios o desactivarlo con un motivo.", false);
    }
}

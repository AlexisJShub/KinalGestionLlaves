package org.kinalllaves.system;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;
import java.util.Map;
import org.kinalllaves.util.*;

/**
 * Navegación centralizada: ninguna pantalla protegida se abre sin sesión y
 * permiso.
 */
public class Main extends Application {

    private static Stage stage;
    private static Scene escenaPrincipal;
    private static final String BASE = "/org/kinalllaves/view/";
    private static final Map<String, String> MODULOS = Map.of(
            "USUARIOS", "usuarios.fxml", "EMPLEADOS", "empleados.fxml",
            "SALONES", "salones.fxml", "LLAVES", "llaves.fxml");
    private static final Map<String, String> VISTAS_PROTEGIDAS = Map.ofEntries(
            Map.entry("usuarios.fxml", "USUARIOS"),
            Map.entry("empleados.fxml", "EMPLEADOS"),
            Map.entry("salones.fxml", "SALONES"),
            Map.entry("llaves.fxml", "LLAVES"),
            Map.entry("estados.fxml", "ESTADOS"),
            Map.entry("operaciones.fxml", "ENTREGAS"),
            Map.entry("reservas.fxml", "RESERVAS"),
            Map.entry("incidencias.fxml", "INCIDENCIAS"),
            Map.entry("auditoria.fxml", "AUDITORIA"),
            Map.entry("reportes.fxml", "REPORTES"));

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primary) {
        stage = primary;
        stage.setMinWidth(780);
        stage.setMinHeight(520);
        abrir("login.fxml", "Iniciar sesión");
        stage.setMaximized(true);
    }

    public static void abrir(String archivo, String titulo) {
        if (stage == null) {
            throw new IllegalStateException("La aplicación todavía no ha iniciado");
        }
        if (!"login.fxml".equals(archivo) && Sesion.actual() == null) {
            mostrarLogin();
            return;
        }
        String permiso = VISTAS_PROTEGIDAS.get(archivo);
        if (permiso != null && !Permisos.puede(permiso)) {
            MensajesUI.error("No tienes permiso para acceder a: " + titulo, null);
            return;
        }
        if (archivo.startsWith("dashboard_")) {
            String rol = Sesion.actual().rol();
            String esperado = switch (rol) {
                case "ADMIN" ->
                    "dashboard_admin.fxml";
                case "JEFE" ->
                    "dashboard_jefe.fxml";
                case "SECRETARIO" ->
                    "dashboard_secretario.fxml";
                default ->
                    "login.fxml";
            };
            if (!archivo.equals(esperado)) {
                MensajesUI.error("Dashboard no permitido para el rol actual", null);
                return;
            }
        }
        try {
            URL fxml = Main.class.getResource(BASE + archivo);
            if (fxml == null) {
                throw new IllegalStateException("No se encontró el archivo " + BASE + archivo);
            }
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            VistaAdaptable contenidoAjustable = new VistaAdaptable(root);
            if (escenaPrincipal == null) {
                escenaPrincipal = new Scene(contenidoAjustable, 1360, 830);
                URL css = Main.class.getResource(BASE + "style/app.css");
                if (css != null) {
                    escenaPrincipal.getStylesheets().add(css.toExternalForm());
                }
                stage.setScene(escenaPrincipal);
            } else {
                // Conservar la misma Scene evita saltos de dimensiones al navegar.
                escenaPrincipal.setRoot(contenidoAjustable);
            }
            stage.setTitle("KinalLlaves | " + titulo);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            MensajesUI.error("No se pudo cargar la pantalla " + archivo, ex);
        }
    }

    private static void mostrarLogin() {
        if (Sesion.actual() != null) {
            Sesion.cerrar();
        }
        abrir("login.fxml", "Acceso");
    }

    public static void dashboard() {
        if (Sesion.actual() == null) {
            mostrarLogin();
            return;
        }
        switch (Sesion.actual().rol()) {
            case "ADMIN" ->
                abrir("dashboard_admin.fxml", "Administración");
            case "JEFE" ->
                abrir("dashboard_jefe.fxml", "Jefatura de Secretaría");
            case "SECRETARIO" ->
                abrir("dashboard_secretario.fxml", "Secretaría");
            default ->
                mostrarLogin();
        }
    }

    public static void modulo(String modulo) {
        String archivo = MODULOS.get(modulo);
        if (archivo == null) {
            MensajesUI.error("Módulo desconocido: " + modulo, null);
            return;
        }
        abrir(archivo, modulo);
    }
}

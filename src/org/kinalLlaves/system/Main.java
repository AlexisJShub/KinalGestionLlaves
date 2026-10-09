package org.kinalllaves.system;

import java.net.URL;
import java.util.Map;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kinalllaves.util.MensajesUI;
import org.kinalllaves.util.Permisos;
import org.kinalllaves.util.Sesion;
import org.kinalllaves.util.VistaAdaptable;

/**
 * Entrada y navegación principal de KinalLlaves.
 * Ejecutar esta clase en NetBeans; no se necesita una clase Lanzador.
 */
public class Main extends Application {

    private static final String BASE_VISTAS = "/org/kinalllaves/view/";
    private static final String CSS = BASE_VISTAS + "style/app.css";
    private static final Map<String, String> MODULOS = Map.of(
            "USUARIOS", "usuarios.fxml",
            "EMPLEADOS", "empleados.fxml",
            "SALONES", "salones.fxml",
            "LLAVES", "llaves.fxml");
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

    private static Stage stagePrincipal;
    private static Scene escenaPrincipal;
    private static Object controladorVistaActual;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stagePrincipal = stage;
        stage.setResizable(true);
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        stage.setMinWidth(Math.min(780, area.getWidth()));
        stage.setMinHeight(Math.min(520, area.getHeight()));
        abrir("login.fxml", "Iniciar sesión");
        stage.setMaximized(true);
    }

    /**
     * Entrada de navegación utilizada por los controladores del proyecto.
     * Se conserva por compatibilidad con sus acciones FXML.
     */
    public static void abrir(String archivoFxml, String titulo) {
        cambiarVista(archivoFxml, titulo, 1360, 830);
    }

    /**
     * Carga una vista en la MISMA ventana, sin recrear el Stage.
     * Admite tanto rutas completas como nombres de archivos del directorio view.
     * Se mantiene la comprobación de sesión, rol y permisos.
     */
    public static void cambiarVista(String rutaFxml, String titulo, double ancho, double alto) {
        if (stagePrincipal == null) {
            throw new IllegalStateException("La aplicación aún no ha iniciado");
        }
        if (rutaFxml == null || rutaFxml.isBlank()) {
            MensajesUI.error("Indica una vista válida", null);
            return;
        }

        String archivo = rutaFxml.replace('\\', '/');
        archivo = archivo.substring(archivo.lastIndexOf('/') + 1);
        if (!"login.fxml".equals(archivo) && Sesion.actual() == null) {
            abrirLogin();
            return;
        }
        String permiso = VISTAS_PROTEGIDAS.get(archivo);
        if (permiso != null && !Permisos.puede(permiso)) {
            MensajesUI.error("No tienes permiso para acceder a: " + titulo, null);
            return;
        }
        if (archivo.startsWith("dashboard_") && !archivo.equals(dashboardDelRol())) {
            MensajesUI.error("Dashboard no permitido para el rol actual", null);
            return;
        }

        try {
            String ruta = rutaFxml.startsWith("/") ? rutaFxml : BASE_VISTAS + archivo;
            URL recurso = Main.class.getResource(ruta);
            if (recurso == null) {
                throw new IllegalStateException("No se encontró el FXML: " + ruta);
            }
            FXMLLoader loader = new FXMLLoader(recurso);
            Parent raiz = loader.load();
            VistaAdaptable vista = new VistaAdaptable(raiz);
            Rectangle2D area = Screen.getPrimary().getVisualBounds();
            double anchoReal = Math.max(1, Math.min(ancho, area.getWidth()));
            double altoReal = Math.max(1, Math.min(alto, area.getHeight()));

            if (escenaPrincipal == null) {
                escenaPrincipal = new Scene(vista, anchoReal, altoReal);
                URL estilos = Main.class.getResource(CSS);
                if (estilos != null) {
                    escenaPrincipal.getStylesheets().add(estilos.toExternalForm());
                }
                stagePrincipal.setScene(escenaPrincipal);
            } else {
                escenaPrincipal.setRoot(vista);
            }
            controladorVistaActual = loader.getController();
            stagePrincipal.setTitle("KinalLlaves | " + titulo);
            stagePrincipal.setMaximized(true);
            stagePrincipal.show();
        } catch (Exception ex) {
            MensajesUI.error("No se pudo abrir la pantalla: " + rutaFxml
                    + "\n" + MensajesUI.explicar(ex), ex);
        }
    }

    public static void configurarVistaActual(Consumer<Object> configurador) {
        if (controladorVistaActual != null && configurador != null) {
            configurador.accept(controladorVistaActual);
        }
    }

    public static Stage getStagePrincipal() {
        return stagePrincipal;
    }

    private static String dashboardDelRol() {
        if (Sesion.actual() == null) {
            return "login.fxml";
        }
        return switch (Sesion.actual().rol()) {
            case "ADMIN" -> "dashboard_admin.fxml";
            case "JEFE" -> "dashboard_jefe.fxml";
            case "SECRETARIO" -> "dashboard_secretario.fxml";
            default -> "login.fxml";
        };
    }

    private static void abrirLogin() {
        if (Sesion.actual() != null) {
            Sesion.cerrar();
        }
        abrir("login.fxml", "Acceso");
    }

    public static void dashboard() {
        if (Sesion.actual() == null) {
            abrirLogin();
            return;
        }
        String archivo = dashboardDelRol();
        abrir(archivo, "Panel principal");
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

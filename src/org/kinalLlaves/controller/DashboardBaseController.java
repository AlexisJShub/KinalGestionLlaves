package org.kinalllaves.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kinalllaves.dao.OperacionDAO;
import org.kinalllaves.dao.impl.OperacionDAOImpl;
import org.kinalllaves.system.Main;
import org.kinalllaves.util.*;
import java.util.*;

public abstract class DashboardBaseController {

    @FXML
    protected Label lblUsuario, lblRol, kEmpleados, kSalones, kDisponibles, kEntregadas, kReservas, kIncidencias, lblMensaje;
    @FXML
    protected TableView<Map<String, Object>> tablaDisponibilidad, tablaPendientes;

    protected abstract String rol();

    @FXML
    protected void initialize() {
        if (Sesion.actual() == null || !Sesion.actual().rol().equals(rol())) {
            javafx.application.Platform.runLater(() -> {
                Sesion.cerrar();
                Main.abrir("login.fxml", "Acceso");
            });
            return;
        }
        lblUsuario.setText(Sesion.actual().nombre());
        lblRol.setText(Sesion.actual().rol().replace('_', ' '));
        javafx.application.Platform.runLater(this::cargarDatos);
    }

    private void cargarDatos() {
        try {
            OperacionDAO dao = new OperacionDAOImpl();
            Map<String, Integer> k = dao.indicadores();
            kEmpleados.setText("" + k.getOrDefault("empleados", 0));
            kSalones.setText("" + k.getOrDefault("salones", 0));
            kDisponibles.setText("" + k.getOrDefault("disponibles", 0));
            kEntregadas.setText("" + k.getOrDefault("entregadas", 0));
            kReservas.setText("" + k.getOrDefault("reservas", 0));
            kIncidencias.setText("" + k.getOrDefault("incidencias", 0));
            if (tablaDisponibilidad != null) {
                Tablas.mostrar(tablaDisponibilidad, dao.tabla("ESTADOS"), "",
                        "salon", "llave", "estado", "responsable");
            }
            if (tablaPendientes != null) {
                Tablas.mostrar(tablaPendientes, dao.tabla("ENTREGAS").stream()
                        .filter(r -> "ACTIVA".equalsIgnoreCase(Objects.toString(r.get("estado"), ""))).limit(100).toList(), "",
                        "id_entrega", "llave", "empleado", "fecha_entrega", "estado");
            }
            lblMensaje.setText("Última actualización: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + " · datos de MySQL");
        } catch (Exception ex) {
            lblMensaje.setText("No se pudieron cargar datos de MySQL: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    protected void usuarios() {
        Main.modulo("USUARIOS");
    }

    @FXML
    protected void empleados() {
        Main.modulo("EMPLEADOS");
    }

    @FXML
    protected void salones() {
        Main.modulo("SALONES");
    }

    @FXML
    protected void llaves() {
        Main.modulo("LLAVES");
    }

    @FXML
    protected void estados() {
        Main.abrir("estados.fxml", "Estado de llaves");
    }

    @FXML
    protected void entregas() {
        if (Permisos.puede("ENTREGAS")) {
            Main.abrir("operaciones.fxml", "Entrega y devolución");
        }
    }

    @FXML
    protected void incidencias() {
        if (Permisos.puede("INCIDENCIAS")) {
            Main.abrir("incidencias.fxml", "Incidencias");
        }
    }

    @FXML
    protected void auditoria() {
        if (Permisos.puede("AUDITORIA")) {
            Main.abrir("auditoria.fxml", "Auditoría");
        }
    }

    @FXML
    protected void reservas() {
        if (Permisos.puede("RESERVAS")) {
            Main.abrir("reservas.fxml", "Reservas");
        }
    }

    @FXML
    protected void reportes() {
        if (Permisos.puede("REPORTES")) {
            Main.abrir("reportes.fxml", "Reportes");
        }
    }

    @FXML
    protected void actualizar() {
        cargarDatos();
    }

    @FXML
    protected void cambiarClave() {
        Main.abrir("cambiar_clave.fxml", "Cambiar contraseña");
    }

    @FXML
    protected void cerrarSesion() {
        Sesion.cerrar();
        Main.abrir("login.fxml", "Iniciar sesión");
    }
}

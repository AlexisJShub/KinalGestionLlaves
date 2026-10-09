package org.kinalLlaves.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

/**
 * Solicita el carné al operador o al empleado antes de acciones delicadas.
 */
public final class CarnetConfirmacion {

    private CarnetConfirmacion() {
    }

    public static boolean operador(String accion) throws SQLException {
        String sql = "SELECT carnet FROM usuarios WHERE id_usuario=? AND activo=1";
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, Sesion.id());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new SQLException("Tu sesión ya no está activa.");
                }
                String esperado = r.getString(1);
                if (esperado == null || esperado.isBlank()) {
                    throw new SQLException("Tu cuenta no tiene carné registrado. Pide al administrador que lo configure.");
                }
                return pedir("Confirmar " + accion,
                        "Lee o escribe tu carné de usuario para continuar.", esperado);
            }
        }
    }

    public static boolean empleado(long idEmpleado, String accion) throws SQLException {
        String sql = "SELECT carnet,uid_rfid FROM empleados WHERE id_empleado=? AND activo=1";
        try (Connection c = Conexion.getInstancia().conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, idEmpleado);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new SQLException("El empleado no está activo o ya no existe.");
                }
                String carnet = r.getString("carnet");
                String rfid = r.getString("uid_rfid");
                if ((carnet == null || carnet.isBlank()) && (rfid == null || rfid.isBlank())) {
                    throw new SQLException("Ese empleado no tiene carné o RFID. Regístralo primero en Empleados.");
                }
                TextInputDialog dialogo = new TextInputDialog();
                dialogo.setTitle("Confirmación de carné");
                dialogo.setHeaderText("Confirmar " + accion);
                dialogo.setContentText("Acerca el carné del empleado y presiona Enter:");
                var respuesta = dialogo.showAndWait();
                if (respuesta.isEmpty()) {
                    return false;
                }
                String ingresado = respuesta.get().trim();
                boolean coincide = !ingresado.isEmpty() && (ingresado.equals(carnet) || ingresado.equals(rfid));
                if (!coincide) {
                    alertar("El carné leído no coincide con la persona seleccionada.");
                }
                return coincide;
            }
        }
    }

    private static boolean pedir(String titulo, String indicacion, String esperado) {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Confirmación con carné");
        dialogo.setHeaderText(titulo);
        dialogo.setContentText(indicacion);
        var respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty()) {
            return false;
        }
        boolean coincide = !respuesta.get().isBlank() && esperado.equals(respuesta.get().trim());
        if (!coincide) {
            alertar("El carné no coincide con el usuario que inició sesión.");
        }
        return coincide;
    }

    private static void alertar(String texto) {
        new Alert(Alert.AlertType.WARNING, texto, ButtonType.OK).showAndWait();
    }
}

package org.kinalLlaves.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kinalllaves.dao.CatalogoDAO;
import org.kinalLlaves.dao.ReservaDAO;
import org.kinalllaves.dao.impl.CatalogoDAOImpl;
import org.kinalLlaves.dao.impl.ReservaDAOImpl;
import org.kinalllaves.model.Opcion;
import org.kinalllaves.system.Main;
import org.kinalllaves.util.Permisos;

public class ReservasController {
    @FXML private ComboBox<Opcion> salon, empleado;
    @FXML private DatePicker fecha;
    @FXML private ComboBox<String> inicio, fin;
    @FXML private Spinner<Integer> semanas;
    @FXML private TextField observacion;
    @FXML private TableView<Map<String,Object>> tabla;
    @FXML private Label estadoReserva;

    private final CatalogoDAO catalogo = new CatalogoDAOImpl();
    private final ReservaDAO dao = new ReservaDAOImpl();

    @FXML public void initialize() {
        if (!Permisos.puede("RESERVAS")) {
            javafx.application.Platform.runLater(Main::dashboard);
            return;
        }
        fecha.setValue(LocalDate.now());
        inicio.getItems().setAll(horas());
        fin.getItems().setAll(horas());
        inicio.setValue("07:00");
        fin.setValue("09:00");
        semanas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,16,1));
        semanas.setEditable(false);
        tabla.setPlaceholder(new Label("Aún no hay reservas. Selecciona salón, empleado, fecha y hora para crear una."));
        recargar();
    }

    private List<String> horas() {
        List<String> lista = new ArrayList<>();
        for (int hora=6;hora<=21;hora++) for(int minuto:new int[]{0,30})
            lista.add(String.format("%02d:%02d",hora,minuto));
        lista.add("22:00");
        return lista;
    }

    @FXML private void recargar() {
        try {
            Long anteriorSalon = salon.getValue()==null ? null : salon.getValue().id();
            Long anteriorEmpleado = empleado.getValue()==null ? null : empleado.getValue().id();
            salon.getItems().setAll(catalogo.opciones("SALONES"));
            empleado.getItems().setAll(catalogo.opciones("EMPLEADOS"));
            if (anteriorSalon!=null) salon.getItems().stream().filter(x->x.id()==anteriorSalon)
                    .findFirst().ifPresent(salon::setValue);
            if (anteriorEmpleado!=null) empleado.getItems().stream().filter(x->x.id()==anteriorEmpleado)
                    .findFirst().ifPresent(empleado::setValue);
            Tablas.mostrar(tabla, dao.listar(), "", "id_reserva","salon","empleado","inicio","fin","semanas","estado");
        } catch(Exception ex) {
            notificar("No se pudo actualizar la lista: " + ex.getMessage(),true);
        }
    }

    @FXML private void guardar() {
        if (salon.getValue()==null) { notificar("Selecciona el salón que vas a apartar.",true);salon.requestFocus();return; }
        if (empleado.getValue()==null) { notificar("Selecciona al empleado que utilizará el salón.",true);empleado.requestFocus();return; }
        if (fecha.getValue()==null) { notificar("Selecciona la fecha de la reserva.",true);fecha.requestFocus();return; }
        if (inicio.getValue()==null || fin.getValue()==null) { notificar("Elige una hora de inicio y de finalización.",true);return; }
        try {
            LocalDateTime desde=LocalDateTime.of(fecha.getValue(),LocalTime.parse(inicio.getValue()));
            LocalDateTime hasta=LocalDateTime.of(fecha.getValue(),LocalTime.parse(fin.getValue()));
            if (!hasta.isAfter(desde)) {notificar("La hora final debe ser posterior a la de inicio.",true);return;}
            int cantidad=semanas.getValue();
            dao.reservar(salon.getValue().id(), empleado.getValue().id(), desde, hasta, cantidad, observacion.getText().trim());
            notificar(cantidad==1 ? "Reserva guardada correctamente. La lista está actualizada."
                    : "Reserva semanal guardada: " + cantidad + " fechas agrupadas en una sola fila.",false);
            recargar();
        } catch (Exception ex) {
            notificar("No se pudo reservar: " + ex.getMessage(),true);
        }
    }

    @FXML private void cancelar() { cambiar("CANCELADA"); }
    @FXML private void cerrar() { cambiar("CERRADA"); }

    private void cambiar(String estado) {
        var fila=tabla.getSelectionModel().getSelectedItem();
        if (fila==null) {notificar("Selecciona una reserva de la lista primero.",true);return;}
        int cantidad=((Number)fila.getOrDefault("semanas",1)).intValue();
        String accion="CANCELADA".equals(estado)?"cancelar":"cerrar";
        String aviso=cantidad>1
            ? "¿Quieres " + accion + " las " + cantidad + " fechas de esta reserva semanal?"
            : "¿Quieres " + accion + " la reserva seleccionada?";
        if (!org.kinalllaves.util.MensajesUI.confirmar(aviso)) return;
        try {
            dao.cambiarEstado(((Number)fila.get("id_reserva")).longValue(),estado);
            recargar();
            notificar("Reserva " + (estado.equals("CANCELADA")?"cancelada":"cerrada") + " correctamente.",false);
        } catch(Exception ex) {notificar("No se pudo actualizar la reserva: " + ex.getMessage(),true);}
    }

    private void notificar(String texto, boolean error) {
        estadoReserva.setText(texto);
        estadoReserva.getStyleClass().removeAll("inline-error","inline-success");
        estadoReserva.getStyleClass().add(error?"inline-error":"inline-success");
    }

    @FXML private void volver() {Main.dashboard();}
}

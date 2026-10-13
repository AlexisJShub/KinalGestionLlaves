package org.kinalllaves.util;
import javafx.scene.control.Alert;
public final class MensajesUI {
 public static String explicar(Throwable error){
  Throwable causa=error;
  while(causa.getCause()!=null && causa.getCause()!=causa)causa=causa.getCause();
  String texto=causa.getMessage()==null?"Error desconocido":causa.getMessage();
  if(causa instanceof java.sql.SQLException sql){
   return switch(sql.getErrorCode()){
    case 1062 -> "Ese dato ya está registrado (usuario, carné o código repetido).";
    case 1044,1045,1142 -> "Tu usuario de MySQL no tiene permiso. Revisa la conexión.";
    case 1054 -> "La base de datos tiene una estructura diferente al proyecto. Revisa los scripts SQL.";
    case 1146 -> "Falta una tabla en MySQL. Revisa la instalación de la base.";
    case 1451,1452 -> "Este registro tiene datos relacionados. Revisa la selección.";
    default -> texto;
   };
  }
  return texto;
 }

 private MensajesUI(){}
 public static void error(String mensaje, Throwable ex) {
  String detalle="";
  if(ex!=null){
   Throwable causa=ex;
   int pasos=0;
   while(causa.getCause()!=null && causa.getCause()!=causa && pasos++<15) causa=causa.getCause();
   detalle="\nCausa: "+causa.getClass().getSimpleName()+": "+String.valueOf(causa.getMessage());
  }
  new Alert(Alert.AlertType.ERROR,mensaje+detalle).showAndWait();
 }
 public static void info(String mensaje){new Alert(Alert.AlertType.INFORMATION,mensaje).showAndWait();}
 public static boolean confirmar(String mensaje){return new Alert(Alert.AlertType.CONFIRMATION,mensaje,javafx.scene.control.ButtonType.YES,javafx.scene.control.ButtonType.NO).showAndWait().orElse(javafx.scene.control.ButtonType.NO)==javafx.scene.control.ButtonType.YES;}
}

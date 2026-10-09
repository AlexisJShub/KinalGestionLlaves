package org.kinalllaves.controller;
import javafx.fxml.FXML;import javafx.scene.control.*;import org.kinalllaves.dao.UsuarioDAO;
import org.kinalllaves.dao.impl.UsuarioDAOImpl;import org.kinalllaves.system.Main;import org.kinalllaves.util.*;
public class CambiarClaveController {
 @FXML private PasswordField anterior,nueva,repetir;
 private final UsuarioDAO dao=new UsuarioDAOImpl();
 @FXML private void guardar(){try{
  if(!nueva.getText().equals(repetir.getText()))throw new IllegalArgumentException("La confirmación no coincide");
  dao.cambiarClave(Sesion.id(),anterior.getText(),nueva.getText());MensajesUI.info("Contraseña actualizada");Main.dashboard();
 }catch(Exception e){MensajesUI.error("No se pudo cambiar contraseña",e);}}
 @FXML private void volver(){Main.dashboard();}
}

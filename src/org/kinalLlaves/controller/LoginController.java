package org.kinalllaves.controller;
import javafx.fxml.FXML;import javafx.scene.control.*;import org.kinalllaves.dao.UsuarioDAO;
import org.kinalllaves.dao.impl.UsuarioDAOImpl;import org.kinalllaves.system.Main;import org.kinalllaves.util.*;
public class LoginController {
 @FXML private TextField username;@FXML private PasswordField clave;@FXML private Label estado;
 @FXML private Button btnInicial;
 private final UsuarioDAO dao=new UsuarioDAOImpl();
 @FXML public void initialize(){try{boolean vacio=!dao.hayUsuarios();btnInicial.setVisible(vacio);btnInicial.setManaged(vacio);
  estado.setText(vacio?"Primera ejecución: configura la cuenta Administrador.":"Inicia sesión con tu usuario y contraseña.");}
  catch(Exception ex){estado.setText("Sin conexión. Revisa db.properties, MySQL y scripts SQL.");btnInicial.setVisible(false);btnInicial.setManaged(false);}}
 @FXML private void ingresar(){try{if(username.getText().isBlank()||clave.getText().isBlank()){estado.setText("Escribe usuario y contraseña");return;}
  var u=dao.autenticar(username.getText().trim(),clave.getText());if(u==null){estado.setText("Usuario, contraseña o estado inválido");return;}
  Sesion.iniciar(u);Main.dashboard();
 }catch(Exception ex){MensajesUI.error("Error de inicio de sesión",ex);}}
 @FXML private void configurar(){
  try{if(dao.hayUsuarios()){estado.setText("La cuenta inicial ya fue registrada");return;}
   Dialog<ButtonType> dlg=new Dialog<>();dlg.setTitle("Crear administrador inicial");dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
   TextField usuario=new TextField(),nombre=new TextField();PasswordField pass=new PasswordField(),confirmar=new PasswordField();
   usuario.setPromptText("Nombre de usuario");nombre.setPromptText("Nombre completo");pass.setPromptText("Mínimo 8 caracteres");confirmar.setPromptText("Repetir contraseña");
   javafx.scene.layout.VBox v=new javafx.scene.layout.VBox(10,new Label("Usuario"),usuario,new Label("Nombre completo"),nombre,new Label("Contraseña"),pass,new Label("Confirmación"),confirmar);
   v.setPadding(new javafx.geometry.Insets(12));dlg.getDialogPane().setContent(v);
   if(dlg.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK){
    if(usuario.getText().isBlank()||nombre.getText().isBlank()||!pass.getText().equals(confirmar.getText()))throw new IllegalArgumentException("Comprueba usuario, nombre y confirmación");
    dao.primerAdmin(usuario.getText().trim(),nombre.getText().trim(),pass.getText());estado.setText("Administrador creado. Ya puedes iniciar sesión.");
    btnInicial.setVisible(false);btnInicial.setManaged(false);}
  }catch(Exception e){MensajesUI.error("No se pudo crear el administrador",e);}
 }
}

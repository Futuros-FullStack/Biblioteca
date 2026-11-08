package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.UsuarioDAO;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


public class LoginController {

    private static final String ADMINISTRADOR = "Administrador";

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private ComboBox<String> cbRol;
    @FXML
    private VBox boxCredenciales;
    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtContrasena;
    @FXML
    private Label lblError;
    @FXML
    private Button btnIngresar;

    @FXML
    private void initialize() {
        mostrarCredenciales(false);
        btnIngresar.disableProperty().bind(cbRol.valueProperty().isNull());

        cbRol.valueProperty().addListener((obs, anterior, rol) -> {
            mostrarCredenciales(ADMINISTRADOR.equals(rol));
            ocultarError();
        });
        txtUsuario.textProperty().addListener((obs, anterior, texto) -> ocultarError());
        txtContrasena.textProperty().addListener((obs, anterior, texto) -> ocultarError());
    }

    @FXML
    private void ingresar() {
        if (!ADMINISTRADOR.equals(cbRol.getValue())) {
            Sesion.cerrar();
            App.setAdministrador(false);
            App.irA("catalogo");
            return;
        }

        String correo = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText();
        if (correo.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Ingresa tu correo y contraseña.");
            return;
        }
        autenticarAdministrador(correo, contrasena);
    }

   
    private void autenticarAdministrador(String correo, String contrasena) {
        Usuario admin;
        try {
            admin = usuarioDAO.autenticarAdministrador(correo, contrasena);
        } catch (Exception e) {
            mostrarError("No se pudo conectar con la base de datos.");
            return;
        }
        if (admin == null) {
            mostrarError("Correo o contraseña incorrectos.");
            return;
        }
        Sesion.iniciar(admin);
        App.setAdministrador(true);
        App.irA("catalogo");
    }

    private void mostrarCredenciales(boolean mostrar) {
        boxCredenciales.setVisible(mostrar);
        boxCredenciales.setManaged(mostrar);
        if (!mostrar) {
            txtUsuario.clear();
            txtContrasena.clear();
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}

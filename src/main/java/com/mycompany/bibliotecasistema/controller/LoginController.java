package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


public class LoginController {

    private static final String ADMINISTRADOR = "Administrador";

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
        boolean administrador = ADMINISTRADOR.equals(cbRol.getValue());

        if (administrador && (txtUsuario.getText().isBlank() || txtContrasena.getText().isEmpty())) {
            mostrarError("Ingresa tu usuario y contraseña.");
            return;
        }
        App.setAdministrador(administrador);
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

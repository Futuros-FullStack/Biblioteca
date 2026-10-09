package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

/**
 * Login: el usuario y la contraseña solo se piden para Administrador.
 * "Ingresar" entra al catálogo (sin validar) recordando el rol elegido.
 */
public class LoginController {

    private static final String ADMINISTRADOR = "Administrador";

    @FXML
    private ComboBox<String> cbRol;
    @FXML
    private VBox boxCredenciales;
    @FXML
    private Button btnIngresar;

    @FXML
    private void initialize() {
        mostrarCredenciales(false);
        btnIngresar.disableProperty().bind(cbRol.valueProperty().isNull());
        cbRol.valueProperty().addListener((obs, anterior, rol) -> mostrarCredenciales(ADMINISTRADOR.equals(rol)));
    }

    private void mostrarCredenciales(boolean mostrar) {
        boxCredenciales.setVisible(mostrar);
        boxCredenciales.setManaged(mostrar);
    }

    @FXML
    private void ingresar() {
        App.setAdministrador(ADMINISTRADOR.equals(cbRol.getValue()));
        App.irA("catalogo");
    }
}

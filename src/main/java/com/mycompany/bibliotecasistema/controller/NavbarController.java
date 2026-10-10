package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Barra superior: cambia de sección o regresa al login. El estudiante solo ve "Libros". */
public class NavbarController {

    @FXML
    private Button btnPrestamo;
    @FXML
    private Button btnInventario;
    @FXML
    private Label lblIniciales;
    @FXML
    private Label lblNombre;

    @FXML
    private void initialize() {
        boolean admin = App.esAdministrador();
        btnPrestamo.setVisible(admin);
        btnPrestamo.setManaged(admin);
        btnInventario.setVisible(admin);
        btnInventario.setManaged(admin);
        lblIniciales.setText(admin ? "AD" : "ES");
        lblNombre.setText(admin ? "Administrador" : "Estudiante");
    }

    @FXML
    private void irALibros() {
        App.irA("catalogo");
    }

    @FXML
    private void irAPrestamos() {
        App.irA("prestamos");
    }

    @FXML
    private void irAInventario() {
        App.irA("inventario");
    }

    @FXML
    private void salir() {
        App.irA("login");
    }
}

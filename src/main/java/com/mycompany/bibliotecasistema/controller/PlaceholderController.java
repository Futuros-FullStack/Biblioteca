package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.util.Navegador;
import com.mycompany.bibliotecasistema.util.Vista;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Vista temporal "En construcción" para Préstamos, Inventario y Agregar libro.
 */
public class PlaceholderController {

    @FXML
    private NavbarController navbarController;
    @FXML
    private Label lblTitulo;

    public void configurar(String titulo, NavbarController.Seccion seccionActiva) {
        lblTitulo.setText(titulo);
        navbarController.setSeccionActiva(seccionActiva);
    }

    @FXML
    private void volver() {
        Navegador.irA(Vista.CATALOGO);
    }
}

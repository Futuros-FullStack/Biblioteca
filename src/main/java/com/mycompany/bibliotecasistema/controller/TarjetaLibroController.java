package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.model.Libro;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Tarjeta de un libro dentro del catálogo.
 */
public class TarjetaLibroController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblCiudadAnio;
    @FXML
    private Label lblAutores;

    private Libro libro;
    private Consumer<Libro> alSeleccionar;

    public void setLibro(Libro libro, Consumer<Libro> alSeleccionar) {
        this.libro = libro;
        this.alSeleccionar = alSeleccionar;
        lblTitulo.setText(libro.getTitulo());
        lblCiudadAnio.setText(libro.getCiudadAnio());
        lblAutores.setText(libro.getAutoresTexto());
    }

    @FXML
    private void seleccionar() {
        if (alSeleccionar != null) {
            alSeleccionar.accept(libro);
        }
    }
}

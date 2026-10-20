package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.util.PortadaLibro;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class TarjetaLibroController {

    @FXML
    private VBox tarjeta;
    @FXML
    private ImageView imgPortada;
    @FXML
    private HBox portadaTexto;

    // Si el libro tiene portada se muestra la imagen; si no, queda la portada de color con el título
    public void mostrarPortada(Libro libro) {
        Image imagen = PortadaLibro.cargar(libro.getPortada(), 265, 188);
        boolean hayImagen = imagen != null;
        imgPortada.setImage(imagen);
        imgPortada.setVisible(hayImagen);
        imgPortada.setManaged(hayImagen);
        portadaTexto.setVisible(!hayImagen);
        portadaTexto.setManaged(!hayImagen);
    }

    @FXML
    private void abrirDetalle() {
        // implementar lógica de Abrir detalle del libro
    }
}

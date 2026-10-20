package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.util.PortadaLibro;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class TarjetaLibroController {

    private Libro libro;

    @FXML
    private VBox tarjeta;
    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblCiudadAnio;
    @FXML
    private Label lblNombre;
    @FXML
    private Label lblAutores;
    @FXML
    private HBox disponibilidad;
    @FXML
    private Label lblDisponibles;
    @FXML
    private ImageView imgPortada;
    @FXML
    private HBox portadaTexto;

    public void setLibro(Libro libro) {
        this.libro = libro;
        lblTitulo.setText(libro.getTitulo());
        lblCiudadAnio.setText(libro.getEditorial() == null ? "" : libro.getEditorial());
        lblNombre.setText(libro.getTitulo());
        lblAutores.setText(libro.getAutoresTexto());
        int copias = libro.getCopiasDisponibles();
        lblDisponibles.setText(copias == 0 ? "Sin ejemplares disponibles" : copias + (copias == 1 ? " disponible" : " disponibles"));
        disponibilidad.getStyleClass().removeAll("estado-disponible", "estado-prestado");
        disponibilidad.getStyleClass().add(copias == 0 ? "estado-prestado" : "estado-disponible");
        mostrarPortada(libro);
    }

    
    public void mostrarPortada(Libro libro) {
        Image imagen = PortadaLibro.cargar(libro.getPortada(), 188, 170);
        boolean hayImagen = imagen != null;
        imgPortada.setImage(imagen);
        imgPortada.setVisible(hayImagen);
        imgPortada.setManaged(hayImagen);
        portadaTexto.setVisible(!hayImagen);
        portadaTexto.setManaged(!hayImagen);
    }

    @FXML
    private void abrirDetalle() {
        if (libro == null) {
            return;
        }
        FXMLLoader loader = App.prepararVentana("detalle-libro");
        try {
            ((DetalleLibroController) loader.getController()).setLibro(libro);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo cargar el detalle del libro.").showAndWait();
            return;
        }
        App.mostrarVentana(loader, tarjeta);
    }
}

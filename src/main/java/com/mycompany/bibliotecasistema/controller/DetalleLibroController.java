package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.EjemplarDAO;
import com.mycompany.bibliotecasistema.dao.LibroDAO;
import com.mycompany.bibliotecasistema.model.Autor;
import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.util.Tarjetas;
import java.sql.SQLException;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;


public class DetalleLibroController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblDescripcion;
    @FXML
    private Label lblAutor;
    @FXML
    private Label lblIsbn;
    @FXML
    private Label lblEditorial;
    @FXML
    private Label lblAnio;
    @FXML
    private VBox listaEjemplares;

   
    public void setLibro(Libro resumen) throws SQLException {
        Libro libro = new LibroDAO().buscarPorId(resumen.getIdLibro());
        lblTitulo.setText(libro.getTitulo());
        lblDescripcion.setText(libro.getDescripcion() == null ? "Sin descripción" : libro.getDescripcion());
        lblAutor.setText(libro.getAutores().stream().map(Autor::getNombre).collect(Collectors.joining(", ")));
        lblIsbn.setText(libro.getIsbn());
        lblEditorial.setText(libro.getEditorial());
        lblAnio.setText(libro.getAnioPublicacion() == null ? "" : String.valueOf(libro.getAnioPublicacion()));

        listaEjemplares.getChildren().clear();
        for (Ejemplar ejemplar : new EjemplarDAO().listarPorLibro(libro.getIdLibro())) {
            listaEjemplares.getChildren().add(Tarjetas.ejemplar(ejemplar));
        }
    }

    @FXML
    private void cerrar(ActionEvent evento) {
        App.cerrar((Node) evento.getSource());
    }
}

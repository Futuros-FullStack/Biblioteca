package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.EjemplarDAO;
import com.mycompany.bibliotecasistema.dao.LibroDAO;
import com.mycompany.bibliotecasistema.model.Autor;
import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import java.sql.SQLException;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// Ventana con el detalle de un libro y la ubicación de cada ejemplar, que se abre desde el catálogo
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

    // Se llama antes de mostrar la ventana; el catálogo solo trae un resumen, así que se lee el libro completo
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
            listaEjemplares.getChildren().add(crearTarjeta(ejemplar));
        }
    }

    private HBox crearTarjeta(Ejemplar ejemplar) {
        Label codigo = new Label(ejemplar.getCodigoBarras());
        codigo.getStyleClass().add("ejemplar-codigo");
        Label ubicacion = new Label(ejemplar.getUbicacion().toString());
        ubicacion.getStyleClass().add("ejemplar-ubicacion");

        String estado = ejemplar.getEstado().getNombreEstadoEjemplar();
        Label lblEstado = new Label(estado);
        lblEstado.getStyleClass().addAll("estado", "estado-" + (estado.startsWith("EN ") ? "reparacion" : estado.toLowerCase()));

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox tarjeta = new HBox(new VBox(2, codigo, ubicacion), espacio, lblEstado);
        tarjeta.getStyleClass().add("ejemplar-tarjeta");
        return tarjeta;
    }

    @FXML
    private void cerrar(ActionEvent evento) {
        App.cerrar((Node) evento.getSource());
    }
}

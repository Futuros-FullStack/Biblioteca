package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.LibroDAO;
import com.mycompany.bibliotecasistema.model.Libro;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.util.List;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

// Catálogo de libros: una tarjeta por libro, con búsqueda por título, autor o ISBN
public class CatalogoController {

    private final LibroDAO libroDAO = new LibroDAO();

    @FXML
    private TextField txtBusqueda;
    @FXML
    private FlowPane panelLibros;
    @FXML
    private Label lblSinResultados;

    @FXML
    private void initialize() {
        // Se vuelve a consultar cada vez que cambia el texto de búsqueda
        txtBusqueda.textProperty().addListener((obs, anterior, texto) -> cargarLibros());
        cargarLibros();
    }

    // La consulta se hace fuera del hilo de JavaFX para no congelar la ventana
    private void cargarLibros() {
        String busqueda = txtBusqueda.getText().trim();
        Task<List<Libro>> tarea = new Task<>() {
            @Override
            protected List<Libro> call() throws SQLException {
                return libroDAO.buscarCatalogo(busqueda);
            }
        };
        tarea.setOnSucceeded(e -> {
            // Si el usuario siguió escribiendo, este resultado ya no es el vigente
            if (busqueda.equals(txtBusqueda.getText().trim())) {
                mostrar(tarea.getValue(), busqueda.isEmpty()
                        ? "Aún no hay libros registrados."
                        : "No se encontraron libros.");
            }
        });
        tarea.setOnFailed(e -> mostrar(List.of(), "No se pudo cargar el catálogo."));
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrar(List<Libro> libros, String mensajeVacio) {
        panelLibros.getChildren().clear();
        for (Libro libro : libros) {
            panelLibros.getChildren().add(crearTarjeta(libro));
        }
        lblSinResultados.setText(mensajeVacio);
        lblSinResultados.setVisible(libros.isEmpty());
        lblSinResultados.setManaged(libros.isEmpty());
    }

    private Node crearTarjeta(Libro libro) {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("view/tarjeta-libro.fxml"));
        try {
            Node tarjeta = loader.load();
            ((TarjetaLibroController) loader.getController()).setLibro(libro);
            return tarjeta;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar view/tarjeta-libro.fxml", e);
        }
    }
}

package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.dao.LibroDAO;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.util.Tarjetas;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;


public class CatalogoController {

   
    private static final int LIMITE_LIBROS = 100;
    private static final double ANCHO_MINIMO_TARJETA = 190;
    private static final double ESPACIO = 22;
    private static final double RELLENO = 8;

    private final LibroDAO libroDAO = new LibroDAO();
    private final PauseTransition espera = new PauseTransition(Duration.millis(300));

    @FXML
    private TextField txtBusqueda;
    @FXML
    private FlowPane panelLibros;
    @FXML
    private ScrollPane scrollLibros;
    @FXML
    private Label lblSinResultados;
    @FXML
    private Label lblNota;

    @FXML
    private void initialize() {
      
        espera.setOnFinished(e -> cargarLibros());
        txtBusqueda.textProperty().addListener((obs, anterior, texto) -> espera.playFromStart());
        scrollLibros.viewportBoundsProperty().addListener((obs, anterior, medidas) -> ajustarAnchoTarjetas());
        cargarLibros();
    }

   
    private void cargarLibros() {
        String busqueda = txtBusqueda.getText().trim();
        try {
            List<Node> tarjetas = new ArrayList<>();
            for (Libro libro : libroDAO.buscarCatalogo(busqueda, LIMITE_LIBROS)) {
                tarjetas.add(Tarjetas.libro(libro));
            }
            mostrar(tarjetas, busqueda.isEmpty()
                    ? "Aún no hay libros registrados."
                    : "No se encontraron libros.");
        } catch (SQLException e) {
            mostrar(List.of(), "No se pudo cargar el catálogo.");
        }
    }

    private void mostrar(List<Node> tarjetas, String mensajeVacio) {
        panelLibros.getChildren().setAll(tarjetas);
        ajustarAnchoTarjetas();
        boolean recortado = tarjetas.size() >= LIMITE_LIBROS;
        lblNota.setText("Se muestran los primeros " + LIMITE_LIBROS + " libros. Usa el buscador para encontrar otros.");
        lblNota.setVisible(recortado);
        lblNota.setManaged(recortado);
        lblSinResultados.setText(mensajeVacio);
        lblSinResultados.setVisible(tarjetas.isEmpty());
        lblSinResultados.setManaged(tarjetas.isEmpty());
    }

    private void ajustarAnchoTarjetas() {
        double disponible = scrollLibros.getViewportBounds().getWidth() - 2 * RELLENO;
        if (disponible <= 0) {
            return;
        }
        int columnas = Math.max(1, (int) ((disponible + ESPACIO) / (ANCHO_MINIMO_TARJETA + ESPACIO)));
        double ancho = Math.floor((disponible - (columnas - 1) * ESPACIO) / columnas);
        String estilo = "-fx-min-width: " + ancho + "; -fx-pref-width: " + ancho + "; -fx-max-width: " + ancho + ";";
        for (Node tarjeta : panelLibros.getChildren()) {
            tarjeta.setStyle(estilo);
        }
    }
}

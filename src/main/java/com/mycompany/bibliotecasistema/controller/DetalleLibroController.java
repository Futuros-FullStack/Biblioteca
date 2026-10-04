package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.util.Navegador;
import com.mycompany.bibliotecasistema.util.Sesion;
import com.mycompany.bibliotecasistema.util.Vista;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Modal con el detalle de un libro y la lista de sus ejemplares.
 * El administrador puede prestar los ejemplares disponibles.
 */
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
    @FXML
    private Button btnAgregarEjemplar;

    private Libro libro;
    private boolean admin;

    @FXML
    private void initialize() {
        admin = Sesion.get().esAdministrador();
        btnAgregarEjemplar.setVisible(admin);
        btnAgregarEjemplar.setManaged(admin);
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
        lblTitulo.setText(libro.getTitulo());
        lblDescripcion.setText(libro.getDescripcion());
        lblAutor.setText(libro.getAutoresTexto());
        lblIsbn.setText(libro.getIsbn());
        lblEditorial.setText(libro.getEditorial());
        lblAnio.setText(String.valueOf(libro.getAnio()));
        mostrarEjemplares();
    }

    private void mostrarEjemplares() {
        listaEjemplares.getChildren().clear();
        if (libro.getEjemplares().isEmpty()) {
            Label vacio = new Label("Este libro no tiene ejemplares registrados.");
            vacio.getStyleClass().add("sin-ejemplares");
            listaEjemplares.getChildren().add(vacio);
            return;
        }
        for (Ejemplar ejemplar : libro.getEjemplares()) {
            listaEjemplares.getChildren().add(crearTarjetaEjemplar(ejemplar));
        }
    }

    /** Tarjeta de un ejemplar (elemento repetido, se arma por código). */
    private Node crearTarjetaEjemplar(Ejemplar ejemplar) {
        Label codigo = new Label(ejemplar.getCodigo());
        codigo.getStyleClass().add("ejemplar-codigo");
        Label ubicacion = new Label(ejemplar.getUbicacion());
        ubicacion.getStyleClass().add("ejemplar-ubicacion");
        VBox datos = new VBox(2, codigo, ubicacion);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        EstadoEjemplar estado = ejemplar.getEstado();
        Label lblEstado = new Label(estado.getEtiqueta());
        lblEstado.getStyleClass().addAll("estado", estado.getClaseCss());

        HBox tarjeta = new HBox(datos, espacio, lblEstado);
        tarjeta.getStyleClass().add("ejemplar-tarjeta");

        if (admin && ejemplar.estaDisponible()) {
            Button prestar = new Button("Prestar");
            prestar.getStyleClass().add("boton-prestar");
            prestar.setOnAction(e -> prestar(ejemplar));
            tarjeta.getChildren().add(prestar);
        }
        return tarjeta;
    }

    private void prestar(Ejemplar ejemplar) {
        PrestamoController prestamo = Navegador.<PrestamoController>abrirModal(Vista.PRESTAMO,
                listaEjemplares.getScene().getWindow(), c -> c.setDatos(libro, ejemplar));
        if (prestamo.isPrestamoRegistrado()) {
            // Por si la implementación del backend no actualiza el objeto en memoria.
            ejemplar.setEstado(EstadoEjemplar.PRESTADO);
            mostrarEjemplares();
        }
    }

    @FXML
    private void agregarEjemplar() {
        // "Agregar ejemplar" aún no se desarrolla.
        Navegador.mostrarAlerta(Alert.AlertType.INFORMATION, "Agregar ejemplar",
                "Esta función está en construcción.", listaEjemplares.getScene().getWindow());
    }

    @FXML
    private void cerrar() {
        Navegador.cerrarVentana(listaEjemplares);
    }
}

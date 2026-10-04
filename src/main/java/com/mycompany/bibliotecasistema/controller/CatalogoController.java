package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.service.LibroService;
import com.mycompany.bibliotecasistema.service.ServiceLocator;
import com.mycompany.bibliotecasistema.util.Navegador;
import com.mycompany.bibliotecasistema.util.Sesion;
import com.mycompany.bibliotecasistema.util.Texto;
import com.mycompany.bibliotecasistema.util.Vista;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

/**
 * Catálogo de libros. Un solo controlador para ambos roles:
 * el administrador además ve "Agregar libro" y puede prestar desde el detalle.
 */
public class CatalogoController {

    @FXML
    private NavbarController navbarController;
    @FXML
    private TextField txtBusqueda;
    @FXML
    private Button btnAgregarLibro;
    @FXML
    private FlowPane panelLibros;
    @FXML
    private Label lblSinResultados;

    private final LibroService libroService = ServiceLocator.libros();
    private final ObservableList<Libro> libros = FXCollections.observableArrayList();
    private final FilteredList<Libro> librosFiltrados = new FilteredList<>(libros, l -> true);
    /** Las tarjetas se crean una sola vez por libro y se reutilizan al filtrar. */
    private final Map<Libro, Node> tarjetas = new HashMap<>();

    @FXML
    private void initialize() {
        navbarController.setSeccionActiva(NavbarController.Seccion.LIBROS);

        boolean admin = Sesion.get().esAdministrador();
        btnAgregarLibro.setVisible(admin);
        btnAgregarLibro.setManaged(admin);

        txtBusqueda.textProperty().addListener((obs, anterior, texto) -> filtrar(texto));
        librosFiltrados.addListener((ListChangeListener<Libro>) cambio -> mostrarTarjetas());

        libros.setAll(libroService.obtenerLibros());
        mostrarTarjetas();
    }

    private void filtrar(String texto) {
        String filtro = Texto.normalizar(texto);
        // Si se escribe un ISBN, se compara sin guiones ni espacios.
        String filtroIsbn = filtro.matches("[\\d\\s-]+") ? soloDigitos(filtro) : "";
        librosFiltrados.setPredicate(libro -> filtro.isEmpty()
                || Texto.normalizar(libro.getTitulo()).contains(filtro)
                || Texto.normalizar(libro.getAutoresTexto()).contains(filtro)
                || Texto.normalizar(libro.getIsbn()).contains(filtro)
                || (!filtroIsbn.isEmpty() && soloDigitos(libro.getIsbn()).contains(filtroIsbn)));
    }

    private void mostrarTarjetas() {
        List<Node> nodos = librosFiltrados.stream()
                .map(libro -> tarjetas.computeIfAbsent(libro, this::crearTarjeta))
                .collect(Collectors.toList());
        panelLibros.getChildren().setAll(nodos);

        boolean vacio = nodos.isEmpty();
        lblSinResultados.setVisible(vacio);
        lblSinResultados.setManaged(vacio);
    }

    private Node crearTarjeta(Libro libro) {
        return Navegador.<TarjetaLibroController>cargarComponente(Vista.TARJETA_LIBRO,
                c -> c.setLibro(libro, this::abrirDetalle));
    }

    private void abrirDetalle(Libro libro) {
        Navegador.<DetalleLibroController>abrirModal(Vista.DETALLE_LIBRO,
                panelLibros.getScene().getWindow(), c -> c.setLibro(libro));
    }

    @FXML
    private void agregarLibro() {
        // "Agregar libro" aún no se desarrolla.
        Navegador.<PlaceholderController>irA(Vista.PLACEHOLDER,
                c -> c.configurar("Agregar libro", NavbarController.Seccion.LIBROS));
    }

    private static String soloDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }
}

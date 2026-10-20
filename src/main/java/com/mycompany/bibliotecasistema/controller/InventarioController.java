package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.LibroDAO;
import com.mycompany.bibliotecasistema.model.Libro;
import java.sql.SQLException;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;


public class InventarioController {

    private final LibroDAO libroDAO = new LibroDAO();

    @FXML
    private TextField txtBusqueda;
    @FXML
    private Label lblSinResultados;
    @FXML
    private TableView<Libro> tablaInventario;
    @FXML
    private TableColumn<Libro, String> colLibro;
    @FXML
    private TableColumn<Libro, String> colTotal;
    @FXML
    private TableColumn<Libro, String> colDisponibles;
    @FXML
    private TableColumn<Libro, String> colPrestados;
    @FXML
    private TableColumn<Libro, String> colAcciones;

    @FXML
    private void initialize() {
        colLibro.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getTitulo()));
        colTotal.setCellValueFactory(fila -> new SimpleStringProperty(String.valueOf(fila.getValue().getTotalEjemplares())));
        colDisponibles.setCellValueFactory(fila -> new SimpleStringProperty(String.valueOf(fila.getValue().getCopiasDisponibles())));
        colPrestados.setCellValueFactory(fila -> new SimpleStringProperty(String.valueOf(fila.getValue().getCopiasPrestadas())));

        colAcciones.setCellFactory(c -> new TableCell<Libro, String>() {
            @Override
            protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setGraphic(vacia ? null : crearAcciones(getTableView().getItems().get(getIndex())));
            }
        });

        // Se vuelve a consultar cada vez que cambia el texto de búsqueda
        txtBusqueda.textProperty().addListener((obs, anterior, texto) -> cargarInventario());
        cargarInventario();
    }

    // La consulta se hace fuera del hilo de JavaFX para no congelar la ventana
    private void cargarInventario() {
        String busqueda = txtBusqueda.getText().trim();
        Task<List<Libro>> tarea = new Task<>() {
            @Override
            protected List<Libro> call() throws SQLException {
                return libroDAO.buscarInventario(busqueda);
            }
        };
        tarea.setOnSucceeded(e -> {
            // Si el usuario siguió escribiendo, este resultado ya no es el vigente
            if (busqueda.equals(txtBusqueda.getText().trim())) {
                lblSinResultados.setText(busqueda.isEmpty()
                        ? "Aún no hay libros registrados."
                        : "No se encontraron libros.");
                tablaInventario.setItems(FXCollections.observableArrayList(tarea.getValue()));
                // Libro.equals compara solo el id: sin refresh() las celdas conservan los conteos anteriores
                tablaInventario.refresh();
            }
        });
        tarea.setOnFailed(e -> {
            lblSinResultados.setText("No se pudo cargar el inventario.");
            tablaInventario.getItems().clear();
        });
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void agregarLibro() {
        App.abrirVentana("agregar-libro", tablaInventario);
        cargarInventario();
    }

    private void verEjemplares(Libro libro) {
        FXMLLoader loader = App.prepararVentana("ejemplares-libro");
        EjemplaresLibroController detalle = loader.getController();
        detalle.setLibro(libro);
        detalle.setAlCambiar(this::cargarInventario);
        App.mostrarVentana(loader, tablaInventario);
    }

    private void agregarEjemplar(Libro libro) {
        FXMLLoader loader = App.prepararVentana("ejemplar");
        EjemplarController ventana = loader.getController();
        ventana.prepararAgregar(libro);
        App.mostrarVentana(loader, tablaInventario);
        cargarInventario();
    }

    // Con ejemplares no se puede eliminar; sin ejemplares se borra definitivamente
    private void eliminarLibro(Libro libro) {
        if (libro.getTotalEjemplares() > 0) {
            avisar(Alert.AlertType.WARNING, "No se puede eliminar \"" + libro.getTitulo()
                    + "\" porque tiene ejemplares asociados. Elimina primero sus ejemplares.");
            return;
        }
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "\"" + libro.getTitulo() + "\" se borrará de su inventario completamente. ¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        alerta.initOwner(tablaInventario.getScene().getWindow());
        alerta.setHeaderText(null);
        if (alerta.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            libroDAO.eliminar(libro.getIdLibro());
            cargarInventario();
        } catch (SQLException e) {
            avisar(Alert.AlertType.ERROR, "No se pudo eliminar el libro.");
        }
    }

    private void avisar(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.initOwner(tablaInventario.getScene().getWindow());
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    private HBox crearAcciones(Libro libro) {
        Button ver = new Button("Ver ejemplares");
        ver.getStyleClass().add("boton-tabla");
        ver.setOnAction(e -> verEjemplares(libro));

        Button ejemplar = new Button("+ Ejemplar");
        ejemplar.getStyleClass().add("boton-tabla-secundario");
        ejemplar.setOnAction(e -> agregarEjemplar(libro));

        Button eliminar = EjemplaresLibroController.botonIcono("icono-eliminar");
        eliminar.getStyleClass().add("peligro");
        eliminar.setOnAction(e -> eliminarLibro(libro));

        HBox acciones = new HBox(6, ver, ejemplar, eliminar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        return acciones;
    }
}

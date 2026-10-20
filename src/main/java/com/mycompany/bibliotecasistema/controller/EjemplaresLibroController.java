package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.EjemplarDAO;
import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

// Ventana con todos los ejemplares de un libro, que se abre desde el inventario
public class EjemplaresLibroController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();

    private Libro libro;
    // Lo usa el inventario para actualizar sus conteos cuando cambia algún ejemplar
    private Runnable alCambiar = () -> { };

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblMensaje;
    @FXML
    private Label lblSinEjemplares;
    @FXML
    private TableView<Ejemplar> tablaEjemplares;
    @FXML
    private TableColumn<Ejemplar, String> colCodigo;
    @FXML
    private TableColumn<Ejemplar, String> colUbicacion;
    @FXML
    private TableColumn<Ejemplar, String> colAdquirido;
    @FXML
    private TableColumn<Ejemplar, String> colEstado;
    @FXML
    private TableColumn<Ejemplar, String> colAcciones;

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getCodigoBarras()));
        colUbicacion.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getUbicacion().toString()));
        colAdquirido.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getFechaAdquisicion() == null ? "" : fila.getValue().getFechaAdquisicion().format(FORMATO_FECHA)));
        colEstado.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getEstado().getNombreEstadoEjemplar()));

        colAcciones.setCellFactory(c -> new TableCell<Ejemplar, String>() {
            @Override
            protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setGraphic(vacia ? null : crearAcciones(getTableView().getItems().get(getIndex())));
            }
        });
    }

    // Se llama antes de mostrar la ventana, con el libro elegido
    public void setLibro(Libro libro) {
        this.libro = libro;
        lblTitulo.setText(libro.getTitulo());
        cargarEjemplares();
    }

    public void setAlCambiar(Runnable alCambiar) {
        this.alCambiar = alCambiar;
    }

    // Después de agregar, editar o eliminar: actualiza esta tabla y la del inventario
    private void recargar() {
        cargarEjemplares();
        alCambiar.run();
    }

    private void cargarEjemplares() {
        try {
            tablaEjemplares.setItems(FXCollections.observableArrayList(ejemplarDAO.listarPorLibro(libro.getIdLibro())));
            // Ejemplar.equals compara solo el id: sin refresh() no se ven los cambios de estado o fecha
            tablaEjemplares.refresh();
        } catch (SQLException e) {
            lblSinEjemplares.setText("No se pudieron cargar los ejemplares.");
        }
    }

    private void editarEjemplar(Ejemplar ejemplar) {
        ocultarMensaje();
        if (estaPrestado(ejemplar)) {
            mostrarMensaje("No se puede editar un ejemplar que está prestado.");
            return;
        }
        FXMLLoader loader = App.prepararVentana("ejemplar");
        EjemplarController ventana = loader.getController();
        ventana.prepararEditar(ejemplar);
        App.mostrarVentana(loader, tablaEjemplares);
        recargar();
    }

    // Sin historial se borra de verdad; con historial solo se marca como PERDIDO
    private void eliminarEjemplar(Ejemplar ejemplar) {
        ocultarMensaje();
        if (estaPrestado(ejemplar)) {
            mostrarMensaje("No se puede eliminar un ejemplar que está prestado.");
            return;
        }
        try {
            boolean borrar = !ejemplarDAO.tieneHistorial(ejemplar.getIdEjemplar());
            if (!borrar && EstadoEjemplar.PERDIDO.equals(ejemplar.getEstado().getNombreEstadoEjemplar())) {
                mostrarMensaje("Este ejemplar tiene historial y ya está marcado como PERDIDO.");
                return;
            }
            String pregunta = borrar
                    ? "¿Eliminar el ejemplar " + ejemplar.getCodigoBarras() + " definitivamente?"
                    : "El ejemplar " + ejemplar.getCodigoBarras() + " tiene historial y no se puede borrar.\n"
                    + "¿Marcarlo como PERDIDO?";
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, pregunta, ButtonType.OK, ButtonType.CANCEL);
            alerta.initOwner(tablaEjemplares.getScene().getWindow());
            alerta.setHeaderText(null);
            if (alerta.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            if (borrar) {
                ejemplarDAO.eliminar(ejemplar.getIdEjemplar());
            } else {
                ejemplarDAO.cambiarEstado(ejemplar.getIdEjemplar(), EstadoEjemplar.PERDIDO);
            }
            recargar();
        } catch (SQLException e) {
            mostrarMensaje("No se pudo eliminar el ejemplar.");
        }
    }

    private boolean estaPrestado(Ejemplar ejemplar) {
        return EstadoEjemplar.PRESTADO.equals(ejemplar.getEstado().getNombreEstadoEjemplar());
    }

    private HBox crearAcciones(Ejemplar ejemplar) {
        Button editar = botonIcono("icono-editar");
        editar.setOnAction(e -> editarEjemplar(ejemplar));

        Button eliminar = botonIcono("icono-eliminar");
        eliminar.getStyleClass().add("peligro");
        eliminar.setOnAction(e -> eliminarEjemplar(ejemplar));

        HBox acciones = new HBox(6, editar, eliminar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        return acciones;
    }

    static Button botonIcono(String claseIcono) {
        Region icono = new Region();
        icono.getStyleClass().addAll("icono", claseIcono);
        Button boton = new Button();
        boton.setGraphic(icono);
        boton.getStyleClass().add("boton-icono");
        return boton;
    }

    private void mostrarMensaje(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);
    }

    @FXML
    private void cerrar() {
        App.cerrar(tablaEjemplares);
    }
}

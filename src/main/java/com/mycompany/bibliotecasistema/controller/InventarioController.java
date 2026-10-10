package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * Inventario: abre "Agregar libro" y, desde el renglón de ejemplo,
 * "Agregar ejemplar", "Editar ejemplar" y la confirmación de eliminar.
 */
public class InventarioController {

    @FXML
    private TableView<String> tablaInventario;
    @FXML
    private TableColumn<String, String> colCodigo;
    @FXML
    private TableColumn<String, String> colLibro;
    @FXML
    private TableColumn<String, String> colUbicacion;
    @FXML
    private TableColumn<String, String> colAdquirido;
    @FXML
    private TableColumn<String, String> colEstado;
    @FXML
    private TableColumn<String, String> colAcciones;

    @FXML
    private void initialize() {
        texto(colCodigo, "[Código]");
        texto(colLibro, "[Título del libro]");
        texto(colUbicacion, "Pasillo [A] - Estante [1] - Fila [1]");
        texto(colAdquirido, "[Fecha]");
        texto(colEstado, "[Estado]");
        colAcciones.setCellFactory(c -> new TableCell<String, String>() {
            @Override
            protected void updateItem(String fila, boolean vacia) {
                super.updateItem(fila, vacia);
                setGraphic(vacia ? null : crearAcciones());
            }
        });
        tablaInventario.getItems().setAll("ejemplo");
    }

    @FXML
    private void agregarLibro() {
        App.abrirModal("agregar-libro", tablaInventario);
    }

    private HBox crearAcciones() {
        Button ejemplar = new Button("+ Ejemplar");
        ejemplar.getStyleClass().add("boton-tabla-secundario");
        ejemplar.setOnAction(e -> App.abrirModal("ejemplar", tablaInventario));

        Button editar = botonIcono("icono-editar");
        editar.setOnAction(e -> {
            FXMLLoader modal = App.prepararModal("ejemplar");
            ((EjemplarController) modal.getController()).modoEditar();
            App.mostrarModal(modal, tablaInventario);
        });

        Button eliminar = botonIcono("icono-eliminar");
        eliminar.getStyleClass().add("peligro");
        eliminar.setOnAction(e -> confirmarEliminar());

        HBox acciones = new HBox(6, ejemplar, editar, eliminar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        return acciones;
    }

    private void confirmarEliminar() {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "Se eliminará el ejemplar [Código] de \u201c[Título del libro]\u201d.");
        alerta.initOwner(tablaInventario.getScene().getWindow());
        alerta.setTitle("Eliminar ejemplar");
        alerta.setHeaderText("Eliminar ejemplar");
        alerta.showAndWait();
    }

    private static Button botonIcono(String claseIcono) {
        Region icono = new Region();
        icono.getStyleClass().addAll("icono", claseIcono);
        Button boton = new Button();
        boton.setGraphic(icono);
        boton.getStyleClass().add("boton-icono");
        return boton;
    }

    private static void texto(TableColumn<String, String> columna, String texto) {
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(texto));
    }
}

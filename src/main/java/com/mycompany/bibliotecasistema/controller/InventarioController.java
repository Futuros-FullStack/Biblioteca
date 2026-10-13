package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;


public class InventarioController {

    @FXML
    private TableView<Object> tablaInventario;
    @FXML
    private TableColumn<Object, String> colCodigo;
    @FXML
    private TableColumn<Object, String> colLibro;
    @FXML
    private TableColumn<Object, String> colUbicacion;
    @FXML
    private TableColumn<Object, String> colAdquirido;
    @FXML
    private TableColumn<Object, String> colEstado;
    @FXML
    private TableColumn<Object, String> colAcciones;

    @FXML
    private void initialize() {
        colAcciones.setCellFactory(c -> new TableCell<Object, String>() {
            @Override
            protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setGraphic(vacia ? null : crearAcciones());
            }
        });

        // cargar los ejemplares.
    }

    @FXML
    private void agregarLibro() {
        App.abrirVentana("agregar-libro", tablaInventario);
    }


    private HBox crearAcciones() {

        Button ejemplar = new Button("+ Ejemplar");
        ejemplar.getStyleClass().add("boton-tabla-secundario");
        ejemplar.setOnAction(e -> {
            // agregar ejemplar
        });

        Button editar = botonIcono("icono-editar");
        editar.setOnAction(e -> {
            // editar ejemplar
        });

        Button eliminar = botonIcono("icono-eliminar");
        eliminar.getStyleClass().add("peligro");
        eliminar.setOnAction(e -> {
            // eliminar ejemplar
        });

        HBox acciones = new HBox(6, ejemplar, editar, eliminar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        return acciones;
    }

    private static Button botonIcono(String claseIcono) {
        Region icono = new Region();
        icono.getStyleClass().addAll("icono", claseIcono);
        Button boton = new Button();
        boton.setGraphic(icono);
        boton.getStyleClass().add("boton-icono");
        return boton;
    }
}

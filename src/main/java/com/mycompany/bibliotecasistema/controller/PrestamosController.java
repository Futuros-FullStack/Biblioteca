package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class PrestamosController {

    @FXML
    private ToggleButton tabEntregas;
    @FXML
    private ToggleButton tabAlumnos;
    @FXML
    private VBox panelEntregas;
    @FXML
    private VBox panelAlumnos;
    @FXML
    private TextField txtBuscarAlumno;
    @FXML
    private TableView<Object> tablaEntregas;
    @FXML
    private TableColumn<Object, String> colAlumno;
    @FXML
    private TableColumn<Object, String> colLibro;
    @FXML
    private TableColumn<Object, String> colEntrega;
    @FXML
    private TableColumn<Object, String> colEstado;
    @FXML
    private TableColumn<Object, String> colAccionEntrega;
    @FXML
    private TableView<Object> tablaAlumnos;
    @FXML
    private TableColumn<Object, String> colMatricula;
    @FXML
    private TableColumn<Object, String> colNombre;
    @FXML
    private TableColumn<Object, String> colActivos;
    @FXML
    private TableColumn<Object, String> colTotal;
    @FXML
    private TableColumn<Object, String> colSituacion;
    @FXML
    private TableColumn<Object, String> colAccionAlumno;

    @FXML
    private void initialize() {

        ToggleGroup pestanas = new ToggleGroup();
        tabEntregas.setToggleGroup(pestanas);
        tabAlumnos.setToggleGroup(pestanas);
        pestanas.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                pestanas.selectToggle(anterior);
            } else {
                panelEntregas.setVisible(actual == tabEntregas);
                panelAlumnos.setVisible(actual == tabAlumnos);
            }
        });

        // Tabla: Próximas entregas
        columnaBoton(colAccionEntrega, "Registrar devolución");

        // Tabla: Alumnos
        columnaBoton(colAccionAlumno, "Prestar / ver  ›");
    }

    @FXML
    private void nuevoPrestamo() {
        tabAlumnos.setSelected(true);
        txtBuscarAlumno.requestFocus();
    }

    @FXML
    private void registrarAlumno() {
        App.abrirVentana("registrar-alumno", tablaAlumnos);
    }


    private static <S> void columnaBoton(TableColumn<S, String> columna, String texto) {
        columna.setCellFactory(c -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                if (vacia) {
                    setGraphic(null);
                } else {
                    Button boton = new Button(texto);
                    boton.getStyleClass().add("boton-tabla");
                    boton.setOnAction(e -> {
                        // acción de fila
                    });
                    setGraphic(boton);
                }
            }
        });
    }
}

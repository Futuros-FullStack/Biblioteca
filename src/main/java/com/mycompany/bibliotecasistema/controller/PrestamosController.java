package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 * Préstamos: cambia de pestaña y abre los modales.
 * Cada tabla tiene un renglón de ejemplo (solo textos entre corchetes)
 * para poder probar sus botones.
 */
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
    private TableView<String> tablaEntregas;
    @FXML
    private TableColumn<String, String> colAlumno;
    @FXML
    private TableColumn<String, String> colLibro;
    @FXML
    private TableColumn<String, String> colEntrega;
    @FXML
    private TableColumn<String, String> colEstado;
    @FXML
    private TableColumn<String, String> colAccionEntrega;

    @FXML
    private TableView<String> tablaAlumnos;
    @FXML
    private TableColumn<String, String> colMatricula;
    @FXML
    private TableColumn<String, String> colNombre;
    @FXML
    private TableColumn<String, String> colActivos;
    @FXML
    private TableColumn<String, String> colTotal;
    @FXML
    private TableColumn<String, String> colSituacion;
    @FXML
    private TableColumn<String, String> colAccionAlumno;

    @FXML
    private void initialize() {
        ToggleGroup pestanas = new ToggleGroup();
        tabEntregas.setToggleGroup(pestanas);
        tabAlumnos.setToggleGroup(pestanas);
        pestanas.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                pestanas.selectToggle(anterior); // siempre queda una pestaña elegida
            } else {
                panelEntregas.setVisible(actual == tabEntregas);
                panelAlumnos.setVisible(actual == tabAlumnos);
            }
        });

        // Renglón de ejemplo: Próximas entregas
        texto(colAlumno, "[Alumno]");
        texto(colLibro, "[Título del libro] · [Código]");
        texto(colEntrega, "[Fecha]");
        texto(colEstado, "[Estado]");
        boton(colAccionEntrega, "Registrar devolución", () -> App.abrirModal("devolucion", tablaEntregas));
        tablaEntregas.getItems().setAll("ejemplo");

        // Renglón de ejemplo: Alumnos
        texto(colMatricula, "[Matrícula]");
        texto(colNombre, "[Nombre del alumno]");
        texto(colActivos, "[N]");
        texto(colTotal, "[N]");
        texto(colSituacion, "[Situación]");
        boton(colAccionAlumno, "Prestar / ver  \u203a", this::abrirAlumno);
        tablaAlumnos.getItems().setAll("ejemplo");
    }

    @FXML
    private void nuevoPrestamo() {
        tabAlumnos.setSelected(true);
        txtBuscarAlumno.requestFocus();
    }

    @FXML
    private void registrarAlumno() {
        App.abrirModal("registrar-alumno", tablaAlumnos);
    }

    private void abrirAlumno() {
        DetalleAlumnoController ficha = App.abrirModal("detalle-alumno", tablaAlumnos);
        if (ficha.isPrestarSolicitado()) {
            App.abrirModal("nuevo-prestamo", tablaAlumnos);
        }
    }

    /** Columna que muestra siempre el mismo texto. */
    private static void texto(TableColumn<String, String> columna, String texto) {
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(texto));
    }

    /** Columna con un botón que ejecuta la acción indicada. */
    private static void boton(TableColumn<String, String> columna, String texto, Runnable accion) {
        columna.setCellFactory(c -> new TableCell<String, String>() {
            @Override
            protected void updateItem(String fila, boolean vacia) {
                super.updateItem(fila, vacia);
                if (vacia) {
                    setGraphic(null);
                } else {
                    Button boton = new Button(texto);
                    boton.getStyleClass().add("boton-tabla");
                    boton.setOnAction(e -> accion.run());
                    setGraphic(boton);
                }
            }
        });
    }
}

package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Modal de ejemplar: por defecto es "Agregar ejemplar"; modoEditar() lo cambia a "Editar ejemplar". */
public class EjemplarController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private VBox boxEstado;
    @FXML
    private Button btnGuardar;

    public void modoEditar() {
        lblTitulo.setText("Editar ejemplar");
        lblSubtitulo.setText("[Título del libro] · [Código]");
        btnGuardar.setText("Guardar cambios");
        boxEstado.setVisible(true);
        boxEstado.setManaged(true);
    }

    @FXML
    private void cerrar() {
        App.cerrar(lblTitulo);
    }
}

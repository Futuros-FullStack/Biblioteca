package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Ficha del alumno: abre "Notificar" o pasa al modal de préstamo. */
public class DetalleAlumnoController {

    @FXML
    private Label lblNombre;

    /** true si se eligió "Realizar otro préstamo" (PrestamosController abre el préstamo). */
    private boolean prestarSolicitado;

    public boolean isPrestarSolicitado() {
        return prestarSolicitado;
    }

    @FXML
    private void notificar() {
        App.abrirModal("notificar", lblNombre);
    }

    @FXML
    private void prestar() {
        prestarSolicitado = true;
        cerrar();
    }

    @FXML
    private void cerrar() {
        App.cerrar(lblNombre);
    }
}

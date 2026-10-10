package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;

/**
 * Controlador compartido por los modales que solo se cierran
 * (detalle del libro, devolución, registrar alumno, notificar, préstamo y agregar libro).
 * Tanto "Cancelar" como el botón principal cierran el modal.
 */
public class ModalController {

    @FXML
    private void cerrar(ActionEvent evento) {
        App.cerrar((Node) evento.getSource());
    }
}

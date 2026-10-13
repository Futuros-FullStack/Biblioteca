package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;


public class VentanaEmergenteController {

    @FXML
    private void cerrar(ActionEvent evento) {

        App.cerrar((Node) evento.getSource());
    }

    @FXML
    private void guardar() {
        // Implementar lógica de Guardar (alumno, libro)
    }
}

package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/** Tarjeta del catálogo: al dar clic abre el detalle del libro. */
public class TarjetaLibroController {

    @FXML
    private VBox tarjeta;

    @FXML
    private void abrirDetalle() {
        App.abrirModal("detalle-libro", tarjeta);
    }
}

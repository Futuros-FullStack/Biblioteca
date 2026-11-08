package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.EjemplarDAO;
import com.mycompany.bibliotecasistema.dao.UbicacionDAO;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Ubicacion;
import com.mycompany.bibliotecasistema.util.Sesion;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;


public class ReubicarLibroController {

    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();
    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();

    private Libro libro;
    private Ubicacion actual;

    @FXML
    private Label lblSubtitulo;
    @FXML
    private TextField txtPasillo;
    @FXML
    private TextField txtEstante;
    @FXML
    private TextField txtFila;
    @FXML
    private TextField txtMotivo;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    
    public void prepararReubicar(Libro libro) {
        this.libro = libro;
        try {
            actual = ubicacionDAO.buscarPorLibro(libro.getIdLibro());
        } catch (SQLException e) {
            btnGuardar.setDisable(true);
            mostrarError("No se pudo cargar la ubicación actual.");
            return;
        }
        if (actual == null) {
            btnGuardar.setDisable(true);
            mostrarError("Este libro no tiene ejemplares que reubicar.");
            return;
        }
        lblSubtitulo.setText("«" + libro.getTitulo() + "» · ahora en " + actual);
    }

    @FXML
    private void guardar() {
        String pasillo = txtPasillo.getText().trim();
        String estante = txtEstante.getText().trim();
        String fila = txtFila.getText().trim();
        String motivo = txtMotivo.getText().trim();
        if (pasillo.isEmpty() || estante.isEmpty() || fila.isEmpty() || motivo.isEmpty()) {
            mostrarError("Captura pasillo, estante, fila y el motivo.");
            return;
        }
        try {
            Ubicacion nueva = ubicacionDAO.buscarOCrear(pasillo, estante, fila);
            if (nueva.getIdUbicacion().equals(actual.getIdUbicacion())) {
                mostrarError("El libro ya está en esa ubicación.");
                return;
            }
            ejemplarDAO.reubicarLibro(libro.getIdLibro(), nueva.getIdUbicacion(),
                    Sesion.getAdministrador().getIdUsuario(), motivo);
            App.cerrar(btnGuardar);
        } catch (SQLException e) {
            mostrarError("No se pudo reubicar el libro.");
        }
    }

    @FXML
    private void cerrar() {
        App.cerrar(btnGuardar);
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

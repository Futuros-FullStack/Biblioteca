package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.EjemplarDAO;
import com.mycompany.bibliotecasistema.dao.UbicacionDAO;
import com.mycompany.bibliotecasistema.dao.UsuarioDAO;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Ubicacion;
import com.mycompany.bibliotecasistema.model.Usuario;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;


public class ReposicionController {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();
    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();

    private Usuario alumno;
    private Libro libro;
    private Ubicacion ubicacion;

    @FXML
    private Label lblSubtitulo;
    @FXML
    private TextField txtCodigo;
    @FXML
    private DatePicker dpAdquisicion;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    
    public void prepararReposicion(Usuario alumno) {
        this.alumno = alumno;
        dpAdquisicion.setValue(LocalDate.now());
        btnGuardar.setDisable(true);
        try {
            libro = usuarioDAO.buscarLibroPerdido(alumno.getIdUsuario());
            if (libro == null) {
                mostrarError("No se encontró el libro perdido de este alumno.");
                return;
            }
            
            ubicacion = ubicacionDAO.buscarPorLibro(libro.getIdLibro());
            txtCodigo.setText(ejemplarDAO.siguienteCodigoBarras());
            lblSubtitulo.setText(alumno.getNombre() + " · repone «" + libro.getTitulo() + "»");
            btnGuardar.setDisable(false);
        } catch (SQLException e) {
            mostrarError("No se pudo cargar la información.");
        }
    }

    @FXML
    private void guardar() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty() || dpAdquisicion.getValue() == null) {
            mostrarError("Captura el código de barras y la fecha de adquisición.");
            return;
        }
        try {
            usuarioDAO.registrarReposicion(alumno.getIdUsuario(), libro.getIdLibro(),
                    ubicacion.getIdUbicacion(), codigo, dpAdquisicion.getValue());
            alumno.setBloqueado(false);
            App.cerrar(btnGuardar);
        } catch (SQLException e) {
            mostrarError(e.getMessage() != null && e.getMessage().contains("codigo_barras")
                    ? "Ese código de barras ya existe." : "No se pudo registrar la reposición.");
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

package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.UsuarioDAO;
import com.mycompany.bibliotecasistema.model.Usuario;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class RegistrarAlumnoController {

    private static final Pattern MATRICULA = Pattern.compile("\\d{9}");
    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtMatricula;
    @FXML
    private TextField txtCorreo;
    @FXML
    private Label lblError;
    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private Button btnGuardar;

    private Usuario alumnoEditado;

    
    public void prepararEditar(Usuario alumno) {
        alumnoEditado = alumno;
        lblTitulo.setText("Editar alumno");
        lblSubtitulo.setText("Modifica los datos del alumno y guarda los cambios.");
        btnGuardar.setText("Guardar cambios");
        txtNombre.setText(alumno.getNombre());
        txtMatricula.setText(alumno.getMatricula());
        txtCorreo.setText(alumno.getCorreo());
    }

    @FXML
    private void initialize() {
        txtNombre.textProperty().addListener((obs, anterior, texto) -> lblError.setText(""));
        txtMatricula.textProperty().addListener((obs, anterior, texto) -> lblError.setText(""));
        txtCorreo.textProperty().addListener((obs, anterior, texto) -> lblError.setText(""));
    }

    @FXML
    private void cerrar(ActionEvent evento) {
        App.cerrar((Node) evento.getSource());
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String matricula = txtMatricula.getText().trim();
        String correo = txtCorreo.getText().trim();

        if (nombre.isEmpty() || matricula.isEmpty() || correo.isEmpty()) {
            lblError.setText("Captura el nombre, la matrícula y el correo.");
            return;
        }
        if (!MATRICULA.matcher(matricula).matches()) {
            lblError.setText("La matrícula debe tener exactamente 9 dígitos.");
            return;
        }
        if (!CORREO.matcher(correo).matches()) {
            lblError.setText("Escribe un correo válido.");
            return;
        }

        Usuario alumno = new Usuario();
        alumno.setNombre(nombre);
        alumno.setMatricula(matricula);
        alumno.setCorreo(correo);
        if (alumnoEditado != null) {
            alumno.setIdUsuario(alumnoEditado.getIdUsuario());
        }
        guardarEnBaseDeDatos(alumno);
    }

    
    private void guardarEnBaseDeDatos(Usuario alumno) {
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (alumnoEditado == null) {
                    usuarioDAO.insertarAlumno(alumno);
                } else {
                    usuarioDAO.actualizarAlumno(alumno);
                }
                return null;
            }
        };
        tarea.setOnSucceeded(e -> App.cerrar(txtNombre));
        tarea.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            lblError.setText(mensajeDeError(tarea.getException()));
        });

        btnGuardar.setDisable(true);
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    
    private String mensajeDeError(Throwable error) {
        if (error instanceof SQLException && error.getMessage() != null) {
            String detalle = error.getMessage();
            if (detalle.contains("usuarios_matricula_key")) {
                return "Ya existe un alumno con esa matrícula.";
            }
            if (detalle.contains("usuarios_correo_key")) {
                return "Ya existe un usuario con ese correo.";
            }
            if (detalle.contains("check_correo")) {
                return "Escribe un correo válido.";
            }
        }
        return "No se pudo guardar el alumno.";
    }
}

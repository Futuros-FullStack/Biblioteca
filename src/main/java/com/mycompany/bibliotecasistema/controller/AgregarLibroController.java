package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.LibroDAO;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.util.PortadaLibro;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class AgregarLibroController {

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    private final LibroDAO libroDAO = new LibroDAO();

    @FXML
    private TextField txtTitulo;
    @FXML
    private TextField txtAutores;
    @FXML
    private TextArea txtDescripcion;
    @FXML
    private TextField txtIsbn;
    @FXML
    private TextField txtEditorial;
    @FXML
    private TextField txtAnio;
    @FXML
    private ImageView imgPortada;
    @FXML
    private Label lblArchivoPortada;
    @FXML
    private Button btnQuitarPortada;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    // Imagen elegida por el usuario; null mientras no elija ninguna (la portada es opcional)
    private File archivoPortada;

    @FXML
    private void initialize() {
        btnQuitarPortada.setVisible(false);
        btnQuitarPortada.setManaged(false);
    }

    @FXML
    private void cerrar(ActionEvent evento) {
        App.cerrar((Node) evento.getSource());
    }

    @FXML
    private void elegirPortada(ActionEvent evento) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir portada del libro");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File archivo = selector.showOpenDialog(((Node) evento.getSource()).getScene().getWindow());
        if (archivo == null) {
            return;
        }
        if (archivo.length() > TAMANO_MAXIMO) {
            lblError.setText("La imagen no debe pesar más de 5 MB.");
            return;
        }
        lblError.setText("");
        archivoPortada = archivo;
        imgPortada.setImage(new Image(archivo.toURI().toString(), 120, 160, true, true));
        lblArchivoPortada.setText(archivo.getName());
        btnQuitarPortada.setVisible(true);
        btnQuitarPortada.setManaged(true);
    }

    @FXML
    private void quitarPortada() {
        archivoPortada = null;
        imgPortada.setImage(null);
        lblArchivoPortada.setText("Sin imagen");
        btnQuitarPortada.setVisible(false);
        btnQuitarPortada.setManaged(false);
    }

    @FXML
    private void guardar() {
        String titulo = txtTitulo.getText().trim();
        String autores = txtAutores.getText().trim();

        if (titulo.isEmpty() || autores.isEmpty()) {
            lblError.setText("Captura el título y al menos un autor.");
            return;
        }

        Integer anio = null;
        if (!txtAnio.getText().isBlank()) {
            try {
                anio = Integer.parseInt(txtAnio.getText().trim());
            } catch (NumberFormatException e) {
                lblError.setText("El año debe ser un número.");
                return;
            }
        }

        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setIsbn(textoOpcional(txtIsbn.getText()));
        libro.setEditorial(textoOpcional(txtEditorial.getText()));
        libro.setAnioPublicacion(anio);
        libro.setDescripcion(textoOpcional(txtDescripcion.getText()));

        List<String> nombresAutores = new ArrayList<>();
        for (String nombre : autores.split(",")) {
            if (!nombre.isBlank()) {
                nombresAutores.add(nombre.trim());
            }
        }
        guardarEnBaseDeDatos(libro, nombresAutores);
    }

    // Devuelve el texto del campo o null si está vacío
    private String textoOpcional(String valor) {
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }

    // El guardado se hace fuera del hilo de JavaFX para no congelar la ventana
    private void guardarEnBaseDeDatos(Libro libro, List<String> nombresAutores) {
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (archivoPortada != null) {
                    libro.setPortada(PortadaLibro.guardar(archivoPortada));
                }
                libroDAO.registrar(libro, nombresAutores);
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            Alert aviso = new Alert(Alert.AlertType.INFORMATION,
                    "El libro se guardó. Ahora debes agregar sus ejemplares desde el inventario.");
            aviso.initOwner(txtTitulo.getScene().getWindow());
            aviso.setHeaderText(null);
            aviso.showAndWait();
            App.cerrar(txtTitulo);
        });
        tarea.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            lblError.setText(mensajeDeError(tarea.getException()));
        });

        btnGuardar.setDisable(true);
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    // La base rechaza un ISBN repetido (restricción UNIQUE)
    private String mensajeDeError(Throwable error) {
        if (error instanceof SQLException && error.getMessage() != null
                && error.getMessage().contains("libros_isbn_key")) {
            return "Ya existe un libro con ese ISBN.";
        }
        error.printStackTrace();
        return "No se pudo guardar el libro: " + error.getMessage();
    }
}

package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.EjemplarDAO;
import com.mycompany.bibliotecasistema.dao.UbicacionDAO;
import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Ubicacion;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


public class EjemplarController {

    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();
    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();

   
    private Libro libro;
    private Ejemplar ejemplar;

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private TextField txtPasillo;
    @FXML
    private TextField txtEstante;
    @FXML
    private TextField txtFila;
    @FXML
    private DatePicker dpAdquisicion;
    @FXML
    private VBox boxEstado;
    @FXML
    private ComboBox<String> cbEstado;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

   
    public void prepararAgregar(Libro libro) {
        this.libro = libro;
        dpAdquisicion.setValue(LocalDate.now());
        try {
           
            Ubicacion actual = ubicacionDAO.buscarPorLibro(libro.getIdLibro());
            if (actual == null) {
                lblSubtitulo.setText(libro.getTitulo() + " · es su primer ejemplar, captura dónde se guardará");
            } else {
                mostrarUbicacion(actual);
                lblSubtitulo.setText(libro.getTitulo() + " · usará la ubicación de sus otros ejemplares");
            }
        } catch (SQLException e) {
            mostrarError("No se pudo cargar la información.");
        }
    }


    public void prepararEditar(Ejemplar ejemplar) {
        this.ejemplar = ejemplar;
        lblTitulo.setText("Editar ejemplar");
        lblSubtitulo.setText(ejemplar.getLibro().getTitulo() + " · " + ejemplar.getCodigoBarras());
        mostrarUbicacion(ejemplar.getUbicacion());
        dpAdquisicion.setValue(ejemplar.getFechaAdquisicion());
        cbEstado.setValue(ejemplar.getEstado().getNombreEstadoEjemplar());
        boxEstado.setVisible(true);
        boxEstado.setManaged(true);
    }

    @FXML
    private void guardar() {
        try {
            if (ejemplar == null) {
                String pasillo = txtPasillo.getText().trim();
                String estante = txtEstante.getText().trim();
                String fila = txtFila.getText().trim();
                if (pasillo.isEmpty() || estante.isEmpty() || fila.isEmpty()) {
                    mostrarError("Captura el pasillo, el estante y la fila.");
                    return;
                }
                Ejemplar nuevo = new Ejemplar();
                nuevo.setLibro(libro);
                nuevo.setUbicacion(ubicacionDAO.buscarOCrear(pasillo, estante, fila));
                nuevo.setCodigoBarras(ejemplarDAO.siguienteCodigoBarras());
                nuevo.setFechaAdquisicion(dpAdquisicion.getValue());
                ejemplarDAO.insertar(nuevo);
            } else {
                ejemplarDAO.actualizar(ejemplar.getIdEjemplar(), cbEstado.getValue(), dpAdquisicion.getValue());
            }
            App.cerrar(btnGuardar);
        } catch (SQLException e) {
            mostrarError("No se pudo guardar el ejemplar.");
        }
    }

    @FXML
    private void cerrar() {
        App.cerrar(btnGuardar);
    }

    
    private void mostrarUbicacion(Ubicacion ubicacion) {
        txtPasillo.setText(ubicacion.getPasillo());
        txtEstante.setText(ubicacion.getEstante());
        txtFila.setText(ubicacion.getFila());
        txtPasillo.setDisable(true);
        txtEstante.setDisable(true);
        txtFila.setDisable(true);
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

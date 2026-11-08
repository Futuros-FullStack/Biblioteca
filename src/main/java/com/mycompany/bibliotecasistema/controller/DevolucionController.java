package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.PrestamoDAO;
import com.mycompany.bibliotecasistema.model.DetallePrestamo;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class DevolucionController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> AYUDA_ESTADO = Map.of(
            EstadoEjemplar.DISPONIBLE, "Con “DISPONIBLE” vuelve a quedar listo para préstamo.",
            EstadoEjemplar.EN_REPARACION, "Queda fuera de préstamo hasta que se repare.",
            EstadoEjemplar.PERDIDO, "Se registra como perdido y se bloquea al alumno.");

    private final PrestamoDAO prestamoDAO = new PrestamoDAO();

    private DetallePrestamo detalle;

    @FXML
    private Label lblSubtitulo;
    @FXML
    private Label lblFechaPactada;
    @FXML
    private DatePicker dpDevolucion;
    @FXML
    private Label lblPuntualidad;
    @FXML
    private ComboBox<String> cbEstado;
    @FXML
    private Label lblAyudaEstado;
    @FXML
    private TextArea txtObservaciones;
    @FXML
    private Button btnRegistrar;

    @FXML
    private void initialize() {
        cbEstado.valueProperty().addListener((obs, anterior, estado) -> lblAyudaEstado.setText(AYUDA_ESTADO.get(estado)));
    }

   
    public void prepararDevolucion(DetallePrestamo detalle) {
        this.detalle = detalle;
        LocalDate pactada = detalle.getPrestamo().getFechaEntregaEsperada();
        long atraso = ChronoUnit.DAYS.between(pactada, LocalDate.now());

        lblSubtitulo.setText(detalle.getEjemplar().getLibro().getTitulo() + " · "
                + detalle.getEjemplar().getCodigoBarras() + " · " + detalle.getPrestamo().getAlumno().getNombre());
        lblFechaPactada.setText(pactada.format(FORMATO_FECHA));

       
        dpDevolucion.setValue(LocalDate.now());
        dpDevolucion.setDisable(true);

        boolean aTiempo = atraso <= 0;
        lblPuntualidad.setText(aTiempo ? "✓ A tiempo" : atraso + (atraso == 1 ? " día de retraso" : " días de retraso"));
        lblPuntualidad.getStyleClass().add(aTiempo ? "indicador-a-tiempo" : "indicador-retraso");
        lblPuntualidad.getStyleClass().remove(aTiempo ? "indicador-retraso" : "indicador-a-tiempo");
    }

    @FXML
    private void registrar() {
        String observaciones = txtObservaciones.getText().trim();
        btnRegistrar.setDisable(true);
        try {
            prestamoDAO.devolver(detalle.getPrestamo().getIdPrestamo(), detalle.getEjemplar().getIdEjemplar(),
                    cbEstado.getValue(), observaciones.isEmpty() ? null : observaciones);
            App.cerrar(btnRegistrar);
        } catch (SQLException e) {
            btnRegistrar.setDisable(false);
            Alert alerta = new Alert(Alert.AlertType.ERROR, "No se pudo registrar la devolución.");
            alerta.initOwner(btnRegistrar.getScene().getWindow());
            alerta.setHeaderText(null);
            alerta.showAndWait();
        }
    }

    @FXML
    private void cerrar(ActionEvent evento) {
        App.cerrar((Node) evento.getSource());
    }
}

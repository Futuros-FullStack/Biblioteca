package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.Estudiante;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.service.EstudianteService;
import com.mycompany.bibliotecasistema.service.PrestamoService;
import com.mycompany.bibliotecasistema.service.ServiceLocator;
import com.mycompany.bibliotecasistema.util.Animaciones;
import com.mycompany.bibliotecasistema.util.Navegador;
import com.mycompany.bibliotecasistema.util.Texto;
import java.time.LocalDate;
import java.util.Optional;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Modal de préstamo en dos pasos dentro del mismo Stage:
 * 1) identificar al alumno (consulta a EstudianteService con debounce),
 * 2) confirmar fechas y registrar con PrestamoService.
 */
public class PrestamoController {

    private static final Duration DEBOUNCE = Duration.millis(400);
    private static final String PATRON_MATRICULA = "\\d{9}";
    private static final int DIAS_PRESTAMO = 7;

    /** Campo que el usuario editó por última vez (define por qué dato se busca). */
    private enum Campo {
        NOMBRE, MATRICULA
    }

    // Paso 1
    @FXML
    private VBox paso1;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtMatricula;
    @FXML
    private Label lblErrorMatricula;
    @FXML
    private Label lblEstadoAlumno;
    @FXML
    private Label lblAyudaNuevo;
    @FXML
    private Button btnSiguiente;

    // Paso 2
    @FXML
    private VBox paso2;
    @FXML
    private TextField txtNombreBloqueado;
    @FXML
    private TextField txtMatriculaBloqueada;
    @FXML
    private TextField txtEjemplar;
    @FXML
    private DatePicker dpPrestamo;
    @FXML
    private DatePicker dpEntrega;
    @FXML
    private Label lblErrorFechas;
    @FXML
    private Button btnRegistrar;

    private final EstudianteService estudianteService = ServiceLocator.estudiantes();
    private final PrestamoService prestamoService = ServiceLocator.prestamos();

    private final PauseTransition debounce = new PauseTransition(DEBOUNCE);
    private final BooleanProperty buscando = new SimpleBooleanProperty(false);
    private final BooleanProperty debeLibro = new SimpleBooleanProperty(false);
    private final BooleanProperty alumnoNuevo = new SimpleBooleanProperty(false);

    private Campo ultimoCampo = Campo.NOMBRE;
    private boolean autocompletando;
    /** Alumno encontrado en el sistema; null si es nuevo. */
    private Estudiante estudianteRegistrado;

    private Ejemplar ejemplar;
    private boolean prestamoRegistrado;

    @FXML
    private void initialize() {
        configurarPaso1();
        configurarPaso2();
        Animaciones.ocultarInmediato(paso2);
    }

    /** Recibe el libro y el ejemplar elegidos en el detalle. */
    public void setDatos(Libro libro, Ejemplar ejemplar) {
        this.ejemplar = ejemplar;
        txtEjemplar.setText(ejemplar.getCodigo());
        txtEjemplar.setTooltip(new javafx.scene.control.Tooltip(libro.getTitulo()));
    }

    public boolean isPrestamoRegistrado() {
        return prestamoRegistrado;
    }

    // ------------------------------------------------------------------ Paso 1

    private void configurarPaso1() {
        txtNombre.textProperty().addListener((obs, a, n) -> programarBusqueda(Campo.NOMBRE));
        txtMatricula.textProperty().addListener((obs, a, n) -> programarBusqueda(Campo.MATRICULA));
        debounce.setOnFinished(e -> consultarEstudiante());

        BooleanBinding matriculaValida = Bindings.createBooleanBinding(
                () -> txtMatricula.getText().trim().matches(PATRON_MATRICULA), txtMatricula.textProperty());
        BooleanBinding nombreCapturado = Bindings.createBooleanBinding(
                () -> !Texto.estaVacio(txtNombre.getText()), txtNombre.textProperty());

        // "Siguiente" solo con nombre, matrícula de 9 dígitos y sin adeudo.
        btnSiguiente.disableProperty().bind(
                nombreCapturado.and(matriculaValida).and(debeLibro.not()).and(buscando.not()).not());

        BooleanBinding errorMatricula = Bindings.createBooleanBinding(
                () -> !txtMatricula.getText().trim().isEmpty() && !matriculaValida.get(),
                txtMatricula.textProperty());
        enlazarVisibilidad(lblErrorMatricula, errorMatricula);
        enlazarVisibilidad(lblAyudaNuevo,
                alumnoNuevo.and(nombreCapturado).and(matriculaValida).and(buscando.not()));
    }

    private void programarBusqueda(Campo campo) {
        if (autocompletando) {
            return;
        }
        ultimoCampo = campo;
        estudianteRegistrado = null;
        debeLibro.set(false);
        alumnoNuevo.set(false);
        mostrarEstado("", null);
        buscando.set(true);
        debounce.playFromStart();
    }

    private void consultarEstudiante() {
        String nombre = txtNombre.getText().trim();
        String matricula = txtMatricula.getText().trim();

        Optional<Estudiante> encontrado = Optional.empty();
        if (ultimoCampo == Campo.NOMBRE && !nombre.isEmpty()) {
            encontrado = estudianteService.buscarPorNombre(nombre);
        } else if (ultimoCampo == Campo.MATRICULA && !matricula.isEmpty()) {
            encontrado = estudianteService.buscarPorMatricula(matricula);
        }

        if (encontrado.isPresent()) {
            Estudiante estudiante = encontrado.get();
            estudianteRegistrado = estudiante;
            autocompletar(estudiante);
            if (estudianteService.tieneAdeudo(estudiante.getMatricula())) {
                debeLibro.set(true);
                mostrarEstado("Debe un libro", "adeudo");
            } else {
                mostrarEstado("Estudiante ya registrado", "registrado");
            }
        } else {
            // No existe: se tratará como alumno nuevo al registrar el préstamo.
            alumnoNuevo.set(!nombre.isEmpty() || !matricula.isEmpty());
            mostrarEstado("", null);
        }
        buscando.set(false);
    }

    /** Llena el otro campo con el dato del alumno encontrado sin disparar otra búsqueda. */
    private void autocompletar(Estudiante estudiante) {
        autocompletando = true;
        try {
            if (ultimoCampo == Campo.NOMBRE) {
                txtMatricula.setText(estudiante.getMatricula());
            } else {
                txtNombre.setText(estudiante.getNombre());
            }
        } finally {
            autocompletando = false;
        }
    }

    private void mostrarEstado(String texto, String clase) {
        lblEstadoAlumno.setText(texto);
        lblEstadoAlumno.getStyleClass().removeAll("registrado", "adeudo");
        if (clase != null) {
            lblEstadoAlumno.getStyleClass().add(clase);
        }
    }

    @FXML
    private void siguiente() {
        if (buscando.get()) {
            debounce.stop();
            consultarEstudiante();
        }
        if (btnSiguiente.isDisabled()) {
            return;
        }
        String nombre = estudianteRegistrado != null ? estudianteRegistrado.getNombre() : txtNombre.getText().trim();
        txtNombreBloqueado.setText(nombre);
        txtMatriculaBloqueada.setText(txtMatricula.getText().trim());
        mostrarPaso2();
    }

    private void mostrarPaso2() {
        Animaciones.ocultar(paso1);
        Animaciones.mostrar(paso2);
        Navegador.ajustarAlContenido(paso2.getScene().getWindow());
        btnRegistrar.setDefaultButton(true);
        dpEntrega.requestFocus();
    }

    // ------------------------------------------------------------------ Paso 2

    private void configurarPaso2() {
        dpPrestamo.setValue(LocalDate.now());
        dpEntrega.setValue(LocalDate.now().plusDays(DIAS_PRESTAMO));
        dpPrestamo.setEditable(false);
        dpEntrega.setEditable(false);

        BooleanBinding fechasValidas = Bindings.createBooleanBinding(
                () -> dpPrestamo.getValue() != null && dpEntrega.getValue() != null
                && dpEntrega.getValue().isAfter(dpPrestamo.getValue()),
                dpPrestamo.valueProperty(), dpEntrega.valueProperty());

        btnRegistrar.disableProperty().bind(fechasValidas.not());
        enlazarVisibilidad(lblErrorFechas, fechasValidas.not());
    }

    @FXML
    private void registrar() {
        Estudiante estudiante = estudianteRegistrado != null
                ? estudianteRegistrado
                : new Estudiante(txtMatriculaBloqueada.getText(), txtNombreBloqueado.getText());
        try {
            prestamoService.registrarPrestamo(estudiante, ejemplar, dpPrestamo.getValue(), dpEntrega.getValue());
        } catch (RuntimeException ex) {
            Navegador.mostrarAlerta(Alert.AlertType.ERROR, "No se pudo registrar el préstamo",
                    ex.getMessage(), paso2.getScene().getWindow());
            return;
        }
        prestamoRegistrado = true;
        Navegador.mostrarAlerta(Alert.AlertType.INFORMATION, "Préstamo registrado",
                "Se prestó el ejemplar " + ejemplar.getCodigo() + " a " + estudiante.getNombre()
                + ".\nFecha de entrega: " + dpEntrega.getEditor().getText(),
                paso2.getScene().getWindow());
        cerrar();
    }

    @FXML
    private void cerrar() {
        debounce.stop();
        Navegador.cerrarVentana(paso1);
    }

    private static void enlazarVisibilidad(javafx.scene.Node nodo, BooleanBinding condicion) {
        nodo.visibleProperty().bind(condicion);
        nodo.managedProperty().bind(condicion);
    }
}

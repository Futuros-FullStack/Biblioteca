package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.PrestamoDAO;
import com.mycompany.bibliotecasistema.model.DetallePrestamo;
import com.mycompany.bibliotecasistema.model.Usuario;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DetalleAlumnoController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int LIMITE_LIBROS = 2;

    private final PrestamoDAO prestamoDAO = new PrestamoDAO();

    private Usuario alumno;

    @FXML
    private Label lblNombre;
    @FXML
    private Label lblMatricula;
    @FXML
    private Label lblCorreo;
    @FXML
    private StackPane cajaSituacion;
    @FXML
    private VBox listaPrestamos;
    @FXML
    private Label lblAviso;
    @FXML
    private Button btnPrestar;

    // Se llama antes de mostrar la ventana, con el alumno elegido en la lista
    public void setAlumno(Usuario alumno) {
        this.alumno = alumno;
        lblNombre.setText(alumno.getNombre());
        lblMatricula.setText("Matrícula " + alumno.getMatricula());
        lblCorreo.setText(alumno.getCorreo());

        listaPrestamos.getChildren().clear();
        cajaSituacion.getChildren().clear();
        ocultarAviso();
        btnPrestar.setDisable(true);
        cargarPrestamos(alumno);
    }

    @FXML
    private void prestar() {
        FXMLLoader loader = App.prepararVentana("nuevo-prestamo");
        PrestamosController nuevoPrestamo = loader.getController();
        nuevoPrestamo.prepararNuevoPrestamo(alumno);
        App.mostrarVentana(loader, btnPrestar);
        setAlumno(alumno);
    }

    @FXML
    private void cerrar() {
        App.cerrar(lblNombre);
    }

    
    private void cargarPrestamos(Usuario alumno) {
        Task<List<DetallePrestamo>> tarea = new Task<>() {
            @Override
            protected List<DetallePrestamo> call() throws Exception {
                return prestamoDAO.listarEnPoder(alumno.getIdUsuario());
            }
        };
        tarea.setOnSucceeded(e -> mostrarPrestamos(alumno, tarea.getValue()));
        tarea.setOnFailed(e -> {
            Label error = new Label("No se pudo cargar la información.");
            error.getStyleClass().add("mensaje-error");
            listaPrestamos.getChildren().setAll(error);
        });
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarPrestamos(Usuario alumno, List<DetallePrestamo> prestamos) {
        boolean hayVencido = false;
        for (DetallePrestamo detalle : prestamos) {
            boolean vencido = detalle.getPrestamo().getFechaEntregaEsperada().isBefore(LocalDate.now());
            hayVencido = hayVencido || vencido;
            listaPrestamos.getChildren().add(crearTarjeta(detalle, vencido));
        }
        if (prestamos.isEmpty()) {
            Label vacio = new Label("No tiene préstamos activos.");
            vacio.getStyleClass().add("mensaje-ayuda");
            listaPrestamos.getChildren().add(vacio);
        }

        if (alumno.isBloqueado()) {
            mostrarSituacion("Bloqueado", true);
        } else if (hayVencido) {
            mostrarSituacion("Con adeudo", true);
        } else {
            mostrarSituacion("Al corriente", false);
        }
        aplicarCupo(alumno, prestamos);
    }

    
    private void aplicarCupo(Usuario alumno, List<DetallePrestamo> prestamos) {
        if (alumno.isBloqueado()) {
            mostrarAviso("Está bloqueado por un libro perdido. No puede recibir préstamos hasta que se registre la reposición.");
            return;
        }
        if (prestamos.size() >= LIMITE_LIBROS) {
            mostrarAviso("Esta matrícula ya tiene 2 libros prestados. No puede realizar otro préstamo hasta devolver alguno.");
            return;
        }
        if (prestamos.size() == 1) {
            DetallePrestamo detalle = prestamos.get(0);
            mostrarAviso("Esta matrícula ya tiene 1 libro prestado: «" + detalle.getEjemplar().getLibro().getTitulo()
                    + "» (entrega: " + detalle.getPrestamo().getFechaEntregaEsperada().format(FORMATO_FECHA)
                    + "). Solo puede llevarse 1 libro más.");
        }
        btnPrestar.setDisable(false);
    }

    private HBox crearTarjeta(DetallePrestamo detalle, boolean vencido) {
        Label libro = new Label(detalle.getEjemplar().getLibro().getTitulo()
                + " · " + detalle.getEjemplar().getCodigoBarras());
        libro.getStyleClass().add("ejemplar-codigo");
        Label entrega = new Label("Entrega: " + detalle.getPrestamo().getFechaEntregaEsperada().format(FORMATO_FECHA));
        entrega.getStyleClass().add("ejemplar-ubicacion");
        VBox textos = new VBox(2, libro, entrega);

        Region espacio = new Region();
        HBox.setHgrow(espacio, javafx.scene.layout.Priority.ALWAYS);

        HBox tarjeta = new HBox(textos, espacio, crearEstado(vencido ? "Vencido" : "Prestado",
                vencido ? "estado-vencido" : "estado-prestado"));
        tarjeta.getStyleClass().add("ejemplar-tarjeta");
        if (vencido) {
            tarjeta.getStyleClass().add("vencido");
        }
        return tarjeta;
    }

    private void mostrarSituacion(String texto, boolean alerta) {
        cajaSituacion.getChildren().setAll(crearEstado(texto, alerta ? "estado-vencido" : "estado-disponible"));
    }

    private Label crearEstado(String texto, String estilo) {
        Region punto = new Region();
        punto.getStyleClass().add("punto");
        Label estado = new Label(texto, punto);
        estado.getStyleClass().addAll("estado", estilo);
        estado.setGraphicTextGap(7);
        return estado;
    }

    private void mostrarAviso(String mensaje) {
        lblAviso.setText(mensaje);
        lblAviso.setVisible(true);
        lblAviso.setManaged(true);
    }

    private void ocultarAviso() {
        lblAviso.setVisible(false);
        lblAviso.setManaged(false);
    }
}

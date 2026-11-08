package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.PrestamoDAO;
import com.mycompany.bibliotecasistema.model.DetallePrestamo;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.Tarjetas;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DetalleAlumnoController {

    private static final DateTimeFormatter FORMATO_FECHA = Tarjetas.FORMATO_FECHA;
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
    @FXML
    private Button btnReposicion;

    
    public void setAlumno(Usuario alumno) {   //prepara los datos del datalle del alumno
        this.alumno = alumno;
        lblNombre.setText(alumno.getNombre());
        lblMatricula.setText("Matrícula " + alumno.getMatricula());
        lblCorreo.setText(alumno.getCorreo());
        listaPrestamos.getChildren().clear();
        cajaSituacion.getChildren().clear();
        ocultarAviso();
        btnPrestar.setDisable(true);
        btnReposicion.setVisible(alumno.isBloqueado()); //isbloqueado() viene del modelo
        btnReposicion.setManaged(alumno.isBloqueado());
        cargarPrestamos(alumno);
    }

    @FXML
    private void prestar() {
        FXMLLoader loader = App.prepararVentana("nuevo-prestamo"); //prepara la ventana de nuevo prestamo
        PrestamosController nuevoPrestamo = loader.getController(); //obtiene el controlador de la ventana de nuevo prestamo
        nuevoPrestamo.prepararNuevoPrestamo(alumno);//prepara el nuevo prestamo con los datos del alumno
        App.mostrarVentana(loader, btnPrestar);//muestra la ventana de nuevo prestamo
        setAlumno(alumno);//actualiza la informacion del detalle del alumno
    }

    @FXML
    private void registrarReposicion() {
        FXMLLoader loader = App.prepararVentana("reposicion"); //prepara la ventana de reposicion
        ReposicionController ventana = loader.getController();//obtiene el controlador de la ventana de reposicion
        ventana.prepararReposicion(alumno);//prepara la ventana de reposicion con los datos del alumno
        App.mostrarVentana(loader, btnReposicion);
        setAlumno(alumno);//actualiza la informacion del detalle del alumno
    }

    @FXML
    private void cerrar() {
        App.cerrar(lblNombre);//cierra la ventana de detalle del alumno
    }

    
    private void cargarPrestamos(Usuario alumno) { //carga los prestamos del alumno
        try {
            mostrarPrestamos(alumno, prestamoDAO.listarEnPoder(alumno.getIdUsuario())); //obtiene la lista de prestamos en poder del alumno y los muestra
        } catch (Exception e) {
            Label error = new Label("No se pudo cargar la información."); //si hay un error al cargar los prestamos, muestra un mensaje de error
            error.getStyleClass().add("mensaje-error"); 
            listaPrestamos.getChildren().setAll(error); //muestra el mensaje de error en la lista de prestamos
        }
    }

    private void mostrarPrestamos(Usuario alumno, List<DetallePrestamo> prestamos) { //muestra los prestamos del alumno
        boolean hayVencido = false; //variable para saber si hay algun prestamo vencido
        for (DetallePrestamo detalle : prestamos) { 
            boolean vencido = detalle.getPrestamo().getFechaEntregaEsperada().isBefore(LocalDate.now()); //si la fecha de entrega esperada es anterior a la fecha actual, el prestamo esta vencido
            hayVencido = hayVencido || vencido; //si hay algun prestamo vencido, la variable hayVencido se vuelve true
            listaPrestamos.getChildren().add(Tarjetas.prestamo(detalle, vencido));
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

    private void mostrarSituacion(String texto, boolean alerta) {
        cajaSituacion.getChildren().setAll(Tarjetas.estado(texto, alerta ? "estado-vencido" : "estado-disponible")); //muestra el estado del alumno en la caja de situacion
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

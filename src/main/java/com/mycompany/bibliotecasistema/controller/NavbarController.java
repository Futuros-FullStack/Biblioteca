package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.util.Navegador;
import com.mycompany.bibliotecasistema.util.Sesion;
import com.mycompany.bibliotecasistema.util.Texto;
import com.mycompany.bibliotecasistema.util.Vista;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Barra superior: navegación según el rol y datos del usuario desde Sesion.
 */
public class NavbarController {

    /** Secciones de la navegación; cada vista marca la suya como activa. */
    public enum Seccion {
        LIBROS, PRESTAMO, INVENTARIO
    }

    private static final String CLASE_ACTIVO = "activo";

    @FXML
    private Button btnLibros;
    @FXML
    private Button btnPrestamo;
    @FXML
    private Button btnInventario;
    @FXML
    private Label lblIniciales;
    @FXML
    private Label lblNombre;
    @FXML
    private Label lblRol;

    @FXML
    private void initialize() {
        Sesion sesion = Sesion.get();
        boolean admin = sesion.esAdministrador();

        lblNombre.setText(sesion.getNombre());
        lblIniciales.setText(Texto.iniciales(sesion.getNombre()));
        lblRol.setText(sesion.getRol() == null ? "" : sesion.getRol().getEtiqueta());

        mostrar(lblRol, admin);
        mostrar(btnPrestamo, admin);
        mostrar(btnInventario, admin);
    }

    public void setSeccionActiva(Seccion seccion) {
        marcar(btnLibros, seccion == Seccion.LIBROS);
        marcar(btnPrestamo, seccion == Seccion.PRESTAMO);
        marcar(btnInventario, seccion == Seccion.INVENTARIO);
    }

    @FXML
    private void irALibros() {
        Navegador.irA(Vista.CATALOGO);
    }

    @FXML
    private void irAPrestamo() {
        // Vista "Préstamos" pendiente: por ahora se muestra el placeholder.
        Navegador.<PlaceholderController>irA(Vista.PLACEHOLDER,
                c -> c.configurar("Préstamos", Seccion.PRESTAMO));
    }

    @FXML
    private void irAInventario() {
        // Vista "Inventario" pendiente: por ahora se muestra el placeholder.
        Navegador.<PlaceholderController>irA(Vista.PLACEHOLDER,
                c -> c.configurar("Inventario", Seccion.INVENTARIO));
    }

    @FXML
    private void salir() {
        Sesion.get().cerrar();
        Navegador.irA(Vista.LOGIN);
    }

    private static void marcar(Button boton, boolean activo) {
        boton.getStyleClass().remove(CLASE_ACTIVO);
        if (activo) {
            boton.getStyleClass().add(CLASE_ACTIVO);
        }
    }

    private static void mostrar(javafx.scene.Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}

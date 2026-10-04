package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.model.Rol;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.service.AuthService;
import com.mycompany.bibliotecasistema.service.ServiceLocator;
import com.mycompany.bibliotecasistema.util.Animaciones;
import com.mycompany.bibliotecasistema.util.Navegador;
import com.mycompany.bibliotecasistema.util.Sesion;
import com.mycompany.bibliotecasistema.util.Texto;
import com.mycompany.bibliotecasistema.util.Vista;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Login con selección de rol. El administrador se autentica con AuthService;
 * el estudiante entra directo al catálogo.
 */
public class LoginController {

    @FXML
    private Label lblEligeRol;
    @FXML
    private ComboBox<Rol> cbRol;
    @FXML
    private VBox boxCredenciales;
    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtContrasena;
    @FXML
    private Label lblError;
    @FXML
    private Button btnIngresar;

    private final AuthService authService = ServiceLocator.auth();

    @FXML
    private void initialize() {
        cbRol.getItems().setAll(Rol.values());
        cbRol.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, rol) -> actualizarFormulario(rol));

        Animaciones.ocultarInmediato(boxCredenciales);
        Animaciones.ocultarInmediato(btnIngresar);
        Animaciones.ocultarInmediato(lblError);

        // Al escribir de nuevo se quita el mensaje de error.
        txtUsuario.textProperty().addListener((obs, a, n) -> Animaciones.ocultar(lblError));
        txtContrasena.textProperty().addListener((obs, a, n) -> Animaciones.ocultar(lblError));
    }

    private void actualizarFormulario(Rol rol) {
        Animaciones.ocultar(lblError);
        if (rol == null) {
            Animaciones.mostrar(lblEligeRol);
            Animaciones.ocultar(boxCredenciales);
            Animaciones.ocultar(btnIngresar);
        } else if (rol == Rol.ESTUDIANTE) {
            Animaciones.mostrar(lblEligeRol);
            Animaciones.ocultar(boxCredenciales);
            Animaciones.mostrar(btnIngresar);
        } else {
            Animaciones.ocultar(lblEligeRol);
            Animaciones.mostrar(boxCredenciales);
            Animaciones.mostrar(btnIngresar);
            txtUsuario.requestFocus();
        }
    }

    @FXML
    private void enfocarContrasena() {
        txtContrasena.requestFocus();
    }

    @FXML
    private void ingresar() {
        Rol rol = cbRol.getValue();
        if (rol == Rol.ESTUDIANTE) {
            Sesion.get().iniciarComoEstudiante();
            Navegador.irA(Vista.CATALOGO);
        } else if (rol == Rol.ADMINISTRADOR) {
            ingresarComoAdministrador();
        }
    }

    private void ingresarComoAdministrador() {
        String usuario = txtUsuario.getText();
        String contrasena = txtContrasena.getText();

        if (Texto.estaVacio(usuario) || Texto.estaVacio(contrasena)) {
            mostrarError("Escribe tu nombre de usuario y tu contraseña.");
            (Texto.estaVacio(usuario) ? txtUsuario : txtContrasena).requestFocus();
            return;
        }

        Optional<Usuario> autenticado = authService.login(usuario.trim(), contrasena);
        if (autenticado.isEmpty()) {
            mostrarError("Usuario o contraseña incorrectos.");
            txtContrasena.clear();
            txtContrasena.requestFocus();
            return;
        }

        Sesion.get().iniciar(autenticado.get());
        Navegador.irA(Vista.CATALOGO);
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        Animaciones.mostrar(lblError);
    }
}

package com.mycompany.bibliotecasistema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Arranca la aplicación y se encarga de la navegación entre pantallas.
 * Solo navega: no hay datos ni lógica.
 */
public class App extends Application {

    private static final String CSS = App.class.getResource("css/estilos.css").toExternalForm();

    /** Botón de la barra que se resalta en cada pantalla. */
    private static final Map<String, String> BOTON_ACTIVO = Map.of(
            "catalogo", "#btnLibros",
            "prestamos", "#btnPrestamo",
            "inventario", "#btnInventario");

    private static Scene escenaPrincipal;

    /** Rol elegido en el login: el estudiante solo ve "Libros". */
    private static boolean administrador;

    public static boolean esAdministrador() {
        return administrador;
    }

    public static void setAdministrador(boolean valor) {
        administrador = valor;
    }

    @Override
    public void start(Stage stage) {
        escenaPrincipal = new Scene(cargar("login"), 1100, 650);
        escenaPrincipal.getStylesheets().add(CSS);
        stage.setTitle("Biblioteca - Facultad de Negocios");
        stage.setMinWidth(1100);
        stage.setMinHeight(650);
        stage.setScene(escenaPrincipal);
        stage.show();
    }

    /** Cambia la pantalla de la ventana principal (por ejemplo "prestamos"). */
    public static void irA(String vista) {
        Parent raiz = cargar(vista);
        String boton = BOTON_ACTIVO.get(vista);
        if (boton != null) {
            Node activo = raiz.lookup(boton);
            if (activo != null) {
                activo.getStyleClass().add("activo");
            }
        }
        escenaPrincipal.setRoot(raiz);
    }

    /**
     * Abre un modal encima de la ventana donde está {@code desde} y espera a que se cierre.
     *
     * @return el controlador del modal, por si hay que configurarlo antes (ver {@link #prepararModal}).
     */
    public static <T> T abrirModal(String vista, Node desde) {
        FXMLLoader loader = prepararModal(vista);
        mostrarModal(loader, desde);
        return loader.getController();
    }

    /** Carga un modal sin mostrarlo, para poder configurar su controlador antes. */
    public static FXMLLoader prepararModal(String vista) {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("view/" + vista + ".fxml"));
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar view/" + vista + ".fxml", e);
        }
        return loader;
    }

    /** Muestra un modal ya cargado con {@link #prepararModal}. */
    public static void mostrarModal(FXMLLoader loader, Node desde) {
        Scene escena = new Scene(loader.getRoot());
        escena.setFill(Color.TRANSPARENT);
        escena.getStylesheets().add(CSS);
        escena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                escena.getWindow().hide();
            }
        });

        Window duena = desde.getScene().getWindow();
        Stage modal = new Stage(StageStyle.TRANSPARENT);
        modal.initOwner(duena);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setScene(escena);
        modal.setOnShown(e -> {
            modal.setX(duena.getX() + (duena.getWidth() - modal.getWidth()) / 2);
            modal.setY(duena.getY() + (duena.getHeight() - modal.getHeight()) / 2);
        });

        Parent fondo = duena.getScene().getRoot();
        fondo.setEffect(new GaussianBlur(6));
        try {
            modal.showAndWait();
        } finally {
            fondo.setEffect(null);
        }
    }

    /** Cierra la ventana (modal) donde está el nodo. */
    public static void cerrar(Node nodo) {
        nodo.getScene().getWindow().hide();
    }

    private static Parent cargar(String vista) {
        try {
            return FXMLLoader.load(App.class.getResource("view/" + vista + ".fxml"));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar view/" + vista + ".fxml", e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}

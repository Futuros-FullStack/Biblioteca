package com.mycompany.bibliotecasistema.util;

import com.mycompany.bibliotecasistema.App;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Cambia la vista de la ventana principal, abre modales y muestra alertas.
 * Todas las escenas usan la misma hoja de estilos.
 */
public final class Navegador {

    private static final String CSS = App.class.getResource("css/estilos.css").toExternalForm();
    private static final boolean INTER_DISPONIBLE = Font.getFamilies().contains("Inter");
    private static final double DESENFOQUE = 8;

    private static Stage principal;

    private Navegador() {
    }

    /** Prepara la ventana principal y muestra la primera vista. */
    public static void iniciar(Stage stage, Vista inicial, double ancho, double alto) {
        principal = stage;
        Scene escena = new Scene(cargar(inicial, null), ancho, alto);
        escena.getStylesheets().add(CSS);
        stage.setScene(escena);
    }

    public static <T> T irA(Vista vista) {
        return irA(vista, null);
    }

    /**
     * Reemplaza el contenido de la ventana principal.
     *
     * @param inicializar recibe el controlador antes de mostrar la vista (puede ser null).
     */
    public static <T> T irA(Vista vista, Consumer<T> inicializar) {
        FXMLLoader loader = loader(vista);
        Parent raiz = cargar(loader, inicializar);
        principal.getScene().setRoot(raiz);
        return loader.getController();
    }

    /** Carga un componente reutilizable (por ejemplo una tarjeta) y entrega su controlador. */
    public static <T> Parent cargarComponente(Vista vista, Consumer<T> inicializar) {
        return cargar(loader(vista), inicializar);
    }

    /**
     * Abre una vista como modal sin decoración sobre {@code duena}, desenfocando el fondo
     * mientras está abierta. Bloquea hasta que se cierra y regresa su controlador.
     */
    public static <T> T abrirModal(Vista vista, Window duena, Consumer<T> inicializar) {
        FXMLLoader loader = loader(vista);
        Parent raiz = cargar(loader, inicializar);

        Scene escena = new Scene(raiz);
        escena.setFill(Color.TRANSPARENT);
        escena.getStylesheets().add(CSS);
        escena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                ((Stage) escena.getWindow()).close();
            }
        });

        Stage modal = new Stage(StageStyle.TRANSPARENT);
        modal.initOwner(duena);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setScene(escena);
        modal.setOnShown(e -> ajustarAlContenido(modal));

        Parent fondo = duena.getScene().getRoot();
        fondo.setEffect(new GaussianBlur(DESENFOQUE));
        try {
            modal.showAndWait();
        } finally {
            fondo.setEffect(null);
        }
        return loader.getController();
    }

    /**
     * Ajusta la altura de la ventana a su contenido ya con el ancho real
     * (los textos con wrapText necesitan el ancho para calcular su alto),
     * manteniéndola centrada verticalmente.
     */
    public static void ajustarAlContenido(Window ventana) {
        Parent raiz = ventana.getScene().getRoot();
        raiz.applyCss();
        raiz.layout();
        double alto = raiz.prefHeight(ventana.getWidth());
        ventana.setY(ventana.getY() - (alto - ventana.getHeight()) / 2);
        ventana.setHeight(alto);
    }

    /** Cierra la ventana (modal) que contiene al nodo. */
    public static void cerrarVentana(Node nodo) {
        ((Stage) nodo.getScene().getWindow()).close();
    }

    public static void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje, Window duena) {
        Alert alerta = new Alert(tipo);
        alerta.initOwner(duena);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        alerta.getDialogPane().getStylesheets().add(CSS);
        alerta.getDialogPane().getStyleClass().add("alerta");
        aplicarFuente(alerta.getDialogPane());
        alerta.showAndWait();
    }

    private static FXMLLoader loader(Vista vista) {
        return new FXMLLoader(App.class.getResource(vista.getRuta()));
    }

    private static <T> Parent cargar(Vista vista, Consumer<T> inicializar) {
        return cargar(loader(vista), inicializar);
    }

    private static <T> Parent cargar(FXMLLoader loader, Consumer<T> inicializar) {
        try {
            Parent raiz = loader.load();
            if (inicializar != null) {
                inicializar.accept(loader.getController());
            }
            aplicarFuente(raiz);
            return raiz;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + loader.getLocation(), e);
        }
    }

    /** Usa Inter si está instalada; si no, el CSS deja "Segoe UI". */
    private static void aplicarFuente(Parent raiz) {
        if (INTER_DISPONIBLE && !raiz.getStyleClass().contains("fuente-inter")) {
            raiz.getStyleClass().add("fuente-inter");
        }
    }
}

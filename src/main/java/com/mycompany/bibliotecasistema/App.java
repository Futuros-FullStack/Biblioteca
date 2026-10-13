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

public class App extends Application {

    private static final String CSS = App.class.getResource("css/estilos.css").toExternalForm();

    private static final Map<String, String> BOTON_ACTIVO = Map.of(
            "catalogo", "#btnLibros",
            "prestamos", "#btnPrestamo",
            "inventario", "#btnInventario");

    private static Scene escenaPrincipal;

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


    public static <T> T abrirVentana(String vista, Node desde) {
        FXMLLoader loader = prepararVentana(vista);
        mostrarVentana(loader, desde);
        return loader.getController();
    }

    public static FXMLLoader prepararVentana(String vista) {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("view/" + vista + ".fxml"));
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar view/" + vista + ".fxml", e);
        }
        return loader;
    }

    public static void mostrarVentana(FXMLLoader loader, Node desde) {
        Scene escena = new Scene(loader.getRoot());
        escena.setFill(Color.TRANSPARENT);
        escena.getStylesheets().add(CSS);
        escena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                escena.getWindow().hide();
            }
        });

        Window duena = desde.getScene().getWindow();
        Stage ventana = new Stage(StageStyle.TRANSPARENT);
        ventana.initOwner(duena);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setScene(escena);
        ventana.setOnShown(e -> {
            ventana.setX(duena.getX() + (duena.getWidth() - ventana.getWidth()) / 2);
            ventana.setY(duena.getY() + (duena.getHeight() - ventana.getHeight()) / 2);
        });

        Parent fondo = duena.getScene().getRoot();
        fondo.setEffect(new GaussianBlur(6));
        try {
            ventana.showAndWait();
        } finally {
            fondo.setEffect(null);
        }
    }

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

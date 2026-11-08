package com.mycompany.bibliotecasistema.util;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.controller.TarjetaLibroController;
import com.mycompany.bibliotecasistema.model.DetallePrestamo;
import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Construye las tarjetas que se repiten en varias pantallas. */
public final class Tarjetas {

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Tarjetas() {
    }

    /** Tarjeta del catálogo (viene de tarjeta-libro.fxml). */
    public static Node libro(Libro libro) {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("view/tarjeta-libro.fxml"));
        try {
            Node tarjeta = loader.load();
            ((TarjetaLibroController) loader.getController()).setLibro(libro);
            return tarjeta;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar view/tarjeta-libro.fxml", e);
        }
    }

    /** Fila de un ejemplar: código, ubicación y estado. */
    public static HBox ejemplar(Ejemplar ejemplar) {
        Label codigo = new Label(ejemplar.getCodigoBarras());
        codigo.getStyleClass().add("ejemplar-codigo");
        Label ubicacion = new Label(ejemplar.getUbicacion().toString());
        ubicacion.getStyleClass().add("ejemplar-ubicacion");

        String estado = ejemplar.getEstado().getNombreEstadoEjemplar();
        Label lblEstado = new Label(estado);
        lblEstado.getStyleClass().addAll("estado", "estado-" + (estado.startsWith("EN ") ? "reparacion" : estado.toLowerCase()));

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox tarjeta = new HBox(new VBox(2, codigo, ubicacion), espacio, lblEstado);
        tarjeta.getStyleClass().add("ejemplar-tarjeta");
        return tarjeta;
    }

    /** Fila de un préstamo en poder del alumno. */
    public static HBox prestamo(DetallePrestamo detalle, boolean vencido) {
        Label libro = new Label(detalle.getEjemplar().getLibro().getTitulo()
                + " · " + detalle.getEjemplar().getCodigoBarras());
        libro.getStyleClass().add("ejemplar-codigo");
        Label entrega = new Label("Entrega: " + detalle.getPrestamo().getFechaEntregaEsperada().format(FORMATO_FECHA));
        entrega.getStyleClass().add("ejemplar-ubicacion");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox tarjeta = new HBox(new VBox(2, libro, entrega), espacio,
                estado(vencido ? "Vencido" : "Prestado", vencido ? "estado-vencido" : "estado-prestado"));
        tarjeta.getStyleClass().add("ejemplar-tarjeta");
        if (vencido) {
            tarjeta.getStyleClass().add("vencido");
        }
        return tarjeta;
    }

    /** Etiqueta de estado con su puntito de color. */
    public static Label estado(String texto, String estilo) {
        Region punto = new Region();
        punto.getStyleClass().add("punto");
        Label estado = new Label(texto, punto);
        estado.getStyleClass().addAll("estado", estilo);
        estado.setGraphicTextGap(7);
        return estado;
    }
}

package com.mycompany.bibliotecasistema.util;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Transiciones suaves para mostrar y ocultar nodos.
 * Un nodo oculto deja de ocupar espacio (managed = false).
 */
public final class Animaciones {

    private static final Duration DURACION = Duration.millis(220);
    private static final String CLAVE = "animaciones.fade";

    private Animaciones() {
    }

    public static void mostrar(Node nodo) {
        if (nodo.isVisible() && nodo.getOpacity() == 1) {
            return;
        }
        FadeTransition fade = transicion(nodo);
        nodo.setManaged(true);
        nodo.setVisible(true);
        fade.setFromValue(nodo.getOpacity() == 1 ? 0 : nodo.getOpacity());
        fade.setToValue(1);
        fade.setOnFinished(null);
        fade.playFromStart();
    }

    public static void ocultar(Node nodo) {
        if (!nodo.isVisible()) {
            return;
        }
        FadeTransition fade = transicion(nodo);
        fade.setFromValue(nodo.getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            nodo.setVisible(false);
            nodo.setManaged(false);
            nodo.setOpacity(1);
        });
        fade.playFromStart();
    }

    /** Oculta sin animación (estado inicial de una vista). */
    public static void ocultarInmediato(Node nodo) {
        nodo.setVisible(false);
        nodo.setManaged(false);
    }

    private static FadeTransition transicion(Node nodo) {
        Object guardada = nodo.getProperties().get(CLAVE);
        FadeTransition fade;
        if (guardada instanceof FadeTransition) {
            fade = (FadeTransition) guardada;
            fade.stop();
        } else {
            fade = new FadeTransition(DURACION, nodo);
            nodo.getProperties().put(CLAVE, fade);
        }
        return fade;
    }
}

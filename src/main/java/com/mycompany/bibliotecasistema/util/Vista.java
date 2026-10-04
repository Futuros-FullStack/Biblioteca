package com.mycompany.bibliotecasistema.util;

/**
 * Archivos FXML de la aplicación (carpeta resources/.../view).
 */
public enum Vista {
    LOGIN("login"),
    CATALOGO("catalogo"),
    TARJETA_LIBRO("tarjeta-libro"),
    DETALLE_LIBRO("detalle-libro"),
    PRESTAMO("prestamo"),
    PLACEHOLDER("placeholder");

    private final String archivo;

    Vista(String archivo) {
        this.archivo = archivo;
    }

    public String getRuta() {
        return "view/" + archivo + ".fxml";
    }
}

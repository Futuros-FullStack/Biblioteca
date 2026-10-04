package com.mycompany.bibliotecasistema.model;

/**
 * Roles con los que se puede ingresar al sistema.
 */
public enum Rol {
    ESTUDIANTE("Estudiante"),
    ADMINISTRADOR("Administrador");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}

package com.mycompany.bibliotecasistema.model;

/**
 * Estado de circulación de un ejemplar.
 * La clase CSS se usa para colorear el estado en las vistas.
 */
public enum EstadoEjemplar {
    DISPONIBLE("Disponible", "estado-disponible"),
    PRESTADO("Prestado", "estado-prestado"),
    VENCIDO("Vencido", "estado-vencido"),
    DANADO("Dañado", "estado-danado");

    private final String etiqueta;
    private final String claseCss;

    EstadoEjemplar(String etiqueta, String claseCss) {
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClaseCss() {
        return claseCss;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}

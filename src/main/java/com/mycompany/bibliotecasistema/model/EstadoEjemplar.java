package com.mycompany.bibliotecasistema.model;

import java.util.Objects;

public class EstadoEjemplar {

    public static final String DISPONIBLE = "DISPONIBLE";
    public static final String PRESTADO = "PRESTADO";
    public static final String EN_REPARACION = "EN REPARACION";
    public static final String PERDIDO = "PERDIDO";

    private Integer idEstadoEjemplar;
    private String nombreEstadoEjemplar;

    public EstadoEjemplar() {
    }

    public EstadoEjemplar(Integer idEstadoEjemplar, String nombreEstadoEjemplar) {
        this.idEstadoEjemplar = idEstadoEjemplar;
        this.nombreEstadoEjemplar = nombreEstadoEjemplar;
    }

    public Integer getIdEstadoEjemplar() {
        return idEstadoEjemplar;
    }

    public void setIdEstadoEjemplar(Integer idEstadoEjemplar) {
        this.idEstadoEjemplar = idEstadoEjemplar;
    }

    public String getNombreEstadoEjemplar() {
        return nombreEstadoEjemplar;
    }

    public void setNombreEstadoEjemplar(String nombreEstadoEjemplar) {
        this.nombreEstadoEjemplar = nombreEstadoEjemplar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EstadoEjemplar)) {
            return false;
        }
        EstadoEjemplar otro = (EstadoEjemplar) o;
        return idEstadoEjemplar != null && idEstadoEjemplar.equals(otro.idEstadoEjemplar);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idEstadoEjemplar);
    }

    @Override
    public String toString() {
        return nombreEstadoEjemplar;
    }
}

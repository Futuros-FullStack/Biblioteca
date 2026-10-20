package com.mycompany.bibliotecasistema.model;

import java.util.Objects;

public class EstadoPrestamo {

    public static final String ACTIVO = "ACTIVO";
    public static final String ATRASADO = "ATRASADO";
    public static final String DEVUELTO = "DEVUELTO";
    public static final String CANCELADO = "CANCELADO";

    private Integer idEstadoPrestamo;
    private String nombreEstadoPrestamo;

    public EstadoPrestamo() {
    }

    public EstadoPrestamo(Integer idEstadoPrestamo, String nombreEstadoPrestamo) {
        this.idEstadoPrestamo = idEstadoPrestamo;
        this.nombreEstadoPrestamo = nombreEstadoPrestamo;
    }

    public Integer getIdEstadoPrestamo() {
        return idEstadoPrestamo;
    }

    public void setIdEstadoPrestamo(Integer idEstadoPrestamo) {
        this.idEstadoPrestamo = idEstadoPrestamo;
    }

    public String getNombreEstadoPrestamo() {
        return nombreEstadoPrestamo;
    }

    public void setNombreEstadoPrestamo(String nombreEstadoPrestamo) {
        this.nombreEstadoPrestamo = nombreEstadoPrestamo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EstadoPrestamo)) {
            return false;
        }
        EstadoPrestamo otro = (EstadoPrestamo) o;
        return idEstadoPrestamo != null && idEstadoPrestamo.equals(otro.idEstadoPrestamo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idEstadoPrestamo);
    }

    @Override
    public String toString() {
        return nombreEstadoPrestamo;
    }
}

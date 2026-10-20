package com.mycompany.bibliotecasistema.model;

import java.util.Objects;

public class EstadoNotificacion {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String EXITOSO = "EXITOSO";
    public static final String FALLIDO = "FALLIDO";

    private Integer idEstadoNoti;
    private String nombreEstadoNoti;

    public EstadoNotificacion() {
    }

    public EstadoNotificacion(Integer idEstadoNoti, String nombreEstadoNoti) {
        this.idEstadoNoti = idEstadoNoti;
        this.nombreEstadoNoti = nombreEstadoNoti;
    }

    public Integer getIdEstadoNoti() {
        return idEstadoNoti;
    }

    public void setIdEstadoNoti(Integer idEstadoNoti) {
        this.idEstadoNoti = idEstadoNoti;
    }

    public String getNombreEstadoNoti() {
        return nombreEstadoNoti;
    }

    public void setNombreEstadoNoti(String nombreEstadoNoti) {
        this.nombreEstadoNoti = nombreEstadoNoti;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EstadoNotificacion)) {
            return false;
        }
        EstadoNotificacion otro = (EstadoNotificacion) o;
        return idEstadoNoti != null && idEstadoNoti.equals(otro.idEstadoNoti);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idEstadoNoti);
    }

    @Override
    public String toString() {
        return nombreEstadoNoti;
    }
}

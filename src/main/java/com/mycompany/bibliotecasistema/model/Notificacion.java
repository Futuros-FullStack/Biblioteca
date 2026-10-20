package com.mycompany.bibliotecasistema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Notificacion {

    private Integer idNotificacion;
    private DetallePrestamo detalle;
    private LocalDateTime fechaEnvio;
    private EstadoNotificacion estado;
    private LocalDate fechaEntregaAvisada;
    private String detalleError;

    public Notificacion() {
    }

    public Integer getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(Integer idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public DetallePrestamo getDetalle() {
        return detalle;
    }

    public void setDetalle(DetallePrestamo detalle) {
        this.detalle = detalle;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public EstadoNotificacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoNotificacion estado) {
        this.estado = estado;
    }

    public LocalDate getFechaEntregaAvisada() {
        return fechaEntregaAvisada;
    }

    public void setFechaEntregaAvisada(LocalDate fechaEntregaAvisada) {
        this.fechaEntregaAvisada = fechaEntregaAvisada;
    }

    public String getDetalleError() {
        return detalleError;
    }

    public void setDetalleError(String detalleError) {
        this.detalleError = detalleError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notificacion)) {
            return false;
        }
        Notificacion otra = (Notificacion) o;
        return idNotificacion != null && idNotificacion.equals(otra.idNotificacion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idNotificacion);
    }

    @Override
    public String toString() {
        return "Notificacion{idNotificacion=" + idNotificacion + ", estado=" + estado
                + ", fechaEntregaAvisada=" + fechaEntregaAvisada + "}";
    }
}

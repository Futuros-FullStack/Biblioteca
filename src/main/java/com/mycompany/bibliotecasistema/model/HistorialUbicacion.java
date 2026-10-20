package com.mycompany.bibliotecasistema.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class HistorialUbicacion {

    private Integer idHistorial;
    private Ejemplar ejemplar;
    private Ubicacion ubicacionAnterior;
    private Ubicacion ubicacionNueva;
    private Usuario adminResponsable;
    private LocalDateTime fechaCambio;
    private String motivo;

    public HistorialUbicacion() {
    }

    public Integer getIdHistorial() {
        return idHistorial;
    }

    public void setIdHistorial(Integer idHistorial) {
        this.idHistorial = idHistorial;
    }

    public Ejemplar getEjemplar() {
        return ejemplar;
    }

    public void setEjemplar(Ejemplar ejemplar) {
        this.ejemplar = ejemplar;
    }

    public Ubicacion getUbicacionAnterior() {
        return ubicacionAnterior;
    }

    public void setUbicacionAnterior(Ubicacion ubicacionAnterior) {
        this.ubicacionAnterior = ubicacionAnterior;
    }

    public Ubicacion getUbicacionNueva() {
        return ubicacionNueva;
    }

    public void setUbicacionNueva(Ubicacion ubicacionNueva) {
        this.ubicacionNueva = ubicacionNueva;
    }

    public Usuario getAdminResponsable() {
        return adminResponsable;
    }

    public void setAdminResponsable(Usuario adminResponsable) {
        this.adminResponsable = adminResponsable;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistorialUbicacion)) {
            return false;
        }
        HistorialUbicacion otro = (HistorialUbicacion) o;
        return idHistorial != null && idHistorial.equals(otro.idHistorial);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idHistorial);
    }

    @Override
    public String toString() {
        return "HistorialUbicacion{idHistorial=" + idHistorial + ", fechaCambio=" + fechaCambio + "}";
    }
}

package com.mycompany.bibliotecasistema.model;

import java.time.LocalDate;
import java.util.Objects;

public class Ejemplar {

    private Integer idEjemplar;
    private Libro libro;
    private Ubicacion ubicacion;
    private EstadoEjemplar estado;
    private String codigoBarras;
    private LocalDate fechaAdquisicion;

    public Ejemplar() {
    }

    public Integer getIdEjemplar() {
        return idEjemplar;
    }

    public void setIdEjemplar(Integer idEjemplar) {
        this.idEjemplar = idEjemplar;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }

    public EstadoEjemplar getEstado() {
        return estado;
    }

    public void setEstado(EstadoEjemplar estado) {
        this.estado = estado;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public LocalDate getFechaAdquisicion() {
        return fechaAdquisicion;
    }

    public void setFechaAdquisicion(LocalDate fechaAdquisicion) {
        this.fechaAdquisicion = fechaAdquisicion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ejemplar)) {
            return false;
        }
        Ejemplar otro = (Ejemplar) o;
        return idEjemplar != null && idEjemplar.equals(otro.idEjemplar);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idEjemplar);
    }

    @Override
    public String toString() {
        return "Ejemplar{idEjemplar=" + idEjemplar + ", codigoBarras=" + codigoBarras + ", estado=" + estado + "}";
    }
}

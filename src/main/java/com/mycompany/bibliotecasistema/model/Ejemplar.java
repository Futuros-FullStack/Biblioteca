package com.mycompany.bibliotecasistema.model;

import java.time.LocalDate;

/**
 * Copia física de un libro.
 */
public class Ejemplar {

    private String codigo;
    private String ubicacion;
    private LocalDate fechaAdquisicion;
    private EstadoEjemplar estado;

    public Ejemplar() {
    }

    public Ejemplar(String codigo, String ubicacion, LocalDate fechaAdquisicion, EstadoEjemplar estado) {
        this.codigo = codigo;
        this.ubicacion = ubicacion;
        this.fechaAdquisicion = fechaAdquisicion;
        this.estado = estado;
    }

    public boolean estaDisponible() {
        return estado == EstadoEjemplar.DISPONIBLE;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public LocalDate getFechaAdquisicion() {
        return fechaAdquisicion;
    }

    public void setFechaAdquisicion(LocalDate fechaAdquisicion) {
        this.fechaAdquisicion = fechaAdquisicion;
    }

    public EstadoEjemplar getEstado() {
        return estado;
    }

    public void setEstado(EstadoEjemplar estado) {
        this.estado = estado;
    }
}

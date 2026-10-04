package com.mycompany.bibliotecasistema.model;

import java.time.LocalDate;

/**
 * Préstamo de un ejemplar a un estudiante.
 */
public class Prestamo {

    private Estudiante estudiante;
    private Ejemplar ejemplar;
    private LocalDate fechaPrestamo;
    private LocalDate fechaEntrega;
    private EstadoPrestamo estado;

    public Prestamo() {
    }

    public Prestamo(Estudiante estudiante, Ejemplar ejemplar, LocalDate fechaPrestamo,
            LocalDate fechaEntrega, EstadoPrestamo estado) {
        this.estudiante = estudiante;
        this.ejemplar = ejemplar;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaEntrega = fechaEntrega;
        this.estado = estado;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Ejemplar getEjemplar() {
        return ejemplar;
    }

    public void setEjemplar(Ejemplar ejemplar) {
        this.ejemplar = ejemplar;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDate fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }
}

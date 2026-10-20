package com.mycompany.bibliotecasistema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Prestamo {

    private Integer idPrestamo;
    private Usuario admin;
    private Usuario alumno;
    private EstadoPrestamo estado;
    private LocalDateTime fechaPrestamo;
    private LocalDate fechaEntregaEsperada;
    private boolean renovado;
    private LocalDateTime fechaRenovacion;
    private String observaciones;
    private List<DetallePrestamo> detalles = new ArrayList<>();

    public Prestamo() {
    }

    public Integer getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Integer idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Usuario getAdmin() {
        return admin;
    }

    public void setAdmin(Usuario admin) {
        this.admin = admin;
    }

    public Usuario getAlumno() {
        return alumno;
    }

    public void setAlumno(Usuario alumno) {
        this.alumno = alumno;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDateTime fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDate getFechaEntregaEsperada() {
        return fechaEntregaEsperada;
    }

    public void setFechaEntregaEsperada(LocalDate fechaEntregaEsperada) {
        this.fechaEntregaEsperada = fechaEntregaEsperada;
    }

    public boolean isRenovado() {
        return renovado;
    }

    public void setRenovado(boolean renovado) {
        this.renovado = renovado;
    }

    public LocalDateTime getFechaRenovacion() {
        return fechaRenovacion;
    }

    public void setFechaRenovacion(LocalDateTime fechaRenovacion) {
        this.fechaRenovacion = fechaRenovacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<DetallePrestamo> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    public void setDetalles(List<DetallePrestamo> detalles) {
        this.detalles = detalles == null ? new ArrayList<>() : new ArrayList<>(detalles);
    }

    public void agregarDetalle(DetallePrestamo detalle) {
        if (detalle != null) {
            detalles.add(detalle);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Prestamo)) {
            return false;
        }
        Prestamo otro = (Prestamo) o;
        return idPrestamo != null && idPrestamo.equals(otro.idPrestamo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idPrestamo);
    }

    @Override
    public String toString() {
        return "Prestamo{idPrestamo=" + idPrestamo + ", fechaEntregaEsperada=" + fechaEntregaEsperada
                + ", estado=" + estado + "}";
    }
}

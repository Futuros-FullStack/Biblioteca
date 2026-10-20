package com.mycompany.bibliotecasistema.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class DetallePrestamo {

    private Prestamo prestamo;
    private Ejemplar ejemplar;
    private LocalDateTime fechaDevolucionReal;

    public DetallePrestamo() {
    }

    public DetallePrestamo(Prestamo prestamo, Ejemplar ejemplar) {
        this.prestamo = prestamo;
        this.ejemplar = ejemplar;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public Ejemplar getEjemplar() {
        return ejemplar;
    }

    public void setEjemplar(Ejemplar ejemplar) {
        this.ejemplar = ejemplar;
    }

    public LocalDateTime getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(LocalDateTime fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public boolean isDevuelto() {
        return fechaDevolucionReal != null;
    }

    // La clave es compuesta: (id_prestamo, id_ejemplar)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DetallePrestamo)) {
            return false;
        }
        DetallePrestamo otro = (DetallePrestamo) o;
        return prestamo != null && ejemplar != null
                && Objects.equals(prestamo.getIdPrestamo(), otro.prestamo == null ? null : otro.prestamo.getIdPrestamo())
                && Objects.equals(ejemplar.getIdEjemplar(), otro.ejemplar == null ? null : otro.ejemplar.getIdEjemplar());
    }

    @Override
    public int hashCode() {
        return Objects.hash(prestamo == null ? null : prestamo.getIdPrestamo(),
                ejemplar == null ? null : ejemplar.getIdEjemplar());
    }

    @Override
    public String toString() {
        return "DetallePrestamo{ejemplar=" + ejemplar + ", fechaDevolucionReal=" + fechaDevolucionReal + "}";
    }
}

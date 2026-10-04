package com.mycompany.bibliotecasistema.model;

/**
 * Estado de un préstamo. ACTIVO y VENCIDO cuentan como adeudo del alumno.
 */
public enum EstadoPrestamo {
    ACTIVO,
    VENCIDO,
    DEVUELTO;

    public boolean esAdeudo() {
        return this == ACTIVO || this == VENCIDO;
    }
}

package com.mycompany.bibliotecasistema.service.mock;

import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import com.mycompany.bibliotecasistema.model.EstadoPrestamo;
import com.mycompany.bibliotecasistema.model.Estudiante;
import com.mycompany.bibliotecasistema.model.Prestamo;
import com.mycompany.bibliotecasistema.service.PrestamoService;
import java.time.LocalDate;

/**
 * Préstamos de prueba: se guardan en memoria mientras la app está abierta.
 */
public class MockPrestamoService implements PrestamoService {

    private final MockDatos datos = MockDatos.get();

    @Override
    public void registrarPrestamo(Estudiante estudiante, Ejemplar ejemplar, LocalDate prestamo, LocalDate entrega) {
        if (!ejemplar.estaDisponible()) {
            throw new IllegalStateException("El ejemplar " + ejemplar.getCodigo() + " ya no está disponible.");
        }
        boolean registrado = datos.estudiantes.stream()
                .anyMatch(e -> e.getMatricula().equals(estudiante.getMatricula()));
        if (!registrado) {
            datos.estudiantes.add(estudiante);
        }
        ejemplar.setEstado(EstadoEjemplar.PRESTADO);
        datos.prestamos.add(new Prestamo(estudiante, ejemplar, prestamo, entrega, EstadoPrestamo.ACTIVO));
    }
}

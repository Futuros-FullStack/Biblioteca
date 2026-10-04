package com.mycompany.bibliotecasistema.service;

import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.Estudiante;
import java.time.LocalDate;

/**
 * Registro de préstamos.
 * BACKEND: implementar en una transacción y registrar en ServiceLocator.
 */
public interface PrestamoService {

    /**
     * Registra el préstamo. Si el estudiante no existe debe darse de alta,
     * y el ejemplar debe quedar en estado PRESTADO.
     *
     * @throws RuntimeException si no se pudo registrar (la vista muestra el mensaje).
     */
    void registrarPrestamo(Estudiante estudiante, Ejemplar ejemplar, LocalDate prestamo, LocalDate entrega);
}

package com.mycompany.bibliotecasistema.service;

import com.mycompany.bibliotecasistema.service.mock.MockAuthService;
import com.mycompany.bibliotecasistema.service.mock.MockEstudianteService;
import com.mycompany.bibliotecasistema.service.mock.MockLibroService;
import com.mycompany.bibliotecasistema.service.mock.MockPrestamoService;

/**
 * Único punto donde se eligen las implementaciones de los services.
 * BACKEND: reemplazar aquí cada "new Mock..." por la implementación real.
 * Las vistas y controladores no necesitan cambiar.
 */
public final class ServiceLocator {

    private static final AuthService AUTH = new MockAuthService();
    private static final LibroService LIBROS = new MockLibroService();
    private static final EstudianteService ESTUDIANTES = new MockEstudianteService();
    private static final PrestamoService PRESTAMOS = new MockPrestamoService();

    private ServiceLocator() {
    }

    public static AuthService auth() {
        return AUTH;
    }

    public static LibroService libros() {
        return LIBROS;
    }

    public static EstudianteService estudiantes() {
        return ESTUDIANTES;
    }

    public static PrestamoService prestamos() {
        return PRESTAMOS;
    }
}

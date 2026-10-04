package com.mycompany.bibliotecasistema.service;

import com.mycompany.bibliotecasistema.model.Estudiante;
import java.util.Optional;

/**
 * Consulta de alumnos registrados.
 * BACKEND: implementar contra la tabla de estudiantes/préstamos y registrar en ServiceLocator.
 */
public interface EstudianteService {

    /** Busca un alumno por su nombre completo (sin distinguir mayúsculas ni acentos). */
    Optional<Estudiante> buscarPorNombre(String nombre);

    Optional<Estudiante> buscarPorMatricula(String matricula);

    /** true si el alumno tiene algún préstamo activo o vencido sin devolver. */
    boolean tieneAdeudo(String matricula);
}

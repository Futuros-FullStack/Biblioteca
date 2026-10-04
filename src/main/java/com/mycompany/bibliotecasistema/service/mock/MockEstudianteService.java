package com.mycompany.bibliotecasistema.service.mock;

import com.mycompany.bibliotecasistema.model.Estudiante;
import com.mycompany.bibliotecasistema.service.EstudianteService;
import com.mycompany.bibliotecasistema.util.Texto;
import java.util.Optional;

/**
 * Estudiantes de prueba en memoria.
 */
public class MockEstudianteService implements EstudianteService {

    private final MockDatos datos = MockDatos.get();

    @Override
    public Optional<Estudiante> buscarPorNombre(String nombre) {
        String buscado = Texto.normalizar(nombre);
        if (buscado.isEmpty()) {
            return Optional.empty();
        }
        return datos.estudiantes.stream()
                .filter(e -> Texto.normalizar(e.getNombre()).equals(buscado))
                .findFirst();
    }

    @Override
    public Optional<Estudiante> buscarPorMatricula(String matricula) {
        String buscada = matricula == null ? "" : matricula.trim();
        return datos.estudiantes.stream()
                .filter(e -> e.getMatricula().equals(buscada))
                .findFirst();
    }

    @Override
    public boolean tieneAdeudo(String matricula) {
        return datos.prestamos.stream()
                .anyMatch(p -> p.getEstudiante().getMatricula().equals(matricula)
                && p.getEstado().esAdeudo());
    }
}

package com.mycompany.bibliotecasistema.service;

import com.mycompany.bibliotecasistema.model.Libro;
import java.util.List;

/**
 * Consulta del catálogo de libros.
 * BACKEND: implementar con la consulta real (libros + ejemplares) y registrar en ServiceLocator.
 */
public interface LibroService {

    /** Todos los libros del catálogo, cada uno con su lista de ejemplares. */
    List<Libro> obtenerLibros();

    /** Libros cuyo título, autor o ISBN contengan el texto indicado. */
    List<Libro> buscar(String texto);
}

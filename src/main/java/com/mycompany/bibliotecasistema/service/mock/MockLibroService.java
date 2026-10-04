package com.mycompany.bibliotecasistema.service.mock;

import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.service.LibroService;
import com.mycompany.bibliotecasistema.util.Texto;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Catálogo de prueba en memoria.
 */
public class MockLibroService implements LibroService {

    private final MockDatos datos = MockDatos.get();

    @Override
    public List<Libro> obtenerLibros() {
        return new ArrayList<>(datos.libros);
    }

    @Override
    public List<Libro> buscar(String texto) {
        String filtro = Texto.normalizar(texto);
        return datos.libros.stream()
                .filter(l -> Texto.normalizar(l.getTitulo()).contains(filtro)
                || Texto.normalizar(l.getAutoresTexto()).contains(filtro)
                || Texto.normalizar(l.getIsbn()).contains(filtro))
                .collect(Collectors.toList());
    }
}

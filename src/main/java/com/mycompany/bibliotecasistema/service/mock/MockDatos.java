package com.mycompany.bibliotecasistema.service.mock;

import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import com.mycompany.bibliotecasistema.model.EstadoPrestamo;
import com.mycompany.bibliotecasistema.model.Estudiante;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Prestamo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * "Base de datos" en memoria compartida por los services mock.
 * BACKEND: esta clase desaparece cuando existan las implementaciones reales.
 */
final class MockDatos {

    private static final MockDatos INSTANCIA = new MockDatos();

    final List<Libro> libros = new ArrayList<>();
    final List<Estudiante> estudiantes = new ArrayList<>();
    final List<Prestamo> prestamos = new ArrayList<>();

    static MockDatos get() {
        return INSTANCIA;
    }

    private MockDatos() {
        cargarLibros();
        cargarEstudiantesYPrestamos();
    }

    private void cargarLibros() {
        String pasilloA1 = "Pasillo A - Estante 1 - Fila 1";

        libros.add(new Libro(1, "Cancelen el dif",
                List.of("Fernando Córdova Flores"),
                "Una mirada crítica y con humor al cálculo diferencial desde las aulas de la "
                + "Facultad de Negocios: anécdotas, errores comunes y estrategias para sobrevivir "
                + "a la materia.",
                "978-607-0000-01-1", "Editorial UNACH", 2026, "Tapachula",
                List.of(
                        ejemplar("BC-001", pasilloA1, 2026, EstadoEjemplar.PRESTADO),
                        ejemplar("BC-002", pasilloA1, 2026, EstadoEjemplar.DISPONIBLE))));

        libros.add(new Libro(2, "Contabilidad financiera",
                List.of("Gerardo Guajardo Cantú", "Nora Andrade de Guajardo"),
                "Introduce los fundamentos de la contabilidad financiera: el ciclo contable, "
                + "la elaboración de estados financieros y su interpretación para la toma de decisiones.",
                "978-607-0000-02-8", "McGraw-Hill", 2018, "Ciudad de México",
                List.of(
                        ejemplar("CF-001", "Pasillo A - Estante 2 - Fila 3", 2019, EstadoEjemplar.DISPONIBLE),
                        ejemplar("CF-002", "Pasillo A - Estante 2 - Fila 3", 2019, EstadoEjemplar.VENCIDO),
                        ejemplar("CF-003", "Pasillo A - Estante 2 - Fila 3", 2021, EstadoEjemplar.DISPONIBLE))));

        libros.add(new Libro(3, "Administración",
                List.of("Stephen P. Robbins", "Mary Coulter"),
                "Texto clásico sobre las funciones administrativas: planeación, organización, "
                + "dirección y control, con casos de empresas reales.",
                "978-607-0000-03-5", "Pearson", 2014, "Ciudad de México",
                List.of(
                        ejemplar("AD-001", "Pasillo B - Estante 1 - Fila 2", 2015, EstadoEjemplar.DISPONIBLE),
                        ejemplar("AD-002", "Pasillo B - Estante 1 - Fila 2", 2015, EstadoEjemplar.DANADO))));

        libros.add(new Libro(4, "Fundamentos de marketing",
                List.of("Philip Kotler", "Gary Armstrong"),
                "Conceptos esenciales de mercadotecnia: segmentación, posicionamiento, "
                + "mezcla de marketing y construcción de relaciones con el cliente.",
                "978-607-0000-04-2", "Pearson", 2017, "Ciudad de México",
                List.of(
                        ejemplar("MK-001", "Pasillo B - Estante 3 - Fila 1", 2018, EstadoEjemplar.DISPONIBLE))));

        libros.add(new Libro(5, "Economía",
                List.of("Paul A. Samuelson", "William D. Nordhaus"),
                "Panorama completo de micro y macroeconomía, desde la oferta y la demanda "
                + "hasta la política monetaria y el comercio internacional.",
                "978-607-0000-05-9", "McGraw-Hill", 2010, "Madrid",
                List.of(
                        ejemplar("EC-001", "Pasillo C - Estante 1 - Fila 4", 2011, EstadoEjemplar.DISPONIBLE),
                        ejemplar("EC-002", "Pasillo C - Estante 1 - Fila 4", 2011, EstadoEjemplar.DISPONIBLE))));

        libros.add(new Libro(6, "Estadística para administración y economía",
                List.of("David R. Anderson", "Dennis J. Sweeney", "Thomas A. Williams"),
                "Métodos estadísticos aplicados a problemas de negocios: probabilidad, "
                + "inferencia, regresión y análisis de series de tiempo.",
                "978-607-0000-06-6", "Cengage Learning", 2019, "Ciudad de México",
                List.of(
                        ejemplar("ES-001", "Pasillo C - Estante 2 - Fila 1", 2020, EstadoEjemplar.PRESTADO),
                        ejemplar("ES-002", "Pasillo C - Estante 2 - Fila 1", 2020, EstadoEjemplar.DISPONIBLE))));

        libros.add(new Libro(7, "Finanzas corporativas",
                List.of("Stephen A. Ross", "Randolph W. Westerfield", "Jeffrey Jaffe"),
                "Valuación, presupuesto de capital, estructura de capital y política de "
                + "dividendos desde la perspectiva del director financiero.",
                "978-607-0000-07-3", "McGraw-Hill", 2012, "Ciudad de México",
                List.of(
                        ejemplar("FC-001", "Pasillo D - Estante 1 - Fila 2", 2013, EstadoEjemplar.DISPONIBLE))));
    }

    private void cargarEstudiantesYPrestamos() {
        Estudiante dana = new Estudiante("100012345", "Dana Constantino");
        Estudiante benjamin = new Estudiante("100054321", "Benjamín García");
        Estudiante luis = new Estudiante("100067890", "Luis Pérez");
        estudiantes.add(dana);
        estudiantes.add(benjamin);
        estudiantes.add(luis);
        // Fernando Córdova (100029531) NO está aquí: se registra como nuevo al prestar.

        LocalDate hoy = LocalDate.now();
        prestamos.add(new Prestamo(benjamin, buscarEjemplar("CF-002"),
                hoy.minusDays(20), hoy.minusDays(13), EstadoPrestamo.VENCIDO));
        prestamos.add(new Prestamo(luis, buscarEjemplar("BC-001"),
                hoy.minusDays(2), hoy.plusDays(5), EstadoPrestamo.ACTIVO));
        prestamos.add(new Prestamo(luis, buscarEjemplar("ES-001"),
                hoy.minusDays(1), hoy.plusDays(6), EstadoPrestamo.ACTIVO));
        // Dana tuvo un préstamo, pero ya lo devolvió: no tiene adeudo.
        prestamos.add(new Prestamo(dana, buscarEjemplar("MK-001"),
                hoy.minusDays(30), hoy.minusDays(23), EstadoPrestamo.DEVUELTO));
    }

    private Ejemplar buscarEjemplar(String codigo) {
        return libros.stream()
                .flatMap(l -> l.getEjemplares().stream())
                .filter(e -> e.getCodigo().equals(codigo))
                .findFirst()
                .orElseThrow();
    }

    private static Ejemplar ejemplar(String codigo, String ubicacion, int anioAdquisicion, EstadoEjemplar estado) {
        return new Ejemplar(codigo, ubicacion, LocalDate.of(anioAdquisicion, 1, 15), estado);
    }
}

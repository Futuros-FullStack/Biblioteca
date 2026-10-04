package com.mycompany.bibliotecasistema.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Libro del catálogo con sus ejemplares.
 */
public class Libro {

    private int id;
    private String titulo;
    private List<String> autores = new ArrayList<>();
    private String descripcion;
    private String isbn;
    private String editorial;
    private int anio;
    private String ciudad;
    private List<Ejemplar> ejemplares = new ArrayList<>();

    public Libro() {
    }

    public Libro(int id, String titulo, List<String> autores, String descripcion, String isbn,
            String editorial, int anio, String ciudad, List<Ejemplar> ejemplares) {
        this.id = id;
        this.titulo = titulo;
        this.autores = new ArrayList<>(autores);
        this.descripcion = descripcion;
        this.isbn = isbn;
        this.editorial = editorial;
        this.anio = anio;
        this.ciudad = ciudad;
        this.ejemplares = new ArrayList<>(ejemplares);
    }

    /** Autores separados por coma, para mostrar en las vistas. */
    public String getAutoresTexto() {
        return String.join(", ", autores);
    }

    /** "Ciudad - Año", tal como aparece en la portada de la tarjeta. */
    public String getCiudadAnio() {
        return ciudad + " - " + anio;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public List<String> getAutores() {
        return autores;
    }

    public void setAutores(List<String> autores) {
        this.autores = autores;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getEditorial() {
        return editorial;
    }

    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public List<Ejemplar> getEjemplares() {
        return ejemplares;
    }

    public void setEjemplares(List<Ejemplar> ejemplares) {
        this.ejemplares = ejemplares;
    }
}

package com.mycompany.bibliotecasistema.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Libro {

    private Integer idLibro;
    private String isbn;
    private String titulo;
    private String editorial;
    private Integer anioPublicacion;
    private String descripcion;
    // Nombre del archivo de la portada; es opcional (null si no tiene)
    private String portada;
    private List<Autor> autores = new ArrayList<>();
    // Datos que entrega la vista v_catalogo_libros
    private String autoresTexto;
    private int copiasDisponibles;
    // Conteos del inventario (LibroDAO.buscarInventario)
    private int totalEjemplares;
    private int copiasPrestadas;

    public Libro() {
    }

    public int getTotalEjemplares() {
        return totalEjemplares;
    }

    public void setTotalEjemplares(int totalEjemplares) {
        this.totalEjemplares = totalEjemplares;
    }

    public int getCopiasPrestadas() {
        return copiasPrestadas;
    }

    public void setCopiasPrestadas(int copiasPrestadas) {
        this.copiasPrestadas = copiasPrestadas;
    }

    public String getAutoresTexto() {
        return autoresTexto;
    }

    public void setAutoresTexto(String autoresTexto) {
        this.autoresTexto = autoresTexto;
    }

    public int getCopiasDisponibles() {
        return copiasDisponibles;
    }

    public void setCopiasDisponibles(int copiasDisponibles) {
        this.copiasDisponibles = copiasDisponibles;
    }

    public Integer getIdLibro() {
        return idLibro;
    }

    public void setIdLibro(Integer idLibro) {
        this.idLibro = idLibro;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getEditorial() {
        return editorial;
    }

    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }

    public Integer getAnioPublicacion() {
        return anioPublicacion;
    }

    public void setAnioPublicacion(Integer anioPublicacion) {
        this.anioPublicacion = anioPublicacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPortada() {
        return portada;
    }

    public void setPortada(String portada) {
        this.portada = portada;
    }

    public List<Autor> getAutores() {
        return Collections.unmodifiableList(autores);
    }

    public void setAutores(List<Autor> autores) {
        this.autores = autores == null ? new ArrayList<>() : new ArrayList<>(autores);
    }

    public void agregarAutor(Autor autor) {
        if (autor != null && !autores.contains(autor)) {
            autores.add(autor);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Libro)) {
            return false;
        }
        Libro otro = (Libro) o;
        return idLibro != null && idLibro.equals(otro.idLibro);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idLibro);
    }

    @Override
    public String toString() {
        return titulo;
    }
}

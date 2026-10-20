package com.mycompany.bibliotecasistema.model;

import java.util.Objects;

public class Autor {

    private Integer idAutor;
    private String nombre;

    public Autor() {
    }

    public Autor(Integer idAutor, String nombre) {
        this.idAutor = idAutor;
        this.nombre = nombre;
    }

    public Integer getIdAutor() {
        return idAutor;
    }

    public void setIdAutor(Integer idAutor) {
        this.idAutor = idAutor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Autor)) {
            return false;
        }
        Autor otro = (Autor) o;
        return idAutor != null && idAutor.equals(otro.idAutor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idAutor);
    }

    @Override
    public String toString() {
        return nombre;
    }
}

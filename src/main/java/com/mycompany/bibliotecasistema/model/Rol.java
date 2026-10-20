package com.mycompany.bibliotecasistema.model;

import java.util.Objects;

public class Rol {

    private Integer idRol;
    private String nombreRol;

    public Rol() {
    }

    public Rol(Integer idRol, String nombreRol) {
        this.idRol = idRol;
        this.nombreRol = nombreRol;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rol)) {
            return false;
        }
        Rol otro = (Rol) o;
        return idRol != null && idRol.equals(otro.idRol);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idRol);
    }

    @Override
    public String toString() {
        return nombreRol;
    }
}

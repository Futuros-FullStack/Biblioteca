package com.mycompany.bibliotecasistema.model;

import java.util.Objects;

public class Ubicacion {

    private Integer idUbicacion;
    private String pasillo;
    private String estante;
    private String fila;

    public Ubicacion() {
    }

    public Ubicacion(Integer idUbicacion, String pasillo, String estante, String fila) {
        this.idUbicacion = idUbicacion;
        this.pasillo = pasillo;
        this.estante = estante;
        this.fila = fila;
    }

    public Integer getIdUbicacion() {
        return idUbicacion;
    }

    public void setIdUbicacion(Integer idUbicacion) {
        this.idUbicacion = idUbicacion;
    }

    public String getPasillo() {
        return pasillo;
    }

    public void setPasillo(String pasillo) {
        this.pasillo = pasillo;
    }

    public String getEstante() {
        return estante;
    }

    public void setEstante(String estante) {
        this.estante = estante;
    }

    public String getFila() {
        return fila;
    }

    public void setFila(String fila) {
        this.fila = fila;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ubicacion)) {
            return false;
        }
        Ubicacion otra = (Ubicacion) o;
        return idUbicacion != null && idUbicacion.equals(otra.idUbicacion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idUbicacion);
    }

    @Override
    public String toString() {
        return "Pasillo " + pasillo + ", Estante " + estante + ", Fila " + fila;
    }
}

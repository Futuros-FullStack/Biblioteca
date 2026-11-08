package com.mycompany.bibliotecasistema.model;

import java.util.Objects;

public class Usuario {

    private Integer idUsuario;
    private Rol rol;
    private String nombre;
    private String correo;
    private String passwordHash;
    private String matricula;
    private boolean bloqueado;
    
    private int prestamosActivos;
    private int prestamosTotal;

    public Usuario() {
    }

    public int getPrestamosActivos() {
        return prestamosActivos;
    }

    public void setPrestamosActivos(int prestamosActivos) {
        this.prestamosActivos = prestamosActivos;
    }

    public int getPrestamosTotal() {
        return prestamosTotal;
    }

    public void setPrestamosTotal(int prestamosTotal) {
        this.prestamosTotal = prestamosTotal;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public void setBloqueado(boolean bloqueado) {
        this.bloqueado = bloqueado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario)) {
            return false;
        }
        Usuario otro = (Usuario) o;
        return idUsuario != null && idUsuario.equals(otro.idUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idUsuario);
    }

    @Override
    public String toString() {
        return "Usuario{idUsuario=" + idUsuario + ", nombre=" + nombre + ", matricula=" + matricula
                + ", correo=" + correo + ", bloqueado=" + bloqueado + "}";
    }
}

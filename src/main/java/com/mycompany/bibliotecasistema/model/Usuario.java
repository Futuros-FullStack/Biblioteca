package com.mycompany.bibliotecasistema.model;

/**
 * Usuario que inicia sesión en el sistema.
 */
public class Usuario {

    private String usuario;
    private String nombre;
    private Rol rol;

    public Usuario() {
    }

    public Usuario(String usuario, String nombre, Rol rol) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}

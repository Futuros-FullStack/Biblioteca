package com.mycompany.bibliotecasistema.util;

import com.mycompany.bibliotecasistema.model.Rol;
import com.mycompany.bibliotecasistema.model.Usuario;

/**
 * Usuario que tiene la sesión abierta (singleton).
 * Las vistas leen de aquí el nombre y el rol; nunca usan texto fijo.
 */
public final class Sesion {

    private static final Sesion INSTANCIA = new Sesion();

    private Usuario usuario;

    private Sesion() {
    }

    public static Sesion get() {
        return INSTANCIA;
    }

    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
    }

    /** El estudiante todavía no inicia sesión con cuenta propia: se guarda con nombre genérico. */
    public void iniciarComoEstudiante() {
        iniciar(new Usuario(null, Rol.ESTUDIANTE.getEtiqueta(), Rol.ESTUDIANTE));
    }

    public void cerrar() {
        usuario = null;
    }

    public boolean estaActiva() {
        return usuario != null;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return usuario == null ? "" : usuario.getNombre();
    }

    public Rol getRol() {
        return usuario == null ? null : usuario.getRol();
    }

    public boolean esAdministrador() {
        return getRol() == Rol.ADMINISTRADOR;
    }
}

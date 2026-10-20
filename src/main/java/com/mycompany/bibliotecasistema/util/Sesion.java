package com.mycompany.bibliotecasistema.util;

import com.mycompany.bibliotecasistema.model.Usuario;

// Guarda al administrador que inicio sesion; sirve para registrar prestamos (id_admin)
public final class Sesion {

    private static Usuario administrador;

    private Sesion() {
    }

    public static Usuario getAdministrador() {
        return administrador;
    }

    public static void iniciar(Usuario usuario) {
        administrador = usuario;
    }

    public static void cerrar() {
        administrador = null;
    }
}

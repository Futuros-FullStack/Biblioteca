package com.mycompany.bibliotecasistema.util;

import org.mindrot.jbcrypt.BCrypt;

public final class Seguridad {

    private Seguridad() {
    }

    public static String generarHash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verificar(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

package com.mycompany.bibliotecasistema.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilidades de texto para búsquedas sin distinguir mayúsculas ni acentos.
 */
public final class Texto {

    private Texto() {
    }

    /** Minúsculas, sin acentos y sin espacios extra. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    public static boolean estaVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    /** Iniciales de las dos primeras palabras: "Administrador General" -> "AG". */
    public static String iniciales(String nombre) {
        if (estaVacio(nombre)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String palabra : nombre.trim().split("\\s+")) {
            sb.append(Character.toUpperCase(palabra.charAt(0)));
            if (sb.length() == 2) {
                break;
            }
        }
        return sb.toString();
    }
}

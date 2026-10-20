package com.mycompany.bibliotecasistema.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javafx.scene.image.Image;

// Guarda y carga las imagenes de portada. En la base solo se guarda el nombre del archivo.
public final class PortadaLibro {

    private static final Path CARPETA = Paths.get(System.getProperty("user.home"), ".biblioteca", "portadas");

    private PortadaLibro() {
    }

    // Copia la imagen a la carpeta de portadas con un nombre unico y devuelve ese nombre
    public static String guardar(File origen) throws IOException {
        Files.createDirectories(CARPETA);
        String nombre = UUID.randomUUID() + extension(origen.getName());
        Files.copy(origen.toPath(), CARPETA.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        return nombre;
    }

    // Devuelve la imagen guardada, o null si no hay nombre o el archivo ya no existe
    public static Image cargar(String nombre, double ancho, double alto) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        File archivo = CARPETA.resolve(nombre).toFile();
        if (!archivo.exists()) {
            return null;
        }
        return new Image(archivo.toURI().toString(), ancho, alto, true, true);
    }

    private static String extension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        return punto < 0 ? "" : nombreArchivo.substring(punto).toLowerCase();
    }
}

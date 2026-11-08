package com.mycompany.bibliotecasistema.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionBD {

    private static final Path ARCHIVO_ENV = ubicarEnv();
    private static final Properties CONFIG = cargarEnv();

    private ConexionBD() {
    }

    private static Path ubicarEnv() {
        Path local = Path.of(".env");
        String datos = System.getenv("LOCALAPPDATA");
        if (Files.exists(local) || datos == null) {
            return local;
        }
        return Path.of(datos, "BibliotecaUNACH", ".env");
    }

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("DB_URL"),
                CONFIG.getProperty("DB_USER"),
                CONFIG.getProperty("DB_PASSWORD"));
    }

    public static String variable(String clave) {
        String valor = CONFIG.getProperty(clave);
        if (valor == null) {
            throw new IllegalStateException("Falta " + clave + " en el archivo .env");
        }
        return valor;
    }

    private static Properties cargarEnv() {
        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(ARCHIVO_ENV)) {
            propiedades.load(lector);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo .env", e);
        }
        return propiedades;
    }
}

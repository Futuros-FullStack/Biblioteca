package com.mycompany.bibliotecasistema.service;

import com.mycompany.bibliotecasistema.model.Usuario;
import java.util.Optional;

/**
 * Autenticación de administradores.
 * BACKEND: implementar contra la tabla de usuarios y registrar la clase en ServiceLocator.
 */
public interface AuthService {

    /**
     * @return el usuario autenticado, o vacío si las credenciales no son válidas.
     */
    Optional<Usuario> login(String usuario, String contrasena);
}

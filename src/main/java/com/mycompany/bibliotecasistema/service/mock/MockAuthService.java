package com.mycompany.bibliotecasistema.service.mock;

import com.mycompany.bibliotecasistema.model.Rol;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.service.AuthService;
import java.util.Optional;

/**
 * Autenticación de prueba: usuario "admin", contraseña "admin123".
 */
public class MockAuthService implements AuthService {

    @Override
    public Optional<Usuario> login(String usuario, String contrasena) {
        if ("admin".equalsIgnoreCase(usuario.trim()) && "admin123".equals(contrasena)) {
            return Optional.of(new Usuario("admin", "Administrador General", Rol.ADMINISTRADOR));
        }
        return Optional.empty();
    }
}

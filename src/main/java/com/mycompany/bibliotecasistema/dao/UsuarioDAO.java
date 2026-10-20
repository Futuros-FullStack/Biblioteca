package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Rol;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import com.mycompany.bibliotecasistema.util.Seguridad;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private static final String SELECT_USUARIO
            = "SELECT u.id_usuario, u.nombre, u.correo, u.password_hash, u.matricula, u.bloqueado, "
            + "r.id_rol, r.nombre_rol "
            + "FROM usuarios u INNER JOIN rol r ON u.id_rol = r.id_rol ";

    // Devuelve el administrador si las credenciales son correctas, si no devuelve null
    public Usuario autenticarAdministrador(String correo, String password) throws SQLException {
        Usuario admin = buscar(SELECT_USUARIO + "WHERE LOWER(u.correo) = LOWER(?) AND r.nombre_rol = 'Administrador'", correo);
        if (admin != null && Seguridad.verificar(password, admin.getPasswordHash())) {
            return admin;
        }
        return null;
    }

    public Usuario buscarAlumnoPorMatricula(String matricula) throws SQLException {
        return buscar(SELECT_USUARIO + "WHERE u.matricula = ? AND r.nombre_rol = 'Alumno'", matricula);
    }

    // Alumnos con sus conteos de prestamos. Busca por matricula o nombre; con texto vacio devuelve todos
    public List<Usuario> buscarAlumnos(String texto) throws SQLException {
        String sql = "SELECT u.id_usuario, u.matricula, u.nombre, u.correo, u.bloqueado, "
                + "(SELECT COUNT(*) FROM detalle_prestamo dp "
                + "INNER JOIN prestamos p ON dp.id_prestamo = p.id_prestamo "
                + "INNER JOIN estado_prestamo ep ON p.id_estado_prestamo = ep.id_estado_prestamo "
                + "WHERE p.id_usuario = u.id_usuario AND dp.fecha_devolucion_real IS NULL "
                + "AND ep.nombre_estado_prestamo IN ('ACTIVO', 'ATRASADO')) AS activos, "
                + "(SELECT COUNT(*) FROM prestamos p WHERE p.id_usuario = u.id_usuario) AS total "
                + "FROM usuarios u INNER JOIN rol r ON u.id_rol = r.id_rol "
                + "WHERE r.nombre_rol = 'Alumno' AND (u.matricula ILIKE ? OR u.nombre ILIKE ?) "
                + "ORDER BY u.id_usuario DESC";
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        List<Usuario> alumnos = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario alumno = new Usuario();
                    alumno.setIdUsuario(rs.getInt("id_usuario"));
                    alumno.setMatricula(rs.getString("matricula"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setCorreo(rs.getString("correo"));
                    alumno.setBloqueado(rs.getBoolean("bloqueado"));
                    alumno.setPrestamosActivos(rs.getInt("activos"));
                    alumno.setPrestamosTotal(rs.getInt("total"));
                    alumnos.add(alumno);
                }
            }
        }
        return alumnos;
    }

    // Usa la vista v_alumnos_bloqueados
    public List<Usuario> listarBloqueados() throws SQLException {
        String sql = "SELECT id_usuario, matricula, nombre, correo FROM v_alumnos_bloqueados ORDER BY nombre";
        List<Usuario> alumnos = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Usuario alumno = new Usuario();
                alumno.setIdUsuario(rs.getInt("id_usuario"));
                alumno.setMatricula(rs.getString("matricula"));
                alumno.setNombre(rs.getString("nombre"));
                alumno.setCorreo(rs.getString("correo"));
                alumno.setBloqueado(true);
                alumnos.add(alumno);
            }
        }
        return alumnos;
    }

    public void insertarAlumno(Usuario alumno) throws SQLException {
        String sql = "INSERT INTO usuarios (id_rol, nombre, correo, matricula) "
                + "VALUES ((SELECT id_rol FROM rol WHERE nombre_rol = 'Alumno'), ?, ?, ?) RETURNING id_usuario";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, alumno.getNombre());
            ps.setString(2, alumno.getCorreo());
            ps.setString(3, alumno.getMatricula());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    alumno.setIdUsuario(rs.getInt(1));
                }
            }
        }
    }

    public void actualizarAlumno(Usuario alumno) throws SQLException {
        String sql = "UPDATE usuarios SET nombre = ?, correo = ? WHERE id_usuario = ?";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, alumno.getNombre());
            ps.setString(2, alumno.getCorreo());
            ps.setInt(3, alumno.getIdUsuario());
            ps.executeUpdate();
        }
    }

    // La base lo rechaza (SQLState 23503) si el alumno tiene préstamos, aunque ya estén devueltos
    public void eliminarAlumno(int idUsuario) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE id_usuario = ? "
                + "AND id_rol = (SELECT id_rol FROM rol WHERE nombre_rol = 'Alumno')";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    private Usuario buscar(String sql, String valor) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setCorreo(rs.getString("correo"));
                usuario.setPasswordHash(rs.getString("password_hash"));
                usuario.setMatricula(rs.getString("matricula"));
                usuario.setBloqueado(rs.getBoolean("bloqueado"));
                usuario.setRol(new Rol(rs.getInt("id_rol"), rs.getString("nombre_rol")));
                return usuario;
            }
        }
    }
}

package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Rol;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import com.mycompany.bibliotecasistema.util.Seguridad;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

   
    public Usuario autenticarAdministrador(String correo, String password) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_autenticar_administrador(?)")) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && Seguridad.verificar(password, rs.getString("password"))) {
                    return mapear(rs);
                }
                return null;
            }
        }
    }

  
    public Usuario buscarAlumnoPorMatricula(String matricula) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_buscar_alumno_por_matricula(?)")) {
            ps.setString(1, matricula);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    
    public List<Usuario> buscarAlumnos(String texto) throws SQLException {
        List<Usuario> alumnos = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_buscar_alumnos(?)")) {
            ps.setString(1, texto == null ? "" : texto.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario alumno = new Usuario();
                    alumno.setIdUsuario(rs.getInt("id_usuario"));
                    alumno.setMatricula(rs.getString("matricula"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setCorreo(rs.getString("correo"));
                    alumno.setBloqueado(rs.getBoolean("bloqueado"));
                    alumno.setPrestamosActivos(rs.getInt("prestamos_activos"));
                    alumno.setPrestamosTotal(rs.getInt("prestamos_total"));
                    alumnos.add(alumno);
                }
            }
        }
        return alumnos;
    }

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
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("CALL p_registrar_alumno(?, ?, ?, ?)")) {
            ps.setString(1, alumno.getNombre());
            ps.setString(2, alumno.getCorreo());
            ps.setString(3, alumno.getMatricula());
            ps.setNull(4, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    alumno.setIdUsuario(rs.getInt(1));
                }
            }
        }
    }

    public void actualizarAlumno(Usuario alumno) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_actualizar_alumno(?, ?, ?, ?)")) {
            cs.setInt(1, alumno.getIdUsuario());
            cs.setString(2, alumno.getNombre());
            cs.setString(3, alumno.getCorreo());
            cs.setString(4, alumno.getMatricula());
            cs.execute();
        }
    }

    
    public void eliminarAlumno(int idUsuario) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_eliminar_alumno(?)")) {
            cs.setInt(1, idUsuario);
            cs.execute();
        }
    }

    
    public Libro buscarLibroPerdido(int idAlumno) throws SQLException {
        String sql = "SELECT l.id_libro, l.titulo FROM detalle_prestamo dp "
                + "INNER JOIN prestamos p ON p.id_prestamo = dp.id_prestamo "
                + "INNER JOIN ejemplares e ON e.id_ejemplar = dp.id_ejemplar "
                + "INNER JOIN estado_ejemplares ee ON ee.id_estado_ejemplar = e.id_estado_ejemplar "
                + "INNER JOIN libros l ON l.id_libro = e.id_libro "
                + "WHERE p.id_usuario = ? AND ee.nombre_estado_ejemplar = 'PERDIDO' "
                + "ORDER BY dp.fecha_devolucion_real DESC LIMIT 1";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAlumno);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Libro libro = new Libro();
                libro.setIdLibro(rs.getInt("id_libro"));
                libro.setTitulo(rs.getString("titulo"));
                return libro;
            }
        }
    }

    public void registrarReposicion(int idAlumno, int idLibro, int idUbicacion, String codigoBarras,
            LocalDate fechaAdquisicion) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_registrar_reposicion(?, ?, ?, ?, ?)")) {
            cs.setInt(1, idAlumno);
            cs.setInt(2, idLibro);
            cs.setInt(3, idUbicacion);
            cs.setString(4, codigoBarras);
            cs.setDate(5, Date.valueOf(fechaAdquisicion));
            cs.execute();
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setCorreo(rs.getString("correo"));
        usuario.setMatricula(rs.getString("matricula"));
        usuario.setBloqueado(rs.getBoolean("bloqueado"));
        usuario.setRol(new Rol(null, rs.getString("rol")));
        return usuario;
    }
}

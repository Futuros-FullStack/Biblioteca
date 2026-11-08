package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.DetallePrestamo;
import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoPrestamo;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Prestamo;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PrestamoDAO {

    private static final int DIAS_PRESTAMO = 2;

    public void registrar(String matricula, String codigoBarras, int idAdmin, String observaciones) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_registrar_prestamo(?, ?, ?, ?, ?)")) {
            cs.setString(1, matricula);
            cs.setString(2, codigoBarras);
            cs.setInt(3, idAdmin);
            cs.setInt(4, DIAS_PRESTAMO);
            cs.setString(5, observaciones);
            cs.execute();
        }
    }

    public LocalDate fechaEntregaPrevista() throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT fn_sumar_dias_habiles(CURRENT_DATE, ?)")) {
            ps.setInt(1, DIAS_PRESTAMO);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject(1, LocalDate.class);
            }
        }
    }

    public void devolver(int idPrestamo, int idEjemplar, String nombreEstadoEjemplar, String observaciones) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_devolucion_ejemplar(?, ?, ?, ?)")) {
            cs.setInt(1, idPrestamo);
            cs.setInt(2, idEjemplar);
            cs.setString(3, nombreEstadoEjemplar);
            cs.setString(4, observaciones);
            cs.execute();
        }
    }

    public void renovar(int idPrestamo, int idEjemplar) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_renovar_prestamo(?, ?)")) {
            cs.setInt(1, idPrestamo);
            cs.setInt(2, idEjemplar);
            cs.execute();
        }
    }

    public void anular(int idPrestamo) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_anular_prestamo(?)")) {
            cs.setInt(1, idPrestamo);
            cs.execute();
        }
    }

    public void actualizarRetrasos() throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_actualizar_retraso()")) {
            cs.execute();
        }
    }

    public List<DetallePrestamo> listarProximasEntregas(String busqueda) throws SQLException {
        return consultar("fn_proximas_entregas", texto(busqueda));
    }

    public List<DetallePrestamo> listarVencidos(String busqueda) throws SQLException {
        return consultar("fn_prestamos_vencidos", texto(busqueda));
    }

    public List<DetallePrestamo> listarDevueltos(String busqueda) throws SQLException {
        return consultar("fn_historial_prestamos", texto(busqueda));
    }

    public List<DetallePrestamo> listarEnPoder(int idAlumno) throws SQLException {
        return consultar("fn_listar_en_poder", idAlumno);
    }

    public List<DetallePrestamo> listarMasRecientes(String busqueda) throws SQLException {
        return consultar("fn_listar_prestamos_recientes", texto(busqueda));
    }

    private List<DetallePrestamo> consultar(String funcion, Object parametro) throws SQLException {
        List<DetallePrestamo> detalles = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM " + funcion + "(?)")) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario alumno = new Usuario();
                    alumno.setIdUsuario(rs.getInt("id_usuario"));
                    alumno.setNombre(rs.getString("nombre"));
                    alumno.setMatricula(rs.getString("matricula"));
                    alumno.setCorreo(rs.getString("correo"));

                    Prestamo prestamo = new Prestamo();
                    prestamo.setIdPrestamo(rs.getInt("id_prestamo"));
                    prestamo.setAlumno(alumno);
                    prestamo.setEstado(new EstadoPrestamo(rs.getInt("id_estado_prestamo"), rs.getString("nombre_estado_prestamo")));
                    prestamo.setFechaPrestamo(rs.getObject("fecha_prestamo", LocalDateTime.class));
                    prestamo.setFechaEntregaEsperada(rs.getObject("fecha_entrega_esperada", LocalDate.class));
                    prestamo.setRenovado(rs.getBoolean("renovado"));
                    prestamo.setFechaRenovacion(rs.getObject("fecha_renovacion", LocalDateTime.class));
                    prestamo.setObservaciones(rs.getString("observaciones"));

                    Libro libro = new Libro();
                    libro.setIdLibro(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));

                    Ejemplar ejemplar = new Ejemplar();
                    ejemplar.setIdEjemplar(rs.getInt("id_ejemplar"));
                    ejemplar.setCodigoBarras(rs.getString("codigo_barras"));
                    ejemplar.setLibro(libro);

                    DetallePrestamo detalle = new DetallePrestamo(prestamo, ejemplar);
                    detalle.setFechaDevolucionReal(rs.getObject("fecha_devolucion_real", LocalDateTime.class));
                    detalles.add(detalle);
                }
            }
        }
        return detalles;
    }

    private String texto(String busqueda) {
        return busqueda == null ? "" : busqueda.trim();
    }
}

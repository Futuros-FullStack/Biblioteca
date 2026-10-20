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

    // Una fila por ejemplar prestado (RN-63). Se usa cuando las vistas no traen los datos necesarios
    private static final String SELECT_DETALLE
            = "SELECT p.id_prestamo, p.fecha_prestamo, p.fecha_entrega_esperada, p.renovado, "
            + "p.fecha_renovacion, p.observaciones, "
            + "ep.id_estado_prestamo, ep.nombre_estado_prestamo, "
            + "u.id_usuario, u.nombre, u.matricula, u.correo, "
            + "e.id_ejemplar, e.codigo_barras, l.id_libro, l.titulo, "
            + "dp.fecha_devolucion_real "
            + "FROM prestamos p "
            + "INNER JOIN detalle_prestamo dp ON p.id_prestamo = dp.id_prestamo "
            + "INNER JOIN usuarios u ON p.id_usuario = u.id_usuario "
            + "INNER JOIN estado_prestamo ep ON p.id_estado_prestamo = ep.id_estado_prestamo "
            + "INNER JOIN ejemplares e ON dp.id_ejemplar = e.id_ejemplar "
            + "INNER JOIN libros l ON e.id_libro = l.id_libro ";

    // ---------- Procedimientos almacenados ----------

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

    // Fecha de entrega que tendria un prestamo hecho hoy (2 dias habiles), calculada por la base de datos
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

    public void actualizarRetrasos() throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_actualizar_retraso()")) {
            cs.execute();
        }
    }

    // ---------- Vistas ----------

    // Vista v_prestamos_activos
    public List<DetallePrestamo> listarProximasEntregas(String busqueda) throws SQLException {
        String sql = "SELECT id_prestamo, lector, libro, codigo_barras, fecha_prestamo, fecha_entrega_esperada, estado_prestamo, matricula, id_ejemplar "
                + "FROM v_prestamos_activos WHERE estado_prestamo = 'ACTIVO'";
        boolean hayBusqueda = hayTexto(busqueda);
        if (hayBusqueda) {
            sql += " AND (matricula ILIKE ? OR lector ILIKE ?)";
        }
        sql += " ORDER BY fecha_entrega_esperada ASC";
        List<DetallePrestamo> detalles = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            if (hayBusqueda) {
                ps.setString(1, patron(busqueda));
                ps.setString(2, patron(busqueda));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario alumno = new Usuario();
                    alumno.setNombre(rs.getString("lector"));
                    alumno.setMatricula(rs.getString("matricula"));

                    Prestamo prestamo = new Prestamo();
                    prestamo.setIdPrestamo(rs.getInt("id_prestamo"));
                    prestamo.setAlumno(alumno);
                    prestamo.setEstado(new EstadoPrestamo(null, rs.getString("estado_prestamo")));
                    prestamo.setFechaPrestamo(rs.getObject("fecha_prestamo", LocalDateTime.class));
                    prestamo.setFechaEntregaEsperada(rs.getObject("fecha_entrega_esperada", LocalDate.class));

                    Libro libro = new Libro();
                    libro.setTitulo(rs.getString("libro"));

                    Ejemplar ejemplar = new Ejemplar();
                    ejemplar.setIdEjemplar(rs.getInt("id_ejemplar"));
                    ejemplar.setCodigoBarras(rs.getString("codigo_barras"));
                    ejemplar.setLibro(libro);

                    detalles.add(new DetallePrestamo(prestamo, ejemplar));
                }
            }
        }
        return detalles;
    }

    // Vista v_prestamos_vencidos. Los dias de atraso = hoy - fecha de entrega esperada
    public List<DetallePrestamo> listarVencidos(String busqueda) throws SQLException {
        String sql = "SELECT id_prestamo, matricula, lector, correo, libro, codigo_barras, fecha_prestamo, "
                + "fecha_entrega_esperada, estado_prestamo, id_ejemplar FROM v_prestamos_vencidos";
        boolean hayBusqueda = hayTexto(busqueda);
        if (hayBusqueda) {
            sql += " WHERE matricula ILIKE ? OR lector ILIKE ?";
        }
        sql += " ORDER BY fecha_entrega_esperada ASC";
        List<DetallePrestamo> detalles = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            if (hayBusqueda) {
                ps.setString(1, patron(busqueda));
                ps.setString(2, patron(busqueda));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario alumno = new Usuario();
                    alumno.setNombre(rs.getString("lector"));
                    alumno.setMatricula(rs.getString("matricula"));
                    alumno.setCorreo(rs.getString("correo"));

                    Prestamo prestamo = new Prestamo();
                    prestamo.setIdPrestamo(rs.getInt("id_prestamo"));
                    prestamo.setAlumno(alumno);
                    prestamo.setEstado(new EstadoPrestamo(null, rs.getString("estado_prestamo")));
                    prestamo.setFechaPrestamo(rs.getObject("fecha_prestamo", LocalDateTime.class));
                    prestamo.setFechaEntregaEsperada(rs.getObject("fecha_entrega_esperada", LocalDate.class));

                    Libro libro = new Libro();
                    libro.setTitulo(rs.getString("libro"));

                    Ejemplar ejemplar = new Ejemplar();
                    ejemplar.setIdEjemplar(rs.getInt("id_ejemplar"));
                    ejemplar.setCodigoBarras(rs.getString("codigo_barras"));
                    ejemplar.setLibro(libro);

                    detalles.add(new DetallePrestamo(prestamo, ejemplar));
                }
            }
        }
        return detalles;
    }

    // Vista v_historial_prestamos
    public List<DetallePrestamo> listarDevueltos(String busqueda) throws SQLException {
        String sql = "SELECT id_prestamo, matricula, lector, libro, codigo_barras, fecha_prestamo, "
                + "fecha_entrega_esperada, fecha_devolucion_real, estado FROM v_historial_prestamos";
        boolean hayBusqueda = hayTexto(busqueda);
        if (hayBusqueda) {
            sql += " WHERE matricula ILIKE ? OR lector ILIKE ?";
        }
        sql += " ORDER BY fecha_prestamo DESC";
        List<DetallePrestamo> detalles = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            if (hayBusqueda) {
                ps.setString(1, patron(busqueda));
                ps.setString(2, patron(busqueda));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario alumno = new Usuario();
                    alumno.setNombre(rs.getString("lector"));
                    alumno.setMatricula(rs.getString("matricula"));

                    Prestamo prestamo = new Prestamo();
                    prestamo.setIdPrestamo(rs.getInt("id_prestamo"));
                    prestamo.setAlumno(alumno);
                    prestamo.setEstado(new EstadoPrestamo(null, rs.getString("estado")));
                    prestamo.setFechaPrestamo(rs.getObject("fecha_prestamo", LocalDateTime.class));
                    prestamo.setFechaEntregaEsperada(rs.getObject("fecha_entrega_esperada", LocalDate.class));

                    Libro libro = new Libro();
                    libro.setTitulo(rs.getString("libro"));

                    Ejemplar ejemplar = new Ejemplar();
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

    // ---------- Consultas directas a las tablas (ninguna vista trae estos datos) ----------

    // Libros que el alumno tiene en su poder (maximo 2)
    public List<DetallePrestamo> listarEnPoder(int idAlumno) throws SQLException {
        String filtro = "u.id_usuario = " + idAlumno
                + " AND dp.fecha_devolucion_real IS NULL AND ep.nombre_estado_prestamo IN ('ACTIVO', 'ATRASADO')";
        return consultar(filtro, "p.fecha_entrega_esperada", null);
    }

    public List<DetallePrestamo> listarMasRecientes(String busqueda) throws SQLException {
        return consultar("TRUE", "p.fecha_prestamo DESC", busqueda);
    }

    // filtro y orden son texto fijo escrito en esta clase; la busqueda del usuario va como parametro
    private List<DetallePrestamo> consultar(String filtro, String orden, String busqueda) throws SQLException {
        boolean hayBusqueda = hayTexto(busqueda);
        String sql = SELECT_DETALLE + "WHERE " + filtro;
        if (hayBusqueda) {
            sql += " AND (u.matricula ILIKE ? OR u.nombre ILIKE ?)";
        }
        sql += " ORDER BY " + orden;

        List<DetallePrestamo> detalles = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            if (hayBusqueda) {
                ps.setString(1, patron(busqueda));
                ps.setString(2, patron(busqueda));
            }
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

    private boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private String patron(String texto) {
        return "%" + texto.trim() + "%";
    }
}

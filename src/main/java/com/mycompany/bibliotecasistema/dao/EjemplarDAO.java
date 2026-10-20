package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Ubicacion;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EjemplarDAO {

    private static final String SELECT_EJEMPLAR
            = "SELECT e.id_ejemplar, e.codigo_barras, e.fecha_adquisicion, "
            + "l.id_libro, l.titulo, l.isbn, "
            + "ub.id_ubicacion, ub.pasillo, ub.estante, ub.fila, "
            + "ee.id_estado_ejemplar, ee.nombre_estado_ejemplar "
            + "FROM ejemplares e "
            + "INNER JOIN libros l ON e.id_libro = l.id_libro "
            + "INNER JOIN ubicaciones ub ON e.id_ubicacion = ub.id_ubicacion "
            + "INNER JOIN estado_ejemplares ee ON e.id_estado_ejemplar = ee.id_estado_ejemplar ";

    public List<Ejemplar> listarPorLibro(int idLibro) throws SQLException {
        return consultar(SELECT_EJEMPLAR + "WHERE e.id_libro = ? ORDER BY e.codigo_barras", String.valueOf(idLibro), true);
    }

    public Ejemplar buscarPorCodigo(String codigoBarras) throws SQLException {
        List<Ejemplar> resultado = consultar(SELECT_EJEMPLAR + "WHERE e.codigo_barras = ?", codigoBarras, false);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    // Usa la vista v_catalogo_libros
    public int contarDisponibles(int idLibro) throws SQLException {
        String sql = "SELECT copias_disponibles FROM v_catalogo_libros WHERE id_libro = ?";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idLibro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Codigo consecutivo global con formato BC-001, BC-002...
    public String siguienteCodigoBarras() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_barras FROM 4) AS INT)), 0) + 1 "
                + "FROM ejemplares WHERE codigo_barras ~ '^BC-[0-9]+$'";
        try (Connection con = ConexionBD.getConexion();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return String.format("BC-%03d", rs.getInt(1));
        }
    }

    // El ejemplar nuevo siempre queda Disponible
    public void insertar(Ejemplar ejemplar) throws SQLException {
        String sql = "INSERT INTO ejemplares (id_libro, id_ubicacion, id_estado_ejemplar, codigo_barras, fecha_adquisicion) "
                + "VALUES (?, ?, (SELECT id_estado_ejemplar FROM estado_ejemplares WHERE nombre_estado_ejemplar = ?), ?, ?) "
                + "RETURNING id_ejemplar";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ejemplar.getLibro().getIdLibro());
            ps.setInt(2, ejemplar.getUbicacion().getIdUbicacion());
            ps.setString(3, EstadoEjemplar.DISPONIBLE);
            ps.setString(4, ejemplar.getCodigoBarras());
            ps.setObject(5, ejemplar.getFechaAdquisicion());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ejemplar.setIdEjemplar(rs.getInt(1));
                }
            }
        }
    }

    public void cambiarEstado(int idEjemplar, String nombreEstado) throws SQLException {
        String sql = "UPDATE ejemplares SET id_estado_ejemplar = "
                + "(SELECT id_estado_ejemplar FROM estado_ejemplares WHERE nombre_estado_ejemplar = ?) "
                + "WHERE id_ejemplar = ?";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreEstado);
            ps.setInt(2, idEjemplar);
            ps.executeUpdate();
        }
    }

    public void actualizar(int idEjemplar, String nombreEstado, LocalDate fechaAdquisicion) throws SQLException {
        String sql = "UPDATE ejemplares SET fecha_adquisicion = ?, id_estado_ejemplar = "
                + "(SELECT id_estado_ejemplar FROM estado_ejemplares WHERE nombre_estado_ejemplar = ?) "
                + "WHERE id_ejemplar = ?";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaAdquisicion);
            ps.setString(2, nombreEstado);
            ps.setInt(3, idEjemplar);
            ps.executeUpdate();
        }
    }

    // Un ejemplar con prestamos o cambios de ubicacion ya no se puede borrar de la base
    public boolean tieneHistorial(int idEjemplar) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM detalle_prestamo WHERE id_ejemplar = ?) "
                + "OR EXISTS (SELECT 1 FROM historial_ubicaciones WHERE id_ejemplar = ?)";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEjemplar);
            ps.setInt(2, idEjemplar);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    public void eliminar(int idEjemplar) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("DELETE FROM ejemplares WHERE id_ejemplar = ?")) {
            ps.setInt(1, idEjemplar);
            ps.executeUpdate();
        }
    }

    // Se mueven todos los ejemplares del libro juntos (RN-62)
    public void reubicarLibro(int idLibro, int idUbicacion) throws SQLException {
        String sql = "UPDATE ejemplares SET id_ubicacion = ? WHERE id_libro = ?";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUbicacion);
            ps.setInt(2, idLibro);
            ps.executeUpdate();
        }
    }

    private List<Ejemplar> consultar(String sql, String valor, boolean esNumero) throws SQLException {
        List<Ejemplar> ejemplares = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            if (esNumero) {
                ps.setInt(1, Integer.parseInt(valor));
            } else {
                ps.setString(1, valor);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Libro libro = new Libro();
                    libro.setIdLibro(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setIsbn(rs.getString("isbn"));

                    Ejemplar ejemplar = new Ejemplar();
                    ejemplar.setIdEjemplar(rs.getInt("id_ejemplar"));
                    ejemplar.setCodigoBarras(rs.getString("codigo_barras"));
                    ejemplar.setFechaAdquisicion(rs.getObject("fecha_adquisicion", LocalDate.class));
                    ejemplar.setLibro(libro);
                    ejemplar.setUbicacion(new Ubicacion(rs.getInt("id_ubicacion"), rs.getString("pasillo"),
                            rs.getString("estante"), rs.getString("fila")));
                    ejemplar.setEstado(new EstadoEjemplar(rs.getInt("id_estado_ejemplar"), rs.getString("nombre_estado_ejemplar")));
                    ejemplares.add(ejemplar);
                }
            }
        }
        return ejemplares;
    }
}

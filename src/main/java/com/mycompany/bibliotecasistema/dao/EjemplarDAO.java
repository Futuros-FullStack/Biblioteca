package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Ubicacion;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EjemplarDAO {

    public List<Ejemplar> listarPorLibro(int idLibro) throws SQLException {
        return consultar("fn_listar_ejemplares_por_libro", idLibro);
    }

    public Ejemplar buscarPorCodigo(String codigoBarras) throws SQLException {
        List<Ejemplar> resultado = consultar("fn_buscar_ejemplar_por_codigo", codigoBarras);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public int contarDisponibles(int idLibro) throws SQLException {
        return escalar("SELECT fn_contar_disponibles(?)", idLibro, Integer.class);
    }

    public String siguienteCodigoBarras() throws SQLException {
        return escalar("SELECT fn_siguiente_codigo_barras()", null, String.class);
    }

    public void insertar(Ejemplar ejemplar) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("CALL p_registrar_ejemplar(?, ?, ?, ?, ?)")) {
            ps.setInt(1, ejemplar.getLibro().getIdLibro());
            ps.setInt(2, ejemplar.getUbicacion().getIdUbicacion());
            ps.setString(3, ejemplar.getCodigoBarras());
            ps.setObject(4, ejemplar.getFechaAdquisicion());
            ps.setNull(5, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ejemplar.setIdEjemplar(rs.getInt(1));
                }
            }
        }
    }

    public void cambiarEstado(int idEjemplar, String nombreEstado) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_cambiar_estado_ejemplar(?, ?)")) {
            cs.setInt(1, idEjemplar);
            cs.setString(2, nombreEstado);
            cs.execute();
        }
    }

    public void actualizar(int idEjemplar, String nombreEstado, LocalDate fechaAdquisicion) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_actualizar_ejemplar(?, ?, ?)")) {
            cs.setInt(1, idEjemplar);
            cs.setString(2, nombreEstado);
            cs.setObject(3, fechaAdquisicion);
            cs.execute();
        }
    }

    public boolean tieneHistorial(int idEjemplar) throws SQLException {
        return escalar("SELECT fn_ejemplar_tiene_historial(?)", idEjemplar, Boolean.class);
    }

    public void eliminar(int idEjemplar) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_eliminar_ejemplar(?)")) {
            cs.setInt(1, idEjemplar);
            cs.execute();
        }
    }

    public void reubicarLibro(int idLibro, int idUbicacion, int idAdmin, String motivo) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_reubicar_libro(?, ?, ?, ?)")) {
            cs.setInt(1, idLibro);
            cs.setInt(2, idUbicacion);
            cs.setInt(3, idAdmin);
            cs.setString(4, motivo);
            cs.execute();
        }
    }

    private <T> T escalar(String sql, Object parametro, Class<T> tipo) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setObject(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject(1, tipo);
            }
        }
    }

    private List<Ejemplar> consultar(String funcion, Object parametro) throws SQLException {
        List<Ejemplar> ejemplares = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM " + funcion + "(?)")) {
            ps.setObject(1, parametro);
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

package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Autor;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class LibroDAO {

    public List<Libro> buscarCatalogo(String texto) throws SQLException {
        return buscarCatalogo(texto, Integer.MAX_VALUE);
    }

    public List<Libro> buscarCatalogo(String texto, int limite) throws SQLException {
        List<Libro> libros = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_buscar_catalogo(?) LIMIT ?")) {
            ps.setString(1, texto(texto));
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Libro libro = new Libro();
                    libro.setIdLibro(rs.getInt("id_libro"));
                    libro.setIsbn(rs.getString("isbn"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setEditorial(rs.getString("editorial"));
                    libro.setPortada(rs.getString("portada"));
                    libro.setAutoresTexto(rs.getString("autores"));
                    libro.setCopiasDisponibles(rs.getInt("copias_disponibles"));
                    libros.add(libro);
                }
            }
        }
        return libros;
    }

    public List<Libro> buscarInventario(String texto) throws SQLException {
        return buscarInventario(texto, Integer.MAX_VALUE);
    }

    public List<Libro> buscarInventario(String texto, int limite) throws SQLException {
        List<Libro> libros = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_buscar_inventario(?) LIMIT ?")) {
            ps.setString(1, texto(texto));
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Libro libro = new Libro();
                    libro.setIdLibro(rs.getInt("id_libro"));
                    libro.setTitulo(rs.getString("titulo"));
                    libro.setIsbn(rs.getString("isbn"));
                    libro.setTotalEjemplares(rs.getInt("total"));
                    libro.setCopiasDisponibles(rs.getInt("disponibles"));
                    libro.setCopiasPrestadas(rs.getInt("prestados"));
                    libros.add(libro);
                }
            }
        }
        return libros;
    }

    public Libro buscarPorId(int idLibro) throws SQLException {
        return buscarUno("fn_buscar_libro_por_id", idLibro);
    }

    public Libro buscarPorIsbn(String isbn) throws SQLException {
        return buscarUno("fn_buscar_libro_por_isbn", isbn);
    }

    
    public void registrar(Libro libro, List<String> nombresAutores) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("CALL p_registrar_libro(?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, libro.getIsbn());
            ps.setString(2, libro.getTitulo());
            ps.setString(3, libro.getEditorial());
            ps.setObject(4, libro.getAnioPublicacion());
            ps.setString(5, libro.getDescripcion());
            ps.setString(6, libro.getPortada());
            ps.setArray(7, con.createArrayOf("text", nombresAutores.toArray()));
            ps.setNull(8, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    libro.setIdLibro(rs.getInt(1));
                }
            }
        }
    }

    public void actualizar(Libro libro) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_actualizar_libro(?, ?, ?, ?, ?, ?, ?)")) {
            cs.setInt(1, libro.getIdLibro());
            cs.setString(2, libro.getIsbn());
            cs.setString(3, libro.getTitulo());
            cs.setString(4, libro.getEditorial());
            cs.setObject(5, libro.getAnioPublicacion());
            cs.setString(6, libro.getDescripcion());
            cs.setString(7, libro.getPortada());
            cs.execute();
        }
    }

    public void eliminar(int idLibro) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_eliminar_libro(?)")) {
            cs.setInt(1, idLibro);
            cs.execute();
        }
    }

    private Libro buscarUno(String funcion, Object valor) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM " + funcion + "(?)")) {
            ps.setObject(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Libro libro = new Libro();
                libro.setIdLibro(rs.getInt("id_libro"));
                libro.setIsbn(rs.getString("isbn"));
                libro.setTitulo(rs.getString("titulo"));
                libro.setEditorial(rs.getString("editorial"));
                libro.setAnioPublicacion(rs.getObject("anio_publicacion", Integer.class));
                libro.setDescripcion(rs.getString("descripcion"));
                libro.setPortada(rs.getString("portada"));
                libro.setAutores(listarAutores(con, libro.getIdLibro()));
                return libro;
            }
        }
    }

    private List<Autor> listarAutores(Connection con, int idLibro) throws SQLException {
        List<Autor> autores = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_listar_autores_libro(?)")) {
            ps.setInt(1, idLibro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    autores.add(new Autor(rs.getInt("id_autor"), rs.getString("nombre")));
                }
            }
        }
        return autores;
    }

    private String texto(String busqueda) {
        return busqueda == null ? "" : busqueda.trim();
    }
}

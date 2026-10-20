package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Autor;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LibroDAO {

    private static final String SELECT_LIBRO
            = "SELECT l.id_libro, l.isbn, l.titulo, l.editorial, l.anio_publicacion, l.descripcion, "
            + "p.url_portada AS portada FROM libros l LEFT JOIN portada p ON p.id_libro = l.id_libro ";

    // Usa la vista v_catalogo_libros. Busca por titulo, autor o ISBN; con texto vacio devuelve todos
    public List<Libro> buscarCatalogo(String texto) throws SQLException {
        String sql = "SELECT id_libro, isbn, titulo, editorial, portada, autores, copias_disponibles "
                + "FROM v_catalogo_libros WHERE titulo ILIKE ? OR isbn ILIKE ? OR autores ILIKE ? ORDER BY titulo";
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        List<Libro> libros = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
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

    // Un renglon por libro con sus conteos de ejemplares. Busca por titulo, ISBN o codigo de barras de un ejemplar
    public List<Libro> buscarInventario(String texto) throws SQLException {
        String sql = "SELECT l.id_libro, l.titulo, l.isbn, COUNT(e.id_ejemplar) AS total, "
                + "COUNT(*) FILTER (WHERE ee.nombre_estado_ejemplar = ?) AS disponibles, "
                + "COUNT(*) FILTER (WHERE ee.nombre_estado_ejemplar = ?) AS prestados "
                + "FROM libros l "
                + "LEFT JOIN ejemplares e ON e.id_libro = l.id_libro "
                + "LEFT JOIN estado_ejemplares ee ON ee.id_estado_ejemplar = e.id_estado_ejemplar "
                + "WHERE l.titulo ILIKE ? OR l.isbn ILIKE ? OR EXISTS "
                + "(SELECT 1 FROM ejemplares x WHERE x.id_libro = l.id_libro AND x.codigo_barras ILIKE ?) "
                + "GROUP BY l.id_libro ORDER BY l.titulo";
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        List<Libro> libros = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, EstadoEjemplar.DISPONIBLE);
            ps.setString(2, EstadoEjemplar.PRESTADO);
            ps.setString(3, patron);
            ps.setString(4, patron);
            ps.setString(5, patron);
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
        return buscarUno("WHERE l.id_libro = ?", idLibro);
    }

    public Libro buscarPorIsbn(String isbn) throws SQLException {
        return buscarUno("WHERE l.isbn = ?", isbn);
    }

    // Inserta el libro y sus autores en una sola transaccion
    public void insertar(Libro libro) throws SQLException {
        String sqlLibro = "INSERT INTO libros (isbn, titulo, editorial, anio_publicacion, descripcion) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id_libro";
        String sqlAutor = "INSERT INTO libro_autor (id_libro, id_autor) VALUES (?, ?)";
        try (Connection con = ConexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sqlLibro)) {
                    llenarDatos(ps, libro);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        libro.setIdLibro(rs.getInt(1));
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(sqlAutor)) {
                    for (Autor autor : libro.getAutores()) {
                        ps.setInt(1, libro.getIdLibro());
                        ps.setInt(2, autor.getIdAutor());
                        ps.executeUpdate();
                    }
                }
                guardarPortada(con, libro);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // Registra el libro con sus autores (los que no existen se crean). Los ejemplares se agregan aparte
    public void registrar(Libro libro, List<String> nombresAutores) throws SQLException {
        AutorDAO autorDAO = new AutorDAO();

        for (String nombre : nombresAutores) {
            Autor autor = autorDAO.buscarPorNombre(nombre);
            if (autor == null) {
                autor = new Autor(null, nombre);
                autorDAO.insertar(autor);
            }
            libro.agregarAutor(autor);
        }
        insertar(libro);
    }

    public void actualizar(Libro libro) throws SQLException {
        String sql = "UPDATE libros SET isbn = ?, titulo = ?, editorial = ?, anio_publicacion = ?, descripcion = ? "
                + "WHERE id_libro = ?";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            llenarDatos(ps, libro);
            ps.setInt(6, libro.getIdLibro());
            ps.executeUpdate();
            guardarPortada(con, libro);
        }
    }

    // Sus autores y su portada se borran solos; la base rechaza el borrado si el libro tiene ejemplares
    public void eliminar(int idLibro) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("DELETE FROM libros WHERE id_libro = ?")) {
            ps.setInt(1, idLibro);
            ps.executeUpdate();
        }
    }

    // La portada es opcional y vive en su propia tabla; si el libro ya tenia una se reemplaza
    private void guardarPortada(Connection con, Libro libro) throws SQLException {
        if (libro.getPortada() == null) {
            return;
        }
        String sql = "INSERT INTO portada (id_libro, url_portada) VALUES (?, ?) "
                + "ON CONFLICT (id_libro) DO UPDATE SET url_portada = EXCLUDED.url_portada";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, libro.getIdLibro());
            ps.setString(2, libro.getPortada());
            ps.executeUpdate();
        }
    }

    private Libro buscarUno(String condicion, Object valor) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(SELECT_LIBRO + condicion)) {
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

    private void llenarDatos(PreparedStatement ps, Libro libro) throws SQLException {
        ps.setString(1, libro.getIsbn());
        ps.setString(2, libro.getTitulo());
        ps.setString(3, libro.getEditorial());
        ps.setObject(4, libro.getAnioPublicacion());
        ps.setString(5, libro.getDescripcion());
    }

    private List<Autor> listarAutores(Connection con, int idLibro) throws SQLException {
        String sql = "SELECT a.id_autor, a.nombre FROM autores a "
                + "INNER JOIN libro_autor la ON a.id_autor = la.id_autor WHERE la.id_libro = ? ORDER BY a.nombre";
        List<Autor> autores = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idLibro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    autores.add(new Autor(rs.getInt("id_autor"), rs.getString("nombre")));
                }
            }
        }
        return autores;
    }
}

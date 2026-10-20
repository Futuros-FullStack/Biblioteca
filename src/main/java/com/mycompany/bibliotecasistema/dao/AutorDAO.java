package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Autor;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AutorDAO {

    public List<Autor> buscar(String texto) throws SQLException {
        String sql = "SELECT id_autor, nombre FROM autores WHERE nombre ILIKE ? ORDER BY nombre";
        List<Autor> autores = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    autores.add(new Autor(rs.getInt("id_autor"), rs.getString("nombre")));
                }
            }
        }
        return autores;
    }

    // Devuelve el autor con ese nombre exacto (sin importar mayusculas) o null si no existe
    public Autor buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT id_autor, nombre FROM autores WHERE LOWER(nombre) = LOWER(?)";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Autor(rs.getInt("id_autor"), rs.getString("nombre")) : null;
            }
        }
    }

    public void insertar(Autor autor) throws SQLException {
        String sql = "INSERT INTO autores (nombre) VALUES (?) RETURNING id_autor";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, autor.getNombre());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    autor.setIdAutor(rs.getInt(1));
                }
            }
        }
    }
}

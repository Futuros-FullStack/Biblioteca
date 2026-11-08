package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Autor;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AutorDAO {

    public List<Autor> buscar(String texto) throws SQLException {
        List<Autor> autores = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_buscar_autores(?)")) {
            ps.setString(1, texto == null ? "" : texto.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    autores.add(new Autor(rs.getInt("id_autor"), rs.getString("nombre")));
                }
            }
        }
        return autores;
    }

    public Autor registrar(String nombre) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("CALL sp_registrar_autor(?, ?)")) {
            ps.setString(1, nombre);
            ps.setNull(2, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Autor(rs.getInt(1), nombre.trim());
            }
        }
    }
}

package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Ubicacion;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class UbicacionDAO {

    public Ubicacion buscarOCrear(String pasillo, String estante, String fila) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("CALL sp_buscar_o_crear_ubicacion(?, ?, ?, ?)")) {
            ps.setString(1, pasillo);
            ps.setString(2, estante);
            ps.setString(3, fila);
            ps.setNull(4, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Ubicacion(rs.getInt(1), pasillo, estante, fila);
            }
        }
    }

    public Ubicacion buscarPorLibro(int idLibro) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_ubicacion_de_libro(?)")) {
            ps.setInt(1, idLibro);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Ubicacion(rs.getInt("id_ubicacion"), rs.getString("pasillo"),
                        rs.getString("estante"), rs.getString("fila"));
            }
        }
    }
}

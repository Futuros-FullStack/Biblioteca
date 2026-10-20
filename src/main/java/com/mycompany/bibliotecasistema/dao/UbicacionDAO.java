package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Ubicacion;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UbicacionDAO {

    
    public Ubicacion buscarOCrear(String pasillo, String estante, String fila) throws SQLException {
        String sqlBuscar = "SELECT id_ubicacion FROM ubicaciones WHERE pasillo = ? AND estante = ? AND fila = ?";
        String sqlCrear = "INSERT INTO ubicaciones (pasillo, estante, fila) VALUES (?, ?, ?) RETURNING id_ubicacion";
        try (Connection con = ConexionBD.getConexion()) {
            try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
                ps.setString(1, pasillo);
                ps.setString(2, estante);
                ps.setString(3, fila);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Ubicacion(rs.getInt(1), pasillo, estante, fila);
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sqlCrear)) {
                ps.setString(1, pasillo);
                ps.setString(2, estante);
                ps.setString(3, fila);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return new Ubicacion(rs.getInt(1), pasillo, estante, fila);
                }
            }
        }
    }


    public Ubicacion buscarPorLibro(int idLibro) throws SQLException {
        String sql = "SELECT ub.id_ubicacion, ub.pasillo, ub.estante, ub.fila "
                + "FROM ubicaciones ub INNER JOIN ejemplares e ON e.id_ubicacion = ub.id_ubicacion "
                + "WHERE e.id_libro = ? LIMIT 1";
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
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

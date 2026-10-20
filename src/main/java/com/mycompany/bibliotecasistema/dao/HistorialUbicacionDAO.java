package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.HistorialUbicacion;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Ubicacion;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistorialUbicacionDAO {

    
    public List<HistorialUbicacion> listar() throws SQLException {
        String sql = "SELECT id_historial, libro, codigo_barras, fecha_cambio, bibliotecario_responsable, motivo, "
                + "pasillo_anterior, estante_anterior, fila_anterior, pasillo_nuevo, estante_nuevo, fila_nueva FROM v_ubicaciones";
        List<HistorialUbicacion> historial = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Libro libro = new Libro();
                libro.setTitulo(rs.getString("libro"));

                Ejemplar ejemplar = new Ejemplar();
                ejemplar.setCodigoBarras(rs.getString("codigo_barras"));
                ejemplar.setLibro(libro);

                Usuario admin = new Usuario();
                admin.setNombre(rs.getString("bibliotecario_responsable"));

                // La vista no trae los ids de las ubicaciones
                HistorialUbicacion registro = new HistorialUbicacion();
                registro.setIdHistorial(rs.getInt("id_historial"));
                registro.setEjemplar(ejemplar);
                registro.setAdminResponsable(admin);
                registro.setFechaCambio(rs.getObject("fecha_cambio", LocalDateTime.class));
                registro.setMotivo(rs.getString("motivo"));
                String pasilloAnterior = rs.getString("pasillo_anterior");
                if (pasilloAnterior != null) {
                    registro.setUbicacionAnterior(new Ubicacion(null, pasilloAnterior,
                            rs.getString("estante_anterior"), rs.getString("fila_anterior")));
                }
                registro.setUbicacionNueva(new Ubicacion(null, rs.getString("pasillo_nuevo"),
                        rs.getString("estante_nuevo"), rs.getString("fila_nueva")));
                historial.add(registro);
            }
        }
        return historial;
    }
}

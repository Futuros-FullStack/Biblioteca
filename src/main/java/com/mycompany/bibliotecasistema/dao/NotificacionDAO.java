package com.mycompany.bibliotecasistema.dao;

import com.mycompany.bibliotecasistema.model.DetallePrestamo;
import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoNotificacion;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Notificacion;
import com.mycompany.bibliotecasistema.model.Prestamo;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificacionDAO {

    public void generar() throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_generar_notificaciones()")) {
            cs.execute();
        }
    }

    public List<Notificacion> listarPendientes() throws SQLException {
        List<Notificacion> pendientes = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_notificaciones_pendientes()");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Usuario alumno = new Usuario();
                alumno.setIdUsuario(rs.getInt("id_usuario"));
                alumno.setNombre(rs.getString("nombre"));
                alumno.setCorreo(rs.getString("correo"));

                Prestamo prestamo = new Prestamo();
                prestamo.setAlumno(alumno);

                Libro libro = new Libro();
                libro.setTitulo(rs.getString("titulo"));

                Ejemplar ejemplar = new Ejemplar();
                ejemplar.setLibro(libro);

                Notificacion notificacion = new Notificacion();
                notificacion.setIdNotificacion(rs.getInt("id_notificacion"));
                notificacion.setFechaEntregaAvisada(rs.getObject("fecha_entrega_avisada", LocalDate.class));
                notificacion.setDetalle(new DetallePrestamo(prestamo, ejemplar));
                pendientes.add(notificacion);
            }
        }
        return pendientes;
    }

    public void marcar(List<Integer> ids, String nombreEstado, String detalleError) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_marcar_notificaciones(?, ?, ?)")) {
            cs.setArray(1, con.createArrayOf("integer", ids.toArray()));
            cs.setString(2, nombreEstado);
            cs.setString(3, detalleError);
            cs.execute();
        }
    }

    public Map<String, Notificacion> listarVigentes() throws SQLException {
        Map<String, Notificacion> avisos = new HashMap<>();
        try (Connection con = ConexionBD.getConexion();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM fn_avisos_vigentes()");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Notificacion aviso = new Notificacion();
                aviso.setIdNotificacion(rs.getInt("id_notificacion"));
                aviso.setEstado(new EstadoNotificacion(null, rs.getString("nombre_estado_noti")));
                aviso.setDetalleError(rs.getString("detalle_error"));
                avisos.put(rs.getInt("id_prestamo") + "|" + rs.getInt("id_ejemplar"), aviso);
            }
        }
        return avisos;
    }

    public void reenviar(int idNotificacion) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
                CallableStatement cs = con.prepareCall("CALL p_reenviar_notificacion(?)")) {
            cs.setInt(1, idNotificacion);
            cs.execute();
        }
    }
}

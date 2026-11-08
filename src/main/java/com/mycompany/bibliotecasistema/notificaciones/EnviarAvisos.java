package com.mycompany.bibliotecasistema.notificaciones;

import com.mycompany.bibliotecasistema.dao.NotificacionDAO;
import com.mycompany.bibliotecasistema.model.EstadoNotificacion;
import com.mycompany.bibliotecasistema.model.Notificacion;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.ConexionBD;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;


public class EnviarAvisos {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
    private static final int TIEMPO_LIMITE_MS = 15000;

    public static void main(String[] args) {
        NotificacionDAO dao = new NotificacionDAO();
        try {
            dao.generar();
            List<Notificacion> pendientes = dao.listarPendientes();
            if (pendientes.isEmpty()) {
                System.out.println("Sin avisos pendientes.");
                return;
            }
            enviar(dao, agrupar(pendientes));
        } catch (Exception e) {
     
            System.err.println("Error: " + e);
            System.exit(1);
        }
    }

   
    private static Map<String, List<Notificacion>> agrupar(List<Notificacion> pendientes) {
        Map<String, List<Notificacion>> grupos = new LinkedHashMap<>();
        for (Notificacion n : pendientes) {
            String clave = n.getDetalle().getPrestamo().getAlumno().getIdUsuario() + "|" + n.getFechaEntregaAvisada();
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(n);
        }
        return grupos;
    }

    private static void enviar(NotificacionDAO dao, Map<String, List<Notificacion>> grupos) throws Exception {
        String remitente = ConexionBD.variable("SMTP_USER");
        Session sesion = Session.getInstance(propiedades());
        try (Transport transporte = sesion.getTransport("smtp")) {
            transporte.connect(remitente, ConexionBD.variable("SMTP_PASSWORD"));
            for (List<Notificacion> grupo : grupos.values()) {
                List<Integer> ids = new ArrayList<>();
                for (Notificacion n : grupo) {
                    ids.add(n.getIdNotificacion());
                }
                String destino = grupo.get(0).getDetalle().getPrestamo().getAlumno().getCorreo();
                try {
                    MimeMessage mensaje = armar(sesion, remitente, grupo);
                    transporte.sendMessage(mensaje, mensaje.getAllRecipients());
                    dao.marcar(ids, EstadoNotificacion.EXITOSO, null);
                    System.out.println("Enviado a " + destino);
                } catch (MessagingException | UnsupportedEncodingException e) {
                    String detalle = detalle(e);
                    dao.marcar(ids, EstadoNotificacion.FALLIDO, detalle);
                    System.err.println("Falló " + destino + ": " + detalle);
                }
            }
        }
    }

  
    private static String detalle(Exception e) {
        StringBuilder detalle = new StringBuilder(e.getMessage());
        Exception siguiente = e instanceof MessagingException ? ((MessagingException) e).getNextException() : null;
        while (siguiente != null) {
            detalle.append(" | ").append(siguiente.getMessage());
            siguiente = siguiente instanceof MessagingException ? ((MessagingException) siguiente).getNextException() : null;
        }
        return detalle.toString();
    }

    private static Properties propiedades() {
        Properties p = new Properties();
        p.put("mail.smtp.host", ConexionBD.variable("SMTP_HOST"));
        p.put("mail.smtp.port", ConexionBD.variable("SMTP_PORT"));
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.starttls.required", "true");
        p.put("mail.smtp.connectiontimeout", String.valueOf(TIEMPO_LIMITE_MS));
        p.put("mail.smtp.timeout", String.valueOf(TIEMPO_LIMITE_MS));
        p.put("mail.smtp.writetimeout", String.valueOf(TIEMPO_LIMITE_MS));
        return p;
    }

    private static MimeMessage armar(Session sesion, String remitente, List<Notificacion> grupo)
            throws MessagingException, UnsupportedEncodingException {
        Usuario alumno = grupo.get(0).getDetalle().getPrestamo().getAlumno();
        String fecha = grupo.get(0).getFechaEntregaAvisada().format(FECHA);
        boolean varios = grupo.size() > 1;
        StringBuilder libros = new StringBuilder();
        for (Notificacion n : grupo) {
            libros.append("  - ").append(n.getDetalle().getEjemplar().getLibro().getTitulo()).append('\n');
        }
        String texto = "Hola " + alumno.getNombre() + ",\n\n"
                + "Te recordamos que " + (varios ? "los siguientes libros vencen" : "el siguiente libro vence")
                + " el " + fecha + ":\n\n" + libros
                + "\nPor favor entrégal" + (varios ? "os" : "o") + " en la biblioteca a tiempo.\n\nBiblioteca UNACH";

        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(remitente, "Biblioteca UNACH", "UTF-8"));
        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(alumno.getCorreo()));
        mensaje.setSubject("Recordatorio: tu préstamo vence el " + fecha, "UTF-8");
        mensaje.setText(texto, "UTF-8");
        return mensaje;
    }
}

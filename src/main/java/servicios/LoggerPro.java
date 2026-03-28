package servicios;

import config.ConexionBD;
import config.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class LoggerPro {

    public static void registrar(String accion, String detalles) {
        // Ejecutar en un hilo separado para no alentar la interfaz
        new Thread(() -> {
            try (Connection conn = ConexionBD.conectar()) {
                String sql = "INSERT INTO logs_sistema (usuario, accion, detalles) VALUES (?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, Sesion.usuarioActual);
                ps.setString(2, accion);
                ps.setString(3, detalles);
                ps.executeUpdate();
            } catch (Exception e) {
                System.err.println("Error al guardar log: " + e.getMessage());
            }
        }).start();
    }
}
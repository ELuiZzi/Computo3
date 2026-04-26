package ui.paneles;

import util.Estilos;
import config.ConexionBD;
import config.Sesion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class PanelDashboard extends JPanel {

    public PanelDashboard() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Lógica de Saludo
        String user = Sesion.usuarioActual;
        if (user != null && user.length() > 0) {
            user = user.substring(0, 1).toUpperCase() + user.substring(1);
        }

        String saludo = "Bienvenid" + ("F".equals(Sesion.generoActual) ? "a" : "o");

        JLabel lblTitulo = new JLabel("Hola, " + user + ". " + saludo + " a LUMTECH");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setBorder(new EmptyBorder(0, 0, 40, 0)); // Más margen abajo

        add(lblTitulo, BorderLayout.NORTH);

        // Panel de Tarjetas
        JPanel pCards = new JPanel(new GridLayout(1, 3, 20, 0));
        pCards.setBackground(Estilos.COLOR_FONDO);

        pCards.add(crearTarjeta("Ventas del Día", obtenerVentasHoy(), new Color(41, 98, 255)));
        pCards.add(crearTarjeta("Servicios en Taller", obtenerServiciosPendientes(), new Color(255, 140, 0)));
        pCards.add(crearTarjeta("Productos Agotados", obtenerAgotados(), new Color(231, 76, 60)));

        JPanel pCentro = new JPanel(new BorderLayout());
        pCentro.setBackground(Estilos.COLOR_FONDO);
        pCentro.add(pCards, BorderLayout.NORTH);

        JLabel lblLogoFondo = new JLabel("", SwingConstants.CENTER);
        try {
            ImageIcon icon = new ImageIcon("recursos/logo.png");
            Image img = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            lblLogoFondo.setIcon(new ImageIcon(img));
        } catch(Exception e){}
        pCentro.add(lblLogoFondo, BorderLayout.CENTER);

        add(pCentro, BorderLayout.CENTER);
    }

    private JPanel crearTarjeta(String titulo, String valor, Color colorBorde) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Estilos.COLOR_PANEL);
        p.setBorder(BorderFactory.createMatteBorder(0, 6, 0, 0, colorBorde));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(Color.LIGHT_GRAY);
        lblTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblTitulo.setBorder(new EmptyBorder(15, 20, 0, 0));

        JLabel lblValor = new JLabel(valor);
        lblValor.setForeground(Color.WHITE);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblValor.setBorder(new EmptyBorder(5, 20, 15, 0));

        p.add(lblTitulo, BorderLayout.NORTH);
        p.add(lblValor, BorderLayout.CENTER);
        return p;
    }

    private String obtenerVentasHoy() {
        try (Connection c = ConexionBD.conectar()) {
            ResultSet rs = c.createStatement().executeQuery("SELECT SUM(total_venta) FROM ventas WHERE DATE(fecha) = CURDATE()");
            if(rs.next()) return "$" + (rs.getString(1) == null ? "0.00" : rs.getString(1));
        } catch(Exception e){} return "$0.00";
    }

    private String obtenerServiciosPendientes() {
        try (Connection c = ConexionBD.conectar()) {
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM ordenes_servicio WHERE estado != 'ENTREGADO'");
            if(rs.next()) return rs.getString(1) + " Equipos";
        } catch(Exception e){} return "0";
    }

    private String obtenerAgotados() {
        try (Connection c = ConexionBD.conectar()) {
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM productos WHERE stock = 0 AND activo = 1");
            if(rs.next()) return rs.getString(1) + " Prods.";
        } catch(Exception e){} return "0";
    }
}
package ui.paneles;

import util.Estilos;
import config.ConexionBD;
import config.Sesion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class PanelDashboard extends JPanel {

    private JProgressBar barraMeta;
    private JLabel lblEstadoMeta;
    private final double META_DIARIA = 200.00;

    public PanelDashboard() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Lógica de Saludo
        String user = Sesion.usuarioActual;
        // Capitalizar primera letra (juan -> Juan)
        if (user != null && user.length() > 0) {
            user = user.substring(0, 1).toUpperCase() + user.substring(1);
        }

        String saludo = "Bienvenid" + ("F".equals(Sesion.generoActual) ? "a" : "o");

        // Título con Bienvenida
        JLabel lblTitulo = new JLabel("Hola, " + user + ". " + saludo + " a LUMTECH");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setBorder(new EmptyBorder(0, 0, 20, 0));

        add(lblTitulo, BorderLayout.NORTH);

        // Panel de Tarjetas
        JPanel pCards = new JPanel(new GridLayout(1, 3, 20, 0));
        pCards.setBackground(Estilos.COLOR_FONDO);

        // Tarjeta 1: Ventas Hoy
        pCards.add(crearTarjeta("Ventas del Día", obtenerVentasHoy(), new Color(41, 98, 255)));
        // Tarjeta 2: Servicios Pendientes
        pCards.add(crearTarjeta("Servicios en Taller", obtenerServiciosPendientes(), new Color(255, 140, 0)));
        // Tarjeta 3: Faltantes Urgentes
        pCards.add(crearTarjeta("Productos Agotados", obtenerAgotados(), new Color(231, 76, 60)));

        // Podrías agregar el logo grande en el centro abajo
        JPanel pCentro = new JPanel(new BorderLayout());
        pCentro.setBackground(Estilos.COLOR_FONDO);
        pCentro.add(pCards, BorderLayout.NORTH);

        // Imagen decorativa o logo
        JLabel lblLogoFondo = new JLabel("", SwingConstants.CENTER);
        try {
            ImageIcon icon = new ImageIcon("recursos/logo.png");
            Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            lblLogoFondo.setIcon(new ImageIcon(img));
        } catch(Exception e){}
        pCentro.add(lblLogoFondo, BorderLayout.CENTER);

        add(pCentro, BorderLayout.CENTER);

        // NUEVO: SECCIÓN DE META COMERCIAL
        JPanel panelMeta = new JPanel(new BorderLayout(10, 10));
        panelMeta.setBackground(new Color(30, 35, 50));
        panelMeta.setBorder(BorderFactory.createTitledBorder(null, "OBJETIVO DE VENTAS DIARIO", 0, 0, Estilos.FONT_BOLD, Color.ORANGE));

        barraMeta = new JProgressBar(0, (int)META_DIARIA);
        barraMeta.setStringPainted(true);
        barraMeta.setFont(new Font("Segoe UI", Font.BOLD, 18));
        barraMeta.setPreferredSize(new Dimension(400, 40));

        lblEstadoMeta = new JLabel("Calculando ventas de hoy...");
        lblEstadoMeta.setForeground(Color.WHITE);
        lblEstadoMeta.setFont(new Font("Segoe UI", Font.BOLD, 14));

        panelMeta.add(barraMeta, BorderLayout.CENTER);
        panelMeta.add(lblEstadoMeta, BorderLayout.SOUTH);

        add(panelMeta, BorderLayout.NORTH); // Añadir arriba en tu dashboard

        calcularVentasHoy();
    }
    public void calcularVentasHoy() {
        double totalHoy = 0;
        String fechaHoy = LocalDate.now().toString();

        // Asumiendo que usas ConexionBD para sumar las ventas del día
        try (Connection conn = config.ConexionBD.conectar()) {
            String sql = "SELECT SUM(total_venta) FROM ventas WHERE DATE(fecha) = '" + fechaHoy + "'";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()) totalHoy = rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }

        barraMeta.setValue((int)totalHoy);

        if (totalHoy < META_DIARIA) {
            barraMeta.setForeground(new Color(220, 50, 50)); // ROJO: Fracaso/Peligro
            double faltante = META_DIARIA - totalHoy;
            barraMeta.setString("$" + totalHoy + " / $" + META_DIARIA);
            lblEstadoMeta.setText("URGENTE: Faltan $" + String.format("%.2f", faltante) + " para la meta.");
            lblEstadoMeta.setForeground(Color.RED);
        } else {
            barraMeta.setForeground(new Color(46, 204, 113)); // VERDE: Éxito
            barraMeta.setString("$" + totalHoy + " (¡META SUPERADA!)");
            lblEstadoMeta.setText("EXCELENTE. Vamos por más. ¡No te detengas!");
            lblEstadoMeta.setForeground(Color.GREEN);
        }
    }

    private JPanel crearTarjeta(String titulo, String valor, Color colorBorde) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Estilos.COLOR_PANEL);
        // Borde izquierdo de color
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

    // Métodos de consulta rápida
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
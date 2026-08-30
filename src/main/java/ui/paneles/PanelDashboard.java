package ui.paneles;

import ui.componentes.JOptionPanePro;
import util.Estilos;
import config.ConexionBD;
import config.Sesion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PanelDashboard extends JPanel {

    JButton btnNotificaciones;

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



        // Dentro del constructor de PanelDashboard()

// 1. Instanciar y configurar el botón para que parezca un icono plano
        btnNotificaciones = new javax.swing.JButton();
        btnNotificaciones.setIcon(new javax.swing.ImageIcon("recursos/campana.png"));
        btnNotificaciones.setToolTipText("Notificaciones CRM");
        btnNotificaciones.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)); // Manita al pasar el mouse
// Las siguientes 3 líneas hacen que el fondo y los bordes del botón sean invisibles
        btnNotificaciones.setContentAreaFilled(false);
        btnNotificaciones.setBorderPainted(false);
        btnNotificaciones.setFocusPainted(false);

// 2. Le asignamos la acción del clic que creamos anteriormente
        btnNotificaciones.addActionListener(e -> procesarNotificacionesCRM());

// 3. Crear un panel contenedor (Wrapper) para el encabezado
        javax.swing.JPanel panelEncabezado = new javax.swing.JPanel(new java.awt.BorderLayout());
        panelEncabezado.setOpaque(false); // Respeta el color de fondo oscuro de tu panel principal

// 4. Agregar los elementos a los extremos
// "lblBienvenida" es tu JLabel que dice "Hola, Luis..."
        panelEncabezado.add(lblTitulo, java.awt.BorderLayout.WEST);
        panelEncabezado.add(btnNotificaciones, java.awt.BorderLayout.EAST);

        // Panel de Tarjetas
        JPanel pCards = new JPanel(new GridLayout(1, 3, 20, 0));
        pCards.setBackground(Estilos.COLOR_FONDO);

        pCards.add(crearTarjeta("Ventas del Día", obtenerVentasHoy(), new Color(41, 98, 255)));
        pCards.add(crearTarjeta("Servicios en Taller", obtenerServiciosPendientes(), new Color(255, 140, 0)));
        pCards.add(crearTarjeta("Productos Agotados", obtenerAgotados(), new Color(231, 76, 60)));

        ;

        JPanel pCentro = new JPanel(new BorderLayout());
        pCentro.setBackground(Estilos.COLOR_FONDO);
        pCentro.add(pCards, BorderLayout.NORTH);

        JLabel lblLogoFondo = new JLabel("", SwingConstants.CENTER);
        try {
            ImageIcon icon = new ImageIcon("recursos/logo2.png");
            Image img = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            lblLogoFondo.setIcon(new ImageIcon(img));
        } catch(Exception e){}
        pCentro.add(lblLogoFondo, BorderLayout.CENTER);

        add(panelEncabezado, java.awt.BorderLayout.NORTH);
        add(pCentro, BorderLayout.CENTER);

        verificarNotificacionesCRM();
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
        try (Connection c = ConexionBD.conectar();
             java.sql.Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT SUM(total_venta) FROM ventas WHERE DATE(fecha) = CURDATE()")) {
            if(rs.next()) return "$" + (rs.getString(1) == null ? "0.00" : rs.getString(1));
        } catch(Exception e) { servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelDashboard.obtenerVentasHoy: " + e.getMessage()); e.printStackTrace(); } return "$0.00";
    }

    private String obtenerServiciosPendientes() {
        try (Connection c = ConexionBD.conectar();
             java.sql.Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ordenes_servicio WHERE estado != 'ENTREGADO'")) {
            if(rs.next()) return rs.getString(1) + " Equipos";
        } catch(Exception e) { servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelDashboard.obtenerServiciosPendientes: " + e.getMessage()); e.printStackTrace(); } return "0";
    }

    private String obtenerAgotados() {
        try (Connection c = ConexionBD.conectar();
             java.sql.Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM productos WHERE stock = 0 AND activo = 1")) {
            if(rs.next()) return rs.getString(1) + " Prods.";
        } catch(Exception e) { servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelDashboard.obtenerAgotados: " + e.getMessage()); e.printStackTrace(); } return "0";
    }

    private void verificarNotificacionesCRM() {
        String sqlMantenimiento = "SELECT COUNT(*) FROM ordenes_servicio WHERE DATE(fecha_entrega) = DATE_SUB(CURDATE(), INTERVAL 1 YEAR) AND apto_mantenimiento = 1";
        String sqlOffice = "SELECT COUNT(*) FROM suscripciones_office WHERE fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 1 MONTH) AND notificado = 0";
        
        try (Connection conn = ConexionBD.conectar()) {
            int pendientes = 0;
            
            try (PreparedStatement psM = conn.prepareStatement(sqlMantenimiento); ResultSet rsM = psM.executeQuery()) {
                if (rsM.next()) pendientes += rsM.getInt(1);
            }
            
            try (PreparedStatement psO = conn.prepareStatement(sqlOffice); ResultSet rsO = psO.executeQuery()) {
                if (rsO.next()) pendientes += rsO.getInt(1);
            }
            
            if (pendientes > 0) {
                btnNotificaciones.setIcon(new javax.swing.ImageIcon("recursos/campana_alerta.png"));
                btnNotificaciones.setToolTipText("Tienes " + pendientes + " notificaciones pendientes.");
            } else {
                btnNotificaciones.setIcon(new javax.swing.ImageIcon("recursos/campana.png"));
                btnNotificaciones.setToolTipText("Sin notificaciones pendientes.");
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelDashboard.verificarNotificacionesCRM: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void procesarNotificacionesCRM() {
        String sqlMantenimiento = "SELECT o.id, c.nombre, c.telefono, o.dispositivo, o.marca_modelo " +
                "FROM ordenes_servicio o " +
                "INNER JOIN clientes c ON o.id_cliente = c.id " +
                "WHERE DATE(o.fecha_entrega) = DATE_SUB(CURDATE(), INTERVAL 1 YEAR) " +
                "AND o.apto_mantenimiento = 1";

        String sqlOffice = "SELECT s.id AS id_suscripcion, c.nombre, c.telefono " +
                "FROM suscripciones_office s " +
                "INNER JOIN ordenes_servicio o ON s.id_orden = o.id " +
                "INNER JOIN clientes c ON o.id_cliente = c.id " +
                "WHERE s.fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 1 MONTH) " +
                "AND s.notificado = 0";

        try (Connection conn = ConexionBD.conectar()) {
            java.util.List<String> listaMostrar = new java.util.ArrayList<>();
            java.util.List<Runnable> accionesWhatsApp = new java.util.ArrayList<>();
            
            // 1. Recopilar Mantenimientos
            try (PreparedStatement psM = conn.prepareStatement(sqlMantenimiento); ResultSet rsM = psM.executeQuery()) {
                while (rsM.next()) {
                    int idOrden = rsM.getInt("id");
                    String nombre = rsM.getString("nombre");
                    String telefono = rsM.getString("telefono").replaceAll("[^0-9]", "");
                    String equipo = rsM.getString("marca_modelo") + " marca: " + rsM.getString("dispositivo");
                    
                    listaMostrar.add(nombre + " - Mantenimiento");
                    
                    if (telefono.length() == 10) telefono = "52" + telefono;
                    String mensaje = "Hola, soy un asistente virtual de Lumtech. Te escribo para avisarte que hace 1 año recogiste tu equipo modelo: " + equipo + ", esperemos lo sigas disfrutando, y para que puedas seguir haciéndolo por mucho tiempo más te recomendamos realizar un mantenimiento a tu equipo. Si gustas agendar una cita o pedir más informes dímelo. Que tengas un excelente día.";
                    String finalTelefono = telefono;
                    
                    accionesWhatsApp.add(() -> {
                        try {
                            String url = "https://wa.me/" + finalTelefono + "?text=" + java.net.URLEncoder.encode(mensaje, "UTF-8").replace("+", "%20");
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                            try (PreparedStatement psU = conn.prepareStatement("UPDATE ordenes_servicio SET apto_mantenimiento = 0 WHERE id = ?")) {
                                psU.setInt(1, idOrden);
                                psU.executeUpdate();
                            }
                            Thread.sleep(500);
                        } catch (Exception ex) { ex.printStackTrace(); }
                    });
                }
            }

            // 2. Recopilar Office 365
            try (PreparedStatement psO = conn.prepareStatement(sqlOffice); ResultSet rsO = psO.executeQuery()) {
                while (rsO.next()) {
                    int idSusc = rsO.getInt("id_suscripcion");
                    String nombre = rsO.getString("nombre");
                    String telefono = rsO.getString("telefono").replaceAll("[^0-9]", "");
                    
                    listaMostrar.add(nombre + " - Office");
                    
                    if (telefono.length() == 10) telefono = "52" + telefono;
                    String mensaje = "Hola " + (nombre != null ? nombre : "") + ", somos de Lumtech. Te recordamos que tu suscripción de Office 365 está por vencer en el próximo mes. ¡Planifica tu renovación con nosotros para no perder acceso a tus programas! Que tengas un excelente día.";
                    String finalTelefono = telefono;
                    
                    accionesWhatsApp.add(() -> {
                        try {
                            String url = "https://wa.me/" + finalTelefono + "?text=" + java.net.URLEncoder.encode(mensaje, "UTF-8").replace("+", "%20");
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                            try (PreparedStatement psU = conn.prepareStatement("UPDATE suscripciones_office SET notificado = 1 WHERE id = ?")) {
                                psU.setInt(1, idSusc);
                                psU.executeUpdate();
                            }
                            Thread.sleep(500);
                        } catch (Exception ex) { ex.printStackTrace(); }
                    });
                }
            }

            if (listaMostrar.isEmpty()) {
                JOptionPanePro.mostrarMensaje(this, "Al Día", "No tienes notificaciones pendientes.", "INFO");
                return;
            }

            // 3. Mostrar Pop-up
            StringBuilder sb = new StringBuilder("Tienes las siguientes notificaciones:\n\n");
            for (String item : listaMostrar) {
                sb.append("• ").append(item).append("\n");
            }
            sb.append("\n¿Deseas enviar los mensajes de WhatsApp ahora?");
            
            int resp = JOptionPane.showConfirmDialog(this, sb.toString(), "Notificaciones CRM", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            
            if (resp == JOptionPane.YES_OPTION) {
                for (Runnable accion : accionesWhatsApp) {
                    accion.run();
                }
                JOptionPanePro.mostrarMensaje(this, "CRM Completado", "Se han procesado las notificaciones.", "INFO");
                verificarNotificacionesCRM();
            }

        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelDashboard.procesarNotificacionesCRM: " + e.getMessage());
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error", "Ocurrió un problema al procesar las notificaciones.", "ERROR");
        }
    }
}
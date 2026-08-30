package ui.ventanas;

import com.formdev.flatlaf.FlatDarkLaf;
import config.Sesion;
import servicios.BackupManager;
import servicios.LoggerPro;
import ui.componentes.BotonPro;
import ui.componentes.JOptionPanePro;
import util.Estilos;
import util.Recursos;
import servicios.ImpresoraTicket;
import ui.componentes.TabbedPanePro;
import ui.paneles.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class SistemaPOS extends JFrame {

    TabbedPanePro tabs;
    private BotonPro btnCerrarSesion;

    public SistemaPOS(String rolUsuario) {

        setTitle("Lumtech " + rolUsuario);
        setResizable(false);
        setSize(1280, 768);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Image iconoApp = Recursos.getImagenApp();
        if (iconoApp != null) setIconImage(iconoApp);
        getContentPane().setBackground(Estilos.COLOR_FONDO);

        // INICIAR RESPALDO AUTOMÁTICO
        BackupManager.iniciarScheduler();

        // Estilizar Tabs
        tabs = new TabbedPanePro();
        tabs.setFont(Estilos.FONT_BOLD);
        tabs.setBackground(Estilos.COLOR_PANEL);
        tabs.setForeground(new java.awt.Color(200, 200, 200));

        // Instancias
        PanelVentas pVentas = new PanelVentas();
        PanelInventario pInventario = new PanelInventario();
        PanelFinanzas pFinanzas = new PanelFinanzas();
        PanelServicios pServicios = new PanelServicios();
        PanelFaltantes pFaltantes = new PanelFaltantes();
        PanelClientes pClientes = new PanelClientes(pServicios, tabs);

        tabs.addTab("INICIO", new PanelDashboard());
        tabs.addTab("CAJA", pVentas);
        tabs.addTab("TALLER", pServicios);

        if ("ADMIN".equals(rolUsuario)) {
            tabs.addTab("PRODUCTOS", pInventario);
            tabs.addTab("SURTIR", pFaltantes);
            tabs.addTab("FINANZAS", pFinanzas);
            tabs.addTab("CLIENTES", pClientes);
        } else {
            tabs.addTab("SURTIR", pFaltantes);
        }

        PanelConfiguracion pConfig = new PanelConfiguracion(rolUsuario);
        tabs.addTab("", pConfig);

        int indexConfig = tabs.getTabCount() - 1;
        tabs.setIconAt(indexConfig, Recursos.getIcono("tuerca.png"));
        tabs.setToolTipTextAt(indexConfig, "Configuración del Sistema");

        final int[] pestanaAnterior = {0};

        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            int indiceActual = tabs.getSelectedIndex();
            Component prev = tabs.getComponentAt(pestanaAnterior[0]);

            if (prev instanceof PanelFinanzas && c != prev) {
                ((PanelFinanzas)prev).limpiarDatos();
            }

            // 1. SEGURIDAD: Verificar si es un área restringida
            if(c instanceof PanelFinanzas || c instanceof PanelConfiguracion) {

                boolean accesoConcedido = solicitarPinSeguridad();

                if (accesoConcedido) {
                    pestanaAnterior[0] = indiceActual; // Actualizar historial, acceso permitido
                } else {
                    tabs.setSelectedIndex(pestanaAnterior[0]); // Rechazado o cerrado, regresar
                    return; // Aborta la carga del resto del código
                }

            } else {
                pestanaAnterior[0] = indiceActual; // Es un panel libre
            }

            if(c == pVentas) { pVentas.cargarCatalogo(); pVentas.darFocoCodigo(); }
            else if(c instanceof PanelFaltantes) ((PanelFaltantes)c).cargarFaltantes();
            else if(c instanceof PanelFinanzas) ((PanelFinanzas)c).consultar();
            else if(c instanceof PanelDashboard) {
                tabs.setComponentAt(0, new PanelDashboard());
            }
        });

        btnCerrarSesion = new BotonPro("", "logout.png", new Color(0,0,0,0), this::cerrarSesion);
        btnCerrarSesion.setSize(35, 35);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(tabs, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(btnCerrarSesion, JLayeredPane.PALETTE_LAYER);

        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream("config.properties")) {
            props.load(fis);
            String tabInicial = props.getProperty("ui.pestaña_inicial", "INICIO");

            for(int i = 0; i < tabs.getTabCount(); i++) {
                if(tabs.getTitleAt(i).equalsIgnoreCase(tabInicial)) {
                    tabs.setSelectedIndex(i);
                    break;
                }
            }
        } catch (Exception ex) {}

        setContentPane(layeredPane);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int contentW = getContentPane().getWidth();
                int contentH = getContentPane().getHeight();
                tabs.setBounds(0, 0, contentW, contentH);
                btnCerrarSesion.setBounds(contentW - 50, 6, 35, 35);
            }
        });
    }

    private void cerrarSesion() {
        boolean confirmar = JOptionPanePro.mostrarConfirmacion(this, "Cerrar Sesión", "¿Deseas cerrar sesión?");
        if (confirmar) {
            LoggerPro.registrar("LOGIN", "Cierre de sesión: " + Sesion.usuarioActual);

            java.util.Properties props = new java.util.Properties();
            java.io.File archivo = new java.io.File("config.properties");
            if (archivo.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(archivo)) { props.load(fis); } catch(Exception ex){}
            }
            props.remove("session.user");
            props.remove("session.pass");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(archivo)) { props.store(fos, "Configuracion Sistema POS"); } catch(Exception ex){}

            this.dispose();
            new Login().setVisible(true);
        }
    }

    private boolean validarContrasenaEnBD(String password) {
        try (java.sql.Connection conn = config.ConexionBD.conectar();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM usuarios_sistema WHERE usuario = ? AND password = ?")) {
            ps.setString(1, Sesion.usuarioActual);
            ps.setString(2, password);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            LoggerPro.registrar("ERROR_DB", "Fallo en SistemaPOS.validarContrasenaEnBD: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Mexico_City"));

        try {
            // 1. PRIMERO instalamos el tema base de FlatLaf
            FlatDarkLaf.setup();

            // 2. DESPUÉS sobrescribimos los colores a nuestro gusto
            javax.swing.UIManager.put("TabbedPane.foreground", new java.awt.Color(200, 200, 200));
            javax.swing.UIManager.put("TabbedPane.selectedForeground", java.awt.Color.WHITE);

        } catch (Exception e) {
            System.err.println("Error al iniciar FlatLaf");
        }

        SwingUtilities.invokeLater(() -> {
            // El arranque limpio, usando la clase que arreglamos antes
            ImpresoraTicket.cargarConfiguracionInicial();
            new Login().setVisible(true);
        });
    }

    private boolean solicitarPinSeguridad() {
        final boolean[] autenticado = {false};

        JDialog dialog = new JDialog(this, "Seguridad", true);
        dialog.setSize(280, 140);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setLayout(new java.awt.BorderLayout());

        JLabel lblMensaje = new JLabel("Ingresa tu PIN (4 dígitos)", SwingConstants.CENTER);
        lblMensaje.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        lblMensaje.setFont(util.Estilos.FONT_BOLD);

        JPasswordField pf = new JPasswordField(4);
        pf.setHorizontalAlignment(JTextField.CENTER);
        pf.setFont(new Font("Consolas", Font.BOLD, 28));

        // LA MAGIA: Escuchar el teclado en tiempo real
        pf.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                // 1. Bloquear letras (solo números) permitiendo borrar (backspace)
                if (!Character.isDigit(c) && c != '\b') {
                    e.consume();
                }
                // 2. Bloquear si ya hay 4 números
                else if (Character.isDigit(c) && pf.getPassword().length >= 4) {
                    e.consume();
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                char[] p = pf.getPassword();

                // 3. Cuando llegue al cuarto dígito, auto-valida sin presionar Enter
                if (p.length == 4) {
                    String pass = new String(p);
                    if (validarContrasenaEnBD(pass)) {
                        autenticado[0] = true;
                        dialog.dispose(); // Cierra y da acceso
                    } else {
                        java.awt.Toolkit.getDefaultToolkit().beep(); // Sonido de error
                        lblMensaje.setText("¡PIN Incorrecto!");
                        lblMensaje.setForeground(Color.RED);
                        pf.setText(""); // Vacía la caja para que el usuario intente de nuevo
                    }
                } else if (p.length < 4) {
                    // Si borró números, restaura el mensaje visual a su estado normal
                    lblMensaje.setText("Ingresa tu PIN (4 dígitos)");
                    lblMensaje.setForeground(UIManager.getColor("Label.foreground"));
                }
            }
        });

        JPanel pnlCentro = new JPanel();
        pnlCentro.add(pf);

        dialog.add(lblMensaje, java.awt.BorderLayout.NORTH);
        dialog.add(pnlCentro, java.awt.BorderLayout.CENTER);

        // DAR FOCO AUTOMÁTICO AL ABRIR
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                pf.requestFocusInWindow();
            }
        });

        dialog.setVisible(true); // El código se pausa aquí hasta que el modal se cierre

        return autenticado[0];
    }
}
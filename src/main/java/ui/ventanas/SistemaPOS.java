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
        //setResizable(false);
        setSize(1280, 768);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Image iconoApp = Recursos.getImagenApp();
        if (iconoApp != null) setIconImage(iconoApp);
        getContentPane().setBackground(Estilos.COLOR_FONDO); // Fondo ventana

        // INICIAR RESPALDO AUTOMÁTICO
        BackupManager.iniciarScheduler();

        // Estilizar Tabs
        tabs = new TabbedPanePro();






        tabs.setFont(Estilos.FONT_BOLD);
        tabs.setBackground(Estilos.COLOR_PANEL);
        tabs.setForeground(Color.BLACK);

        // Instancias
        PanelVentas pVentas = new PanelVentas();
        PanelInventario pInventario = new PanelInventario();
        PanelFinanzas pFinanzas = new PanelFinanzas();
        PanelServicios pServicios = new PanelServicios();
        PanelFaltantes pFaltantes = new PanelFaltantes();
        PanelClientes pClientes = new PanelClientes(pServicios, tabs);

        tabs.addTab("INICIO", new PanelDashboard());
        tabs.addTab("VENTAS", pVentas);
        tabs.addTab("SERVICIOS", pServicios);

        if ("ADMIN".equals(rolUsuario)) {
            // EL ADMIN VE TODO

            tabs.addTab("INVENTARIO", pInventario);
            tabs.addTab("FALTANTES", pFaltantes);
            tabs.addTab("FINANZAS", pFinanzas);
            tabs.addTab("CLIENTES", pClientes);


        } else {
            // EL CAJERO SOLO VE LO OPERATIVO
            // Puedes decidir si el cajero ve inventario/faltantes o no.
            // Según tu petición, quitamos FINANZAS.
            // Normalmente un cajero tampoco debería editar Configuración.

            tabs.addTab("INVENTARIO", pInventario); // Cajero suele necesitar buscar precios
            tabs.addTab("FALTANTES", pFaltantes);   // Cajero puede reportar faltantes
            // FINANZAS -> NO SE AGREGA
            // CONFIGURACION -> NO SE AGREGA (Opcional)
        }
        // --- 3. CONFIGURACIÓN (ICONO TUERCA) ---

        PanelConfiguracion pConfig = new PanelConfiguracion(rolUsuario);

        // Agregamos la pestaña normalmente (al final)
        // Truco: Título vacío "" y le seteamos el icono después
        tabs.addTab("", pConfig);

        // Asignar icono a la última pestaña añadida (Índice count - 1)
        // Asegúrate de tener "tuerca.png" en recursos (tamaño 24x24 blanco)
        int indexConfig = tabs.getTabCount() - 1;
        tabs.setIconAt(indexConfig, Recursos.getIcono("tuerca.png"));
        tabs.setToolTipTextAt(indexConfig, "Configuración del Sistema");

// --- DETECTAR CAMBIO DE PESTAÑA Y DAR FOCO / SEGURIDAD ---
        final int[] pestanaAnterior = {0}; // Para recordar dónde estábamos

        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            int indiceActual = tabs.getSelectedIndex();
            Component prev = tabs.getComponentAt(pestanaAnterior[0]);

            // --- NUEVO: Si la pestaña anterior era Finanzas, borrar sus datos por privacidad ---
            if (prev instanceof PanelFinanzas && c != prev) {
                ((PanelFinanzas)prev).limpiarDatos();
            }
            // 1. SEGURIDAD: Verificar si es un área restringida
            if(c instanceof PanelFinanzas || c instanceof PanelConfiguracion) {
                JPasswordField pf = new JPasswordField();
                int option = JOptionPane.showConfirmDialog(this, pf,
                        "Área Restringida. Confirma tu contraseña:",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                if (option == JOptionPane.OK_OPTION) {
                    String passIngresada = new String(pf.getPassword());
                    if (validarContrasenaEnBD(passIngresada)) {
                        pestanaAnterior[0] = indiceActual; // Acceso concedido, actualizar historial
                    } else {
                        JOptionPanePro.mostrarMensaje(this, "Error", "Contraseña incorrecta.", "ERROR");
                        tabs.setSelectedIndex(pestanaAnterior[0]); // Rechazado, regresar
                        return;
                    }
                } else {
                    tabs.setSelectedIndex(pestanaAnterior[0]); // Canceló, regresar
                    return;
                }
            } else {
                pestanaAnterior[0] = indiceActual; // Es un panel libre, actualizamos historial
            }

            // 2. LÓGICA DE FOCOS Y CARGAS (Tu código original)
            if(c == pVentas) { pVentas.cargarCatalogo(); pVentas.darFocoCodigo(); }
            else if(c instanceof PanelFaltantes) ((PanelFaltantes)c).cargarFaltantes();
            else if(c instanceof PanelFinanzas) ((PanelFinanzas)c).consultar();
            else if(c instanceof PanelDashboard) {
                tabs.setComponentAt(0, new PanelDashboard());
            }
        });

        btnCerrarSesion = new BotonPro("", "logout.png", new Color(0,0,0,0), this::cerrarSesion);
        // Ajustamos tamaño pequeño para que quepa en la barra de título
        btnCerrarSesion.setSize(35, 35);

        // --- 3. JLAYEREDPANE (EL CONTENEDOR MAGICO) ---
        JLayeredPane layeredPane = new JLayeredPane();

        // Agregamos las pestañas en la capa DEFAULT (Fondo)
        layeredPane.add(tabs, JLayeredPane.DEFAULT_LAYER);

        // Agregamos el botón en la capa PALETTE (Arriba)
        layeredPane.add(btnCerrarSesion, JLayeredPane.PALETTE_LAYER);

// --- NUEVO: Leer pestaña por defecto desde config.properties ---
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
        } catch (Exception ex) {
            // Si falla, se queda en el índice 0 (INICIO) por defecto
        }

        // Agregamos el LayeredPane a la ventana
        setContentPane(layeredPane);

        // --- 4. LISTENER DE REDIMENSIÓN (Responsividad) ---
        // Esto asegura que el botón siempre esté a la derecha y los tabs llenen la pantalla
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {

                // Las pestañas ocupan toda la ventana (menos los bordes del SO)
                // Ajustamos un poco el height si es necesario, getContentPane().getHeight() es mejor
                int contentW = getContentPane().getWidth();
                int contentH = getContentPane().getHeight();

                tabs.setBounds(0, 0, contentW, contentH);

                // El botón se pega a la derecha.
                // Y = 5 para que quede alineado con las pestañas
                // X = AnchoTotal - AnchoBoton - Margen(15)
                btnCerrarSesion.setBounds(contentW - 50, 6, 35, 35);
            }
        });
    }

    private void cerrarSesion() {
        boolean confirmar = JOptionPanePro.mostrarConfirmacion(this, "Cerrar Sesión", "¿Deseas cerrar sesión?");
        if (confirmar) {
            LoggerPro.registrar("LOGIN", "Cierre de sesión: " + Sesion.usuarioActual);

            // --- NUEVO: Borrar sesión del archivo de configuración ---
            java.util.Properties props = new java.util.Properties();
            java.io.File archivo = new java.io.File("config.properties");
            if (archivo.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(archivo)) { props.load(fis); } catch(Exception ex){}
            }
            props.remove("session.user");
            props.remove("session.pass");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(archivo)) { props.store(fos, "Configuracion Sistema POS"); } catch(Exception ex){}
            // ---------------------------------------------------------

            // Cerrar esta ventana
            this.dispose();

            // Abrir Login limpio
            new Login().setVisible(true);
        }
    }

    public static void main(String[] args) {
        // 1. Configurar zona horaria
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Mexico_City"));

        // 2. Instalar el Look and Feel de FlatLaf ANTES de cualquier otra cosa
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("Error al iniciar FlatLaf");
        }

        // 3. Iniciar la aplicación
        SwingUtilities.invokeLater(() -> {
            ImpresoraTicket.cargarConfiguracionInicial();
            new Login().setVisible(true);
        });
    }

    private boolean validarContrasenaEnBD(String password) {
        try (java.sql.Connection conn = config.ConexionBD.conectar()) {
            String sql = "SELECT 1 FROM usuarios_sistema WHERE usuario = ? AND password = ?";
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, Sesion.usuarioActual);
            ps.setString(2, password);
            java.sql.ResultSet rs = ps.executeQuery();
            return rs.next(); // Si devuelve un registro, la contraseña es correcta
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
package ui.ventanas;

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


        // --- DETECTAR CAMBIO DE PESTAÑA Y DAR FOCO ---
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if(c == pVentas) { pVentas.cargarCatalogo(); pVentas.darFocoCodigo(); }
            else if(c instanceof PanelFaltantes) ((PanelFaltantes)c).cargarFaltantes();
            else if(c instanceof PanelFinanzas) ((PanelFinanzas)c).consultar();
                // Refrescar dashboard al volver a Inicio
            else if(c instanceof PanelDashboard) {
                // Truco: Reemplazar el panel dashboard por uno nuevo para actualizar datos
                // O mejor, agregar un método public void refrescar() en PanelDashboard y llamarlo.
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

            // Cerrar esta ventana
            this.dispose();

            // Abrir Login limpio
            new Login().setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
            ImpresoraTicket.cargarConfiguracionInicial();
            new Login().setVisible(true);
        });
    }
}
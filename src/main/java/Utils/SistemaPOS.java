package Utils;

import Paneles.*;

import javax.swing.*;
import java.awt.*;

public class SistemaPOS extends JFrame {

    public SistemaPOS(String rolUsuario) {


        setTitle("Punto de Venta - Lumtech " + rolUsuario);
        //setResizable(false);
        setSize(1280, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Estilos.COLOR_FONDO); // Fondo ventana

        Image iconoApp = Recursos.getImagenApp();
        if (iconoApp != null) {
            setIconImage(iconoApp);
        }

        // Estilizar Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Estilos.FONT_BOLD);
        tabs.setBackground(Estilos.COLOR_PANEL);
        tabs.setForeground(Color.BLACK);

        // Instancias
        PanelVentas pVentas = new PanelVentas();
        PanelInventario pInventario = new PanelInventario();
        PanelFinanzas pFinanzas = new PanelFinanzas();
        PanelServicios pServicios = new PanelServicios();
        PanelFaltantes pFaltantes = new PanelFaltantes();
        PanelConfiguracion pConfig = new PanelConfiguracion();

        tabs.addTab("VENTAS", pVentas);
        tabs.addTab("SERVICIOS", pServicios);

        if ("ADMIN".equals(rolUsuario)) {
            // EL ADMIN VE TODO
            tabs.addTab("INVENTARIO", pInventario);
            tabs.addTab("FALTANTES", pFaltantes);
            tabs.addTab("FINANZAS", pFinanzas);
            tabs.addTab("CONFIGURACIÓN", pConfig);
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


        pVentas.darFocoCodigo();

        // --- DETECTAR CAMBIO DE PESTAÑA Y DAR FOCO ---
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if(c == pVentas) {
                pVentas.cargarCatalogo();
                pVentas.darFocoCodigo(); // <--- IMPORTANTE: Poner cursor en Scanner
            }
            else if(c == pFaltantes) pFaltantes.cargarFaltantes();
            else if(c == pFinanzas) pFinanzas.consultar();
            else if(c == pConfig) pConfig.cargarConfiguracion();
        });

        add(tabs);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

            // --- AGREGAR ESTA LÍNEA AQUÍ ---
            ImpresoraTicket.cargarConfiguracionInicial();
            // -------------------------------

            new Login().setVisible(true);
        });
    }
}
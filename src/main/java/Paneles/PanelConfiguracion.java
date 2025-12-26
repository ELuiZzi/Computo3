package Paneles;

import ElementosPro.*;
import Utils.Estilos;
import Utils.ImpresoraTicket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class PanelConfiguracion extends JPanel {

    private JComboBox<String> cmbImpresora;
    private JCheckBox chkAutoImprimir;

    public PanelConfiguracion() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- PANEL DE CONFIGURACIÓN ---
        JPanel pConfig = new JPanel(new GridBagLayout());
        pConfig.setBackground(Estilos.COLOR_PANEL);
        pConfig.setBorder(BorderFactory.createTitledBorder(null, "Configuración de Impresión", 0,0, Estilos.FONT_BOLD, Color.WHITE));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10); g.fill = GridBagConstraints.HORIZONTAL;

        // Impresora
        addLbl(pConfig, g, 0, 0, "Impresora de Tickets:");
        cmbImpresora = new JComboBox<>();
        cmbImpresora.setFont(Estilos.FONT_PLAIN);
        cmbImpresora.setBackground(Estilos.COLOR_INPUT);
        cmbImpresora.setForeground(Color.WHITE);
        g.gridx=1; pConfig.add(cmbImpresora, g);

        // Auto Imprimir
        chkAutoImprimir = new JCheckBox("Imprimir Tickets Automáticamente al Vender");
        chkAutoImprimir.setFont(Estilos.FONT_PLAIN);
        chkAutoImprimir.setForeground(Color.WHITE);
        chkAutoImprimir.setBackground(Estilos.COLOR_PANEL);
        chkAutoImprimir.setFocusPainted(false);
        g.gridx=0; g.gridy=1; g.gridwidth=2; pConfig.add(chkAutoImprimir, g);

        // Botón Guardar
        BotonPro btnGuardar = new BotonPro("Guardar Cambios", "guardar.png", Estilos.COLOR_ACCENT, this::guardarConfiguracion);
        g.gridx=0; g.gridy=2; g.gridwidth=2; pConfig.add(btnGuardar, g);

        add(pConfig, BorderLayout.CENTER);

        cargarConfiguracion();
    }

    public void cargarConfiguracion() {
        // Cargar lista de impresoras
        List<String> impresoras = ImpresoraTicket.obtenerImpresorasDisponibles();
        String impresoraGuardada = null;

        // Intentar leer de un archivo de configuración (crearemos uno si no existe)
        Properties props = new Properties();
        try {
            File configFile = new File("config.properties");
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                    impresoraGuardada = props.getProperty("ticket.impresora");
                }
            } else {
                // Si no existe config.properties, buscamos la impresora por defecto de Windows
                // Esto es menos confiable, pero es un fallback
                impresoraGuardada = System.getProperty("fiscal.printer.name"); // No existe, pero es un placeholder
                if(impresoraGuardada == null) { // Si tampoco existe, intentar primera disponible
                    if (!impresoras.isEmpty()) impresoraGuardada = impresoras.get(0);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        // Llenar ComboBox
        cmbImpresora.removeAllItems();
        for (String imp : impresoras) {
            cmbImpresora.addItem(imp);
        }

        // Seleccionar impresora guardada o primera disponible
        if (impresoraGuardada != null && impresoras.contains(impresoraGuardada)) {
            cmbImpresora.setSelectedItem(impresoraGuardada);
        } else if (!impresoras.isEmpty()) {
            cmbImpresora.setSelectedIndex(0);
        }

        // Cargar Estado Auto Imprimir (Buscaremos en config.properties)
        boolean auto = true; // Default
        try {
            File configFile = new File("config.properties");
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                    auto = Boolean.parseBoolean(props.getProperty("ticket.auto_imprimir", "true"));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        chkAutoImprimir.setSelected(auto);
        ImpresoraTicket.setAutoImprimir(auto); // Aplicar al sistema
    }

    private void guardarConfiguracion() {
        String impresora = (String) cmbImpresora.getSelectedItem();
        boolean auto = chkAutoImprimir.isSelected();

        // Actualizar la clase estática en memoria
        ImpresoraTicket.setImpresora(impresora);
        ImpresoraTicket.setAutoImprimir(auto);

        Properties props = new Properties();
        File archivoConfig = new File("config.properties");

        // PASO 1: CARGAR LA CONFIGURACIÓN EXISTENTE (Base de Datos)
        if (archivoConfig.exists()) {
            try (FileInputStream input = new FileInputStream(archivoConfig)) {
                props.load(input);
            } catch (IOException e) {
                e.printStackTrace();
                // Si falla leer, seguimos con props vacío, pero idealmente no debería fallar
            }
        }

        // PASO 2: AGREGAR O ACTUALIZAR SOLO LAS CLAVES DE IMPRESIÓN
        // (Esto mantiene intactas las claves db.user, db.ip, etc.)
        if (impresora != null) {
            props.setProperty("ticket.impresora", impresora);
        }
        props.setProperty("ticket.auto_imprimir", String.valueOf(auto));

        // PASO 3: GUARDAR EL ARCHIVO COMPLETO
        try (FileOutputStream output = new FileOutputStream(archivoConfig)) {
            props.store(output, "Configuracion General - Sistema POS");
            JOptionPanePro.mostrarMensaje(this, "Configuración Guardada", "Ajustes aplicados correctamente.", "INFO");
        } catch (IOException e) {
            JOptionPanePro.mostrarMensaje(this, "Error", "No se pudo guardar el archivo: " + e.getMessage(), "ERROR");
        }
    }

    // Helpers
    private void addLbl(JPanel p, GridBagConstraints g, int x, int y, String t) { g.gridx=x; g.gridy=y; JLabel l=new JLabel(t); l.setForeground(Color.WHITE); p.add(l,g); }
}
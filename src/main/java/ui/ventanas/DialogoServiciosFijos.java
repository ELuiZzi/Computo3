package ui.ventanas;

import config.ConexionBD;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
// import util.ConexionBD; // <-- Asegúrate de que tu import coincida

public class DialogoServiciosFijos extends JDialog {

    private JComboBox<String> cbServicios;
    private JTextField txtPrecio;
    private int idOrden;

    // Campos Dinámicos
    private JPanel panelOffice;
    private JTextField txtClaveOffice, txtIdInstalacion, txtIdConfirmacion;

    private JComboBox<String> cbAntivirus;
    private JLabel lblAntivirus;

    private JTextField txtConceptoOtro;
    private JLabel lblOtro;

    private JButton btnAgregar;

    public DialogoServiciosFijos(Frame parent, int idOrden) {
        super(parent, "Agregar Servicio", true);
        this.idOrden = idOrden;

        setLayout(new BorderLayout(10, 10));

        // --- PANEL PRINCIPAL ---
        JPanel panelCentral = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Selector Principal
        String[] servicios = {
                "Instalación de Windows",
                "Respaldo de Información", // Sugerencia: mantenerlo por el tiempo que exige
                "Instalación de Office",
                "Antivirus",
                "Mantenimiento Preventivo",
                "Otro..."
        };
        cbServicios = new JComboBox<>(servicios);

        // 2. Sub-menú de Antivirus (Fácil de expandir a futuro)
        String[] marcasAntivirus = {"Kaspersky", "ESET"};
        cbAntivirus = new JComboBox<>(marcasAntivirus);
        lblAntivirus = new JLabel("Marca de Antivirus:");

        // 3. Campo libre para "Otro..."
        txtConceptoOtro = new JTextField(15);
        lblOtro = new JLabel("Escribe el servicio:");

        txtPrecio = new JTextField(10);
        txtPrecio.setText("750.00");

        // Agregando elementos al Layout
        int fila = 0;
        gbc.gridx = 0; gbc.gridy = fila; panelCentral.add(new JLabel("Servicio:"), gbc);
        gbc.gridx = 1; gbc.gridy = fila++; panelCentral.add(cbServicios, gbc);

        gbc.gridx = 0; gbc.gridy = fila; panelCentral.add(lblAntivirus, gbc);
        gbc.gridx = 1; gbc.gridy = fila++; panelCentral.add(cbAntivirus, gbc);

        gbc.gridx = 0; gbc.gridy = fila; panelCentral.add(lblOtro, gbc);
        gbc.gridx = 1; gbc.gridy = fila++; panelCentral.add(txtConceptoOtro, gbc);

        gbc.gridx = 0; gbc.gridy = fila; panelCentral.add(new JLabel("Precio ($):"), gbc);
        gbc.gridx = 1; gbc.gridy = fila++; panelCentral.add(txtPrecio, gbc);

        // --- PANEL DINÁMICO DE OFFICE ---
        panelOffice = new JPanel(new GridLayout(3, 2, 5, 5));
        panelOffice.setBorder(BorderFactory.createTitledBorder("Datos de Licencia"));
        txtClaveOffice = new JTextField();
        txtIdInstalacion = new JTextField();
        txtIdConfirmacion = new JTextField();

        panelOffice.add(new JLabel("Clave de Activación:")); panelOffice.add(txtClaveOffice);
        panelOffice.add(new JLabel("ID de Instalación:"));   panelOffice.add(txtIdInstalacion);
        panelOffice.add(new JLabel("ID Confirmación:"));     panelOffice.add(txtIdConfirmacion);

        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
        panelCentral.add(panelOffice, gbc);

        // --- ESTADO INICIAL (Ocultar dinámicos) ---
        lblAntivirus.setVisible(false); cbAntivirus.setVisible(false);
        lblOtro.setVisible(false);      txtConceptoOtro.setVisible(false);
        panelOffice.setVisible(false);

        // --- LISTENERS DE DINAMISMO ---

        // Cambios en el Antivirus
        cbAntivirus.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && "Antivirus".equals(cbServicios.getSelectedItem())) {
                if ("Kaspersky".equals(cbAntivirus.getSelectedItem())) txtPrecio.setText("350.00");
                else if ("ESET".equals(cbAntivirus.getSelectedItem())) txtPrecio.setText("200.00");
            }
        });

        // Cambios en el Servicio Principal
        cbServicios.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String seleccionado = (String) cbServicios.getSelectedItem();

                // 1. Resetear visibilidad por defecto
                lblAntivirus.setVisible(false); cbAntivirus.setVisible(false);
                lblOtro.setVisible(false);      txtConceptoOtro.setVisible(false);
                panelOffice.setVisible(false);

                // 2. Configurar vista y precios
                switch(seleccionado) {
                    case "Instalación de Windows": txtPrecio.setText("750.00"); break;
                    case "Respaldo de Información": txtPrecio.setText("350.00"); break;
                    case "Mantenimiento Preventivo": txtPrecio.setText("550.00"); break;

                    case "Instalación de Office":
                        txtPrecio.setText("600.00");
                        panelOffice.setVisible(true);
                        break;

                    case "Antivirus":
                        lblAntivirus.setVisible(true);
                        cbAntivirus.setVisible(true);
                        // Disparar precio inicial
                        if ("Kaspersky".equals(cbAntivirus.getSelectedItem())) txtPrecio.setText("350.00");
                        else txtPrecio.setText("200.00");
                        break;

                    case "Otro...":
                        lblOtro.setVisible(true);
                        txtConceptoOtro.setVisible(true);
                        txtPrecio.setText("");
                        txtConceptoOtro.requestFocus();
                        break;
                }

                // Reajustar tamaño de la ventana al contenido visible
                pack();
            }
        });

        // --- BOTÓN AGREGAR ---
        btnAgregar = new JButton("Agregar a la Cuenta");
        btnAgregar.addActionListener(e -> procesarGuardado());

        JPanel panelSur = new JPanel();
        panelSur.add(btnAgregar);

        add(panelCentral, BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        pack(); // Ajustar al abrir
        setLocationRelativeTo(parent);
    }

    private void procesarGuardado() {
        String servicio = (String) cbServicios.getSelectedItem();

        // --- INTERCEPTAR SERVICIOS DINÁMICOS ---
        if ("Otro...".equals(servicio)) {
            servicio = txtConceptoOtro.getText().trim();
            if (servicio.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe el nombre del servicio personalizado.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else if ("Antivirus".equals(servicio)) {
            servicio = "Antivirus " + cbAntivirus.getSelectedItem();
        }

        String precioStr = txtPrecio.getText().trim();
        if (precioStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa un precio válido.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El precio debe ser un número.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean exitoCargo = false;
        try (Connection conn = ConexionBD.conectar()) { // Verifica tu clase de conexión
            String sqlCargo = "INSERT INTO cargos_orden (id_orden, concepto, monto) VALUES (?, ?, ?)";
            PreparedStatement psCargo = conn.prepareStatement(sqlCargo);
            psCargo.setInt(1, this.idOrden);
            psCargo.setString(2, servicio.toUpperCase());
            psCargo.setDouble(3, precio);

            if (psCargo.executeUpdate() > 0) {
                exitoCargo = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al guardar el cobro.");
        }

        if (exitoCargo) {
            if ("Instalación de Office".equals((String) cbServicios.getSelectedItem())) {
                String sqlLicencia = "INSERT INTO detalles_licencias (id_orden, software_nombre, licencia_clave, id_instalacion, id_confirmacion) VALUES (?, ?, ?, ?, ?)";

                try (Connection con = ConexionBD.conectar();
                     PreparedStatement psLic = con.prepareStatement(sqlLicencia)) {

                    psLic.setInt(1, this.idOrden);
                    psLic.setString(2, "Microsoft Office");
                    psLic.setString(3, txtClaveOffice.getText().trim());
                    psLic.setString(4, txtIdInstalacion.getText().trim());
                    psLic.setString(5, txtIdConfirmacion.getText().trim());

                    psLic.executeUpdate();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Se cobró el servicio, pero hubo un error al guardar claves de Office.");
                }
            }

            dispose();
        }
    }
}
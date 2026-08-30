package ui.paneles;

import config.ConexionBD;
import ui.componentes.*;
import util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.sql.*;

public class PanelClientes extends JPanel {

    // Referencias para navegación
    private PanelServicios panelServiciosRef;
    private JTabbedPane tabsPrincipalRef;

    // UI
    private TablaPro tablaClientes, tablaHistorial;
    private DefaultTableModel modeloClientes, modeloHistorial;
    private JTextField txtBuscar, txtNombre, txtTelefono;
    private JLabel lblTotalGastado, lblTotalOrdenes;

    private int idClienteSeleccionado = -1;

    public PanelClientes(PanelServicios panelServicios, JTabbedPane tabs) {
        this.panelServiciosRef = panelServicios;
        this.tabsPrincipalRef = tabs;

        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        // =========================================================
        // 1. LISTA DE CLIENTES (IZQUIERDA)
        // =========================================================
        JPanel pLista = new JPanel(new BorderLayout());
        pLista.setBackground(Estilos.COLOR_FONDO);
        pLista.setBorder(new EmptyBorder(10, 10, 10, 10));
        pLista.setPreferredSize(new Dimension(450, 0));

        // Buscador
        JPanel pBusq = new JPanel(new BorderLayout(5, 0));
        pBusq.setBackground(Estilos.COLOR_PANEL);
        pBusq.setBorder(new EmptyBorder(5,5,5,5));

        txtBuscar = new JTextField(); Estilos.estilizarInput(txtBuscar);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar Cliente...");
        txtBuscar.addActionListener(e -> cargarClientes());

        BotonPro btnBuscar = new BotonPro("🔍", Estilos.COLOR_ACCENT, this::cargarClientes);
        btnBuscar.setPreferredSize(new Dimension(40, 30));

        pBusq.add(txtBuscar, BorderLayout.CENTER);
        pBusq.add(btnBuscar, BorderLayout.EAST);

        // Tabla Clientes
        modeloClientes = new DefaultTableModel(new String[]{"ID", "Nombre", "Teléfono"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaClientes = new TablaPro(modeloClientes);
        tablaClientes.getColumnModel().getColumn(0).setPreferredWidth(40);
        tablaClientes.getColumnModel().getColumn(1).setPreferredWidth(200);

        tablaClientes.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                cargarDetalleCliente();
            }
        });

        pLista.add(pBusq, BorderLayout.NORTH);
        pLista.add(new JScrollPane(tablaClientes), BorderLayout.CENTER);

        // =========================================================
        // 2. DETALLE Y EDICIÓN (DERECHA)
        // =========================================================
        JPanel pDetalle = new JPanel(new BorderLayout(0, 10));
        pDetalle.setBackground(Estilos.COLOR_PANEL);
        pDetalle.setBorder(new EmptyBorder(10, 20, 10, 20));

        // --- Formulario Edición ---
        JPanel pForm = new JPanel(new BorderLayout());
        pForm.setBackground(Estilos.COLOR_PANEL);
        pForm.setBorder(BorderFactory.createTitledBorder(null, "Perfil del Cliente", 0,0, Estilos.FONT_BOLD, Color.WHITE));

        txtNombre = new JTextField(20); Estilos.estilizarInput(txtNombre);
        txtTelefono = new JTextField(15); Estilos.estilizarInput(txtTelefono);
        txtTelefono.setHorizontalAlignment(JTextField.CENTER);

        // Stats
        lblTotalOrdenes = new JLabel("0 Órdenes");
        lblTotalOrdenes.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalOrdenes.setForeground(Color.CYAN);

        lblTotalGastado = new JLabel("$0.00 Invertidos");
        lblTotalGastado.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalGastado.setForeground(Color.GREEN);

        // Armado Form

        // Boton Eliminar Redondo
        JPanel btnEliminarRedondo = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g2d) {
                Graphics2D g2 = (Graphics2D) g2d.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getMousePosition() != null) g2.setColor(new Color(230, 80, 80));
                else g2.setColor(new Color(200, 50, 50));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        btnEliminarRedondo.setOpaque(false);
        btnEliminarRedondo.setPreferredSize(new Dimension(35, 35));
        btnEliminarRedondo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEliminarRedondo.setToolTipText("Eliminar Cliente");
        ImageIcon iconoBasura = Recursos.getIcono("eliminar.png");
        if (iconoBasura != null) {
            Image img = iconoBasura.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            btnEliminarRedondo.add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
        }
        btnEliminarRedondo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { eliminarCliente(); }
            public void mouseEntered(MouseEvent e) { btnEliminarRedondo.repaint(); }
            public void mouseExited(MouseEvent e) { btnEliminarRedondo.repaint(); }
        });

        // Boton WhatsApp Redondo
        JPanel btnWhatsAppRedondo = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g2d) {
                Graphics2D g2 = (Graphics2D) g2d.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getMousePosition() != null) g2.setColor(new Color(60, 220, 120));
                else g2.setColor(new Color(37, 211, 102));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        btnWhatsAppRedondo.setOpaque(false);
        btnWhatsAppRedondo.setPreferredSize(new Dimension(35, 35));
        btnWhatsAppRedondo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnWhatsAppRedondo.setToolTipText("Abrir Chat de WhatsApp");
        ImageIcon iconoWa = Recursos.getIcono("whatsapp.png");
        if (iconoWa != null) {
            Image imgWa = iconoWa.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            btnWhatsAppRedondo.add(new JLabel(new ImageIcon(imgWa)), BorderLayout.CENTER);
        }
        btnWhatsAppRedondo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { abrirWhatsApp(); }
            public void mouseEntered(MouseEvent e) { btnWhatsAppRedondo.repaint(); }
            public void mouseExited(MouseEvent e) { btnWhatsAppRedondo.repaint(); }
        });

        // Panel lateral para botones (Esquina Superior Derecha)
        JPanel pBotonesDerecha = new JPanel();
        pBotonesDerecha.setLayout(new BoxLayout(pBotonesDerecha, BoxLayout.Y_AXIS));
        pBotonesDerecha.setBackground(Estilos.COLOR_PANEL);
        pBotonesDerecha.setBorder(new EmptyBorder(5, 10, 0, 5)); // Margen superior e izquierdo
        pBotonesDerecha.add(btnEliminarRedondo);
        pBotonesDerecha.add(Box.createRigidArea(new Dimension(0, 8))); // Separador
        pBotonesDerecha.add(btnWhatsAppRedondo);

        // Contenedor para que los botones se peguen al techo del Este
        JPanel pEast = new JPanel(new BorderLayout());
        pEast.setBackground(Estilos.COLOR_PANEL);
        pEast.add(pBotonesDerecha, BorderLayout.NORTH);
        pForm.add(pEast, BorderLayout.EAST);

        // Layout Central (Campos centraditos sin estirarse alocadamente)
        JPanel pCampos = new JPanel(new GridBagLayout());
        pCampos.setBackground(Estilos.COLOR_PANEL);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 10, 8, 10);
        g.anchor = GridBagConstraints.WEST;

        g.gridx=0; g.gridy=0; pCampos.add(lbl("WhatsApp:"), g);
        g.gridx=1; pCampos.add(txtTelefono, g);

        g.gridx=0; g.gridy=1; pCampos.add(lbl("Nombre:"), g);
        g.gridx=1; pCampos.add(txtNombre, g);

        g.gridx=0; g.gridy=2; pCampos.add(lbl("Historial:"), g);
        JPanel pStats = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pStats.setBackground(Estilos.COLOR_PANEL);
        pStats.add(lblTotalOrdenes); pStats.add(new JLabel("|")); pStats.add(lblTotalGastado);
        g.gridx=1; pCampos.add(pStats, g);

        pForm.add(pCampos, BorderLayout.CENTER);

        // Botonera Sur
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBtns.setBackground(Estilos.COLOR_PANEL);
        BotonPro btnGuardar = new BotonPro("Guardar", "guardar.png", Estilos.COLOR_ACCENT, this::guardarCambios);
        btnGuardar.setPreferredSize(new Dimension(130, 35)); // Más discreto
        pBtns.add(btnGuardar);

        pForm.add(pBtns, BorderLayout.SOUTH);

        // --- Historial de Reparaciones ---
        JPanel pHistorial = new JPanel(new BorderLayout());
        pHistorial.setBackground(Estilos.COLOR_PANEL);
        pHistorial.setBorder(BorderFactory.createTitledBorder(null, "Historial de Servicios (Doble Clic para ver)", 0,0, Estilos.FONT_BOLD, Color.WHITE));

        modeloHistorial = new DefaultTableModel(new String[]{"Folio", "Fecha", "Equipo", "Estado", "Costo"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaHistorial = new TablaPro(modeloHistorial);

        // EVENTO MÁGICO: IR A SERVICIOS
        tablaHistorial.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    irAOrdenServicio();
                }
            }
        });

        pHistorial.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);

        // Armado Derecha
        pDetalle.add(pForm, BorderLayout.NORTH);
        pDetalle.add(pHistorial, BorderLayout.CENTER);

        // Split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pLista, pDetalle);
        split.setResizeWeight(0.4);
        split.setBorder(null);

        add(split, BorderLayout.CENTER);

        cargarClientes();
    }

    // =============================================================
    // LÓGICA
    // =============================================================

    private void cargarClientes() {
        modeloClientes.setRowCount(0);
        String filtro = txtBuscar.getText().trim();
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM clientes WHERE nombre LIKE ? OR telefono LIKE ? ORDER BY nombre")) {
            ps.setString(1, "%" + filtro + "%");
            ps.setString(2, "%" + filtro + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    modeloClientes.addRow(new Object[]{rs.getInt("id"), rs.getString("nombre"), rs.getString("telefono")});
                }
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelClientes.cargarClientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarDetalleCliente() {
        int row = tablaClientes.getSelectedRow();
        if (row == -1) return;

        idClienteSeleccionado = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
        txtNombre.setText(modeloClientes.getValueAt(row, 1).toString());
        txtTelefono.setText(modeloClientes.getValueAt(row, 2).toString());

        // Cargar Historial y Stats
        cargarHistorial(idClienteSeleccionado);
    }

    private void cargarHistorial(int idCliente) {
        modeloHistorial.setRowCount(0);
        double totalGasto = 0;
        int conteo = 0;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("SELECT id, fecha_recepcion, dispositivo, estado, costo_estimado FROM ordenes_servicio WHERE id_cliente = ? ORDER BY id DESC")) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    conteo++;
                    double costo = rs.getDouble("costo_estimado");
                    String estado = rs.getString("estado");

                    // Sumar al total histórico si ya fue entregado/pagado
                    if("ENTREGADO".equals(estado) || "LISTO".equals(estado)) {
                        totalGasto += costo;
                    }

                    modeloHistorial.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getTimestamp("fecha_recepcion"),
                            rs.getString("dispositivo"),
                            estado,
                            "$" + costo
                    });
                }
            }

            lblTotalOrdenes.setText(conteo + " Órdenes");
            lblTotalGastado.setText("$" + String.format("%.2f", totalGasto) + " Invertidos");

        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelClientes.cargarHistorial: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void eliminarCliente() {
        if (idClienteSeleccionado == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Selecciona un cliente de la lista.", "ADVERTENCIA");
            return;
        }

        if (!JOptionPanePro.mostrarConfirmacion(this, "Eliminar Cliente", "¿Seguro que deseas eliminar permanentemente a este cliente?")) {
            return;
        }

        String pin = JOptionPanePro.solicitarEntrada(this, "Autorización", "Ingresa el PIN de Administrador:");
        if (pin == null || pin.isEmpty()) return;

        // Validar PIN
        boolean pinValido = false;
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM usuarios_sistema WHERE usuario = ? AND password = ?")) {
            ps.setString(1, config.Sesion.usuarioActual);
            ps.setString(2, pin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) pinValido = true;
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo al validar PIN en eliminarCliente: " + e.getMessage());
        }

        if (!pinValido) {
            JOptionPanePro.mostrarMensaje(this, "Denegado", "PIN Incorrecto.", "ERROR");
            return;
        }

        // Eliminar de BD
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM clientes WHERE id = ?")) {
            ps.setInt(1, idClienteSeleccionado);
            int aff = ps.executeUpdate();
            if (aff > 0) {
                JOptionPanePro.mostrarMensaje(this, "Éxito", "Cliente eliminado.", "INFO");
                idClienteSeleccionado = -1;
                txtNombre.setText("");
                txtTelefono.setText("");
                modeloHistorial.setRowCount(0);
                lblTotalOrdenes.setText("0 Órdenes");
                lblTotalGastado.setText("$0.00 Invertidos");
                cargarClientes();
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo al eliminar cliente: " + e.getMessage());
            JOptionPanePro.mostrarMensaje(this, "Error", "No se puede eliminar. Probablemente el cliente tenga órdenes vinculadas en el historial.", "ERROR");
        }
    }

    private void guardarCambios() {
        if (idClienteSeleccionado == -1) return;

        String nuevoNombre = Recursos.capitalizarTexto(txtNombre.getText());
        String nuevoTel = txtTelefono.getText().trim();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("UPDATE clientes SET nombre = ?, telefono = ? WHERE id = ?")) {
            ps.setString(1, nuevoNombre);
            ps.setString(2, nuevoTel);
            ps.setInt(3, idClienteSeleccionado);

            int aff = ps.executeUpdate();
            if (aff > 0) {
                JOptionPanePro.mostrarMensaje(this, "Éxito", "Cliente actualizado.", "INFO");
                cargarClientes(); // Refrescar lista
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelClientes.guardarCambios: " + e.getMessage());
            JOptionPanePro.mostrarMensaje(this, "Error", "Error al actualizar (¿Teléfono duplicado?)", "ERROR");
        }
    }

    private void abrirWhatsApp() {
        String tel = txtTelefono.getText().trim();
        if (tel.isEmpty()) return;
        try {
            Desktop.getDesktop().browse(new URI("https://wa.me/52" + tel));
        } catch(Exception e){}
    }

    // --- MAGIA DE NAVEGACIÓN ---
    private void irAOrdenServicio() {
        int row = tablaHistorial.getSelectedRow();
        if (row == -1) return;

        int idOrden = Integer.parseInt(modeloHistorial.getValueAt(row, 0).toString());

        // 1. Cambiar al Tab de Servicios
        // Necesitamos saber el índice. Si lo agregaste en orden, VENTAS=0, SERVICIOS=1.
        // Lo más seguro es buscarlo por título o guardar el índice.
        // Asumimos índice 1 (ajusta si tienes más tabs antes).
        // Mejor aún: iterar para encontrarlo.
        for (int i = 0; i < tabsPrincipalRef.getTabCount(); i++) {
            if (tabsPrincipalRef.getTitleAt(i).contains("SERVICIOS")) {
                tabsPrincipalRef.setSelectedIndex(i);
                break;
            }
        }

        // 2. Decirle al panel de servicios que cargue esa orden
        panelServiciosRef.mostrarFormulario(idOrden);
    }

    private JLabel lbl(String t) { JLabel l = new JLabel(t); l.setForeground(Color.WHITE); return l; }
}
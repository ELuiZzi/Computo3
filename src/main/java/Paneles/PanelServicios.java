package Paneles;

import Conexión.ConexionBD;
import ElementosPro.BotonPro;
import ElementosPro.JOptionPanePro;
import ElementosPro.TablaPro;
import Utils.Estilos;
import Utils.GeneradorPDF;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PanelServicios extends JPanel {

    private final String VISTA_LISTA = "LISTA";
    private final String VISTA_FORM = "FORM";
    private final List<String> rutasImagenes = new ArrayList<>();
    // CONTROL DE VISTAS
    private final CardLayout cardLayout;
    private final JPanel panelContenedor;
    // --- COMPONENTES VISTA LISTA ---
    private TablaPro tablaOrdenes, tablaHistorial;
    private DefaultTableModel modeloOrdenes, modeloHistorial;
    private JTextField txtBuscar;
    // --- COMPONENTES VISTA FORMULARIO ---
    private JTextField txtNombre, txtTelefono, txtDispositivo, txtMarca, txtPass;
    private JTextArea txtFalla, txtDiagnostico;
    private JTextField txtCosto, txtAnticipo;
    private JComboBox<String> cmbEstado, cmbTipoEquipo;
    private JLabel lblTituloForm, lblFotosCount, lblFolio;
    // --- COMPONENTES IMPRESORA (TINTAS) ---
    private JPanel panelTintas;
    private JSlider slC, slM, slY, slK;
    // Variables Control
    private int idOrdenActual = -1; // Para el formulario (-1 es Nuevo)

    public PanelServicios() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        cardLayout = new CardLayout();
        panelContenedor = new JPanel(cardLayout);

        // 1. INICIALIZAR VISTAS
        panelContenedor.add(crearVistaLista(), VISTA_LISTA);
        panelContenedor.add(crearVistaFormulario(), VISTA_FORM);

        add(panelContenedor, BorderLayout.CENTER);

        cargarOrdenes("ACTIVOS"); // Carga inicial
    }

    // =================================================================
    // 1. VISTA LISTA (TABLAS)
    // =================================================================
    private JPanel crearVistaLista() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Estilos.COLOR_FONDO);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- HEADER ---
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(Estilos.COLOR_FONDO);

        JLabel lblTitulo = new JLabel("Gestión de Servicios");
        lblTitulo.setFont(Estilos.FONT_TITULO);
        lblTitulo.setForeground(Color.WHITE);

        BotonPro btnNuevo = new BotonPro("NUEVA ORDEN", "mas.png", Estilos.COLOR_ACCENT, () -> mostrarFormulario(-1));
        btnNuevo.setPreferredSize(new Dimension(180, 45));

        pHeader.add(lblTitulo, BorderLayout.WEST);
        pHeader.add(btnNuevo, BorderLayout.EAST);

        // --- FILTROS ---
        JPanel pFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pFiltros.setBackground(Estilos.COLOR_FONDO);
        pFiltros.setBorder(new EmptyBorder(10, 0, 10, 0));

        txtBuscar = new JTextField(20);
        Estilos.estilizarInput(txtBuscar);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar cliente, equipo o folio...");
        txtBuscar.addActionListener(e -> cargarOrdenes("TODO"));

        BotonPro btnBuscar = new BotonPro("Buscar", Estilos.COLOR_INPUT, () -> cargarOrdenes("TODO"));
        BotonPro btnActivos = new BotonPro("En Taller", new Color(41, 98, 255), () -> cargarOrdenes("ACTIVOS"));
        BotonPro btnHistorial = new BotonPro("Historial", Color.GRAY, () -> cargarOrdenes("HISTORIAL"));

        pFiltros.add(txtBuscar);
        pFiltros.add(btnBuscar);
        pFiltros.add(Box.createHorizontalStrut(20));
        pFiltros.add(btnActivos);
        pFiltros.add(btnHistorial);

        // --- TABLA ---
        modeloOrdenes = new DefaultTableModel(new String[]{"Folio", "Cliente", "Teléfono", "Equipo", "Estado", "Fecha"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaOrdenes = new TablaPro(modeloOrdenes);

        // 1. POPUP MENU (Click Derecho)
        JPopupMenu popup = new JPopupMenu();

        JMenuItem itemEditar = new JMenuItem("✏ Editar Orden");
        itemEditar.addActionListener(e -> accionTabla("EDITAR"));

        JMenuItem itemPDF = new JMenuItem("📄 Generar PDF");
        itemPDF.addActionListener(e -> accionTabla("PDF"));

        JMenuItem itemWhats = new JMenuItem("📱 Enviar WhatsApp");
        itemWhats.addActionListener(e -> accionTabla("WHATS"));

        popup.add(itemEditar);
        popup.add(itemPDF);
        popup.add(itemWhats);

        tablaOrdenes.setComponentPopupMenu(popup);

        // 2. DOBLE CLICK (Editar)
        tablaOrdenes.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) accionTabla("EDITAR");
            }
        });

        JPanel pCentro = new JPanel(new BorderLayout());
        pCentro.add(pFiltros, BorderLayout.NORTH);
        pCentro.add(new JScrollPane(tablaOrdenes), BorderLayout.CENTER);

        panel.add(pHeader, BorderLayout.NORTH);
        panel.add(pCentro, BorderLayout.CENTER);

        return panel;
    }

    // Helper para acciones de la tabla
    private void accionTabla(String accion) {
        int row = tablaOrdenes.getSelectedRow();
        if (row == -1) return;

        int id = Integer.parseInt(modeloOrdenes.getValueAt(row, 0).toString());

        if (accion.equals("EDITAR")) mostrarFormulario(id);
        else if (accion.equals("PDF")) generarPDFDesdeID(id);
        else if (accion.equals("WHATS")) enviarWhatsAppDesdeID(id);
    }

    // =================================================================
    // 2. VISTA FORMULARIO (DETALLE)
    // =================================================================
    private JPanel crearVistaFormulario() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Estilos.COLOR_PANEL);

        // Header Formulario
        JPanel pBarra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pBarra.setBackground(new Color(30, 35, 50));
        BotonPro btnVolver = new BotonPro("← Volver", Color.GRAY, () -> {
            cargarOrdenes("ACTIVOS");
            cardLayout.show(panelContenedor, VISTA_LISTA);
        });
        lblTituloForm = new JLabel(" DETALLES DE LA ORDEN");
        lblTituloForm.setForeground(Color.WHITE);
        lblTituloForm.setFont(Estilos.FONT_BOLD);
        pBarra.add(btnVolver);
        pBarra.add(lblTituloForm);

        // Grid Formulario
        JPanel pForm = new JPanel(new GridBagLayout());
        pForm.setBackground(Estilos.COLOR_PANEL);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 10, 6, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        lblFolio = new JLabel("NUEVO");
        lblFolio.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFolio.setForeground(Color.GREEN);

        txtNombre = input();
        txtTelefono = input();
        txtDispositivo = input();
        txtMarca = input();
        txtPass = input();
        txtFalla = area();
        txtDiagnostico = area();
        txtCosto = input();
        txtAnticipo = input();

        cmbTipoEquipo = new JComboBox<>(new String[]{"LAPTOP", "PC", "IMPRESORA", "OTRO"});
        cmbTipoEquipo.addActionListener(e -> alternarPanelTintas());

        cmbEstado = new JComboBox<>(new String[]{"RECIBIDO", "DIAGNOSTICO", "EN REPARACION", "ESPERA REFACCION", "LISTO", "ENTREGADO"});

        // --- PANEL TINTAS ---
        panelTintas = new JPanel(new GridLayout(1, 4, 5, 0));
        panelTintas.setBackground(Estilos.COLOR_PANEL);
        panelTintas.setBorder(BorderFactory.createTitledBorder(null, "Niveles Tinta", 0, 0, Estilos.FONT_PLAIN, Color.WHITE));

        slC = slider(Color.CYAN);
        slM = slider(Color.MAGENTA);
        slY = slider(Color.YELLOW);
        slK = slider(Color.GRAY);

        panelTintas.add(slC);
        panelTintas.add(slM);
        panelTintas.add(slY);
        panelTintas.add(slK);
        panelTintas.setVisible(false);

        // --- FOTOS ---
        JPanel pFotos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pFotos.setBackground(Estilos.COLOR_PANEL);
        BotonPro btnAddFoto = new BotonPro("Adjuntar", "imagen.png", Estilos.COLOR_INPUT, this::adjuntarFoto);
        lblFotosCount = new JLabel("0 imgs");
        lblFotosCount.setForeground(Color.WHITE);
        pFotos.add(btnAddFoto);
        pFotos.add(lblFotosCount);

        // ARMADO DEL GRID
        g.gridwidth = 2;
        g.gridx = 0;
        g.gridy = 0;
        pForm.add(lblFolio, g);

        addLbl(pForm, g, 0, 1, "Cliente:");
        g.gridwidth = 1;
        g.gridx = 1;
        pForm.add(txtNombre, g);
        addLbl(pForm, g, 0, 2, "Teléfono:");
        g.gridx = 1;
        pForm.add(txtTelefono, g);

        addLbl(pForm, g, 0, 3, "Tipo:");
        g.gridx = 1;
        pForm.add(cmbTipoEquipo, g);

        // Panel Tintas ocupa 2 columnas
        g.gridwidth = 2;
        g.gridx = 0;
        g.gridy = 4;
        pForm.add(panelTintas, g);

        g.gridwidth = 1;
        addLbl(pForm, g, 0, 5, "Equipo/Modelo:");
        g.gridx = 1;
        pForm.add(txtDispositivo, g);
        addLbl(pForm, g, 0, 6, "Marca:", 2);
        g.gridx = 3;
        pForm.add(txtMarca, g); // Ajuste posición
        addLbl(pForm, g, 0, 7, "Pass/Patrón:");
        g.gridx = 1;
        pForm.add(txtPass, g);

        addLbl(pForm, g, 0, 8, "Evidencias:");
        g.gridx = 1;
        pForm.add(pFotos, g);

        // Text Areas
        g.gridwidth = 2;
        addLbl(pForm, g, 2, 1, "Falla Reportada:");
        g.gridx = 2;
        g.gridy = 2;
        g.gridheight = 2;
        pForm.add(new JScrollPane(txtFalla), g);

        addLbl(pForm, g, 2, 4, "Diagnóstico Téc:"); // Ajuste Y
        g.gridx = 2;
        g.gridy = 5;
        g.gridheight = 3;
        pForm.add(new JScrollPane(txtDiagnostico), g);

        g.gridheight = 1;

        // Costos
        g.gridy = 9;
        addLbl(pForm, g, 0, 9, "Costo Total:");
        g.gridx = 1;
        pForm.add(txtCosto, g);
        addLbl(pForm, g, 2, 9, "Anticipo:");
        g.gridx = 3;
        pForm.add(txtAnticipo, g);

        g.gridy = 10;
        addLbl(pForm, g, 0, 10, "ESTADO:");
        g.gridx = 1;
        pForm.add(cmbEstado, g);

        // --- BOTONES INFERIORES ---
        JPanel pAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pAcciones.setBackground(Estilos.COLOR_PANEL);

        BotonPro btnGuardar = new BotonPro("GUARDAR", "guardar.png", Estilos.COLOR_ACCENT, this::guardarOrden);
        BotonPro btnPDF = new BotonPro("PDF", "ticket.png", new Color(200, 50, 50), this::generarPDFFormulario);

        // Botones de cobro rápido
        BotonPro btnAnticipo = new BotonPro("$ Anticipo", Color.ORANGE, () -> cobrarConcepto("ANTICIPO"));
        BotonPro btnLiquidar = new BotonPro("$ LIQUIDAR", new Color(46, 204, 113), () -> cobrarConcepto("LIQUIDACION"));

        pAcciones.add(btnAnticipo);
        pAcciones.add(btnLiquidar);
        pAcciones.add(Box.createHorizontalStrut(20));
        pAcciones.add(btnPDF);
        pAcciones.add(btnGuardar);

        panel.add(pBarra, BorderLayout.NORTH);
        panel.add(pForm, BorderLayout.CENTER);
        panel.add(pAcciones, BorderLayout.SOUTH);

        return panel;
    }

    // =================================================================
    // MÉTODOS DE LÓGICA Y BD
    // =================================================================

    private void mostrarFormulario(int idOrden) {
        idOrdenActual = idOrden;
        limpiarFormulario();

        if (idOrden == -1) {
            lblTituloForm.setText(" NUEVA ORDEN");
            cmbEstado.setSelectedItem("RECIBIDO");
            alternarPanelTintas();
        } else {
            lblTituloForm.setText(" EDITANDO ORDEN #" + idOrden);
            cargarDatosOrden(idOrden);
        }
        cardLayout.show(panelContenedor, VISTA_FORM);
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtTelefono.setText("");
        txtDispositivo.setText("");
        txtMarca.setText("");
        txtFalla.setText("");
        txtDiagnostico.setText("");
        txtCosto.setText("0.00");
        txtAnticipo.setText("0.00");
    }

    private void cargarDatosOrden(int id) {
        try (Connection conn = ConexionBD.conectar()) {
            PreparedStatement ps = conn.prepareStatement("SELECT o.*, c.nombre, c.telefono FROM ordenes_servicio o JOIN clientes c ON o.id_cliente = c.id WHERE o.id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lblFolio.setText("FOLIO: " + id);
                txtNombre.setText(rs.getString("nombre"));
                txtTelefono.setText(rs.getString("telefono"));

                String tipo = rs.getString("tipo_equipo");
                cmbTipoEquipo.setSelectedItem(tipo);
                alternarPanelTintas();

                txtDispositivo.setText(rs.getString("dispositivo"));
                txtMarca.setText(rs.getString("marca_modelo"));
                txtPass.setText(rs.getString("password_patron"));
                txtFalla.setText(rs.getString("falla_reportada"));
                txtDiagnostico.setText(rs.getString("diagnostico_tecnico"));
                txtCosto.setText(String.valueOf(rs.getDouble("costo_estimado")));
                txtAnticipo.setText(String.valueOf(rs.getDouble("anticipo")));
                cmbEstado.setSelectedItem(rs.getString("estado"));

                // Cargar Tintas
                String tintas = rs.getString("niveles_tinta");
                if (tintas != null && !tintas.isEmpty()) {
                    String[] vals = tintas.split(",");
                    if (vals.length == 4) {
                        slC.setValue(Integer.parseInt(vals[0]));
                        slM.setValue(Integer.parseInt(vals[1]));
                        slY.setValue(Integer.parseInt(vals[2]));
                        slK.setValue(Integer.parseInt(vals[3]));
                    }
                }

                // Cargar Rutas Imagenes
                String imgs = rs.getString("rutas_imagenes");
                if (imgs != null && !imgs.isEmpty()) {
                    Collections.addAll(rutasImagenes, imgs.split(";"));
                    lblFotosCount.setText(rutasImagenes.size() + " imgs");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void guardarOrden() {
        if (txtNombre.getText().isEmpty() || txtDispositivo.getText().isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Datos", "Nombre y Dispositivo obligatorios.", "ADVERTENCIA");
            return;
        }

        try (Connection conn = ConexionBD.conectar()) {
            int idCliente = obtenerOInsertarCliente(conn);
            String tintas = slC.getValue() + "," + slM.getValue() + "," + slY.getValue() + "," + slK.getValue();
            String imgs = String.join(";", rutasImagenes);

            if (idOrdenActual == -1) {
                String sql = "INSERT INTO ordenes_servicio (id_cliente, dispositivo, tipo_equipo, marca_modelo, password_patron, falla_reportada, diagnostico_tecnico, costo_estimado, anticipo, estado, niveles_tinta, rutas_imagenes) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                llenarParams(ps, idCliente, tintas, imgs);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) idOrdenActual = rs.getInt(1);
            } else {
                String sql = "UPDATE ordenes_servicio SET id_cliente=?, dispositivo=?, tipo_equipo=?, marca_modelo=?, password_patron=?, falla_reportada=?, diagnostico_tecnico=?, costo_estimado=?, anticipo=?, estado=?, niveles_tinta=?, rutas_imagenes=? WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                llenarParams(ps, idCliente, tintas, imgs);
                ps.setInt(13, idOrdenActual);
                ps.executeUpdate();
            }
            lblFolio.setText("FOLIO: " + idOrdenActual);
            JOptionPanePro.mostrarMensaje(this, "Guardado", "Orden guardada correctamente.", "INFO");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error", e.getMessage(), "ERROR");
        }
    }

    private void cobrarConcepto(String concepto) {
        if (idOrdenActual == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Guarda la orden primero.", "ADVERTENCIA");
            return;
        }
        String input = JOptionPanePro.solicitarEntrada(this, "Cobro " + concepto, "Monto:");
        if (input == null || input.isEmpty()) return;

        try (Connection conn = ConexionBD.conectar()) {
            double monto = Double.parseDouble(input);
            conn.setAutoCommit(false);

            // 1. Venta Cabecera
            PreparedStatement psV = conn.prepareStatement("INSERT INTO ventas (total_venta, ganancia_total, tipo_venta) VALUES (?, ?, 'SERVICIO')", Statement.RETURN_GENERATED_KEYS);
            psV.setDouble(1, monto);
            psV.setDouble(2, monto);
            psV.executeUpdate();
            ResultSet rs = psV.getGeneratedKeys();
            rs.next();
            int idVenta = rs.getInt(1);

            // 2. Detalle
            String desc = concepto + ": " + txtDispositivo.getText() + " (" + txtNombre.getText() + ")";
            PreparedStatement psD = conn.prepareStatement("INSERT INTO detalle_venta (id_venta, id_producto, cantidad, subtotal, descripcion) VALUES (?, NULL, 1, ?, ?)");
            psD.setInt(1, idVenta);
            psD.setDouble(2, monto);
            psD.setString(3, desc.toUpperCase());
            psD.executeUpdate();

            // 3. Actualizar Orden
            if ("ANTICIPO".equals(concepto)) {
                conn.prepareStatement("UPDATE ordenes_servicio SET anticipo = anticipo + " + monto + " WHERE id=" + idOrdenActual).executeUpdate();
                txtAnticipo.setText(String.valueOf(parser(txtAnticipo.getText()) + monto));
            }
            if ("LIQUIDACION".equals(concepto)) {
                conn.prepareStatement("UPDATE ordenes_servicio SET estado='ENTREGADO' WHERE id=" + idOrdenActual).executeUpdate();
                cmbEstado.setSelectedItem("ENTREGADO");
            }
            conn.commit();
            JOptionPanePro.mostrarMensaje(this, "Cobrado", "Ingreso registrado: $" + monto, "INFO");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- GENERACIÓN PDF (Formulario) ---
    private void generarPDFFormulario() {
        if (idOrdenActual == -1) {
            JOptionPanePro.mostrarMensaje(this, "Error", "Guarda la orden.", "ERROR");
            return;
        }

        String tintas = "IMPRESORA".equals(cmbTipoEquipo.getSelectedItem()) ? slC.getValue() + "," + slM.getValue() + "," + slY.getValue() + "," + slK.getValue() : null;

        GeneradorPDF.crearOrdenServicio(idOrdenActual, txtNombre.getText(), txtTelefono.getText(), txtDispositivo.getText() + " " + txtMarca.getText(), txtFalla.getText(), "Pass: " + txtPass.getText() + "\n" + txtDiagnostico.getText(), tintas, "Orden_" + idOrdenActual + ".pdf");
    }

    // --- ACCIONES DESDE TABLA (Sin abrir formulario) ---
    private void generarPDFDesdeID(int id) {
        // Consultar datos mínimos para PDF
        try (Connection c = ConexionBD.conectar(); PreparedStatement ps = c.prepareStatement("SELECT o.*, cl.nombre, cl.telefono FROM ordenes_servicio o JOIN clientes cl ON o.id_cliente=cl.id WHERE o.id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String infoEq = rs.getString("dispositivo") + " " + rs.getString("marca_modelo");
                String notas = "Pass: " + rs.getString("password_patron") + "\n" + rs.getString("diagnostico_tecnico");
                GeneradorPDF.crearOrdenServicio(id, rs.getString("nombre"), rs.getString("telefono"), infoEq, rs.getString("falla_reportada"), notas, rs.getString("niveles_tinta"), "Orden_" + id + ".pdf");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enviarWhatsAppDesdeID(int id) {
        try (Connection c = ConexionBD.conectar(); PreparedStatement ps = c.prepareStatement("SELECT o.estado, o.costo_estimado, o.anticipo, cl.telefono FROM ordenes_servicio o JOIN clientes cl ON o.id_cliente=cl.id WHERE o.id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String tel = rs.getString("telefono");
                String est = rs.getString("estado");
                String msg = "Actualización Orden #" + id + ": " + est;
                if ("LISTO".equals(est)) {
                    double saldo = rs.getDouble("costo_estimado") - rs.getDouble("anticipo");
                    msg += ". Su equipo está listo. Saldo pendiente: $" + saldo;
                }
                Desktop.getDesktop().browse(new URI("https://wa.me/52" + tel + "?text=" + URLEncoder.encode(msg, StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- UTILIDADES ---
    private void llenarParams(PreparedStatement ps, int idC, String tintas, String imgs) throws SQLException {
        ps.setInt(1, idC);
        ps.setString(2, txtDispositivo.getText());
        ps.setString(3, cmbTipoEquipo.getSelectedItem().toString());
        ps.setString(4, txtMarca.getText());
        ps.setString(5, txtPass.getText());
        ps.setString(6, txtFalla.getText());
        ps.setString(7, txtDiagnostico.getText());
        ps.setDouble(8, parser(txtCosto.getText()));
        ps.setDouble(9, parser(txtAnticipo.getText()));
        ps.setString(10, cmbEstado.getSelectedItem().toString());
        ps.setString(11, tintas);
        ps.setString(12, imgs);
    }

    private void cargarOrdenes(String filtro) {
        modeloOrdenes.setRowCount(0);
        String sql = "SELECT o.id, c.nombre, c.telefono, o.dispositivo, o.estado, o.fecha_recepcion FROM ordenes_servicio o JOIN clientes c ON o.id_cliente=c.id ";
        if ("ACTIVOS".equals(filtro)) sql += "WHERE o.estado != 'ENTREGADO' ";
        else if ("HISTORIAL".equals(filtro)) sql += "WHERE o.estado = 'ENTREGADO' ";
        else
            sql += "WHERE (c.nombre LIKE '%" + txtBuscar.getText() + "%' OR o.id LIKE '%" + txtBuscar.getText() + "%') ";
        sql += "ORDER BY o.id DESC";

        try (Connection c = ConexionBD.conectar(); ResultSet rs = c.createStatement().executeQuery(sql)) {
            while (rs.next())
                modeloOrdenes.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getTimestamp(6)});
        } catch (Exception e) {
        }
    }

    private void alternarPanelTintas() {
        panelTintas.setVisible("IMPRESORA".equals(cmbTipoEquipo.getSelectedItem()));
        panelTintas.revalidate();
    }

    private void adjuntarFoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Imgs", "jpg", "png"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            rutasImagenes.add(fc.getSelectedFile().getAbsolutePath());
            lblFotosCount.setText(rutasImagenes.size() + " imgs");
        }
    }

    private int obtenerOInsertarCliente(Connection c) throws SQLException {
        // Implementación resumida pero segura (reutilizar la lógica de tu código anterior)
        String tel = txtTelefono.getText();
        ResultSet rs = c.createStatement().executeQuery("SELECT id FROM clientes WHERE telefono='" + tel + "'");
        if (rs.next()) return rs.getInt(1);
        PreparedStatement ps = c.prepareStatement("INSERT INTO clientes (nombre, telefono) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, txtNombre.getText());
        ps.setString(2, tel);
        ps.executeUpdate();
        ResultSet rs2 = ps.getGeneratedKeys();
        rs2.next();
        return rs2.getInt(1);
    }

    private JTextField input() {
        JTextField t = new JTextField(10);
        Estilos.estilizarInput(t);
        return t;
    }

    private JTextArea area() {
        JTextArea t = new JTextArea(3, 10);
        t.setBackground(Estilos.COLOR_INPUT);
        t.setForeground(Color.WHITE);
        t.setBorder(BorderFactory.createLineBorder(Estilos.COLOR_BORDER));
        return t;
    }

    private void addLbl(JPanel p, GridBagConstraints g, int x, int y, String t) {
        g.gridx = x;
        g.gridy = y;
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        p.add(l, g);
    }

    private void addLbl(JPanel p, GridBagConstraints g, int x, int y, String t, int w) {
        g.gridwidth = w;
        addLbl(p, g, x, y, t);
        g.gridwidth = 1;
    }

    private double parser(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private JSlider slider(Color c) {
        JSlider s = new JSlider(0, 100, 50);
        s.setBackground(c);
        return s;
    }
}
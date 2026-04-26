package ui.paneles;



import config.ConexionBD;
import servicios.DialogoServicioRapido;
import servicios.GeneradorPDF;
import servicios.GeneradorTicket;
import servicios.ImpresoraTicket;
import ui.componentes.*;
import ui.ventanas.DialogoServiciosFijos;
import util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PanelServicios extends JPanel {

    // CONTROL DE VISTAS
    private final CardLayout cardLayout;
    private final JPanel panelContenedor;
    private final String VISTA_LISTA = "LISTA";
    private final String VISTA_FORM = "FORM";

    // VISTA LISTA
    private TablaPro tablaOrdenes;
    private DefaultTableModel modeloOrdenes;
    private JTextField txtBuscar;

    // VISTA FORMULARIO
    private JTextField txtNombre, txtTelefono, txtDispositivo, txtMarca, txtPass;
    private JTextArea txtFalla, txtDiagnostico;
    private JTextField txtCostoPresupuesto; // Antes txtCosto
    private JLabel lblTituloForm, lblFotosCount, lblFolio, lblSaldoPendiente;

    private DefaultTableModel modeloPagos;

    private JComboBox<String> cmbEstado, cmbTipoEquipo;
    private JPanel panelTintas;
    private JSlider slC, slM, slY, slK;

    // Variables Control
    private int idOrdenActual = -1;
    private final List<String> rutasImagenes = new ArrayList<>();

    private TablaPro tablaPagos;

    public PanelServicios() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        cardLayout = new CardLayout();
        panelContenedor = new JPanel(cardLayout);

        panelContenedor.add(crearVistaLista(), VISTA_LISTA);
        panelContenedor.add(crearVistaFormulario(), VISTA_FORM);

        Estilos.estilizarTabla(tablaPagos);
        Estilos.estilizarTabla(tablaOrdenes);

        add(panelContenedor, BorderLayout.CENTER);
        cargarOrdenes("ACTIVOS");
    }

    // =================================================================
    // 1. VISTA LISTA
    // =================================================================
    private JPanel crearVistaLista() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Estilos.COLOR_FONDO);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // HEADER
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(Estilos.COLOR_FONDO);

        JLabel lblTitulo = new JLabel("Taller y Servicios");
        lblTitulo.setFont(Estilos.FONT_TITULO);
        lblTitulo.setForeground(Color.WHITE);

        // BOTONERA SUPERIOR
        JPanel pBtnsTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBtnsTop.setBackground(Estilos.COLOR_FONDO);

        // 1. Botón Servicio Rápido (Nuevo)
        BotonPro btnRapido = new BotonPro("SERVICIO RÁPIDO", new Color(41, 128, 185), () -> {
            // Abrir el diálogo modal
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            new DialogoServicioRapido(topFrame).setVisible(true);
        });

        // 2. Botón Nueva Orden (Ahora VERDE)
        BotonPro btnNuevo = new BotonPro("NUEVA ORDEN", "mas.png", new Color(46, 204, 113), () -> mostrarFormulario(-1));
        btnNuevo.setPreferredSize(new Dimension(180, 45));

        pBtnsTop.add(btnRapido);
        pBtnsTop.add(Box.createHorizontalStrut(10));
        pBtnsTop.add(btnNuevo);

        pHeader.add(lblTitulo, BorderLayout.WEST);
        pHeader.add(pBtnsTop, BorderLayout.EAST);

        // FILTROS
        JPanel pFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pFiltros.setBackground(Estilos.COLOR_FONDO);
        pFiltros.setBorder(new EmptyBorder(10, 0, 10, 0));

        txtBuscar = new JTextField(20); Estilos.estilizarInput(txtBuscar);
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

        // TABLA
        modeloOrdenes = new DefaultTableModel(new String[]{"Folio", "Cliente", "Teléfono", "Equipo", "Estado", "Fecha"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaOrdenes = new TablaPro(modeloOrdenes);

        // Popup y Eventos
        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemEditar = new JMenuItem("✏ Editar Orden"); itemEditar.addActionListener(e -> accionTabla("EDITAR"));
        JMenuItem itemPDF = new JMenuItem("📄 Generar PDF"); itemPDF.addActionListener(e -> accionTabla("PDF"));
        JMenuItem itemWhats = new JMenuItem("📱 WhatsApp"); itemWhats.addActionListener(e -> accionTabla("WHATS"));
        popup.add(itemEditar); popup.add(itemPDF); popup.add(itemWhats);
        tablaOrdenes.setComponentPopupMenu(popup);

        tablaOrdenes.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { if(e.getClickCount() == 2) accionTabla("EDITAR"); }
        });

        JPanel pCentro = new JPanel(new BorderLayout());
        pCentro.add(pFiltros, BorderLayout.NORTH);
        pCentro.add(new JScrollPane(tablaOrdenes), BorderLayout.CENTER);

        panel.add(pHeader, BorderLayout.NORTH);
        panel.add(pCentro, BorderLayout.CENTER);

        return panel;
    }

    private void accionTabla(String accion) {
        int row = tablaOrdenes.getSelectedRow();
        if (row == -1) return;
        int id = Integer.parseInt(modeloOrdenes.getValueAt(row, 0).toString());
        switch (accion) {
            case "EDITAR" -> mostrarFormulario(id);
            case "PDF" -> generarPDFDesdeID(id);
            case "WHATS" -> enviarWhatsAppDesdeID(id);
        }
    }

    // =================================================================
    // 2. VISTA FORMULARIO
    // =================================================================
    // ==========================================
    // VISTA 2: FORMULARIO (REDIMENSIONADO)
    // ==========================================
    private JPanel crearVistaFormulario() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Estilos.COLOR_PANEL);

        // --- 1. BARRA SUPERIOR (VOLVER) ---
        JPanel pBarra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pBarra.setBackground(new Color(30, 35, 50));
        BotonPro btnVolver = new BotonPro("← Volver", Color.GRAY, () -> {
            cargarOrdenes("ACTIVOS");
            cardLayout.show(panelContenedor, VISTA_LISTA);
        });
        lblTituloForm = new JLabel(" DETALLES DE LA ORDEN");
        lblTituloForm.setForeground(Color.WHITE);
        lblTituloForm.setFont(Estilos.FONT_BOLD);
        pBarra.add(btnVolver); pBarra.add(lblTituloForm);

        // --- 2. CONTENEDOR CENTRAL (DIVIDIDO EN 2 COLUMNAS) ---
        JPanel pCentro = new JPanel(new GridLayout(1, 2, 20, 0)); // 1 Fila, 2 Columnas, Espacio 20
        pCentro.setBackground(Estilos.COLOR_PANEL);
        pCentro.setBorder(new EmptyBorder(10, 20, 10, 20));

        // =========================================================
        // COLUMNA IZQUIERDA: DATOS TÉCNICOS
        // =========================================================
        JPanel pIzquierda = new JPanel(new GridBagLayout());
        pIzquierda.setBackground(Estilos.COLOR_PANEL);
        pIzquierda.setBorder(BorderFactory.createTitledBorder(null, "Datos del Equipo", 0,0, Estilos.FONT_BOLD, Color.WHITE));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5); g.fill = GridBagConstraints.HORIZONTAL;

        lblFolio = new JLabel("NUEVO"); lblFolio.setFont(new Font("Segoe UI", Font.BOLD, 16)); lblFolio.setForeground(Color.GREEN);

        // Inicialización de componentes (si no están ya inicializados arriba)
        txtNombre = input(); txtTelefono = input();
        txtDispositivo = input(); txtMarca = input(); txtPass = input();
        txtFalla = area(); txtDiagnostico = area();

        cmbTipoEquipo = new JComboBox<>(new String[]{"CELULAR", "LAPTOP", "PC", "IMPRESORA", "CONSOLA", "OTRO"});
        cmbTipoEquipo.addActionListener(e -> alternarPanelTintas());

        cmbEstado = new JComboBox<>(new String[]{"RECIBIDO", "DIAGNOSTICO", "EN REPARACION", "ESPERA REFACCION", "LISTO", "ENTREGADO"});

        // Panel Tintas
        panelTintas = new JPanel(new GridLayout(1, 4, 5, 0));
        panelTintas.setBackground(Estilos.COLOR_PANEL);
        slC = slider(Color.CYAN); slM = slider(Color.MAGENTA); slY = slider(Color.YELLOW); slK = slider(Color.GRAY);
        panelTintas.add(slC); panelTintas.add(slM); panelTintas.add(slY); panelTintas.add(slK);
        panelTintas.setVisible(false);

        // Panel Fotos
        JPanel pFotos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pFotos.setBackground(Estilos.COLOR_PANEL);
        BotonPro btnAddFoto = new BotonPro("Adjuntar", "imagen.png", Estilos.COLOR_INPUT, this::adjuntarFoto);
        lblFotosCount = new JLabel("0 imgs"); lblFotosCount.setForeground(Color.WHITE);
        pFotos.add(btnAddFoto); pFotos.add(lblFotosCount);

        // --- ARMADO IZQUIERDO ---
        g.gridwidth=2; g.gridx=0; g.gridy=0; pIzquierda.add(lblFolio, g);

        // Cliente
        g.gridwidth=1;
        addLbl(pIzquierda, g, 0, 1, "Cliente:"); g.gridx=1; pIzquierda.add(txtNombre, g);
        addLbl(pIzquierda, g, 0, 2, "Teléfono:"); g.gridx=1; pIzquierda.add(txtTelefono, g);

        // Equipo
        addLbl(pIzquierda, g, 0, 3, "Tipo:"); g.gridx=1; pIzquierda.add(cmbTipoEquipo, g);

        g.gridwidth=2; g.gridx=0; g.gridy=4; pIzquierda.add(panelTintas, g); // Tintas (Oculto)

        g.gridwidth=1;
        addLbl(pIzquierda, g, 0, 5, "Modelo:"); g.gridx=1; pIzquierda.add(txtDispositivo, g);
        // CORRECCIÓN BUG 1: Aquí agregamos explícitamente la Marca en su propia fila
        addLbl(pIzquierda, g, 0, 6, "Marca:"); g.gridx=1; pIzquierda.add(txtMarca, g);

        addLbl(pIzquierda, g, 0, 7, "Pass/Patrón:"); g.gridx=1; pIzquierda.add(txtPass, g);
        addLbl(pIzquierda, g, 0, 8, "Evidencias:"); g.gridx=1; pIzquierda.add(pFotos, g);

        // Text Areas
        addLbl(pIzquierda, g, 0, 9, "Falla Cliente:");
        g.gridwidth=2; g.gridx=0; g.gridy=10; pIzquierda.add(new JScrollPane(txtFalla), g);

        g.gridwidth=1;
        addLbl(pIzquierda, g, 0, 11, "Diagnóstico:");
        g.gridwidth=2; g.gridx=0; g.gridy=12; pIzquierda.add(new JScrollPane(txtDiagnostico), g);

        g.gridwidth=1;
        addLbl(pIzquierda, g, 0, 13, "ESTADO ACTUAL:"); g.gridx=1; pIzquierda.add(cmbEstado, g);


        // =========================================================
        // COLUMNA DERECHA: FINANZAS E HISTORIAL
        // =========================================================
        JPanel pDerecha = new JPanel(new BorderLayout(0, 10));
        pDerecha.setBackground(Estilos.COLOR_PANEL);
        pDerecha.setBorder(BorderFactory.createTitledBorder(null, "Estado de Cuenta", 0,0, Estilos.FONT_BOLD, Color.WHITE));

        // Tabla de Movimientos (Fecha, Concepto, Cargo, Abono, Saldo)
        // Usaremos un renderizador personalizado más adelante para los colores
        modeloPagos = new DefaultTableModel(new String[]{"Fecha", "Concepto", "Cargo (+)", "Abono (-)", "Saldo", "ID", "TIPO"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaPagos = new TablaPro(modeloPagos);


        // Ajuste de columnas visibles
        tablaPagos.getColumnModel().getColumn(0).setPreferredWidth(120); // Fecha
        tablaPagos.getColumnModel().getColumn(1).setPreferredWidth(200); // Concepto

        // --- NUEVO: OCULTAR LAS COLUMNAS TÉCNICAS (5 y 6) ---
        // ID (Índice 5)
        tablaPagos.getColumnModel().getColumn(5).setMinWidth(0);
        tablaPagos.getColumnModel().getColumn(5).setMaxWidth(0);
        tablaPagos.getColumnModel().getColumn(5).setPreferredWidth(0);

        // TIPO (Índice 6)
        tablaPagos.getColumnModel().getColumn(6).setMinWidth(0);
        tablaPagos.getColumnModel().getColumn(6).setMaxWidth(0);
        tablaPagos.getColumnModel().getColumn(6).setPreferredWidth(0);

        JScrollPane scrollPagos = new JScrollPane(tablaPagos);
        scrollPagos.getViewport().setBackground(Estilos.COLOR_PANEL);

        // Botón para agregar cargos (Servicios nuevos)
        // Botón para agregar cargos (Servicios nuevos)
        JPanel pAddCargo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pAddCargo.setBackground(Estilos.COLOR_PANEL);

        // --- 1. Creamos el menú desplegable ---
        JPopupMenu menuCargos = new JPopupMenu();

        JMenuItem itemServicio = new JMenuItem("🛠 Agregar Servicio (Fijo/Especial)");
        itemServicio.addActionListener(e -> {
            if (idOrdenActual == -1) { JOptionPanePro.mostrarMensaje(this, "Aviso", "Guarda la orden primero.", "ADVERTENCIA"); return; }
            new DialogoServiciosFijos((Frame) SwingUtilities.getWindowAncestor(this), idOrdenActual).setVisible(true);
            cargarHistorialPagos(idOrdenActual);
        });

        JMenuItem itemProducto = new JMenuItem("📦 Agregar Refacción (Inventario)");
        itemProducto.addActionListener(e -> {
            if (idOrdenActual == -1) { JOptionPanePro.mostrarMensaje(this, "Aviso", "Guarda la orden primero.", "ADVERTENCIA"); return; }
            // Llamamos a la ventana transaccional que creamos en el Paso 1
            new ui.ventanas.DialogoAgregarProducto((Frame) SwingUtilities.getWindowAncestor(this), idOrdenActual).setVisible(true);
            cargarHistorialPagos(idOrdenActual);
        });

        menuCargos.add(itemServicio);
        menuCargos.add(itemProducto);

        // --- 2. Creamos el botón (Le añadimos una flechita ▼ visual) ---
        BotonPro btnAddCargo = new BotonPro("➕ Agregar Costo ▼", new Color(41, 98, 255), () -> {});

        // --- 3. Lógica del Mouse (Hover y Clic) ---
        btnAddCargo.addMouseListener(new MouseAdapter() {
           @Override
            public void mousePressed(MouseEvent e) {
                // También se abre al hacer clic, por si el hover falla
                menuCargos.show(btnAddCargo, 0, btnAddCargo.getHeight());
            }
        });

        // Botón para eliminar el último movimiento (por si hay error)
        BotonPro btnBorrarMov = new BotonPro("Borrar Seleccionado", new Color(200, 50, 50), this::eliminarMovimiento);
        btnBorrarMov.setPreferredSize(new Dimension(140, 35));

        pAddCargo.add(btnAddCargo);
        pAddCargo.add(btnBorrarMov);

        // Panel de Saldos Finales
        JPanel pTotales = new JPanel(new GridLayout(2, 1));
        pTotales.setBackground(new Color(40, 45, 60));
        pTotales.setBorder(new EmptyBorder(10, 15, 10, 15));

        // El costo presupuesto ya no se edita manual, es automático
        txtCostoPresupuesto = input();
        txtCostoPresupuesto.setEditable(false);
        txtCostoPresupuesto.setText("0.00");
        txtCostoPresupuesto.setForeground(Color.CYAN); // Resaltar

        lblSaldoPendiente = new JLabel("SALDO PENDIENTE: $0.00");
        lblSaldoPendiente.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblSaldoPendiente.setForeground(Color.ORANGE);
        lblSaldoPendiente.setHorizontalAlignment(SwingConstants.RIGHT);

        pTotales.add(lblSaldoPendiente);

        // Armado Derecha
        pDerecha.add(pAddCargo, BorderLayout.NORTH); // Botón arriba
        pDerecha.add(scrollPagos, BorderLayout.CENTER);
        pDerecha.add(pTotales, BorderLayout.SOUTH);


        // --- UNIÓN ---
        pCentro.add(pIzquierda);
        pCentro.add(pDerecha);

        // --- BOTONES DE ACCIÓN (INFERIOR GLOBAL) ---
        JPanel pAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pAcciones.setBackground(Estilos.COLOR_PANEL);

        BotonPro btnGuardar = new BotonPro("GUARDAR", "guardar.png", Estilos.COLOR_ACCENT, this::guardarOrden);
        BotonPro btnPDF = new BotonPro("PDF", "ticket.png", new Color(200, 50, 50), this::generarPDFFormulario);
        BotonPro btnAbonar = new BotonPro("$ ABONAR", Color.ORANGE, () -> cobrarConcepto("ABONO"));
        BotonPro btnLiquidar = new BotonPro("$ LIQUIDAR", new Color(46, 204, 113), () -> cobrarConcepto("LIQUIDACION"));

        pAcciones.add(btnAbonar); pAcciones.add(btnLiquidar);
        pAcciones.add(Box.createHorizontalStrut(20));
        pAcciones.add(btnPDF); pAcciones.add(btnGuardar);

        // Armado Panel Principal
        panel.add(pBarra, BorderLayout.NORTH);
        panel.add(pCentro, BorderLayout.CENTER);
        panel.add(pAcciones, BorderLayout.SOUTH);

        return panel;
    }

    // =================================================================
    // LÓGICA PRINCIPAL
    // =================================================================

    public void mostrarFormulario(int idOrden) {
        idOrdenActual = idOrden;
        limpiarFormulario();

        if (idOrden == -1) {
            // CORRECCIÓN BUG FOLIO: Obtener el siguiente ID probable
            int siguienteFolio = obtenerSiguienteFolio();
            lblTituloForm.setText(" NUEVA ORDEN DE SERVICIO");
            lblFolio.setText("FOLIO SUGERIDO: " + siguienteFolio); // Solo visual
            cmbEstado.setSelectedItem("RECIBIDO");
            alternarPanelTintas();
        } else {
            lblTituloForm.setText(" EDITANDO ORDEN #" + idOrden);
            cargarDatosOrden(idOrden);
            cargarHistorialPagos(idOrden); // Cargar tabla de pagos
        }
        cardLayout.show(panelContenedor, VISTA_FORM);
    }

    private int obtenerSiguienteFolio() {
        try (Connection c = ConexionBD.conectar()) {
            ResultSet rs = c.createStatement().executeQuery("SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'punto_venta' AND TABLE_NAME = 'ordenes_servicio'");
            if(rs.next()) return rs.getInt(1);
        } catch(Exception ignored){}
        return 0;
    }

    // --- CARGAR DATOS Y PAGOS ---
    private void cargarDatosOrden(int id) {
        try (Connection conn = ConexionBD.conectar()) {
            PreparedStatement ps = conn.prepareStatement("SELECT o.*, c.nombre, c.telefono FROM ordenes_servicio o JOIN clientes c ON o.id_cliente = c.id WHERE o.id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lblFolio.setText("FOLIO: " + id);
                txtNombre.setText(rs.getString("nombre")); txtTelefono.setText(rs.getString("telefono"));
                cmbTipoEquipo.setSelectedItem(rs.getString("tipo_equipo")); alternarPanelTintas();
                txtDispositivo.setText(rs.getString("dispositivo")); txtMarca.setText(rs.getString("marca_modelo"));
                txtPass.setText(rs.getString("password_patron")); txtFalla.setText(rs.getString("falla_reportada"));
                txtDiagnostico.setText(rs.getString("diagnostico_tecnico"));
                txtCostoPresupuesto.setText(String.valueOf(rs.getDouble("costo_estimado")));
                cmbEstado.setSelectedItem(rs.getString("estado"));

                // Cargar Tintas e Imagenes (Igual que antes)
                String tintas = rs.getString("niveles_tinta");
                if (tintas != null && !tintas.isEmpty()) { String[] vals = tintas.split(","); if(vals.length==4){ slC.setValue(Integer.parseInt(vals[0])); slM.setValue(Integer.parseInt(vals[1])); slY.setValue(Integer.parseInt(vals[2])); slK.setValue(Integer.parseInt(vals[3])); }}
                String imgs = rs.getString("rutas_imagenes");
                if(imgs != null && !imgs.isEmpty()) { Collections.addAll(rutasImagenes, imgs.split(";")); lblFotosCount.setText(rutasImagenes.size() + " imgs"); }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private class Movimiento implements Comparable<Movimiento> {
        Timestamp fecha;
        String concepto;
        double monto;
        String tipo; // "CARGO" o "ABONO"
        int idRef;   // ID para poder borrarlo si es necesario

        public Movimiento(Timestamp f, String c, double m, String t, int id) {
            this.fecha = f; this.concepto = c; this.monto = m; this.tipo = t; this.idRef = id;
        }

        @Override
        public int compareTo(Movimiento o) {
            return this.fecha.compareTo(o.fecha);
        }
    }

    private void cargarHistorialPagos(int idOrden) {
        modeloPagos.setRowCount(0);
        List<Movimiento> movimientos = new ArrayList<>();
        double totalCargos = 0;
        double totalAbonos = 0;

        try (Connection conn = ConexionBD.conectar()) {
            // 1. OBTENER CARGOS (Deuda)
            String sqlCargos = "SELECT id, fecha, concepto, monto FROM cargos_orden WHERE id_orden = ?";
            PreparedStatement psC = conn.prepareStatement(sqlCargos);
            psC.setInt(1, idOrden);
            ResultSet rsC = psC.executeQuery();
            while(rsC.next()) {
                movimientos.add(new Movimiento(
                        rsC.getTimestamp("fecha"),
                        rsC.getString("concepto"),
                        rsC.getDouble("monto"),
                        "CARGO",
                        rsC.getInt("id")
                ));
            }

            // 2. OBTENER ABONOS (Pagos en Ventas)
            // Usamos JOIN para traer la descripción exacta
            String sqlAbonos = "SELECT v.id, v.fecha, d.descripcion, v.total_venta " +
                    "FROM ventas v JOIN detalle_venta d ON v.id = d.id_venta " +
                    "WHERE v.id_orden_servicio = ?";
            PreparedStatement psA = conn.prepareStatement(sqlAbonos);
            psA.setInt(1, idOrden);
            ResultSet rsA = psA.executeQuery();
            while(rsA.next()) {
                movimientos.add(new Movimiento(
                        rsA.getTimestamp("fecha"),
                        rsA.getString("descripcion"),
                        rsA.getDouble("total_venta"),
                        "ABONO",
                        rsA.getInt("id") // ID de venta
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 3. ORDENAR CRONOLÓGICAMENTE
        Collections.sort(movimientos);

        // 4. CALCULAR SALDOS Y LLENAR TABLA
        double saldoAcumulado = 0;

        for (Movimiento m : movimientos) {
            String cargoStr = "-";
            String abonoStr = "-";

            if ("CARGO".equals(m.tipo)) {
                saldoAcumulado += m.monto;
                totalCargos += m.monto;
                cargoStr = String.format("$%.2f", m.monto);
            } else {
                saldoAcumulado -= m.monto;
                totalAbonos += m.monto;
                abonoStr = String.format("$%.2f", m.monto);
            }

            // Columna oculta al final con ID y TIPO para poder borrar
            modeloPagos.addRow(new Object[]{
                    m.fecha,
                    m.concepto,
                    cargoStr,
                    abonoStr,
                    String.format("$%.2f", saldoAcumulado),
                    m.idRef, // Columna 5 (Oculta)
                    m.tipo   // Columna 6 (Oculta)
            });
        }

        // 5. ACTUALIZAR VISUALES
        txtCostoPresupuesto.setText(String.format("%.2f", totalCargos));
        lblSaldoPendiente.setText("SALDO PENDIENTE: $" + String.format("%.2f", saldoAcumulado));

        if (saldoAcumulado <= 0 && totalCargos > 0) {
            lblSaldoPendiente.setForeground(Color.GREEN); // Pagado
            lblSaldoPendiente.setText("¡CUENTA LIQUIDADA!");
        } else {
            lblSaldoPendiente.setForeground(Color.ORANGE);
        }

        // Actualizar el costo estimado en la BD principal para que coincida con la suma de cargos
        actualizarCostoTotalOrden(idOrden, totalCargos);
    }

    private void eliminarMovimiento() {
        int row = tablaPagos.getSelectedRow();
        if (row == -1) return;

        int idRef = Integer.parseInt(modeloPagos.getValueAt(row, 5).toString());
        String tipo = modeloPagos.getValueAt(row, 6).toString();

        if (JOptionPanePro.mostrarConfirmacion(this, "Eliminar", "¿Borrar movimiento?")) {
            try (Connection conn = ConexionBD.conectar()) {
                conn.setAutoCommit(false);
                try {
                    if ("CARGO".equals(tipo)) {
                        // A. Consultar si tiene producto vinculado
                        String sqlBusca = "SELECT id_producto, cantidad FROM cargos_orden WHERE id = ?";
                        PreparedStatement psB = conn.prepareStatement(sqlBusca);
                        psB.setInt(1, idRef);
                        ResultSet rsB = psB.executeQuery();

                        if (rsB.next()) {
                            int idProd = rsB.getInt("id_producto");
                            int cant = rsB.getInt("cantidad");

                            // B. Si es un producto (id > 0), devolvemos las unidades al estante
                            if (idProd > 0) {
                                conn.createStatement().executeUpdate(
                                        "UPDATE productos SET stock = stock + " + cant + " WHERE id = " + idProd
                                );
                            }
                        }
                        // C. Borrar el cargo
                        conn.createStatement().executeUpdate("DELETE FROM cargos_orden WHERE id = " + idRef);
                    } else {
                        // Lógica para ABONOS (Ventas)
                        conn.createStatement().executeUpdate("DELETE FROM ventas WHERE id = " + idRef);
                        conn.createStatement().executeUpdate("DELETE FROM detalle_venta WHERE id_venta = " + idRef);
                    }

                    conn.commit();
                    cargarHistorialPagos(idOrdenActual);
                    ToastPro.show("Movimiento eliminado e inventario actualizado", "INFO");
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // Método auxiliar para mantener sincronizada la cabecera de la orden
    private void actualizarCostoTotalOrden(int idOrden, double nuevoTotal) {
        try (Connection conn = ConexionBD.conectar()) {
            conn.createStatement().executeUpdate("UPDATE ordenes_servicio SET costo_estimado = " + nuevoTotal + " WHERE id = " + idOrden);
        } catch (Exception e) {}
    }

    // --- COBRAR CON TICKET Y VINCULACIÓN ---
    private void cobrarConcepto(String concepto) {
        if (idOrdenActual == -1) { JOptionPanePro.mostrarMensaje(this, "Aviso", "Guarda la orden primero.", "ADVERTENCIA"); return; }

        // Si es liquidación, calcular automático
        String preMonto = "";
        if ("LIQUIDACION".equals(concepto)) {
            double presupuesto = parser(txtCostoPresupuesto.getText());
            double pagado = 0;

            // CORRECCIÓN DEL BUG:
            for (int i = 0; i < modeloPagos.getRowCount(); i++) {
                // Obtenemos el valor de la columna ABONO (Índice 3)
                // Columnas: 0=Fecha, 1=Concepto, 2=Cargo, 3=Abono, 4=Saldo
                String valorCelda = modeloPagos.getValueAt(i, 3).toString();

                // Limpiamos el formato ($ y espacios)
                valorCelda = valorCelda.replace("$", "").trim();

                // Solo sumamos si NO es un guion y NO está vacío
                if (!valorCelda.equals("-") && !valorCelda.isEmpty()) {
                    pagado += Double.parseDouble(valorCelda);
                }
            }

            // Calculamos lo que falta
            double restante = presupuesto - pagado;
            // Si el restante es negativo (por error de cálculo previo), ponemos 0
            if (restante < 0) restante = 0;

            preMonto = String.valueOf(restante);
        }


        String input = JOptionPanePro.solicitarEntrada(this, "Cobro - " + concepto, "Monto a ingresar:\n(Si es 'Liquidación', confirma el saldo restante)", preMonto);
        if (input == null || input.isEmpty()) return;

        try {
            double monto = Double.parseDouble(input);
            if (monto <= 0) return;

            try (Connection conn = ConexionBD.conectar()) {
                conn.setAutoCommit(false);

                // 1. Insertar Venta CON VINCULACIÓN (id_orden_servicio)
                String sqlVenta = "INSERT INTO ventas (total_venta, ganancia_total, tipo_venta, id_orden_servicio) VALUES (?, ?, 'SERVICIO', ?)";
                PreparedStatement psV = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS);
                psV.setDouble(1, monto); psV.setDouble(2, monto); psV.setInt(3, idOrdenActual);
                psV.executeUpdate();

                ResultSet rs = psV.getGeneratedKeys(); rs.next(); int idVenta = rs.getInt(1);

                // 2. Detalle
                String desc = concepto + ": " + txtDispositivo.getText() + " (" + txtNombre.getText() + ")";
                if("LIQUIDACION".equals(concepto)) desc = "LIQUIDACIÓN FINAL: " + txtDispositivo.getText();

                PreparedStatement psD = conn.prepareStatement("INSERT INTO detalle_venta (id_venta, id_producto, cantidad, subtotal, descripcion) VALUES (?, NULL, 1, ?, ?)");
                psD.setInt(1, idVenta); psD.setDouble(2, monto); psD.setString(3, desc.toUpperCase());
                psD.executeUpdate();

                // 3. Actualizar Estado si es Liquidación
                if ("LIQUIDACION".equals(concepto)) {
                    conn.prepareStatement("UPDATE ordenes_servicio SET estado='ENTREGADO' WHERE id=" + idOrdenActual).executeUpdate();
                    cmbEstado.setSelectedItem("ENTREGADO");
                }

                conn.commit();

                // 4. IMPRIMIR TICKET AUTOMÁTICO
                if (ImpresoraTicket.isAutoImprimir()) {
                    List<GeneradorTicket.ItemTicket> items = new ArrayList<>();
                    items.add(new GeneradorTicket.ItemTicket(desc.toUpperCase(), 1, monto));
                    String ticket = GeneradorTicket.crearTicket(idVenta, null, items, monto);
                    ImpresoraTicket.imprimir(ticket);
                }

                JOptionPanePro.mostrarMensaje(this, "Éxito", "Cobro registrado.", "INFO");
                cargarHistorialPagos(idOrdenActual); // Refrescar tabla pagos

            }
        } catch (Exception e) { e.printStackTrace(); }
    }



    // --- MÉTODOS AUXILIARES ---
    // (Iguales a la versión anterior: guardarOrden, generarPDF, enviarWhatsApp, cargarOrdenes, getters/setters visuales)
    // Asegúrate de copiar el método llenarParams actualizado para usar txtCostoPresupuesto
    // Y el método obtenerOInsertarCliente.

    // ... Pega aquí los métodos guardarOrden, llenarParams, generarPDF, etc. ...
    // NOTA: En llenarParams, usa parser(txtCostoPresupuesto.getText()) para el costo estimado.
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
                // INSERTAR
                String sql = "INSERT INTO ordenes_servicio (id_cliente, dispositivo, tipo_equipo, marca_modelo, password_patron, falla_reportada, diagnostico_tecnico, costo_estimado, anticipo, estado, niveles_tinta, rutas_imagenes) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                llenarParams(ps, idCliente, tintas, imgs);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) idOrdenActual = rs.getInt(1); // Actualizamos la variable global
            } else {
                // ACTUALIZAR
                String sql = "UPDATE ordenes_servicio SET id_cliente=?, dispositivo=?, tipo_equipo=?, marca_modelo=?, password_patron=?, falla_reportada=?, diagnostico_tecnico=?, costo_estimado=?, anticipo=?, estado=?, niveles_tinta=?, rutas_imagenes=? WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                llenarParams(ps, idCliente, tintas, imgs);
                ps.setInt(13, idOrdenActual);
                ps.executeUpdate();
            }

            lblFolio.setText("FOLIO: " + idOrdenActual);
            JOptionPanePro.mostrarMensaje(this, "Guardado", "Orden guardada correctamente.", "INFO");

            // CORRECCIÓN: Usamos idOrdenActual y lo ponemos aquí para que solo recargue si tuvo éxito
            cargarDatosOrden(idOrdenActual);
            cargarHistorialPagos(idOrdenActual);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error", e.getMessage(), "ERROR");
        }
    }

    private void llenarParams(PreparedStatement ps, int idC, String tintas, String imgs) throws SQLException {
        ps.setInt(1, idC); ps.setString(2, txtDispositivo.getText()); ps.setString(3, cmbTipoEquipo.getSelectedItem().toString());
        ps.setString(4, txtMarca.getText()); ps.setString(5, txtPass.getText()); ps.setString(6, txtFalla.getText());
        ps.setString(7, txtDiagnostico.getText());
        ps.setDouble(8, parser(txtCostoPresupuesto.getText())); // Costo Total
        ps.setDouble(9, 0); // Anticipo ya no se guarda directo aquí, se calcula por pagos, o se deja como referencia legacy
        ps.setString(10, cmbEstado.getSelectedItem().toString()); ps.setString(11, tintas); ps.setString(12, imgs);
    }

    // Resto de métodos auxiliares (input, area, addLbl, parser, etc) se mantienen igual.
    // ...
    private JTextField input() { JTextField t = new JTextField(10); Estilos.estilizarInput(t); return t; }
    private JTextArea area() {
        JTextArea t = new JTextArea(3, 10);
        t.setBackground(Estilos.COLOR_INPUT);
        t.setForeground(Color.WHITE);
        t.setBorder(BorderFactory.createLineBorder(Estilos.COLOR_BORDER));

        // --- NUEVO: Magia para que el texto salte de línea ---
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        // -----------------------------------------------------

        return t;
    }
    private void addLbl(JPanel p, GridBagConstraints g, int x, int y, String t) { g.gridx=x; g.gridy=y; JLabel l=new JLabel(t); l.setForeground(Color.WHITE); p.add(l,g); }
    private double parser(String s) { try{return Double.parseDouble(s);}catch(Exception e){return 0;} }
    private JSlider slider(Color c) { JSlider s = new JSlider(0,100,50); s.setBackground(c); return s; }
    private void alternarPanelTintas() { panelTintas.setVisible("IMPRESORA".equals(cmbTipoEquipo.getSelectedItem())); panelTintas.revalidate(); }
    private void adjuntarFoto() { JFileChooser fc = new JFileChooser(); fc.setFileFilter(new FileNameExtensionFilter("Imgs", "jpg","png")); if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) { rutasImagenes.add(fc.getSelectedFile().getAbsolutePath()); lblFotosCount.setText(rutasImagenes.size()+" imgs"); } }
    private int obtenerOInsertarCliente(Connection c) throws SQLException { String tel=txtTelefono.getText(); ResultSet rs=c.createStatement().executeQuery("SELECT id FROM clientes WHERE telefono='"+tel+"'"); if(rs.next()) return rs.getInt(1); PreparedStatement ps=c.prepareStatement("INSERT INTO clientes (nombre, telefono) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS); ps.setString(1, Recursos.capitalizarTexto(txtNombre.getText())); ps.setString(2, tel); ps.executeUpdate(); ResultSet rs2=ps.getGeneratedKeys(); rs2.next(); return rs2.getInt(1); }
    private void generarPDFFormulario() {
        if (idOrdenActual == -1) {
            JOptionPanePro.mostrarMensaje(this, "Error", "Guarda la orden.", "ERROR");
            return;
        }

        String tintas ="IMPRESORA".equals(cmbTipoEquipo.getSelectedItem()) ?
                slC.getValue()+","+slM.getValue()+","+slY.getValue()+","+slK.getValue() :
                null;

        GeneradorPDF.crearOrdenServicio(
                idOrdenActual,
                txtNombre.getText(),
                txtTelefono.getText(),
                txtDispositivo.getText() + " " + txtMarca.getText()
                , txtFalla.getText(),
                "Pass: "+txtPass.getText()+"\n"+txtDiagnostico.getText(),
                tintas,
                obtenerRutaPDF(idOrdenActual)); }
    // --- ACCIONES DESDE TABLA (Sin abrir formulario) ---
    private void generarPDFDesdeID(int id) {
        // Consultar datos mínimos para PDF
        try (Connection c = ConexionBD.conectar(); PreparedStatement ps = c.prepareStatement("SELECT o.*, cl.nombre, cl.telefono FROM ordenes_servicio o JOIN clientes cl ON o.id_cliente=cl.id WHERE o.id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String infoEq = rs.getString("dispositivo") + " " + rs.getString("marca_modelo");
                String notas = "Pass: " + rs.getString("password_patron") + "\n" + rs.getString("diagnostico_tecnico");
                GeneradorPDF.crearOrdenServicio(id, rs.getString("nombre"), rs.getString("telefono"), infoEq, rs.getString("falla_reportada"), notas, rs.getString("niveles_tinta"), obtenerRutaPDF(id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enviarWhatsAppDesdeID(int id) {
        // Usamos los nombres reales: 'dispositivo' y 'marca_modelo'
        String sql = "SELECT o.estado, o.costo_estimado, o.anticipo, o.dispositivo, o.marca_modelo, cl.telefono " +
                "FROM ordenes_servicio o JOIN clientes cl ON o.id_cliente=cl.id WHERE o.id=?";

        try (Connection c = ConexionBD.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String tel = rs.getString("telefono");
                String est = rs.getString("estado");

                // Extraemos los datos con los nombres correctos
                String dispositivo = rs.getString("dispositivo") != null ? rs.getString("dispositivo") : "";
                String marcaModelo = rs.getString("marca_modelo") != null ? rs.getString("marca_modelo") : "";
                String equipoDesc = (dispositivo + " " + marcaModelo).trim();

                StringBuilder msg = new StringBuilder();
                msg.append("Hola, excelente día.\n\n");

                if (equipoDesc.isEmpty()) {
                    msg.append("Le escribo de LUMTECH para darle una breve actualización sobre su equipo.\n\n");
                } else {
                    msg.append("Le escribo de LUMTECH para darle una breve actualización sobre su equipo (").append(equipoDesc).append(").\n\n");
                }

                // Lógica de mensajes según el estado
                if ("LISTO".equalsIgnoreCase(est)) {
                    double saldo = rs.getDouble("costo_estimado") - rs.getDouble("anticipo");
                    msg.append("¡Su equipo ya está *LISTO* para entrega!\n");
                    msg.append("El saldo pendiente a liquidar es de: *$").append(saldo).append("*\n\n");
                    msg.append("Cualquier duda, quedo a sus órdenes.");
                }
                else if ("EN ESPERA".equalsIgnoreCase(est) || "RETRASADO".equalsIgnoreCase(est)) {
                    // Tu mensaje directo y sin paja
                    msg.append("Seguimos en espera de la pieza, y nos notificaron que llegaría el día [JUEVES]. En cuanto nos llegue la pieza física, realizaremos el ensamble.\n\n");
                    msg.append("¿Me podría proporcionar su contraseña o PIN de inicio de sesión de Windows?\n\n");
                    msg.append("Quedo a la espera de su respuesta, ¡gracias!");
                }
                else {
                    msg.append("El estado actual de su orden #").append(id).append(" es: *").append(est).append("*.\n\n");
                    msg.append("En un momento le envío por este medio el documento PDF con los detalles.\n");
                    msg.append("¡Gracias por su preferencia!");
                }

                // Abrir WhatsApp
                String url = "https://wa.me/52" + tel + "?text=" + URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);
                Desktop.getDesktop().browse(new URI(url));

                // Abrir Carpeta de PDFs (Asegúrate de que esta ruta sea la correcta en tu Ubuntu)
                String rutaCarpetaPDFs = System.getProperty("user.home") + "/Documentos/Lumtech/Ordenes";
                File carpeta = new File(rutaCarpetaPDFs);

                if (carpeta.exists() && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(carpeta);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al enviar WhatsApp: " + e.getMessage());
        }
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
    private void limpiarFormulario() { txtNombre.setText(""); txtTelefono.setText(""); txtDispositivo.setText(""); txtMarca.setText(""); txtFalla.setText(""); txtDiagnostico.setText(""); txtCostoPresupuesto.setText("0.00"); rutasImagenes.clear(); lblFotosCount.setText("0 imgs"); modeloPagos.setRowCount(0); lblSaldoPendiente.setText("RESTANTE: $0.00"); }
    // Método helper para gestionar la carpeta y el nombre del archivo
    private String obtenerRutaPDF(int idOrden) {
        // Nombre de la carpeta
        File carpeta = new File("ordenes");

        // Si no existe, crearla
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        // Retorna la ruta completa: ordenes/Orden_50.pdf
        return new File(carpeta, "Orden_" + idOrden + ".pdf").getAbsolutePath();
    }
}
package ui.paneles;

import config.ConexionBD;
import servicios.GeneradorTicketDigital;
import util.Estilos;
import servicios.GeneradorTicket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import servicios.ImpresoraTicket;
import ui.componentes.BotonPro;
import ui.componentes.JOptionPanePro;
import ui.componentes.TablaPro;
import ui.componentes.ToastPro;
import java.awt.geom.Arc2D;
import java.io.File;

public class PanelFinanzas extends JPanel {

    // Estado interactivo (Reemplaza los JComboBox)
    private int anioSeleccionado;
    private int mesSeleccionado;

    private final JLabel lblVentas;
    private final JLabel lblGanancias;
    private final JLabel lblPeriodoActual;
    private final JTable tablaMaestra;
    private final JTable tablaDetalle;
    private final DefaultTableModel modeloMaestro;
    private final DefaultTableModel modeloDetalle;

    // Paneles para las gráficas
    private final PanelGraficaBarras panelGraficaBarras;
    private final PanelGraficaPastel panelPastel;

    public PanelFinanzas() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        // Inicializar estado al periodo actual
        Calendar cal = Calendar.getInstance();
        anioSeleccionado = cal.get(Calendar.YEAR);
        mesSeleccionado = cal.get(Calendar.MONTH) + 1; // 1 a 12

        // --- 1. FILTROS SUPERIORES (Botones de Acción) ---
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelFiltros.setBackground(Estilos.COLOR_PANEL);
        panelFiltros.setBorder(new EmptyBorder(10, 10, 10, 10));

        lblPeriodoActual = new JLabel();
        lblPeriodoActual.setForeground(Color.WHITE);
        lblPeriodoActual.setFont(Estilos.FONT_BOLD.deriveFont(18f));
        actualizarEtiquetaPeriodo();

        // Botones de Acción Mantenidos
        BotonPro btnTicket = new BotonPro("Imprimir Ticket", "ticket.png", new Color(255, 140, 0), this::reimprimirTicket);
        BotonPro btnDigital = new BotonPro("Ticket Digital", "imagen.png", new Color(0, 158, 227), this::generarTicketDigitalSeleccionado);
        BotonPro btnAnular = new BotonPro("Anular Venta", "eliminar.png", Color.RED, this::anularVentaSeleccionada);

        panelFiltros.add(lblPeriodoActual);
        panelFiltros.add(Box.createHorizontalStrut(30));
        panelFiltros.add(btnTicket);
        panelFiltros.add(btnDigital);
        panelFiltros.add(btnAnular);

        // --- 2. TABLAS MAESTRO-DETALLE ---
        modeloMaestro = new DefaultTableModel(new String[]{"ID", "Fecha", "Total", "Ganancia"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaMaestra = new TablaPro(modeloMaestro);
        Estilos.estilizarTabla(tablaMaestra);

        modeloDetalle = new DefaultTableModel(new String[]{"Producto", "Cant", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaDetalle = new TablaPro(modeloDetalle);
        Estilos.estilizarTabla(tablaDetalle);

        tablaMaestra.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int viewRow = tablaMaestra.getSelectedRow();
                if (viewRow != -1) {
                    // USO ESTRICTO DE convertRowIndexToModel PARA EVITAR DESINCRONIZACIONES
                    int modelRow = tablaMaestra.convertRowIndexToModel(viewRow);
                    verDetalle(Integer.parseInt(modeloMaestro.getValueAt(modelRow, 0).toString()));
                }
            }
        });

        JSplitPane splitTablas = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tablaMaestra), new JScrollPane(tablaDetalle));
        splitTablas.setResizeWeight(0.6);
        splitTablas.setBorder(null);
        splitTablas.setBackground(Estilos.COLOR_FONDO);

        // --- 3. PANEL INFERIOR (KPIs + GRÁFICAS) ---
        JPanel panelInferior = new JPanel(new GridLayout(1, 3, 10, 10)); // 3 Columnas para mejor distribución
        panelInferior.setBackground(Estilos.COLOR_FONDO);
        panelInferior.setPreferredSize(new Dimension(0, 320));

        // KPIs
        JPanel panelKPI = new JPanel(new GridLayout(2, 1));
        panelKPI.setBackground(Estilos.COLOR_PANEL);
        panelKPI.setBorder(new EmptyBorder(10, 10, 10, 10));

        lblVentas = new JLabel("Ventas Mes: $0.00", SwingConstants.CENTER);
        lblVentas.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblVentas.setForeground(Color.WHITE);

        lblGanancias = new JLabel("Ganancia Mes: $0.00", SwingConstants.CENTER);
        lblGanancias.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblGanancias.setForeground(new Color(46, 204, 113));

        panelKPI.add(lblVentas);
        panelKPI.add(lblGanancias);

        // Gráficas
        panelGraficaBarras = new PanelGraficaBarras();
        panelPastel = new PanelGraficaPastel();

        panelInferior.add(panelKPI);
        panelInferior.add(panelGraficaBarras);
        panelInferior.add(panelPastel);

        add(panelFiltros, BorderLayout.NORTH);
        add(splitTablas, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        limpiarDatos();
    }

    private void actualizarEtiquetaPeriodo() {
        String[] nombres = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        lblPeriodoActual.setText("Reporte Actual: " + nombres[mesSeleccionado - 1] + " " + anioSeleccionado);
    }

    private void generarTicketDigitalSeleccionado() {
        int viewRow = tablaMaestra.getSelectedRow();
        if (viewRow == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Selecciona una venta de la lista.", "ADVERTENCIA");
            return;
        }

        int modelRow = tablaMaestra.convertRowIndexToModel(viewRow);
        String folio = modeloMaestro.getValueAt(modelRow, 0).toString();
        double total = Double.parseDouble(modeloMaestro.getValueAt(modelRow, 2).toString());

        String clienteNombre = "Cliente General";
        String conceptoPrincipal = "Varios Productos";

        try (Connection conn = ConexionBD.conectar()) {
            String sqlCliente = "SELECT c.nombre FROM ventas v " +
                    "JOIN ordenes_servicio os ON v.id_orden_servicio = os.id " +
                    "JOIN clientes c ON os.id_cliente = c.id " +
                    "WHERE v.id = ?";
            try (PreparedStatement psC = conn.prepareStatement(sqlCliente)) {
                psC.setInt(1, Integer.parseInt(folio));
                try (ResultSet rsC = psC.executeQuery()) {
                    if (rsC.next()) clienteNombre = rsC.getString("nombre");
                }
            }

            String sqlConcepto = "SELECT COALESCE(d.descripcion, p.nombre) as descr " +
                    "FROM detalle_venta d " +
                    "LEFT JOIN productos p ON d.id_producto = p.id " +
                    "WHERE d.id_venta = ? LIMIT 1";
            try (PreparedStatement psD = conn.prepareStatement(sqlConcepto)) {
                psD.setInt(1, Integer.parseInt(folio));
                try (ResultSet rsD = psD.executeQuery()) {
                    if (rsD.next()) conceptoPrincipal = rsD.getString("descr");
                }
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelFinanzas.generarTicketDigitalSeleccionado: " + e.getMessage());
            e.printStackTrace();
        }

        GeneradorTicketDigital.generarComprobanteUniversal(folio, clienteNombre, conceptoPrincipal, "", total, true);
    }

    public void consultar() {
        modeloMaestro.setRowCount(0);
        modeloDetalle.setRowCount(0);
        actualizarEtiquetaPeriodo();

        double v = 0;
        double g = 0;

        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT * FROM ventas WHERE MONTH(fecha) = ? AND YEAR(fecha) = ? ORDER BY id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, mesSeleccionado);
                ps.setInt(2, anioSeleccionado);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modeloMaestro.addRow(new Object[]{
                                rs.getInt("id"),
                                rs.getTimestamp("fecha"),
                                rs.getDouble("total_venta"),
                                rs.getDouble("ganancia_total")
                        });
                        v += rs.getDouble("total_venta");
                        g += rs.getDouble("ganancia_total");
                    }
                }
            }

            lblVentas.setText("Ventas Mes: $" + String.format("%.2f", v));
            lblGanancias.setText("Ganancia Mes: $" + String.format("%.2f", g));

            cargarDatosBarras(anioSeleccionado);
            cargarDatosPastel(mesSeleccionado, anioSeleccionado);

        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelFinanzas.consultar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarDatosBarras(int anio) {
        double[] ventasPorMes = new double[12];
        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT MONTH(fecha) as mes, SUM(total_venta) as total FROM ventas WHERE YEAR(fecha) = ? GROUP BY MONTH(fecha)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, anio);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int mesIndex = rs.getInt("mes") - 1;
                        if (mesIndex >= 0 && mesIndex < 12) {
                            ventasPorMes[mesIndex] = rs.getDouble("total");
                        }
                    }
                }
            }
            panelGraficaBarras.setDatos(ventasPorMes);
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelFinanzas.cargarDatosBarras: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarDatosPastel(int mes, int anio) {
        double totalProductos = 0;
        double totalServicios = 0;

        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT tipo_venta, SUM(total_venta) as total FROM ventas WHERE MONTH(fecha) = ? AND YEAR(fecha) = ? GROUP BY tipo_venta";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, mes);
                ps.setInt(2, anio);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String tipo = rs.getString("tipo_venta");
                        double monto = rs.getDouble("total");

                        if ("SERVICIO".equalsIgnoreCase(tipo)) totalServicios = monto;
                        else totalProductos += monto;
                    }
                }
            }
            panelPastel.setDatos(totalProductos, totalServicios);
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelFinanzas.cargarDatosPastel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void verDetalle(int idVenta) {
        modeloDetalle.setRowCount(0);
        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT COALESCE(d.descripcion, p.nombre) as concepto, d.cantidad, d.subtotal " +
                    "FROM detalle_venta d " +
                    "LEFT JOIN productos p ON d.id_producto = p.id " +
                    "WHERE d.id_venta = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String desc = rs.getString("concepto");
                        if (desc == null) desc = "PRODUCTO ELIMINADO / SERVICIO";
                        modeloDetalle.addRow(new Object[]{desc, rs.getInt("cantidad"), rs.getDouble("subtotal")});
                    }
                }
            }
        } catch (Exception e) { 
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelFinanzas.verDetalle: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    private void reimprimirTicket() {
        int viewRow = tablaMaestra.getSelectedRow();
        if (viewRow == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Selecciona una venta primero.", "ADVERTENCIA");
            return;
        }

        int modelRow = tablaMaestra.convertRowIndexToModel(viewRow);
        int idVenta = Integer.parseInt(modeloMaestro.getValueAt(modelRow, 0).toString());
        String fecha = modeloMaestro.getValueAt(modelRow, 1).toString();
        double total = Double.parseDouble(modeloMaestro.getValueAt(modelRow, 2).toString());

        List<GeneradorTicket.ItemTicket> items = new ArrayList<>();

        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT COALESCE(d.descripcion, p.nombre) as concepto, d.cantidad, d.subtotal " +
                    "FROM detalle_venta d " +
                    "LEFT JOIN productos p ON d.id_producto = p.id " +
                    "WHERE d.id_venta = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String desc = rs.getString("concepto");
                        if (desc == null) desc = "SERVICIO / PROD";
                        items.add(new GeneradorTicket.ItemTicket(desc, rs.getInt("cantidad"), rs.getDouble("subtotal")));
                    }
                }
            }
        } catch (Exception e) { 
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelFinanzas.reimprimirTicket: " + e.getMessage());
            e.printStackTrace(); 
        }

        String t = GeneradorTicket.crearTicket(idVenta, fecha, items, total);
        ImpresoraTicket.imprimir(t);
        JOptionPanePro.mostrarMensaje(this, "Impresión", "Ticket reimpreso en consola.", "INFO");
    }

    private void anularVentaSeleccionada() {
        int viewRow = tablaMaestra.getSelectedRow();
        if (viewRow == -1) {
            JOptionPanePro.mostrarMensaje(this, "Selección", "Selecciona una venta para anular.", "ADVERTENCIA");
            return;
        }

        int modelRow = tablaMaestra.convertRowIndexToModel(viewRow);
        int idVenta = Integer.parseInt(modeloMaestro.getValueAt(modelRow, 0).toString());
        double total = Double.parseDouble(modeloMaestro.getValueAt(modelRow, 2).toString());

        if (total == 0) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Esta venta ya parece estar anulada.", "INFO");
            return;
        }

        if (JOptionPanePro.mostrarConfirmacion(this, "PELIGRO", "¿Estás seguro de ANULAR la venta #" + idVenta + "?\nSe devolverá el stock al inventario.")) {
            try (Connection conn = ConexionBD.conectar()) {
                conn.setAutoCommit(false);

                String sqlDetalles = "SELECT id_producto, cantidad FROM detalle_venta WHERE id_venta = ?";
                try (PreparedStatement psDet = conn.prepareStatement(sqlDetalles)) {
                    psDet.setInt(1, idVenta);
                    try (ResultSet rs = psDet.executeQuery()) {
                        String sqlUpdateStock = "UPDATE productos SET stock = stock + ? WHERE id = ?";
                        try (PreparedStatement psStock = conn.prepareStatement(sqlUpdateStock)) {
                            while (rs.next()) {
                                int idProd = rs.getInt("id_producto");
                                int cant = rs.getInt("cantidad");
                                if (idProd > 0) {
                                    psStock.setInt(1, cant);
                                    psStock.setInt(2, idProd);
                                    psStock.executeUpdate();
                                }
                            }
                        }
                    }
                }

                String sqlAnular = "UPDATE ventas SET total_venta = 0, ganancia_total = 0, tipo_venta = CONCAT(tipo_venta, ' (CANCEL)') WHERE id = ?";
                try (PreparedStatement psAnular = conn.prepareStatement(sqlAnular)) {
                    psAnular.setInt(1, idVenta);
                    psAnular.executeUpdate();
                }

                conn.commit();
                JOptionPanePro.mostrarMensaje(this, "Éxito", "Venta anulada y stock restaurado.", "INFO");
                consultar();

            } catch (Exception e) {
                servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelFinanzas.anularVentaSeleccionada: " + e.getMessage());
                e.printStackTrace();
                JOptionPanePro.mostrarMensaje(this, "Error", "No se pudo anular: " + e.getMessage(), "ERROR");
            }
        }
    }

    public void limpiarDatos() {
        modeloMaestro.setRowCount(0);
        modeloDetalle.setRowCount(0);
        lblVentas.setText("Ventas Mes: $0.00");
        lblGanancias.setText("Ganancia Mes: $0.00");
        panelGraficaBarras.setDatos(new double[12]);
        panelPastel.setDatos(0, 0);
    }

    // =================================================================================
    // CLASES INTERNAS: COMPONENTES GRÁFICOS (DRILL-DOWN Y RENDERIZADO)
    // =================================================================================

    private class PanelGraficaBarras extends JPanel {
        private final LienzoBarras lienzo;
        private final JLabel lblAnio;

        public PanelGraficaBarras() {
            setLayout(new BorderLayout());
            setBackground(Estilos.COLOR_PANEL);
            setBorder(BorderFactory.createTitledBorder(null, "Análisis Anual", 0, 0, Estilos.FONT_BOLD, Color.WHITE));

            // Carrusel Minimalista (Navegación de Años)
            JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
            navPanel.setOpaque(false);

            BotonPro btnPrev = new BotonPro("<", Estilos.COLOR_ACCENT, () -> cambiarAnio(-1));
            btnPrev.setPreferredSize(new Dimension(40, 30));

            lblAnio = new JLabel(String.valueOf(anioSeleccionado));
            lblAnio.setForeground(Color.WHITE);
            lblAnio.setFont(Estilos.FONT_BOLD.deriveFont(16f));

            BotonPro btnNext = new BotonPro(">", Estilos.COLOR_ACCENT, () -> cambiarAnio(1));
            btnNext.setPreferredSize(new Dimension(40, 30));

            navPanel.add(btnPrev);
            navPanel.add(lblAnio);
            navPanel.add(btnNext);

            lienzo = new LienzoBarras();
            add(navPanel, BorderLayout.NORTH);
            add(lienzo, BorderLayout.CENTER);
        }

        public void setDatos(double[] nuevosValores) {
            lienzo.valores = nuevosValores;
            lienzo.repaint();
        }

        private void cambiarAnio(int incremento) {
            anioSeleccionado += incremento;
            lblAnio.setText(String.valueOf(anioSeleccionado));
            consultar(); // Refresca todo el dashboard con el nuevo año
        }
    }

    private class LienzoBarras extends JPanel {
        private final String[] meses = {"E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};
        private double[] valores = new double[12];
        private final Rectangle[] hitboxes = new Rectangle[12];
        private int hoverIndex = -1;

        public LienzoBarras() {
            setOpaque(false);

            // Lógica Interactiva (Drill-down)
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (int i = 0; i < 12; i++) {
                        if (hitboxes[i] != null && hitboxes[i].contains(e.getPoint())) {
                            mesSeleccionado = i + 1;
                            consultar(); // Dispara la actualización de tablas y pastel
                            repaint();
                            break;
                        }
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int oldHover = hoverIndex;
                    hoverIndex = -1;
                    for (int i = 0; i < 12; i++) {
                        if (hitboxes[i] != null && hitboxes[i].contains(e.getPoint())) {
                            hoverIndex = i;
                            break;
                        }
                    }
                    if (oldHover != hoverIndex) {
                        setCursor(hoverIndex != -1 ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int paddingX = 30;
            int paddingY = 30;
            int barWidth = (w - (paddingX * 2)) / 12 - 8;

            double maxVal = 0;
            for (double v : valores) maxVal = Math.max(maxVal, v);
            if (maxVal == 0) maxVal = 1;

            for (int i = 0; i < 12; i++) {
                int x = paddingX + (i * (barWidth + 8));
                int barHeight = (int) ((valores[i] / maxVal) * (h - paddingY * 2));
                int y = h - paddingY - barHeight;

                // Hitbox para detectar clics (toda la columna verticalmente)
                hitboxes[i] = new Rectangle(x, y, barWidth, Math.max(barHeight, 10));

                // Determinar colores por estado (Seleccionado, Hover, Normal)
                boolean isSelected = (mesSeleccionado == (i + 1));
                Color barColor = isSelected ? new Color(255, 165, 0) : Estilos.COLOR_ACCENT; // Naranja si está seleccionado

                if (hoverIndex == i && !isSelected) {
                    barColor = barColor.brighter();
                }

                // Dibujar Barra
                g2.setColor(barColor);
                g2.fillRoundRect(x, y, barWidth, barHeight, 5, 5);

                // Etiqueta Mes
                g2.setColor(isSelected ? new Color(255, 165, 0) : Color.WHITE);
                g2.setFont(isSelected ? Estilos.FONT_BOLD : new Font("Segoe UI", Font.PLAIN, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(meses[i]);
                g2.drawString(meses[i], x + (barWidth - textW) / 2, h - paddingY + 18);

                // Etiqueta Valor
                if (valores[i] > 0) {
                    String valStr = (valores[i] >= 1000) ? (int) (valores[i] / 1000) + "k" : String.valueOf((int)valores[i]);
                    int valW = fm.stringWidth(valStr);
                    g2.setColor(isSelected ? Color.WHITE : Color.YELLOW);
                    g2.drawString(valStr, x + (barWidth - valW) / 2, y - 5);
                }
            }

            // Línea base
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(paddingX, h - paddingY, w - paddingX, h - paddingY);
        }
    }

    private class PanelGraficaPastel extends JPanel {
        private double valProd = 0;
        private double valServ = 0;

        // Objeto geométrico dinámico que guardará la forma exacta de la rebanada azul
        private Shape hitboxProductos;

        public PanelGraficaPastel() {
            setBackground(Estilos.COLOR_PANEL);
            setBorder(BorderFactory.createTitledBorder(null, "Distribución de Ingresos", 0,0, Estilos.FONT_BOLD, Color.WHITE));

            // Lógica Interactiva: Clic sobre la rebanada de Productos
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hitboxProductos != null && hitboxProductos.contains(e.getPoint())) {
                        lanzarGeneracionPDF();
                    }
                }
            });

            // Lógica Interactiva: Cambiar cursor al pasar sobre Productos
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (hitboxProductos != null && hitboxProductos.contains(e.getPoint())) {
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });
        }

        public void setDatos(double productos, double servicios) {
            this.valProd = productos;
            this.valServ = servicios;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double total = valProd + valServ;
            if (total == 0) return;

            int w = getWidth();
            int h = getHeight();
            int diametro = Math.min(w, h) - 70;
            int x = (w - diametro) / 2 + 30;
            int y = (h - diametro) / 2;

            int anguloProd = (int) ((valProd / total) * 360);
            int anguloServ = 360 - anguloProd;

            // 1. Crear el HITBOX dinámico (Arc2D.PIE une los extremos al centro para detectar colisiones perfectas)
            hitboxProductos = new Arc2D.Double(x, y, diametro, diametro, 90, anguloProd, Arc2D.PIE);

            // Sector Productos (Azul) - Se dibuja usando el objeto geométrico que acabamos de crear
            g2.setColor(Estilos.COLOR_ACCENT);
            g2.fill(hitboxProductos);

            // Sector Servicios (Naranja)
            g2.setColor(new Color(255, 140, 0));
            g2.fillArc(x, y, diametro, diametro, 90 + anguloProd, anguloServ);

            // Leyenda
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            g2.setColor(Estilos.COLOR_ACCENT);
            g2.fillRect(10, 30, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawString("Prod: $" + String.format("%.2f", valProd), 30, 41);

            g2.setColor(new Color(255, 140, 0));
            g2.fillRect(10, 55, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawString("Serv: $" + String.format("%.2f", valServ), 30, 66);
        }

        private void lanzarGeneracionPDF() {
            try {
                // 1. Validar y crear la carpeta 'reportes' en la raíz si no existe
                java.io.File directorio = new java.io.File("reportes");
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                // 2. Armar la ruta del archivo con el nombre dinámico usando las variables de la clase
                String nombreArchivo = "Rotacion_Inventario_" + anioSeleccionado + "_" + mesSeleccionado + ".pdf";
                String rutaFinal = new java.io.File(directorio, nombreArchivo).getAbsolutePath();

                // 3. Invocar al motor de PDF PASÁNDOLE LAS VARIABLES DEL PANEL
                servicios.GeneradorPDF.generarReporteRotacion(rutaFinal, mesSeleccionado, anioSeleccionado);

                // 4. Notificar al usuario discretamente usando Toast
                ToastPro.show("Reporte generado", "EXITO");

                // 5. Abrir el archivo automáticamente
                java.awt.Desktop.getDesktop().open(new java.io.File(rutaFinal));

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPanePro.mostrarMensaje(PanelFinanzas.this, "Error", "No se pudo generar el reporte: " + ex.getMessage(), "ERROR");
            }
        }
    }
}
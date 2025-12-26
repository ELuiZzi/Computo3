package Paneles;

import Conexión.ConexionBD;
import Utils.Estilos;
import Utils.GeneradorTicket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import ElementosPro.*;
import Utils.ImpresoraTicket;

public class PanelFinanzas extends JPanel {
    private final JComboBox<String> cmbMes;
    private final JComboBox<String> cmbAnio;
    private final JLabel lblVentas;
    private final JLabel lblGanancias;
    private final JTable tablaMaestra;
    private final JTable tablaDetalle;
    private final DefaultTableModel modeloMaestro;
    private final DefaultTableModel modeloDetalle;

    // Panel para la gráfica
    private final PanelGraficaBarras panelGraficaBarras;
    private final PanelGraficaPastel panelPastel;

    public PanelFinanzas() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        // --- 1. FILTROS SUPERIORES ---
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltros.setBackground(Estilos.COLOR_PANEL);
        panelFiltros.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblF = new JLabel("Periodo:");
        lblF.setForeground(Color.WHITE);
        lblF.setFont(Estilos.FONT_BOLD);

        cmbMes = new JComboBox<>(new String[]{"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"});
        cmbAnio = new JComboBox<>();
        int y = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = y; i >= y - 5; i--) cmbAnio.addItem(String.valueOf(i));
        cmbMes.setSelectedIndex(Calendar.getInstance().get(Calendar.MONTH));

        // Botón Consultar
        BotonPro btnVer = new BotonPro("Ver Reporte", Estilos.COLOR_ACCENT, this::consultar);

        // Botón Reimprimir Ticket
        BotonPro btnTicket = new BotonPro("Imprimir Ticket","impresora.png", new Color(255, 140, 0), this::reimprimirTicket);
        BotonPro btnAnular = new BotonPro("Anular Venta", "eliminar.png", Color.RED, this::anularVentaSeleccionada);

        panelFiltros.add(lblF);
        panelFiltros.add(cmbMes);
        panelFiltros.add(cmbAnio);
        panelFiltros.add(Box.createHorizontalStrut(20));
        panelFiltros.add(btnVer);
        panelFiltros.add(btnTicket);
        panelFiltros.add(btnAnular);

        // --- 2. TABLAS MAESTRO-DETALLE ---
        modeloMaestro = new DefaultTableModel(new String[]{"ID", "Fecha", "Total", "Ganancia"}, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaMaestra = new TablaPro(modeloMaestro);
        Estilos.estilizarTabla(tablaMaestra);

        modeloDetalle = new DefaultTableModel(new String[]{"Producto", "Cant", "Subtotal"}, 0);
        tablaDetalle = new TablaPro(modeloDetalle);
        Estilos.estilizarTabla(tablaDetalle);

        tablaMaestra.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablaMaestra.getSelectedRow() != -1) {
                verDetalle(Integer.parseInt(tablaMaestra.getValueAt(tablaMaestra.getSelectedRow(), 0).toString()));
            }
        });

        // Split Tables
        JSplitPane splitTablas = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tablaMaestra), new JScrollPane(tablaDetalle));
        splitTablas.setResizeWeight(0.6); // 60% para la tabla maestra
        splitTablas.setBorder(null);
        splitTablas.setBackground(Estilos.COLOR_FONDO);

        // --- 3. PANEL INFERIOR (KPIs + GRÁFICA) ---
        JPanel panelInferior = new JPanel(new GridLayout(1, 2, 10, 10));
        panelInferior.setBackground(Estilos.COLOR_FONDO);
        panelInferior.setPreferredSize(new Dimension(0, 300)); // Altura fija para gráfica

        // KPIs
        JPanel panelKPI = new JPanel(new GridLayout(2, 1));
        panelKPI.setBackground(Estilos.COLOR_PANEL);
        panelKPI.setBorder(new EmptyBorder(10, 10, 10, 10));

        lblVentas = new JLabel("Ventas Mes: $0.00", SwingConstants.CENTER);
        lblVentas.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblVentas.setForeground(Color.WHITE);

        lblGanancias = new JLabel("Ganancia Mes: $0.00", SwingConstants.CENTER);
        lblGanancias.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblGanancias.setForeground(new Color(46, 204, 113)); // Verde Matrix

        panelKPI.add(lblVentas);
        panelKPI.add(lblGanancias);

        // Gráfica
        panelGraficaBarras = new PanelGraficaBarras();
        panelPastel = new PanelGraficaPastel();

        panelInferior.add(panelKPI, BorderLayout.NORTH);
        panelInferior.add(panelGraficaBarras);
        panelInferior.add(panelPastel);

        // Unir todo
        add(panelFiltros, BorderLayout.NORTH);
        add(splitTablas, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);
    }

    public void consultar() {
        modeloMaestro.setRowCount(0);
        modeloDetalle.setRowCount(0);
        double v = 0, g = 0;
        int m = cmbMes.getSelectedIndex() + 1;
        int a = Integer.parseInt(cmbAnio.getSelectedItem().toString());

        try (Connection conn = ConexionBD.conectar()) {
            // 1. Cargar Tabla
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM ventas WHERE MONTH(fecha)=? AND YEAR(fecha)=?");
            ps.setInt(1, m);
            ps.setInt(2, a);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modeloMaestro.addRow(new Object[]{rs.getInt("id"), rs.getTimestamp("fecha"), rs.getDouble("total_venta"), rs.getDouble("ganancia_total")});
                v += rs.getDouble("total_venta");
                g += rs.getDouble("ganancia_total");
            }
            lblVentas.setText("Ventas Mes: $" + String.format("%.2f", v));
            lblGanancias.setText("Ganancia Mes: $" + String.format("%.2f", g));

            // 2. Cargar Gráfica Anual
            cargarDatosBarras(a);
            cargarDatosPastel(m, a);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarDatosBarras(int anio) {
        double[] ventasPorMes = new double[12];
        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT MONTH(fecha) as mes, SUM(total_venta) as total FROM ventas WHERE YEAR(fecha) = ? GROUP BY MONTH(fecha)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, anio);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int mesIndex = rs.getInt("mes") - 1; // BD 1-12, Array 0-11
                if (mesIndex >= 0 && mesIndex < 12) {
                    ventasPorMes[mesIndex] = rs.getDouble("total");
                }
            }
            panelGraficaBarras.setDatos(ventasPorMes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarDatosPastel(int mes, int anio) {
        double totalProductos = 0;
        double totalServicios = 0;

        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT tipo_venta, SUM(total_venta) as total FROM ventas WHERE MONTH(fecha) = ? AND YEAR(fecha) = ? GROUP BY tipo_venta";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, mes);
            ps.setInt(2, anio);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                String tipo = rs.getString("tipo_venta");
                double monto = rs.getDouble("total");

                if ("SERVICIO".equalsIgnoreCase(tipo)) totalServicios = monto;
                else totalProductos += monto; // Asumimos PRODUCTO por defecto
            }

            panelPastel.setDatos(totalProductos, totalServicios);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private class PanelGraficaBarras extends JPanel {
        private final String[] meses = {"E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};
        private double[] valores = new double[12];

        public PanelGraficaBarras() {
            setBackground(Estilos.COLOR_PANEL);
            setBorder(BorderFactory.createTitledBorder(null, "Ventas Anuales", 0, 0, Estilos.FONT_BOLD, Color.WHITE));
        }

        public void setDatos(double[] nuevosValores) {
            this.valores = nuevosValores;
            repaint(); // Volver a pintar
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padding = 40;
            int barWidth = (w - (padding * 2)) / 12 - 10;

            // Buscar valor máximo para escalar
            double maxVal = 0;
            for (double v : valores) maxVal = Math.max(maxVal, v);
            if (maxVal == 0) maxVal = 1; // Evitar div por 0

            // Dibujar Barras
            for (int i = 0; i < 12; i++) {
                int x = padding + (i * (barWidth + 10));
                int barHeight = (int) ((valores[i] / maxVal) * (h - padding * 2));
                int y = h - padding - barHeight;

                // Barra
                g2.setColor(Estilos.COLOR_ACCENT);
                g2.fillRect(x, y, barWidth, barHeight);

                // Borde Barra
                g2.setColor(Color.WHITE);
                g2.drawRect(x, y, barWidth, barHeight);

                // Etiqueta Mes
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(meses[i]);
                g2.drawString(meses[i], x + (barWidth - textW) / 2, h - padding + 15);

                // Etiqueta Valor (Si hay espacio)
                if (valores[i] > 0) {
                    String valStr = (int) valores[i] / 1000 + "k"; // Formato corto 10k
                    int valW = fm.stringWidth(valStr);
                    g2.setColor(Color.YELLOW);
                    g2.drawString(valStr, x + (barWidth - valW) / 2, y - 5);
                }
            }

            // Línea base
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(padding, h - padding, w - padding, h - padding);
        }
    }

    private void verDetalle(int idVenta) {
        modeloDetalle.setRowCount(0);
        try (Connection conn = ConexionBD.conectar()) {
            // LÓGICA SQL:
            // Usamos COALESCE: Si 'd.descripcion' tiene texto (Servicio), usa ese.
            // Si es NULL (Venta vieja), busca 'p.nombre' usando el JOIN.
            String sql = "SELECT COALESCE(d.descripcion, p.nombre) as concepto, d.cantidad, d.subtotal " +
                    "FROM detalle_venta d " +
                    "LEFT JOIN productos p ON d.id_producto = p.id " +
                    "WHERE d.id_venta = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Si por alguna razón ambos son null (producto borrado), ponemos "Item Desconocido"
                String desc = rs.getString("concepto");
                if (desc == null) desc = "PRODUCTO ELIMINADO / SERVICIO";

                modeloDetalle.addRow(new Object[]{
                        desc,
                        rs.getInt("cantidad"),
                        rs.getDouble("subtotal")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void reimprimirTicket() {
        int row = tablaMaestra.getSelectedRow();
        if (row == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Selecciona una venta primero.", "ADVERTENCIA");
            return;
        }

        int idVenta = Integer.parseInt(modeloMaestro.getValueAt(row, 0).toString());
        String fecha = modeloMaestro.getValueAt(row, 1).toString();
        double total = Double.parseDouble(modeloMaestro.getValueAt(row, 2).toString());

        List<GeneradorTicket.ItemTicket> items = new ArrayList<>();

        try (Connection conn = ConexionBD.conectar()) {
            // Misma lógica SQL con COALESCE para soportar Servicios y Productos
            String sql = "SELECT COALESCE(d.descripcion, p.nombre) as concepto, d.cantidad, d.subtotal " +
                    "FROM detalle_venta d " +
                    "LEFT JOIN productos p ON d.id_producto = p.id " +
                    "WHERE d.id_venta = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String desc = rs.getString("concepto");
                if (desc == null) desc = "SERVICIO / PROD";

                items.add(new GeneradorTicket.ItemTicket(desc, rs.getInt("cantidad"), rs.getDouble("subtotal")));
            }
        } catch (Exception e) { e.printStackTrace(); }

        String t = GeneradorTicket.crearTicket(idVenta, fecha, items, total);
        ImpresoraTicket.imprimir(t);
        JOptionPanePro.mostrarMensaje(this, "Impresión", "Ticket reimpreso en consola.", "INFO");
    }

    private class PanelGraficaPastel extends JPanel {
        private double valProd = 0;
        private double valServ = 0;

        public PanelGraficaPastel() {
            setBackground(Estilos.COLOR_PANEL);
            setBorder(BorderFactory.createTitledBorder(null, "Ingresos: Prod vs Serv", 0,0, Estilos.FONT_BOLD, Color.WHITE));
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
            int diametro = Math.min(w, h) - 60;
            int x = (w - diametro) / 2;
            int y = (h - diametro) / 2 + 10;

            // Ángulos
            int anguloProd = (int) ((valProd / total) * 360);
            int anguloServ = 360 - anguloProd; // El resto

            // Dibujar Sector Productos (Azul)
            g2.setColor(new Color(41, 98, 255));
            g2.fillArc(x, y, diametro, diametro, 90, anguloProd);

            // Dibujar Sector Servicios (Naranja)
            g2.setColor(new Color(255, 140, 0));
            g2.fillArc(x, y, diametro, diametro, 90 + anguloProd, anguloServ);

            // Leyenda (Texto)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            // Texto Prod
            g2.setColor(new Color(41, 98, 255));
            g2.fillRect(10, 20, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString("Prod: $" + (int)valProd, 25, 30);

            // Texto Serv
            g2.setColor(new Color(255, 140, 0));
            g2.fillRect(10, 40, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString("Serv: $" + (int)valServ, 25, 50);
        }
    }

    private void anularVentaSeleccionada() {
        int row = tablaMaestra.getSelectedRow();
        if (row == -1) {
            JOptionPanePro.mostrarMensaje(this, "Selección", "Selecciona una venta para anular.", "ADVERTENCIA");
            return;
        }

        int idVenta = Integer.parseInt(modeloMaestro.getValueAt(row, 0).toString());
        String estadoActual = "";

        // Verificar si ya está cancelada (Necesitas traer esa columna o verificar montos)
        double total = Double.parseDouble(modeloMaestro.getValueAt(row, 2).toString());
        if (total == 0) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Esta venta ya parece estar anulada.", "INFO");
            return;
        }

        if (JOptionPanePro.mostrarConfirmacion(this, "PELIGRO", "¿Estás seguro de ANULAR la venta #" + idVenta + "?\nSe devolverá el stock al inventario.")) {
            try (Connection conn = ConexionBD.conectar()) {
                conn.setAutoCommit(false);

                // 1. RECUPERAR PRODUCTOS Y DEVOLVER STOCK
                String sqlDetalles = "SELECT id_producto, cantidad FROM detalle_venta WHERE id_venta = ?";
                PreparedStatement psDet = conn.prepareStatement(sqlDetalles);
                psDet.setInt(1, idVenta);
                ResultSet rs = psDet.executeQuery();

                String sqlUpdateStock = "UPDATE productos SET stock = stock + ? WHERE id = ?";
                PreparedStatement psStock = conn.prepareStatement(sqlUpdateStock);

                while (rs.next()) {
                    int idProd = rs.getInt("id_producto");
                    int cant = rs.getInt("cantidad");

                    // Si idProd es 0 o NULL (es un servicio), no hacemos nada con el stock
                    if (idProd > 0) {
                        psStock.setInt(1, cant);
                        psStock.setInt(2, idProd);
                        psStock.executeUpdate();
                    }
                }

                // 2. MARCAR VENTA COMO CANCELADA (Montos a 0 y etiqueta)
                // Usamos tipo_venta para marcarlo visualmente si deseas, o simplemente montos a 0
                String sqlAnular = "UPDATE ventas SET total_venta = 0, ganancia_total = 0, tipo_venta = CONCAT(tipo_venta, ' (CANCEL)') WHERE id = ?";
                PreparedStatement psAnular = conn.prepareStatement(sqlAnular);
                psAnular.setInt(1, idVenta);
                psAnular.executeUpdate();

                conn.commit();
                JOptionPanePro.mostrarMensaje(this, "Éxito", "Venta anulada y stock restaurado.", "INFO");
                consultar(); // Refrescar tabla

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPanePro.mostrarMensaje(this, "Error", "No se pudo anular: " + e.getMessage(), "ERROR");
            }
        }
    }
}

package ui.paneles;

import config.ConexionBD;
import ui.componentes.ToastPro;
import util.Estilos;
import servicios.GeneradorTicket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import servicios.ImpresoraTicket;
import ui.componentes.BotonPro;
import ui.componentes.JOptionPanePro;
import ui.componentes.TablaPro;

public class PanelVentas extends JPanel {
    private final JTextField txtCodigo;
    private final JPanel panelCatalogo;
    private final DefaultTableModel modeloCarrito;
    private final TablaPro tablaCarrito;
    private final JLabel lblTotal;
    private double totalVenta = 0;
    private double gananciaVenta = 0;
    private final BotonPro btnPagar;

    private int paginaActual = 1;
    private final int LIMITE_PAGINA = 20; // Cambia este número según qué tan grande quieras el catálogo
    private BotonPro btnPaginaAnt;
    private BotonPro btnPaginaSig;
    private JLabel lblPaginaActual;
    private String filtroBusqueda = "";

    public PanelVentas() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);
        
        // Habilitar arrastre táctil en toda la aplicación
        Estilos.habilitarScrollTactilGlobal();

        // --- IZQUIERDA: CATÁLOGO (70%) ---
        JPanel panelIzq = new JPanel(new BorderLayout());
        panelIzq.setBackground(Estilos.COLOR_FONDO);
        panelIzq.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Buscador Estilizado
        JPanel panelBus = new JPanel(new BorderLayout(10, 0));
        panelBus.setBackground(Estilos.COLOR_FONDO);
        panelBus.setBorder(new EmptyBorder(0, 0, 15, 0)); // Espacio abajo

        txtCodigo = new JTextField();
        Estilos.estilizarInput(txtCodigo);
        txtCodigo.putClientProperty("JTextField.placeholderText", " Escanear código o buscar nombre...");

        txtCodigo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarFiltro(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarFiltro(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarFiltro(); }
            private void actualizarFiltro() {
                String texto = txtCodigo.getText().trim();
                if (!filtroBusqueda.equals(texto)) {
                    filtroBusqueda = texto;
                    paginaActual = 1;
                    cargarCatalogo();
                }
            }
        });

        BotonPro btnBuscar = new BotonPro("Buscar", Estilos.COLOR_PANEL, () -> buscarProducto(txtCodigo.getText()));

        btnBuscar.setPreferredSize(new Dimension(100, 40));

        txtCodigo.addActionListener(e -> buscarProducto(txtCodigo.getText()));


        panelBus.add(txtCodigo, BorderLayout.CENTER);
        panelBus.add(btnBuscar, BorderLayout.EAST);

        // Grid Productos (Fondo oscuro)
        panelCatalogo = new JPanel(new GridLayout(0, 2, 15, 15)); // Más espaciado
        panelCatalogo.setBackground(Estilos.COLOR_FONDO);
        
        // Envolver en un BorderLayout.NORTH evita que se estiren verticalmente si hay pocos
        JPanel contenedorCatalogo = new JPanel(new BorderLayout());
        contenedorCatalogo.setBackground(Estilos.COLOR_FONDO);
        contenedorCatalogo.add(panelCatalogo, BorderLayout.NORTH);

        JScrollPane scrollCat = new JScrollPane(contenedorCatalogo);
        scrollCat.setBorder(null);
        scrollCat.getViewport().setBackground(Estilos.COLOR_FONDO);

        scrollCat.getVerticalScrollBar().setUnitIncrement(30); // 30 es ideal, puedes subirlo a 40 si lo quieres más rápido

        panelIzq.add(panelBus, BorderLayout.NORTH);
        panelIzq.add(scrollCat, BorderLayout.CENTER);

        // --- NUEVO: PANEL DE PAGINACIÓN ---
        JPanel panelPaginacion = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelPaginacion.setBackground(Estilos.COLOR_FONDO);

        btnPaginaAnt = new BotonPro("< Anterior", Estilos.COLOR_PANEL, () -> cambiarPagina(-1));
        btnPaginaSig = new BotonPro("Siguiente >", Estilos.COLOR_PANEL, () -> cambiarPagina(1));
        btnPaginaAnt.setPreferredSize(new Dimension(120, 35));
        btnPaginaSig.setPreferredSize(new Dimension(120, 35));

        lblPaginaActual = new JLabel("Página 1");
        lblPaginaActual.setForeground(Color.WHITE);
        lblPaginaActual.setFont(Estilos.FONT_BOLD);

        panelPaginacion.add(btnPaginaAnt);
        panelPaginacion.add(lblPaginaActual);
        panelPaginacion.add(btnPaginaSig);

        // Agregamos la paginación a la parte inferior del lado izquierdo
        panelIzq.add(panelPaginacion, BorderLayout.SOUTH);

        // --- DERECHA: CARRITO (30%) ---
        JPanel panelDer = new JPanel(new BorderLayout());
        panelDer.setBackground(Estilos.COLOR_PANEL); // Color panel lateral
        panelDer.setBorder(new EmptyBorder(20, 15, 20, 15));

        JLabel lblTituloCar = new JLabel("CARRITO ACTUAL");
        lblTituloCar.setFont(Estilos.FONT_TITULO);
        lblTituloCar.setForeground(Estilos.COLOR_TEXTO);
        lblTituloCar.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Tabla Carrito Dark
        modeloCarrito = new DefaultTableModel(new String[]{"ID", "Producto", "Cant", "Precio", "Total", "G"}, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaCarrito = new TablaPro(modeloCarrito);
        Estilos.estilizarTabla(tablaCarrito);

        // Ocultar columnas técnicas
        tablaCarrito.removeColumn(tablaCarrito.getColumnModel().getColumn(5));
        tablaCarrito.removeColumn(tablaCarrito.getColumnModel().getColumn(0));

        // --- NUEVOS BOTONES DE CANTIDAD (+ y -) ---
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panelControles.setBackground(Estilos.COLOR_PANEL);
        panelControles.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Botón Menos (Rojo suave)
        BotonPro btnMenos = new BotonPro("","menos.png", new Color(221, 221, 221, 255), () -> modCantidad(-1));
        btnMenos.setPreferredSize(new Dimension(60, 60)); // Hacemos los botones más grandes para pantallas táctiles

        // Botón Más (Verde suave)
        BotonPro btnMas = new BotonPro("","mas.png", new Color(221, 221, 221), () -> modCantidad(1));
        btnMas.setPreferredSize(new Dimension(60, 60));

        // Botón Quitar (Gris oscuro)
        BotonPro btnQuitar = new BotonPro("Quitar Item","eliminar.png", new Color(200, 80, 80), this::eliminarFila);
        btnQuitar.setPreferredSize(new Dimension(150, 60));

        panelControles.add(btnMenos);
        panelControles.add(btnMas);
        panelControles.add(Box.createHorizontalStrut(20)); // Espacio
        panelControles.add(btnQuitar);

        // Panel inferior derecho (Totales)
        JPanel panelTotales = new JPanel(new GridLayout(2, 1, 0, 10));
        panelTotales.setBackground(Estilos.COLOR_PANEL);
        panelTotales.setBorder(new EmptyBorder(15, 0, 0, 0));

        lblTotal = new JLabel("$0.00", SwingConstants.RIGHT);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblTotal.setForeground(Color.WHITE);

        btnPagar = new BotonPro(" COBRAR", "vender.png", new Color(46, 204, 113), () -> finalizarVenta());
        btnPagar.setFont(new Font("Segoe UI", Font.BOLD, 26));

        btnPagar.setPreferredSize(new Dimension(0, 60));


        JPanel pInfoTotal = new JPanel(new BorderLayout());
        pInfoTotal.setBackground(Estilos.COLOR_PANEL);
        JLabel lblSub = new JLabel("Total a Pagar:");
        lblSub.setForeground(Estilos.COLOR_TEXTO_SEC);
        lblSub.setFont(Estilos.FONT_BOLD);

        pInfoTotal.add(lblSub, BorderLayout.WEST);
        pInfoTotal.add(lblTotal, BorderLayout.CENTER);

        panelTotales.add(pInfoTotal);
        panelTotales.add(btnPagar);


        // Controles pequeños para borrar
        panelDer.add(lblTituloCar, BorderLayout.NORTH);
        JPanel centroDer = new JPanel(new BorderLayout());
        centroDer.setBackground(Estilos.COLOR_PANEL);
        JScrollPane scrollCarrito = new JScrollPane(tablaCarrito);
        centroDer.add(scrollCarrito, BorderLayout.CENTER);

        centroDer.add(panelControles, BorderLayout.SOUTH);

        panelDer.add(centroDer, BorderLayout.CENTER);
        panelDer.add(panelTotales, BorderLayout.SOUTH);

        // Split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzq, panelDer);
        split.setResizeWeight(.6);
        split.setDividerSize(0); // Invisible divider
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        cargarCatalogo();
    }

    private void modCantidad(int delta) {
        int fila = tablaCarrito.getSelectedRow();
        if (fila == -1) return;

        int idProd = Integer.parseInt(modeloCarrito.getValueAt(fila, 0).toString());
        int cantidadActual = Integer.parseInt(modeloCarrito.getValueAt(fila, 2).toString());
        int nuevaCantidad = cantidadActual + delta;

        // Si estamos sumando (+1), verificamos stock antes
        if (delta > 0) {
            int stockReal = obtenerStockReal(idProd);
            if (nuevaCantidad > stockReal) {
                JOptionPanePro.mostrarMensaje(this, "Stock Límite", "No hay más existencias en inventario.", "ADVERTENCIA");
                return;
            }
        }

        if (nuevaCantidad <= 0) {
            if(JOptionPanePro.mostrarConfirmacion(this, "Eliminar", "¿Quitar del carrito?")) eliminarFila();
        } else {
            modCantidadEnFila(fila, delta); // Reutilizamos lógica visual
        }
    }


    // Método público para enfocar el lector
    public void darFocoCodigo() {
        SwingUtilities.invokeLater(() -> txtCodigo.requestFocusInWindow());
    }
    private void cambiarPagina(int delta) {
        int nuevaPagina = paginaActual + delta;
        if (nuevaPagina < 1) return; // No permitir páginas negativas

        paginaActual = nuevaPagina;
        lblPaginaActual.setText("Página " + paginaActual);
        cargarCatalogo();
    }

    public void cargarCatalogo() {
        panelCatalogo.removeAll();
        btnPaginaAnt.setEnabled(paginaActual > 1); // Solo se activa si no estamos en la pag 1

        String sql = "SELECT p.*, COALESCE(SUM(d.cantidad), 0) as vendidos " +
                     "FROM productos p " +
                     "LEFT JOIN detalle_venta d ON p.id = d.id_producto " +
                     "WHERE p.stock > 0 AND p.activo = 1 ";
                     
        if (!filtroBusqueda.isEmpty()) {
            sql += "AND (p.nombre LIKE ? OR p.codigo_barras LIKE ?) ";
        }
        
        sql += "GROUP BY p.id " +
               "ORDER BY vendidos DESC " +
               "LIMIT ? OFFSET ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (!filtroBusqueda.isEmpty()) {
                String likeFiltro = "%" + filtroBusqueda + "%";
                ps.setString(paramIndex++, likeFiltro);
                ps.setString(paramIndex++, likeFiltro);
            }
            
            ps.setInt(paramIndex++, LIMITE_PAGINA);
            int offset = (paginaActual - 1) * LIMITE_PAGINA;
            ps.setInt(paramIndex, offset);

            try (ResultSet rs = ps.executeQuery()) {
                int contadorProductos = 0;

            while (rs.next()) {
                contadorProductos++;
                int id = rs.getInt("id");
                String nom = rs.getString("nombre");
                double pre = rs.getDouble("precio");
                double costo = rs.getDouble("costo");
                String marca = rs.getString("marca");
                String modelo = rs.getString("modelo");
                int stock = rs.getInt("stock");

                BotonPro btnProducto = new BotonPro("", Estilos.COLOR_PANEL, () -> agregarAlCarrito(id, nom, pre, costo, stock));

                String html = "<html><body style='padding: 5px; text-align: left; color: white;'>" +
                        "<div style='font-size: 14px; font-weight: bold; width: 120px;'>" + nom + "</div>" +
                        "<div style='color: #b0b8c4; font-size: 10px;'>" + (marca != null ? marca : "") + " " + (modelo != null ? modelo : "") + "</div>" +
                        "<div style='margin-top: 6px; color: #ff2959; font-size: 11px; font-weight: bold;'>$" + pre + "</div>" +
                        "<div style='color: #4cd964; font-size: 10px;'>Stock: " + stock + "</div>" +
                        "</body></html>";
                JLabel lblInfo = new JLabel(html);
                btnProducto.add(lblInfo);
                panelCatalogo.add(btnProducto);
            }

            // Si la consulta devolvió menos productos que el límite, significa que ya no hay más páginas
            btnPaginaSig.setEnabled(contadorProductos == LIMITE_PAGINA);

            panelCatalogo.revalidate();
            }

        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelVentas.cargarCatalogo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Lógica de Carrito igual que antes ---
    private void buscarProducto(String codigo) {
        if (codigo.isEmpty()) return;
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM productos WHERE (codigo_barras = ? OR id IN (SELECT id_producto FROM codigos_adicionales WHERE codigo = ?)) AND activo = 1")) {
            ps.setString(1, codigo);
            ps.setString(2, codigo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Pasamos todos los datos directo de la consulta
                    agregarAlCarrito(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getDouble("precio"),
                            rs.getDouble("costo"),
                            rs.getInt("stock")
                    );
                    txtCodigo.setText("");
                } else {
                    // Si no lo encuentra como código exacto, asumimos que el usuario solo presionó Enter después de buscar por nombre.
                    // Mostramos error solo si no hay resultados en el catálogo visual (búsqueda fallida).
                    if (panelCatalogo.getComponentCount() == 0) {
                        JOptionPanePro.mostrarMensaje(this, "Error", "Producto no Encontrado", "ERROR");
                        txtCodigo.setText("");
                    }
                }
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelVentas.buscarProducto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void agregarAlCarrito(int id, String nombre, double precio, double costo, int stockReal) {
        // ¡CERO CONSULTAS A LA BASE DE DATOS AQUÍ!

        int cantidadEnCarrito = 0;
        int rowExistente = -1;

        for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
            if (Integer.parseInt(modeloCarrito.getValueAt(i, 0).toString()) == id) {
                cantidadEnCarrito = Integer.parseInt(modeloCarrito.getValueAt(i, 2).toString());
                rowExistente = i;
                break;
            }
        }

        // VALIDACIÓN DE STOCK ESTRICTA
        if ((cantidadEnCarrito + 1) > stockReal) {
            JOptionPanePro.mostrarMensaje(this, "Stock Insuficiente",
                    "Solo quedan " + stockReal + " unidades de " + nombre, "ADVERTENCIA");
            return;
        }

        if(rowExistente != -1) {
            modCantidadEnFila(rowExistente, 1);
        } else {
            modeloCarrito.addRow(new Object[]{id, nombre, 1, precio, precio, precio - costo});
            calcularTotal();
        }
        
        // Auto-limpiar búsqueda y regresar el foco para mayor agilidad
        if (!txtCodigo.getText().isEmpty()) {
            txtCodigo.setText("");
        }
        darFocoCodigo();
    }

    private void modCantidadEnFila(int row, int delta) {
        int cant = Integer.parseInt(modeloCarrito.getValueAt(row, 2).toString()) + delta;
        double precio = Double.parseDouble(modeloCarrito.getValueAt(row, 3).toString());
        // Recalcular ganancia proporcional
        double gananciaTotalActual = Double.parseDouble(modeloCarrito.getValueAt(row, 5).toString());
        // Evitar división por cero si es nuevo
        double gananciaUnit = (Integer.parseInt(modeloCarrito.getValueAt(row, 2).toString()) == 0) ? 0 : gananciaTotalActual / Integer.parseInt(modeloCarrito.getValueAt(row, 2).toString());

        // Si venimos de agregarAlCarrito y es nuevo, necesitamos calcular ganancia unitaria desde cero,
        // pero por simplicidad asumimos que el flujo principal ya manejó la inserción inicial.
        // Corrección rápida para mantener ganancia correcta al sumar:
        double gananciaUnitReal = (Double.parseDouble(modeloCarrito.getValueAt(row, 4).toString()) / (cant - delta)) - (gananciaTotalActual / (cant - delta)); // Aprox, mejor traer costo de BD si se requiere precisión milimétrica, pero funcional.
        // Mejor enfoque simple: Ganancia es (Precio - Costo) * Cant. El modelo no tiene costo, asumiendo precio fijo.

        modeloCarrito.setValueAt(cant, row, 2);
        modeloCarrito.setValueAt(cant * precio, row, 4);
        // Ganancia se ajusta proporcionalmente
        double gananciaAnterior = Double.parseDouble(modeloCarrito.getValueAt(row, 5).toString());
        double gananciaUnitEstimada = gananciaAnterior / (cant - delta);
        modeloCarrito.setValueAt(gananciaUnitEstimada * cant, row, 5);

        calcularTotal();
    }

    private void eliminarFila() {
        int row = tablaCarrito.getSelectedRow();
        if (row != -1) {
            modeloCarrito.removeRow(row);
            calcularTotal();
        }
    }

    private void calcularTotal() {
        totalVenta = 0;
        gananciaVenta = 0;
        for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
            totalVenta += Double.parseDouble(modeloCarrito.getValueAt(i, 4).toString());
            gananciaVenta += Double.parseDouble(modeloCarrito.getValueAt(i, 5).toString());
        }
        lblTotal.setText("$" + String.format("%.2f", totalVenta));
    }

    private void finalizarVenta() {
        if (totalVenta == 0) return;

        // Deshabilitar el botón de venta aquí para evitar doble clic
        btnPagar.setEnabled(false);
        btnPagar.setTexto("Procesando...");

        // 1. EXTRAER DATOS DE LA UI ANTES DE ENTRAR AL HILO
        // Es regla de oro no leer componentes gráficos dentro de doInBackground
        List<Object[]> datosCarrito = new ArrayList<>();
        for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
            datosCarrito.add(new Object[]{
                    modeloCarrito.getValueAt(i, 0), // id
                    modeloCarrito.getValueAt(i, 1), // nombre
                    modeloCarrito.getValueAt(i, 2), // cantidad
                    modeloCarrito.getValueAt(i, 4)  // subtotal
            });
        }

        final double totalFinal = totalVenta;
        final double gananciaFinal = gananciaVenta;



        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // 2. TODO EL TRABAJO PESADO EN SEGUNDO PLANO
                try (Connection conn = ConexionBD.conectar()) {
                    conn.setAutoCommit(false);

                    int idVenta = -1;
                    // Insertar Venta
                    try (PreparedStatement psV = conn.prepareStatement("INSERT INTO ventas (total_venta, ganancia_total, tipo_venta) VALUES (?, ?, 'PRODUCTO')", Statement.RETURN_GENERATED_KEYS)) {
                        psV.setDouble(1, totalFinal);
                        psV.setDouble(2, gananciaFinal);
                        psV.executeUpdate();

                        try (ResultSet rs = psV.getGeneratedKeys()) {
                            if (rs.next()) {
                                idVenta = rs.getInt(1);
                            }
                        }
                    }

                    List<GeneradorTicket.ItemTicket> listaTicket = new ArrayList<>();
                    try (PreparedStatement psD = conn.prepareStatement("INSERT INTO detalle_venta (id_venta, id_producto, cantidad, subtotal) VALUES (?,?,?,?)");
                         PreparedStatement psS = conn.prepareStatement("UPDATE productos SET stock = stock - ?, cantidad_faltante = cantidad_faltante + ?, fecha_faltante = NOW() WHERE id = ?")) {

                    // Usar los datos extraídos previamente
                    for (Object[] fila : datosCarrito) {
                        int idProd = Integer.parseInt(fila[0].toString());
                        String nombreProd = fila[1].toString();
                        int cant = Integer.parseInt(fila[2].toString());
                        double sub = Double.parseDouble(fila[3].toString());

                        // Batch Detalle
                        psD.setInt(1, idVenta); psD.setInt(2, idProd); psD.setInt(3, cant); psD.setDouble(4, sub);
                        psD.addBatch();

                        // Batch Stock
                        psS.setInt(1, cant); psS.setInt(2, cant); psS.setInt(3, idProd);
                        psS.addBatch();

                        listaTicket.add(new GeneradorTicket.ItemTicket(nombreProd, cant, sub));
                    }
                    psD.executeBatch();
                    psS.executeBatch();
                    }
                    conn.commit();

                    // Generar Ticket String
                    String ticket = GeneradorTicket.crearTicket(idVenta, null, listaTicket, totalFinal);

                    // IMPRESIÓN (OPERACIÓN PESADA 1)
                    if (ImpresoraTicket.isAutoImprimir()) {
                        ImpresoraTicket.imprimir(ticket);
                    } else {
                        System.out.println("--- Ticket Generado --- \n" + ticket);
                    }

                    // TELEGRAM (OPERACIÓN PESADA 2 - RED)
                    servicios.NotificadorTelegram.notificarVentaNueva(totalFinal);

                    return true; // Éxito
                } catch (Exception e) {
                    servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelVentas.finalizarVenta: " + e.getMessage());
                    e.printStackTrace();
                    return false; // Falló
                }
            }

            @Override
            protected void done() {
                // 3. ACTUALIZAR LA INTERFAZ CUANDO TODO TERMINE
                try {
                    boolean exito = get();
                    if (exito) {
                        if (ImpresoraTicket.isAutoImprimir()) {
                            ToastPro.show("Venta Finalizada \nNo olvides la bolsita", "EXITO");
                        } else {
                            JOptionPanePro.mostrarMensaje(PanelVentas.this, "Venta Exitosa", "Ticket generado.\nImpresión automática deshabilitada.", "INFO");
                        }

                        // Limpiar UI
                        modeloCarrito.setRowCount(0);
                        calcularTotal();
                        cargarCatalogo(); // Recarga el inventario visual
                        darFocoCodigo();
                    } else {
                        JOptionPanePro.mostrarMensaje(PanelVentas.this, "Error de Venta", "Ocurrió un problema guardando la venta.", "ERROR");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPanePro.mostrarMensaje(PanelVentas.this, "Error Crítico", "Fallo inesperado al procesar la venta.", "ERROR");

                }finally {
                    btnPagar.setEnabled(true);
                    btnPagar.setTexto("Vender");
                }
            }
        };

        worker.execute();
    }

    //Método Auxiliar para consultar Stock en BD
    private int obtenerStockReal(int idProducto) {
        int stock = 0;
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement("SELECT stock FROM productos WHERE id = ?")) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stock = rs.getInt("stock");
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en PanelVentas.obtenerStockReal: " + e.getMessage());
            e.printStackTrace();
        }
        return stock;
    }
}
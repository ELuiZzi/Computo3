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

    public PanelVentas() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

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
        txtCodigo.putClientProperty("JTextField.placeholderText", " Escanear código de barras...");

        BotonPro btnBuscar = new BotonPro("Buscar", Estilos.COLOR_PANEL, () -> buscarProducto(txtCodigo.getText()));

        btnBuscar.setPreferredSize(new Dimension(100, 40));

        txtCodigo.addActionListener(e -> buscarProducto(txtCodigo.getText()));


        panelBus.add(txtCodigo, BorderLayout.CENTER);
        panelBus.add(btnBuscar, BorderLayout.EAST);

        // Grid Productos (Fondo oscuro)
        panelCatalogo = new JPanel(new GridLayout(0, 2, 15, 15)); // Más espaciado
        panelCatalogo.setBackground(Estilos.COLOR_FONDO);

        JScrollPane scrollCat = new JScrollPane(panelCatalogo);
        scrollCat.setBorder(null);
        scrollCat.getViewport().setBackground(Estilos.COLOR_FONDO);

        panelIzq.add(panelBus, BorderLayout.NORTH);
        panelIzq.add(scrollCat, BorderLayout.CENTER);

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
        BotonPro btnMenos = new BotonPro("","menos.png", new Color(200, 80, 80), () -> modCantidad(-1));
        btnMenos.setPreferredSize(new Dimension(50, 40)); // Hacemos los botones cuadrados/pequeños

        // Botón Más (Verde suave)
        BotonPro btnMas = new BotonPro("","mas.png", new Color(80, 180, 80), () -> modCantidad(1));
        btnMas.setPreferredSize(new Dimension(50, 40));

        // Botón Quitar (Gris oscuro)
        BotonPro btnQuitar = new BotonPro("Quitar Item", new Color(100, 100, 100), this::eliminarFila);
        btnQuitar.setPreferredSize(new Dimension(120, 40));

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

        BotonPro btnPagar = new BotonPro("Vender", "vender.png", Estilos.COLOR_FONDO, () -> finalizarVenta());
        btnPagar.setFont(new Font("Segoe UI", Font.BOLD, 20));
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
        centroDer.add(new JScrollPane(tablaCarrito), BorderLayout.CENTER);

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

    public void cargarCatalogo() {
        panelCatalogo.removeAll();
        try (Connection conn = ConexionBD.conectar(); Statement stmt = conn.createStatement()) {
            // QUERY MEJORADA: Ordena por cantidad vendida (Descendente)
            String sql = "SELECT p.*, COALESCE(SUM(d.cantidad), 0) as vendidos " + "FROM productos p " + "LEFT JOIN detalle_venta d ON p.id = d.id_producto " + "WHERE p.stock > 0 AND p.activo = 1 " + "GROUP BY p.id " + "ORDER BY vendidos DESC";

            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nombre");
                double pre = rs.getDouble("precio");
                String marca = rs.getString("marca");
                String modelo = rs.getString("modelo"); // Nuevo
                int stock = rs.getInt("stock");         // Nuevo

                // Diseño de Tarjeta de Producto (Tipo Botón)
                BotonPro btnProducto = new BotonPro("", Estilos.COLOR_PANEL, () -> agregarAlCarrito(id));
// Como ui.componentes.BotonPro usa GridBagLayout centrado, para replicar el HTML multilínea
// tendríamos que modificar un poco ui.componentes.BotonPro o simplemente añadir un JLabel con HTML al ui.componentes.BotonPro.


                // HTML para formatear el texto dentro del botón
                String html = "<html><body style='padding: 5px; text-align: left; color: white;'>" + "<div style='font-size: 14px; font-weight: bold; width: 120px;'>" + nom + "</div>" + "<div style='color: #b0b8c4; font-size: 10px;'>" + (marca != null ? marca : "") + " " + (modelo != null ? modelo : "") + "</div>" + "<div style='margin-top: 6px; color: #ff2959; font-size: 11px; font-weight: bold;'>$" + pre + "</div>" + "<div style='color: #4cd964; font-size: 10px;'>Stock: " + stock + "</div>" + "</body></html>";
                JLabel lblInfo = new JLabel(html);
                btnProducto.add(lblInfo);
                panelCatalogo.add(btnProducto);

            }
            panelCatalogo.revalidate();
            panelCatalogo.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Lógica de Carrito igual que antes ---
    private void buscarProducto(String codigo) {
        if (codigo.isEmpty()) return;
        try (Connection conn = ConexionBD.conectar()) {
            String query = "SELECT * FROM productos WHERE (codigo_barras = ? OR id IN (SELECT id_producto FROM codigos_adicionales WHERE codigo = ?)) AND activo = 1";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, codigo);
            ps.setString(2, codigo);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                agregarAlCarrito(rs.getInt("id"));
            }
            else {
                JOptionPanePro.mostrarMensaje(this, "Error", "Producto no Encontrado", "ERROR");
            }
            txtCodigo.setText("");
            darFocoCodigo(); // Mantener foco
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void agregarAlCarrito(int id) {
        try (Connection conn = ConexionBD.conectar()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM productos WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int stockReal = rs.getInt("stock");
                String nombre = rs.getString("nombre");

                // Verificar cuánto llevamos en el carrito
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
                    return; // DETIENE EL PROCESO
                }

                double precio = rs.getDouble("precio");
                double costo = rs.getDouble("costo");

                if(rowExistente != -1) {
                    modCantidadEnFila(rowExistente, 1); // Este método también debe validarse
                } else {
                    modeloCarrito.addRow(new Object[]{id, nombre, 1, precio, precio, precio-costo});
                    calcularTotal();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        // ... (Misma lógica de guardado a BD que tenías antes) ...
        // Simulado para brevedad visual:
        try (Connection conn = ConexionBD.conectar()) {
            conn.setAutoCommit(false);

            PreparedStatement psV = conn.prepareStatement("INSERT INTO ventas (total_venta, ganancia_total, tipo_venta) VALUES (?, ?, 'PRODUCTO')", Statement.RETURN_GENERATED_KEYS);
            psV.setDouble(1, totalVenta);
            psV.setDouble(2, gananciaVenta);
            psV.executeUpdate();

            ResultSet rs = psV.getGeneratedKeys();
            rs.next();
            int idVenta = rs.getInt(1);

            // Preparar lista para el ticket
            List<GeneradorTicket.ItemTicket> listaTicket = new ArrayList<>();

            PreparedStatement psD = conn.prepareStatement("INSERT INTO detalle_venta (id_venta, id_producto, cantidad, subtotal) VALUES (?,?,?,?)");
            PreparedStatement psS = conn.prepareStatement("UPDATE productos SET stock = stock - ?, cantidad_faltante = cantidad_faltante + ?, fecha_faltante = NOW() WHERE id = ?");

            for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                int idProd = Integer.parseInt(modeloCarrito.getValueAt(i, 0).toString());
                String nombreProd = modeloCarrito.getValueAt(i, 1).toString();
                int cant = Integer.parseInt(modeloCarrito.getValueAt(i, 2).toString());
                double sub = Double.parseDouble(modeloCarrito.getValueAt(i, 4).toString());

                // BD
                // Batch Detalle
                psD.setInt(1, idVenta); psD.setInt(2, idProd); psD.setInt(3, cant); psD.setDouble(4, sub);
                psD.addBatch();

                // Batch Stock + Faltante
                psS.setInt(1, cant); // Restar al stock
                psS.setInt(2, cant); // SUMAR a cantidad_faltante (acumular pedido)
                psS.setInt(3, idProd); // ID
                psS.addBatch();

                // Ticket
                listaTicket.add(new GeneradorTicket.ItemTicket(nombreProd, cant, sub));
            }
            psD.executeBatch();
            psS.executeBatch();
            conn.commit();


            // GENERAR EL TEXTO DEL TICKET
            String ticket = GeneradorTicket.crearTicket(idVenta, null, listaTicket, totalVenta);
            System.out.println(ticket);





            // 2. VERIFICAR SI SE DEBE IMPRIMIR
            if (ImpresoraTicket.isAutoImprimir()) {
                ImpresoraTicket.imprimir(ticket); // ENVÍA A LA IMPRESORA REAL
                ToastPro.show("Venta Finalizada \n" +
                        "No olvides la bolsita", "EXITO");
            } else {
                // Si no es auto, podemos mostrarlo en consola o en un JTextArea
                System.out.println("--- Ticket Generado (Impresión Deshabilitada) ---");
                System.out.println(ticket);
                System.out.println("--------------------------------------------------");
                JOptionPanePro.mostrarMensaje(this, "Venta Exitosa", "Ticket generado.\nImpresión automática deshabilitada.", "INFO");
            }



            modeloCarrito.setRowCount(0);
            calcularTotal();
            cargarCatalogo();
            darFocoCodigo();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    //Método Auxiliar para consultar Stock en BD
    private int obtenerStockReal(int idProducto) {
        int stock = 0;
        try (Connection conn = ConexionBD.conectar()) {
            PreparedStatement ps = conn.prepareStatement("SELECT stock FROM productos WHERE id = ?");
            ps.setInt(1, idProducto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) stock = rs.getInt("stock");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stock;
    }
}
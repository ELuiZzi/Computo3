package ui.ventanas;

import config.ConexionBD;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DialogoAgregarProducto extends JDialog {

    private JTextField txtBuscar;
    private JTable tablaProductos;
    private DefaultTableModel modeloTabla;
    private JSpinner spinCantidad;
    private JButton btnAgregar;
    private int idOrden;

    public DialogoAgregarProducto(Frame parent, int idOrden) {
        super(parent, "Agregar Refacción o Producto", true);
        this.idOrden = idOrden;

        setSize(500, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // --- PANEL SUPERIOR: Búsqueda ---
        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por nombre o código..."); // Tip de FlatLaf

        panelNorte.add(new JLabel("Buscar Pieza:"), BorderLayout.WEST);
        panelNorte.add(txtBuscar, BorderLayout.CENTER);

        // --- PANEL CENTRAL: Tabla de Resultados ---
        String[] columnas = {"ID", "Descripción", "Stock", "Precio ($)"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; } // Tabla de solo lectura
        };
        tablaProductos = new JTable(modeloTabla);
        JScrollPane scroll = new JScrollPane(tablaProductos);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // --- PANEL INFERIOR: Cantidad y Acción ---
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelSur.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        spinCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        btnAgregar = new JButton("Agregar Producto");

        panelSur.add(new JLabel("Cant:"));
        panelSur.add(spinCantidad);
        panelSur.add(btnAgregar);

        add(panelNorte, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        // --- EVENTOS ---
        // Búsqueda en tiempo real al soltar cada tecla
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                buscarProductos(txtBuscar.getText().trim());
            }
        });

        btnAgregar.addActionListener(e -> ejecutarTransaccion());

        // Cargar todos los productos al abrir
        buscarProductos("");
    }

    private void buscarProductos(String busqueda) {
        modeloTabla.setRowCount(0);

        // CORRECCIÓN: Nombres exactos extraídos de tu PanelInventario
        String sql = "SELECT id, nombre, stock, precio FROM productos WHERE nombre LIKE ? OR codigo_barras LIKE ? AND activo = 1 LIMIT 20";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + busqueda + "%");
            ps.setString(2, "%" + busqueda + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                modeloTabla.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("nombre"),       // Antes era descripcion
                        rs.getInt("stock"),
                        rs.getDouble("precio")        // Antes era precio_venta
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void ejecutarTransaccion() {
        int fila = tablaProductos.getSelectedRow();
        if (fila == -1) return;

        int idProd = (int) tablaProductos.getValueAt(fila, 0);
        String nombreProd = (String) tablaProductos.getValueAt(fila, 1);
        int stockDisponible = (int) tablaProductos.getValueAt(fila, 2);
        double precioUnidad = (double) tablaProductos.getValueAt(fila, 3);
        int cant = (int) spinCantidad.getValue();

        if (cant > stockDisponible) {
            JOptionPane.showMessageDialog(this, "Stock insuficiente.");
            return;
        }

        double total = precioUnidad * cant;
        String conceptoFinal = (cant > 1 ? cant + "x " : "") + nombreProd;

        try (Connection conn = ConexionBD.conectar()) {
            conn.setAutoCommit(false);
            try {
                // 1. Guardamos el cargo vinculando el ID del producto y la cantidad
                String sqlCargo = "INSERT INTO cargos_orden (id_orden, concepto, monto, id_producto, cantidad) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlCargo)) {
                    ps.setInt(1, this.idOrden);
                    ps.setString(2, conceptoFinal.toUpperCase());
                    ps.setDouble(3, total);
                    ps.setInt(4, idProd); // <--- Clave para la devolución
                    ps.setInt(5, cant);   // <--- Clave para la devolución
                    ps.executeUpdate();
                }

                // 2. Descontamos stock (Tabla: productos)
                String sqlStock = "UPDATE productos SET stock = stock - ? WHERE id = ?";
                try (PreparedStatement psS = conn.prepareStatement(sqlStock)) {
                    psS.setInt(1, cant);
                    psS.setInt(2, idProd);
                    psS.executeUpdate();
                }

                conn.commit();
                dispose();
            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
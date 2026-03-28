package ui.ventanas;

import config.ConexionBD;
import servicios.GeneradorTicket;
import servicios.ImpresoraTicket;
import ui.componentes.*;
import util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DialogoVentaRapida extends JDialog {

    private JTextField txtScanner;
    private JLabel lblEstado;

    public DialogoVentaRapida(JFrame parent) {
        super(parent, "Venta Rápida - Modo Express", true);
        setSize(500, 300);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Estilos.COLOR_FONDO);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- 1. TÍTULO E INSTRUCCIONES ---
        JLabel lblTitulo = new JLabel("VENTA RÁPIDA (1 Pieza)", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblInstruccion = new JLabel("<html><center>Escanea el código de barras.<br>Se venderá 1 unidad y se imprimirá el ticket al instante.</center></html>", SwingConstants.CENTER);
        lblInstruccion.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblInstruccion.setForeground(Color.LIGHT_GRAY);
        lblInstruccion.setBorder(new EmptyBorder(10, 0, 20, 0));

        JPanel pNorte = new JPanel(new BorderLayout());
        pNorte.setBackground(Estilos.COLOR_FONDO);
        pNorte.add(lblTitulo, BorderLayout.NORTH);
        pNorte.add(lblInstruccion, BorderLayout.CENTER);

        // --- 2. CAMPO ESCÁNER GIGANTE ---
        txtScanner = new JTextField();
        txtScanner.setFont(new Font("Consolas", Font.BOLD, 30));
        txtScanner.setHorizontalAlignment(SwingConstants.CENTER);
        txtScanner.setBackground(Estilos.COLOR_INPUT);
        txtScanner.setForeground(Color.WHITE);
        txtScanner.setCaretColor(Color.WHITE);
        txtScanner.setBorder(BorderFactory.createLineBorder(Estilos.COLOR_ACCENT, 2));

        // Al dar Enter (lo que hace el lector), procesamos
        txtScanner.addActionListener(e -> procesarVenta(txtScanner.getText().trim()));

        // --- 3. ESTADO Y SALIR ---
        lblEstado = new JLabel("Esperando código...", SwingConstants.CENTER);
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEstado.setForeground(Color.YELLOW);
        lblEstado.setBorder(new EmptyBorder(10, 0, 10, 0));

        BotonPro btnCerrar = new BotonPro("SALIR / CERRAR", Color.RED, this::dispose);

        JPanel pSur = new JPanel(new BorderLayout());
        pSur.setBackground(Estilos.COLOR_FONDO);
        pSur.add(lblEstado, BorderLayout.NORTH);
        pSur.add(btnCerrar, BorderLayout.SOUTH);

        panel.add(pNorte, BorderLayout.NORTH);
        panel.add(txtScanner, BorderLayout.CENTER);
        panel.add(pSur, BorderLayout.SOUTH);

        add(panel);
    }

    private void procesarVenta(String codigo) {
        if (codigo.isEmpty()) return;

        lblEstado.setText("Procesando...");
        lblEstado.setForeground(Color.WHITE);

        try (Connection conn = ConexionBD.conectar()) {
            // 1. Buscar Producto (Compatible con Multi-Códigos)
            String sqlBuscar = "SELECT * FROM productos WHERE (codigo_barras = ? OR id IN (SELECT id_producto FROM codigos_adicionales WHERE codigo = ?)) AND activo = 1";
            PreparedStatement ps = conn.prepareStatement(sqlBuscar);
            ps.setString(1, codigo);
            ps.setString(2, codigo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int idProd = rs.getInt("id");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");
                double costo = rs.getDouble("costo");
                int stock = rs.getInt("stock");

                if (stock <= 0) {
                    sonidoError();
                    lblEstado.setText("¡ERROR! PRODUCTO AGOTADO");
                    lblEstado.setForeground(Color.RED);
                    txtScanner.selectAll();
                    return;
                }

                // 2. Realizar Venta (Transacción)
                conn.setAutoCommit(false);

                // Insertar Venta
                PreparedStatement psV = conn.prepareStatement("INSERT INTO ventas (total_venta, ganancia_total, tipo_venta) VALUES (?, ?, 'PRODUCTO')", Statement.RETURN_GENERATED_KEYS);
                psV.setDouble(1, precio);
                psV.setDouble(2, precio - costo);
                psV.executeUpdate();

                ResultSet rsKey = psV.getGeneratedKeys();
                rsKey.next();
                int idVenta = rsKey.getInt(1);

                // Insertar Detalle
                PreparedStatement psD = conn.prepareStatement("INSERT INTO detalle_venta (id_venta, id_producto, cantidad, subtotal) VALUES (?, ?, 1, ?)");
                psD.setInt(1, idVenta);
                psD.setInt(2, idProd);
                psD.setDouble(3, precio);
                psD.executeUpdate();

                // Restar Stock
                PreparedStatement psS = conn.prepareStatement("UPDATE productos SET stock = stock - 1 WHERE id = ?");
                psS.setInt(1, idProd);
                psS.executeUpdate();

                conn.commit();

                // 3. Imprimir
                // Siempre imprimimos en venta rápida para confirmar, o respetamos config
                // Aquí forzamos imprimir para dar feedback físico de que "ya pasó"
                List<GeneradorTicket.ItemTicket> items = new ArrayList<>();
                items.add(new GeneradorTicket.ItemTicket(nombre, 1, precio));

                String ticket = GeneradorTicket.crearTicket(idVenta, null, items, precio);
                ImpresoraTicket.imprimir(ticket);

                // 4. Feedback Éxito
                lblEstado.setText("VENDIDO: " + nombre + " ($" + precio + ")");
                lblEstado.setForeground(Color.GREEN);
                txtScanner.setText("");
                txtScanner.requestFocus();

            } else {
                sonidoError();
                lblEstado.setText("NO ENCONTRADO: " + codigo);
                lblEstado.setForeground(Color.RED);
                txtScanner.selectAll();
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblEstado.setText("Error en Base de Datos");
            lblEstado.setForeground(Color.RED);
        }
    }

    private void sonidoError() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }
}
package Paneles;

import Conexión.ConexionBD;
import Utils.Estilos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import ElementosPro.*;

public class PanelFaltantes extends JPanel {
    private TablaPro tabla;
    private DefaultTableModel modelo;

    public PanelFaltantes() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        // Header
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(Estilos.COLOR_PANEL);
        panelHeader.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblInfo = new JLabel("Productos con Stock Bajo");
        lblInfo.setForeground(Color.WHITE);
        lblInfo.setFont(Estilos.FONT_TITULO);

        // Panel de botones Header
        JPanel panelBtnHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBtnHeader.setBackground(Estilos.COLOR_PANEL);

        // Botón Actualizar
        BotonPro btnRefrescar = new BotonPro("Actualizar", "actualizar.png", Estilos.COLOR_INPUT, this::cargarFaltantes);
        btnRefrescar.setPreferredSize(new Dimension(150, 40));

        // --- BOTÓN WHATSAPP
        BotonPro btnWhatsapp = new BotonPro("Enviar Pedido", "whatsapp.png", new Color(37, 211, 102), this::generarPedidoWhatsApp);

        panelBtnHeader.add(btnRefrescar);
        panelBtnHeader.add(Box.createHorizontalStrut(10));
        panelBtnHeader.add(btnWhatsapp);

        panelHeader.add(lblInfo, BorderLayout.WEST);
        panelHeader.add(panelBtnHeader, BorderLayout.EAST);

        // Tabla
        modelo = new DefaultTableModel(new String[]{"ID", "Producto", "Modelo", "Marca", "Prov.", "Stock", "POR PEDIR", "CostoHide"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new TablaPro(modelo);
        Estilos.estilizarTabla(tabla);

        // Ocultar ID
        ocultarColumna(0); // ID
        ocultarColumna(7); // Costo (Nuevo)

        // --- BOTÓN SURTIR ---
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelSur.setBackground(Estilos.COLOR_FONDO);
        panelSur.setBorder(new EmptyBorder(10,0,10,20));

        BotonPro btnSurtir = new BotonPro("Surtir Producto", "caja.png", new Color(46, 204, 113), this::surtirProducto);
               panelSur.add(btnSurtir);

        add(panelHeader, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        cargarFaltantes();
    }

    private void ocultarColumna(int index) {
        tabla.getColumnModel().getColumn(index).setMinWidth(0);
        tabla.getColumnModel().getColumn(index).setMaxWidth(0);
        tabla.getColumnModel().getColumn(index).setPreferredWidth(0);
    }

    private void generarPedidoWhatsApp() {
        int[] filasSeleccionadas = tabla.getSelectedRows();

        if (filasSeleccionadas.length == 0) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Selecciona al menos un producto de la tabla.", "ADVERTENCIA");
            return;
        }

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("*PEDIDO DE REPOSICIÓN - LUMTECH*").append("\n");
        mensaje.append("------------------------------").append("\n");

        double granTotal = 0;

        for (int row : filasSeleccionadas) {
            String nombre = modelo.getValueAt(row, 1).toString();
            String modeloProd = modelo.getValueAt(row, 2).toString();
            String proveedor = modelo.getValueAt(row, 4).toString();

            // Limpiar " Pzas" del string para obtener número
            String cantStr = modelo.getValueAt(row, 6).toString().replace(" Pzas", "").trim();
            int cantidad = Integer.parseInt(cantStr);

            // Obtener costo de columna oculta
            double costoUnitario = Double.parseDouble(modelo.getValueAt(row, 7).toString());
            double totalLinea = cantidad * costoUnitario;

            granTotal += totalLinea;

            // Formato del mensaje por producto
            mensaje.append("📦 *Prod:* ").append(nombre).append(" (").append(modeloProd).append(")").append("\n");
            mensaje.append("   Prov: ").append(proveedor).append("\n");
            mensaje.append("   Cant: ").append(cantidad).append(" pzas | Costo: $").append(String.format("%.2f", totalLinea)).append("\n");
            mensaje.append("- - - - - - - - -").append("\n");
        }

        mensaje.append("\n");
        mensaje.append("💰 *TOTAL ESTIMADO: $").append(String.format("%.2f", granTotal)).append("*");

        // Imprimir en consola para verificación
        System.out.println(mensaje.toString());

        // ENVIAR A WHATSAPP
        try {
            // Codificar el mensaje para URL (espacios a %20, saltos a %0A, etc.)
            String mensajeCodificado = URLEncoder.encode(mensaje.toString(), StandardCharsets.UTF_8.toString());
            // Número formato internacional México: 52 + 1 + 777... (A veces el 1 es opcional en API, pero 52 es forzoso)
            // Se usa el número que proporcionaste: 7771908024
            String url = "https://wa.me/527771908024?text=" + mensajeCodificado;

            Desktop.getDesktop().browse(new URI(url));

        } catch (Exception e) {
            JOptionPanePro.mostrarMensaje(this, "Error", "No se pudo abrir WhatsApp: " + e.getMessage(), "ERROR");
        }
    }

    private void surtirProducto() {
        int row = tabla.getSelectedRow();

        if (row != -1) {
            // OPCIÓN A: Fila seleccionada en la tabla
            int idProducto = Integer.parseInt(modelo.getValueAt(row, 0).toString());
            String nombre = modelo.getValueAt(row, 1).toString();

            // Llamamos al método común
            procesarEntradaStock(idProducto, nombre);

        } else {
            // OPCIÓN B: Nada seleccionado -> Usar Lector de Barras
            String codigo = JOptionPanePro.solicitarEntrada(this, "Modo Escáner", "Escanear Código de Barras:");

            if (codigo != null && !codigo.isEmpty()) {
                buscarYProcesarCodigo(codigo.trim());
            }
        }
    }

    private void buscarYProcesarCodigo(String codigo) {
        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT id, nombre FROM productos WHERE codigo_barras = ? AND activo = 1";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");

                // ¡Encontrado! Pasamos a pedir la cantidad
                procesarEntradaStock(id, nombre);

            } else {
                JOptionPanePro.mostrarMensaje(this, "Error", "Producto no encontrado o inactivo: " + codigo, "ERROR");
                // Opcional: Volver a pedir código si falló (recursividad simple)
                // surtirProducto();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error BD", e.getMessage(), "ERROR");
        }
    }

    private void procesarEntradaStock(int idProducto, String nombreProducto) {
        String input = JOptionPanePro.solicitarEntrada(this, "Entrada Almacén",
                "Producto: " + nombreProducto + "\n¿Cantidad recibida?");

        if (input != null && !input.isEmpty()) {
            try {
                int cantidadLlegada = Integer.parseInt(input);
                if (cantidadLlegada <= 0) {
                    JOptionPanePro.mostrarMensaje(this, "Aviso", "La cantidad debe ser mayor a 0.", "ADVERTENCIA");
                    return;
                }

                try (Connection conn = ConexionBD.conectar()) {
                    // Lógica: Sumar Stock y Restar Faltante (sin bajar de 0)
                    String sql = "UPDATE productos SET stock = stock + ?, cantidad_faltante = GREATEST(0, cantidad_faltante - ?) WHERE id = ?";

                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, cantidadLlegada);
                    ps.setInt(2, cantidadLlegada);
                    ps.setInt(3, idProducto);

                    int afectados = ps.executeUpdate();

                    if (afectados > 0) {
                        JOptionPanePro.mostrarMensaje(this, "Éxito", "Inventario actualizado: " + nombreProducto + " (+" + cantidadLlegada + ")", "INFO");
                        cargarFaltantes(); // Refrescar tabla
                    } else {
                        JOptionPanePro.mostrarMensaje(this, "Error", "No se pudo actualizar el producto.", "ERROR");
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPanePro.mostrarMensaje(this, "Error", "Ingresa un número válido.", "ERROR");
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPanePro.mostrarMensaje(this, "Error SQL", e.getMessage(), "ERROR");
            }
        }
    }

    public void cargarFaltantes() {
        modelo.setRowCount(0);
        try (Connection conn = ConexionBD.conectar(); Statement stmt = conn.createStatement()) {
            // AHORA TRAEMOS TAMBIÉN EL COSTO
            String sql = "SELECT id, nombre, modelo, marca, proveedor, stock, cantidad_faltante, costo " +
                    "FROM productos " +
                    "WHERE activo = 1 AND (stock = 0 OR cantidad_faltante > 0) " +
                    "ORDER BY stock ASC, cantidad_faltante DESC";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nombre");
                String mod = rs.getString("modelo");
                String mar = rs.getString("marca");
                String pro = rs.getString("proveedor");
                int stock = rs.getInt("stock");
                int faltante = rs.getInt("cantidad_faltante");
                double costo = rs.getDouble("costo"); // Leemos el costo

                int sugerencia = (stock == 0 && faltante == 0) ? 1 : faltante;

                modelo.addRow(new Object[]{
                        id, nom, mod, mar, pro, stock, sugerencia + " Pzas", costo
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error BD", e.getMessage(), "ERROR");
        }
    }
}
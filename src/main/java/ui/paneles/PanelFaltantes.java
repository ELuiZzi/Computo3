package ui.paneles; // Ajusta a tu paquete real


import config.ConexionBD;
import ui.componentes.*;
import util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class PanelFaltantes extends JPanel {
    private TablaPro tabla;
    private DefaultTableModel modelo;
    private JComboBox<String> cmbFiltroTiempo;

    public PanelFaltantes() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        // --- HEADER ---
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(Estilos.COLOR_PANEL);
        panelHeader.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblInfo = new JLabel("<html>Gestión de Pedidos<br><font size='3' color='#b0b8c4'>Productos por reabastecer</font></html>");
        lblInfo.setForeground(Color.WHITE);
        lblInfo.setFont(Estilos.FONT_TITULO);

        // Panel de botones Header + FILTRO
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelControles.setBackground(Estilos.COLOR_PANEL);

        // --- NUEVO: COMBO DE FILTRO ---
        JLabel lblFiltro = new JLabel("Mostrar:");
        lblFiltro.setForeground(Color.WHITE);
        lblFiltro.setFont(Estilos.FONT_BOLD);

        cmbFiltroTiempo = new JComboBox<>(new String[]{"Todo el Historial", "Última Semana", "Último Mes"});
        cmbFiltroTiempo.addActionListener(e -> cargarFaltantes()); // Recargar al cambiar

        BotonPro btnRefrescar = new BotonPro("🔄", Estilos.COLOR_INPUT, this::cargarFaltantes);
        BotonPro btnWhatsapp = new BotonPro("WhatsApp", "whatsapp.png", new Color(37, 211, 102), this::generarPedidoWhatsApp);

        panelControles.add(lblFiltro);
        panelControles.add(cmbFiltroTiempo);
        panelControles.add(Box.createHorizontalStrut(10));
        panelControles.add(btnRefrescar);
        panelControles.add(Box.createHorizontalStrut(5));
        panelControles.add(btnWhatsapp);

        panelHeader.add(lblInfo, BorderLayout.WEST);
        panelHeader.add(panelControles, BorderLayout.EAST);

        // --- TABLA ---
        // Agregamos columna Fecha (Opcional, para ver cuándo cayó en faltante)
        modelo = new DefaultTableModel(new String[]{"ID", "Producto", "Modelo", "Marca", "Prov.", "Stock", "POR PEDIR", "CostoHide", "Fecha Reg."}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tabla = new TablaPro(modelo);
        Estilos.estilizarTabla(tabla);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemCopiar = new JMenuItem("Copiar Modelo");
        itemCopiar.addActionListener(e -> copiarModeloPortapapeles());

        popup.add(itemCopiar);
        tabla.setComponentPopupMenu(popup);

        // Ocultar columnas técnicas
        ocultarColumna(0); // ID
        ocultarColumna(7); // Costo
        ocultarColumna(8); //Fecha Venta ;
        // La fecha la dejamos visible o la ocultamos según tu gusto. Aquí la dejo visible.
        tabla.getColumnModel().getColumn(8).setPreferredWidth(120);

        // --- BOTONERA INFERIOR (Igual que antes) ---
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelSur.setBackground(Estilos.COLOR_FONDO);
        panelSur.setBorder(new EmptyBorder(10,0,10,20));

        BotonPro btnEliminar = new BotonPro("❌ Quitar", new Color(200, 50, 50), this::eliminarDeFaltantes);
        BotonPro btnEditar = new BotonPro("✏ Editar Cant.", Color.ORANGE, this::modificarCantidad);
        BotonPro btnSurtir = new BotonPro("📦 SURTIR", new Color(46, 204, 113), this::surtirProducto);

        panelSur.add(btnEliminar);
        panelSur.add(Box.createHorizontalStrut(10));
        panelSur.add(btnEditar);
        panelSur.add(Box.createHorizontalStrut(20));
        panelSur.add(btnSurtir);

        add(panelHeader, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        cargarFaltantes();
    }

    private void copiarModeloPortapapeles() {
        int row = tabla.getSelectedRow();
        if (row == -1) return;

        // Asumiendo que "Modelo" es la columna 2 (índice 2) según tu código anterior
        // {"ID", "Producto", "Modelo", "Marca", ...}
        Object valor = modelo.getValueAt(row, 2);
        String modeloTexto = (valor != null) ? valor.toString() : "";

        if (!modeloTexto.isEmpty()) {
            java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(modeloTexto);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            ToastPro.show("Modelo copiado", "EXITO");
        } else {
            ToastPro.show("Sin modelo", "ERROR");
        }
    }

    private void ocultarColumna(int index) {
        tabla.getColumnModel().getColumn(index).setMinWidth(0);
        tabla.getColumnModel().getColumn(index).setMaxWidth(0);
        tabla.getColumnModel().getColumn(index).setPreferredWidth(0);
    }

    // ==========================================================
    // NUEVAS FUNCIONALIDADES
    // ==========================================================

    private void modificarCantidad() {
        int row = tabla.getSelectedRow();
        if (row == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Selecciona un producto para editar.", "ADVERTENCIA");
            return;
        }

        int id = Integer.parseInt(modelo.getValueAt(row, 0).toString());
        String nombre = modelo.getValueAt(row, 1).toString();
        String cantActualStr = modelo.getValueAt(row, 6).toString().replace(" Pzas", "").trim();

        String input = JOptionPanePro.solicitarEntrada(this, "Editar Pedido",
                "Producto: " + nombre + "\nCantidad actual a pedir: " + cantActualStr + "\n\nNueva cantidad:");

        if (input != null && !input.isEmpty()) {
            try {
                int nuevaCant = Integer.parseInt(input);
                if (nuevaCant < 0) throw new NumberFormatException();

                try (Connection conn = ConexionBD.conectar()) {
                    PreparedStatement ps = conn.prepareStatement("UPDATE productos SET cantidad_faltante = ? WHERE id = ?");
                    ps.setInt(1, nuevaCant);
                    ps.setInt(2, id);
                    ps.executeUpdate();

                    // Feedback sutil
                    cargarFaltantes();
                }
            } catch (Exception e) {
                JOptionPanePro.mostrarMensaje(this, "Error", "Ingresa un número válido (0 o mayor).", "ERROR");
            }
        }
    }

    private void eliminarDeFaltantes() {
        int row = tabla.getSelectedRow();
        if (row == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Selecciona un producto para quitar.", "ADVERTENCIA");
            return;
        }

        int id = Integer.parseInt(modelo.getValueAt(row, 0).toString());
        String nombre = modelo.getValueAt(row, 1).toString();
        int stockActual = Integer.parseInt(modelo.getValueAt(row, 5).toString());

        // Opciones personalizadas para el usuario
        Object[] opciones = {"Solo Borrar Pedido (0)", "Descontinuar Producto", "Cancelar"};

        int seleccion = JOptionPanePro.mostrarOpciones(
                this,
                "Gestionar Faltante",
                "¿Qué deseas hacer con '" + nombre + "'?\n\n" +
                        "• Solo Borrar Pedido: Pone el contador en 0.\n" +
                        "• Descontinuar: Lo elimina permanentemente.",
                opciones
        );

        // Si cerró la ventana o dio cancelar (índice 2), salimos
        if (seleccion == -1 || seleccion == 2) return;

        try (Connection conn = ConexionBD.conectar()) {
            if (seleccion == 0) {
                // OPCIÓN A: SOLO RESETEAR CONTADOR
                PreparedStatement ps = conn.prepareStatement("UPDATE productos SET cantidad_faltante = 0 WHERE id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();

                // Advertencia si el stock es 0, porque seguirá apareciendo
                if (stockActual == 0) {
                    JOptionPanePro.mostrarMensaje(this, "Ojo",
                            "El pedido se puso en 0, pero como el Stock es 0,\nseguirá apareciendo en la lista (como urgencia)\nhasta que lo descontinúes o agregues stock.", "INFO");
                } else {
                    JOptionPanePro.mostrarMensaje(this, "Listo", "Pedido cancelado para este producto.", "INFO");
                }

            } else if (seleccion == 1) {
                // OPCIÓN B: DESCONTINUAR (SOFT DELETE)
                if (JOptionPanePro.mostrarConfirmacion(this, "Confirmar Baja", "¿Seguro que DESCONTINUAS este producto?\nYa no aparecerá en Inventario ni Faltantes.")) {
                    PreparedStatement ps = conn.prepareStatement("UPDATE productos SET activo = 0 WHERE id = ?");
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    JOptionPanePro.mostrarMensaje(this, "Baja", "Producto descontinuado.", "INFO");
                }
            }
            // Opción 2 es Cancelar, no hace nada

            cargarFaltantes();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================================
    // MÉTODOS EXISTENTES (Surtir, WhatsApp, Cargar...)
    // ==========================================================



    private void surtirProducto() {
        int row = tabla.getSelectedRow();
        if (row != -1) {
            int id = Integer.parseInt(modelo.getValueAt(row, 0).toString());
            String nombre = modelo.getValueAt(row, 1).toString();
            procesarEntradaStock(id, nombre);
        } else {
            String codigo = JOptionPanePro.solicitarEntrada(this, "Scanner", "Código de Barras:");
            if(codigo != null && !codigo.isEmpty()) buscarYProcesarCodigo(codigo);
        }
    }

    // Métodos auxiliares para surtir (buscarYProcesarCodigo, procesarEntradaStock)
    // Copiar de la versión anterior...

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


    // --- MÉTODO CARGAR CON FILTRO DE FECHA ---
    public void cargarFaltantes() {
        modelo.setRowCount(0);

        // Obtener selección del filtro
        String filtro = (String) cmbFiltroTiempo.getSelectedItem();

        try (Connection conn = ConexionBD.conectar(); Statement stmt = conn.createStatement()) {

            // Base de la consulta
            String sql = "SELECT id, nombre, modelo, marca, proveedor, stock, cantidad_faltante, costo, fecha_faltante " +
                    "FROM productos " +
                    "WHERE activo = 1 AND (stock <= 2 OR cantidad_faltante > 0) ";

            // Aplicar Filtros de Tiempo
            if ("Última Semana".equals(filtro)) {
                // Intervalo de 7 días hacia atrás
                sql += "AND fecha_faltante >= DATE_SUB(NOW(), INTERVAL 7 DAY) ";
            } else if ("Último Mes".equals(filtro)) {
                // Intervalo de 30 días
                sql += "AND fecha_faltante >= DATE_SUB(NOW(), INTERVAL 1 MONTH) ";
            }
            // Si es "Todo el Historial", no agregamos nada extra

            // Ordenar: Los más recientes y críticos primero
            sql += "ORDER BY fecha_faltante DESC, stock ASC";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nombre");
                String mod = rs.getString("modelo");
                String mar = rs.getString("marca");
                String pro = rs.getString("proveedor");
                int stock = rs.getInt("stock");
                int faltante = rs.getInt("cantidad_faltante");
                double costo = rs.getDouble("costo");
                Timestamp fecha = rs.getTimestamp("fecha_faltante");

                int sugerencia = (stock <= 2 && faltante == 0) ? 1 : faltante;

                modelo.addRow(new Object[]{
                        id, nom, mod, mar, pro, stock, sugerencia + " Pzas", costo, fecha
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error BD", e.getMessage(), "ERROR");
        }
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



}
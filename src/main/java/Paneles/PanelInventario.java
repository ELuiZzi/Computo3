package Paneles;

import Utils.AlgoritmoPrecios;
import Conexión.ConexionBD;
import Utils.Estilos;
import Utils.GeneradorCodigoBarras;
import Utils.GeneradorEtiquetas;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import ElementosPro.*;

public class PanelInventario extends JPanel {
    // Componentes
    private final JTextField txtId;
    private final JTextField txtCodigo;
    private final JTextField txtNombre;
    private final JTextField txtModelo;
    private final JComboBox<String> cmbMarca;
    private final JComboBox<String> cmbCategoria;
    private final JComboBox<String> cmbProveedor;
    // Precios
    private final JTextField txtMercadoMin;
    private final JTextField txtMercadoProm;
    private final JTextField txtMercadoMax;
    private final JTextField txtCosto;
    private final JTextField txtPrecio;
    private final JTextField txtStock;
    private final JLabel lblMargenCalc;
    private final TablaPro tabla;
    private final DefaultTableModel modelo;
    // Botones Personalizados (Guardamos referencia al de guardar para cambiarle el texto)
    private final BotonPro btnAccionPrincipal;
    private final JTextField txtBuscar;

    public PanelInventario() {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        // --- FORMULARIO IZQUIERDO ---
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Estilos.COLOR_PANEL);
        panelForm.setBorder(BorderFactory.createTitledBorder(null, "Datos del Producto", 0, 0, Estilos.FONT_BOLD, Color.WHITE));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        txtId = crearInput(false, false); // ID oculto o no editable
        txtCodigo = crearInput(true, false);
        txtNombre = crearInput(true, true);
        txtModelo = crearInput(true, true);

        String[] categorias = {"Cables y adaptadores", "Accesorios pequeños", "Periféricos", "Refacciones", "Hardware", "Equipos grandes"};
        cmbCategoria = new JComboBox<>(categorias);
        cmbCategoria.setFont(Estilos.FONT_PLAIN);
        cmbProveedor = new JComboBox<>();
        cmbProveedor.setEditable(true);
        cmbProveedor.setFont(Estilos.FONT_PLAIN);
        cmbMarca = new JComboBox<>();
        cmbMarca.setEditable(true);
        cmbMarca.setFont(Estilos.FONT_PLAIN);

        // Hack para estilizar el editor del combo
        Component editor = cmbMarca.getEditor().getEditorComponent();
        Component editor2 = cmbProveedor.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            setMayusculas((JTextField) editor); // También forzar mayúsculas aquí
            setMayusculas((JTextField) editor2);
        }

        // --- BOTÓN PARA GENERAR CÓDIGO DE BARRAS ---
        BotonPro btnGenCode = new BotonPro("Generar", Estilos.COLOR_INPUT, this::generarCodigoAleatorio);
        btnGenCode.setPreferredSize(new Dimension(70, 25));

        JPanel pCodigo = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pCodigo.setBackground(Estilos.COLOR_PANEL);

        pCodigo.add(txtCodigo);
        pCodigo.add(btnGenCode);

        addCampo(panelForm, g, 0, 0, "ID (Auto):", txtId);
        addCampo(panelForm, g, 0, 1, "Código:", pCodigo);

        addCampo(panelForm, g, 0, 2, "Nombre:", txtNombre);
        addCampo(panelForm, g, 0, 3, "Marca:", cmbMarca);
        addCampo(panelForm, g, 0, 4, "Modelo:", txtModelo);
        addCampo(panelForm, g, 0, 5, "Categoría:", cmbCategoria);
        addCampo(panelForm, g, 0, 6, "Proveedor:", cmbProveedor);

        // --- FORMULARIO DERECHO (Precios) ---
        JPanel panelPrecios = new JPanel(new GridBagLayout());
        panelPrecios.setBackground(Estilos.COLOR_PANEL);
        panelPrecios.setBorder(BorderFactory.createTitledBorder(null, "Estrategia de Precios", 0, 0, Estilos.FONT_BOLD, Color.WHITE));

        txtCosto = crearInput(true, false);
        // Botón pequeño para IVA
        BotonPro btnIVA = new BotonPro("+16%", Estilos.COLOR_INPUT, this::calcIVA);
        btnIVA.setPreferredSize(new Dimension(60, 30));

        txtMercadoMin = crearInput(true, false);
        txtMercadoMin.setText("0");
        txtMercadoMax = crearInput(true, false);
        txtMercadoMax.setText("0");
        txtMercadoProm = crearInput(false, false);
        txtMercadoProm.setText("0"); // Promedio es automático ahora

        // LISTENER MÁGICO: Calcular promedio al escribir
        DocumentListener calculadorPromedio = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                calcularPromedioAuto();
            }

            public void removeUpdate(DocumentEvent e) {
                calcularPromedioAuto();
            }

            public void changedUpdate(DocumentEvent e) {
                calcularPromedioAuto();
            }
        };
        txtMercadoMin.getDocument().addDocumentListener(calculadorPromedio);
        txtMercadoMax.getDocument().addDocumentListener(calculadorPromedio);

        // Botones Funcionales
        BotonPro btnInvestigar = new BotonPro("Web", "lupa.png", new Color(255, 140, 0), this::investigarWeb);
        BotonPro btnCalcular = new BotonPro("Calcular Precio","rayo.png" , new Color(46, 204, 113), this::ejecutarAlgoritmoPrecio);

        txtPrecio = crearInput(true, false);
        txtStock = crearInput(true, false);
        lblMargenCalc = new JLabel("Margen: 0%");
        lblMargenCalc.setForeground(Color.YELLOW);

        // Layout Derecho
        addLabel(panelPrecios, g, 0, 0, "Costo:");
        JPanel pCosto = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pCosto.setBackground(Estilos.COLOR_PANEL);
        pCosto.add(txtCosto);
        pCosto.add(Box.createHorizontalStrut(5));
        pCosto.add(btnIVA);
        g.gridx = 1;
        panelPrecios.add(pCosto, g);

        addLabel(panelPrecios, g, 0, 1, "Mínimo Web:");
        g.gridx = 1;
        panelPrecios.add(txtMercadoMin, g);
        addLabel(panelPrecios, g, 0, 2, "Máximo Web:");
        g.gridx = 1;
        panelPrecios.add(txtMercadoMax, g);
        addLabel(panelPrecios, g, 0, 3, "Promedio (Auto):");
        g.gridx = 1;
        panelPrecios.add(txtMercadoProm, g);

        g.gridx = 1;
        g.gridy = 4;
        panelPrecios.add(btnInvestigar, g); // Botón naranja

        g.gridwidth = 2;
        g.gridx = 0;
        g.gridy = 5;
        panelPrecios.add(new JSeparator(), g);
        g.gridwidth = 1;
        g.gridx = 1;
        g.gridy = 6;
        panelPrecios.add(btnCalcular, g); // Botón verde

        addLabel(panelPrecios, g, 0, 7, "Precio VENTA:");
        g.gridx = 1;
        panelPrecios.add(txtPrecio, g);
        g.gridx = 1;
        g.gridy = 8;
        panelPrecios.add(lblMargenCalc, g);
        addLabel(panelPrecios, g, 0, 9, "Stock:");
        g.gridx = 1;
        panelPrecios.add(txtStock, g);

        // --- C. Botones Guardar/Limpiar (Ahora debajo de los formularios) ---
        JPanel panelBotonesForm = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelBotonesForm.setBackground(Estilos.COLOR_PANEL);
        panelBotonesForm.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Estilos.COLOR_BORDER)); // Línea superior sutil

        btnAccionPrincipal = new BotonPro("GUARDAR PRODUCTO", Estilos.COLOR_ACCENT, this::guardarOActualizar);
        panelBotonesForm.add(new BotonPro("Limpiar / Nuevo", Color.GRAY, this::limpiar));
        panelBotonesForm.add(btnAccionPrincipal);



        // Contenedor de Formularios (Grid 1x2)
        JPanel panelFormulariosContainer = new JPanel(new GridLayout(1, 2, 10, 0));
        panelFormulariosContainer.setBackground(Estilos.COLOR_FONDO);
        panelFormulariosContainer.add(panelForm);
        panelFormulariosContainer.add(panelPrecios);

        // Armado del Panel Norte Completo
        JPanel panelNorteGlobal = new JPanel(new BorderLayout());
        panelNorteGlobal.add(panelFormulariosContainer, BorderLayout.CENTER);
        panelNorteGlobal.add(panelBotonesForm, BorderLayout.SOUTH);

        // =================================================================
        //  PANEL CENTRAL: BÚSQUEDA, TABLA Y BOTÓN ELIMINAR
        // =================================================================

        JPanel panelHerramientas = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelHerramientas.setBackground(Estilos.COLOR_PANEL);
        panelHerramientas.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Estilos.COLOR_BORDER));

        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setForeground(Color.WHITE);
        lblBuscar.setFont(Estilos.FONT_BOLD);

        txtBuscar = new JTextField(20);
        Estilos.estilizarInput(txtBuscar);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Código o Nombre...");
        txtBuscar.addActionListener(e -> filtrarInventario());

        BotonPro btnBuscar = new BotonPro("IR", Estilos.COLOR_ACCENT, this::filtrarInventario);
        BotonPro btnTodo = new BotonPro("TODO", Color.DARK_GRAY, this::cargarTabla);

        // --- AQUÍ MOVIMOS EL BOTÓN ELIMINAR ---
        BotonPro btnEliminar = new BotonPro("ELIMINAR SELECCIONADO", new Color(200, 50, 50), this::eliminar);

        panelHerramientas.add(lblBuscar);
        panelHerramientas.add(txtBuscar);
        panelHerramientas.add(btnBuscar);
        panelHerramientas.add(btnTodo);
        // Separador visual para alejar el botón eliminar
        panelHerramientas.add(Box.createHorizontalStrut(30));
        panelHerramientas.add(btnEliminar);



        // --- TABLA ---
        modelo = new DefaultTableModel(new String[]{"ID", "Nombre", "Marca", "Venta", "Stock"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tabla = new TablaPro(modelo);
        Estilos.estilizarTabla(tabla);

        // --- MENÚ CONTEXTUAL ---
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem itemEtiqueta = new JMenuItem("🖨 Imprimir String (Consola)");
        itemEtiqueta.addActionListener(e -> imprimirEtiquetaSeleccionada());

        JMenuItem itemImagen = new JMenuItem("Generar Imagen PNG");
        itemImagen.setFont(Estilos.FONT_BOLD);
        itemImagen.addActionListener(e -> generarImagenProducto());

        // 2. NUEVO: Código de Barras (Interno)
        JMenuItem itemBarras = new JMenuItem("||| Generar Código de Barras");
        itemBarras.setFont(Estilos.FONT_BOLD);
        itemBarras.addActionListener(e -> generarCodigoBarrasInterno());

        popupMenu.add(itemEtiqueta);
        popupMenu.add(new JSeparator());
        popupMenu.add(itemImagen);
        popupMenu.add(new JSeparator());
        popupMenu.add(itemBarras);


        tabla.setComponentPopupMenu(popupMenu);

        tabla.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                llenarDatosDesdeTabla();
            }
        });

        // Contenedor Central
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.add(panelHerramientas, BorderLayout.NORTH);
        panelCentral.add(new JScrollPane(tabla), BorderLayout.CENTER);



        add(panelNorteGlobal, BorderLayout.NORTH);
        add(panelCentral, BorderLayout.CENTER); // Antes agregabas solo el JScrollPane, ahora el panelCentral


        cargarProveedores();
        cargarCombos();
        cargarTabla();
    }

    private void filtrarInventario() {
        String texto = txtBuscar.getText().trim();

        // Si está vacío, cargamos todo normal
        if (texto.isEmpty()) {
            cargarTabla();
            return;
        }

        modelo.setRowCount(0); // Limpiar tabla
        try (Connection conn = ConexionBD.conectar()) {
            // LÓGICA SQL:
            // 1. (codigo_barras = ?) -> Búsqueda exacta y prioritaria.
            // 2. OR (nombre LIKE ?) -> Búsqueda parcial.
            // 3. AND activo = 1 -> Solo productos no eliminados.
            String sql = "SELECT * FROM productos WHERE (codigo_barras = ? OR nombre LIKE ?) AND activo = 1";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, texto);           // Parámetro 1: Texto exacto (para código)
            ps.setString(2, "%" + texto + "%"); // Parámetro 2: %Texto% (para nombre)

            ResultSet rs = ps.executeQuery();

            boolean hayResultados = false;
            while (rs.next()) {
                hayResultados = true;
                modelo.addRow(new Object[]{rs.getInt("id"), rs.getString("nombre"), rs.getString("marca"), rs.getDouble("precio"), rs.getInt("stock")});
            }

            if (!hayResultados) {
                JOptionPanePro.mostrarMensaje(this, "Sin Resultados", "No se encontró ningún producto con: " + texto, "INFO");
                // Opcional: cargarTabla(); si quieres resetear
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- LÓGICA AUTOMÁTICA ---
    private void calcularPromedioAuto() {
        try {
            double min = Double.parseDouble(txtMercadoMin.getText());
            double max = Double.parseDouble(txtMercadoMax.getText());
            double prom = (min + max) / 2;
            txtMercadoProm.setText(String.format("%.2f", prom).replace(",", "."));
        } catch (Exception e) {
            // Si hay error (campos vacíos), ponemos 0 o no hacemos nada
        }
    }

    // --- CRUD ---
    private void guardarOActualizar() {
        if (txtNombre.getText().isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Error", "El nombre es obligatorio", "ERROR");
            return;
        }


        if (txtId.getText().isEmpty()) {
            ejecutarSQL("INSERT");
        } else {
            ejecutarSQL("UPDATE");
        }

        cargarTabla();
        cargarCombos(); // Recargar marcas nuevas
        limpiar();
    }

    private void eliminar() {
        if (txtId.getText().isEmpty()) return;
        boolean confirm = JOptionPanePro.mostrarConfirmacion(this, "Eliminar", "¿Seguro que deseas eliminar este producto?");
        if (confirm) {
            ejecutarSQL("DELETE");
            JOptionPanePro.mostrarMensaje(this, "Eliminado", "Producto eliminado.", "ADVERTENCIA");
            cargarTabla();
            limpiar();
        }
    }

    private void ejecutarSQL(String accion) {
        try (Connection conn = ConexionBD.conectar()) {
            String sql = "";
            PreparedStatement ps;

            if (accion.equals("INSERT")) {
                sql = "INSERT INTO productos (codigo_barras, nombre, marca, modelo, categoria, proveedor, costo, precio, stock, precio_mercado_min, precio_mercado_prom, precio_mercado_max, margen_ganancia) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
                ps = conn.prepareStatement(sql);
                // ... (Parámetros 1 al 13)
                llenarParams(ps);
                ps.executeUpdate();
                JOptionPanePro.mostrarMensaje(this, "Éxito", "Guardado Exitosamente", "INFO");

            } else if (accion.equals("UPDATE")) {
                sql = "UPDATE productos SET codigo_barras=?, nombre=?, marca=?, modelo=?, categoria=?, proveedor=?, costo=?, precio=?, stock=?, precio_mercado_min=?, precio_mercado_prom=?, precio_mercado_max=?, margen_ganancia=? WHERE id=?";
                ps = conn.prepareStatement(sql);
                llenarParams(ps);
                ps.setInt(14, Integer.parseInt(txtId.getText())); // El ID para el WHERE
                ps.executeUpdate();
                JOptionPanePro.mostrarMensaje(this, "Éxito", "Producto Actualizado", "INFO");

            } else if (accion.equals("DELETE")) {
                sql = "UPDATE productos SET activo = 0 WHERE id = ?";
                ps = conn.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(txtId.getText()));
                ps.executeUpdate();
                JOptionPanePro.mostrarMensaje(this, "Éxito", "Producto Eliminado", "INFO");

            }

            cargarTabla();
            limpiar();
        } catch (Exception ex) {
            JOptionPanePro.mostrarMensaje(this, "Éxito", ex.getMessage(), "ERROR");

            ex.printStackTrace();
        }
    }

    private void llenarParams(PreparedStatement ps) throws SQLException {
        ps.setString(1, txtCodigo.getText());
        ps.setString(2, txtNombre.getText());
        ps.setString(3, (String) cmbMarca.getSelectedItem());
        ps.setString(4, txtModelo.getText());
        ps.setString(5, (String) cmbCategoria.getSelectedItem());
        ps.setString(6, (String) cmbProveedor.getSelectedItem());
        ps.setDouble(7, parser(txtCosto.getText()));
        ps.setDouble(8, parser(txtPrecio.getText()));
        ps.setInt(9, (int) parser(txtStock.getText()));
        ps.setDouble(10, parser(txtMercadoMin.getText()));
        ps.setDouble(11, parser(txtMercadoProm.getText())); // Se guarda el calculado
        ps.setDouble(12, parser(txtMercadoMax.getText()));

        // Obtener el margen limpio
        String m = lblMargenCalc.getText().replaceAll("[^0-9.]", "");
        ps.setDouble(13, m.isEmpty() ? 0 : Double.parseDouble(m));
    }

    private double parser(String t) {
        try {
            return Double.parseDouble(t);
        } catch (Exception e) {
            return 0;
        }
    }

    // --- CARGAS Y EVENTOS ---
    private void llenarDatosDesdeTabla() {
        int r = tabla.getSelectedRow();
        if (r == -1) return;

        // Obtenemos ID de la tabla para buscar en BD todos los datos
        // (La tabla a veces no muestra todo, mejor hacer query por ID para llenar exacto)
        int id = Integer.parseInt(modelo.getValueAt(r, 0).toString());

        try (Connection conn = ConexionBD.conectar()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM productos WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtId.setText(String.valueOf(rs.getInt("id")));
                txtCodigo.setText(rs.getString("codigo_barras"));
                txtNombre.setText(rs.getString("nombre"));
                cmbMarca.setSelectedItem(rs.getString("marca"));
                txtModelo.setText(rs.getString("modelo"));
                cmbCategoria.setSelectedItem(rs.getString("categoria"));
                cmbProveedor.setSelectedItem(rs.getString("proveedor"));
                txtCosto.setText(String.valueOf(rs.getDouble("costo")));
                txtPrecio.setText(String.valueOf(rs.getDouble("precio")));
                txtStock.setText(String.valueOf(rs.getInt("stock")));
                txtMercadoMin.setText(String.valueOf(rs.getDouble("precio_mercado_min")));
                txtMercadoProm.setText(String.valueOf(rs.getDouble("precio_mercado_prom")));
                txtMercadoMax.setText(String.valueOf(rs.getDouble("precio_mercado_max")));
                lblMargenCalc.setText("Margen: " + rs.getDouble("margen_ganancia") + "%");

                // CAMBIAR BOTÓN A MODO EDICIÓN
                btnAccionPrincipal.setTexto("ACTUALIZAR DATOS");
                btnAccionPrincipal.setBackground(new Color(255, 140, 0)); // Naranja
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void limpiar() {
        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        cmbMarca.setSelectedItem("");
        txtModelo.setText("");
        txtCosto.setText("");
        txtPrecio.setText("");
        txtStock.setText("");
        txtMercadoMin.setText("0");
        txtMercadoProm.setText("0");
        txtMercadoMax.setText("0");
        lblMargenCalc.setText("Margen: 0%");
        tabla.clearSelection();

        // REGRESAR BOTÓN A MODO NUEVO
        btnAccionPrincipal.setTexto("GUARDAR NUEVO");
        btnAccionPrincipal.setBackground(Estilos.COLOR_ACCENT); // Azul
    }

    // --- Helpers UI / Web / Algoritmo se mantienen igual o llaman a funciones existentes ---
    private JTextField crearInput(boolean editable, boolean forzarMayus) {
        JTextField t = new JTextField(10);
        Estilos.estilizarInput(t);
        t.setEditable(editable);
        if (forzarMayus) setMayusculas(t);
        return t;
    }

    private void setMayusculas(JTextField t) {
        ((AbstractDocument) t.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                super.insertString(fb, offset, string.toUpperCase(), attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                super.replace(fb, offset, length, text.toUpperCase(), attrs);
            }
        });
    }

    private void addCampo(JPanel p, GridBagConstraints g, int c, int r, String l, Component o) {
        g.gridx = 0;
        g.gridy = r;
        JLabel lbl = new JLabel(l);
        lbl.setForeground(Color.WHITE);
        p.add(lbl, g);
        g.gridx = 1;
        p.add(o, g);
    }

    private void addLabel(JPanel p, GridBagConstraints g, int x, int y, String t) {
        g.gridx = x;
        g.gridy = y;
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        p.add(l, g);
    }

    private void calcIVA() {
        try {
            double c = Double.parseDouble(txtCosto.getText());
            txtCosto.setText(String.format("%.2f", c * 1.16).replace(",", "."));
        } catch (Exception e) {
        }
    }

    private void investigarWeb() {
        try {
            Desktop.getDesktop().browse(new URI("https://www.google.com/search?tbm=shop&q=" + (txtNombre.getText() + " " + txtModelo.getText()).replace(" ", "+")));
        } catch (Exception e) {
        }
    }

    private void ejecutarAlgoritmoPrecio() {
        try {
            double costo = Double.parseDouble(txtCosto.getText());
            double min = Double.parseDouble(txtMercadoMin.getText());
            double prom = Double.parseDouble(txtMercadoProm.getText());
            double max = Double.parseDouble(txtMercadoMax.getText());

            // Check para Algoritmo Inverso (Usuario ya definió el precio de venta)
            double precioUsuario = 0;
            try {
                precioUsuario = Double.parseDouble(txtPrecio.getText());
            } catch (Exception e) {
            }

            if (precioUsuario > 0 && min == 0 && prom == 0 && max == 0) {
                // CASO INVERSO: Calcular Margen basado en Precio Venta
                if (costo >= precioUsuario) {
                    JOptionPanePro.mostrarMensaje(this, "¡Cuidado!", "El precio de venta es menor o igual al costo.", "ADVERTENCIA");

                }

                double margenCalculado = ((precioUsuario - costo) / costo) * 100;
                lblMargenCalc.setText("Margen: " + String.format("%.2f", margenCalculado) + "%");
                //ElementosPro.JOptionPanePro.mostrarMensaje(this, "Cálculo Inverso", "Margen calculado según tu precio de venta.", "INFO");


            } else {
                // CASO NORMAL: Calcular Precio basado en Mercado
                String cat = (String) cmbCategoria.getSelectedItem();
                AlgoritmoPrecios.ResultadoPrecio res = AlgoritmoPrecios.calcular(cat, costo, min, prom, max);

                txtPrecio.setText(String.format("%.2f", res.precioSugerido).replace(",", "."));
                lblMargenCalc.setText("Margen: " + String.format("%.1f", res.margenAplicado) + "%");
                JOptionPanePro.mostrarMensaje(this, "", res.explicacion, "INFO");

            }

        } catch (Exception e) {
            JOptionPanePro.mostrarMensaje(this, "", "Revisa los valores numéricos", "ADVERTENCIA");
        }
    }

    private void cargarProveedores() { /* Mismo código de antes */
        cmbProveedor.removeAllItems();
        try (Connection c = ConexionBD.conectar(); Statement s = c.createStatement()) {
            ResultSet r = s.executeQuery("SELECT DISTINCT proveedor FROM productos");
            while (r.next()) cmbProveedor.addItem(r.getString(1));
        } catch (Exception e) {
        }
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        try (Connection c = ConexionBD.conectar(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM productos WHERE activo=1");
            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getInt("id"), rs.getString("nombre"), rs.getString("marca"), rs.getDouble("precio"), rs.getInt("stock")});
            }
        } catch (Exception e) {
        }

        // AJUSTAR TAMAÑOS (Solo funciona después de cargar o inicializar)
        if (tabla.getColumnModel().getColumnCount() > 0) {
            // ID pequeño
            tabla.getColumnModel().getColumn(0).setMinWidth(40);
            tabla.getColumnModel().getColumn(0).setMaxWidth(60);
            tabla.getColumnModel().getColumn(0).setPreferredWidth(50);

            // Nombre grande
            tabla.getColumnModel().getColumn(1).setPreferredWidth(300);
        }
    }

    private void imprimirEtiquetaSeleccionada() {
        int row = tabla.getSelectedRow();
        if (row == -1) return;

        String nombre = modelo.getValueAt(row, 1).toString();
        String precio = modelo.getValueAt(row, 3).toString();

        String etiqueta = "=== ETIQUETA ===\n" + nombre + "\n" + "$" + precio + "\n" + "================";

        // Aquí podrías mandarlo a una impresora real, por ahora ElementosPro.JOptionPanePro
        JOptionPanePro.mostrarMensaje(this, "Impresión de Etiqueta", etiqueta, "INFO");
        System.out.println(etiqueta);
    }

    private void cargarCombos() {
        cmbProveedor.removeAllItems();
        cmbMarca.removeAllItems();
        try (Connection c = ConexionBD.conectar(); Statement s = c.createStatement()) {
            // Proveedores
            ResultSet rs = s.executeQuery("SELECT DISTINCT proveedor FROM productos");
            while (rs.next()) cmbProveedor.addItem(rs.getString(1));
            // Marcas
            rs = s.executeQuery("SELECT DISTINCT marca FROM productos");
            while (rs.next()) {
                String m = rs.getString(1);
                if (m != null && !m.isEmpty()) cmbMarca.addItem(m);
            }
        } catch (Exception e) {
        }
    }

    private void generarImagenProducto() {
        int row = tabla.getSelectedRow();
        if (row == -1) {
            // Prueba del manejo de null en ElementosPro.JOptionPanePro
            JOptionPanePro.mostrarMensaje(null, "Selección", "Por favor selecciona un producto.", "ADVERTENCIA");
            return;
        }

        String nombreCompleto = modelo.getValueAt(row, 1).toString();
        String precio = "$" + modelo.getValueAt(row, 3).toString() + "0";

        // --- LÓGICA DE DIVISIÓN DE TEXTO ---
        String[] palabras = nombreCompleto.split("\\s+"); // Separa por espacios
        String linea1 = "";
        String linea2 = "";

        if (palabras.length == 1) {
            // CASO 1: Una sola palabra -> Todo en la línea 1 (o centrado)
            linea2 = palabras[0]; // Lo ponemos en la linea 2 visualmente para que quede cerca del precio o usamos una sola linea
            // O mejor, si es una sola palabra, la ponemos como línea única centrada
            linea1 = "";
            linea2 = palabras[0];
        } else {
            // CASO 2: Varias palabras -> Aplicar regla de impares abajo
            int mitad = palabras.length / 2; // División entera redondea abajo (3/2 = 1) -> Cumple requisito

            StringBuilder sb1 = new StringBuilder();
            for (int i = 0; i < mitad; i++) {
                sb1.append(palabras[i]).append(" ");
            }

            StringBuilder sb2 = new StringBuilder();
            for (int i = mitad; i < palabras.length; i++) {
                sb2.append(palabras[i]).append(" ");
            }

            linea1 = sb1.toString().trim();
            linea2 = sb2.toString().trim();
        }

        // --- CONFIGURACIÓN DE LÍNEAS PARA LA IMAGEN ---
        List<GeneradorEtiquetas.LineaTexto> lineas = new ArrayList<>();

        // Configuración visual (Asumiendo plantilla de alto aprox 300-400px)
        // Ajusta los valores de posY según tu imagen de fondo real.

        if (!linea1.isEmpty()) {
            // Renglón Superior (Ajuste Automático)
            // Ajusté posY a 90 para dar espacio hacia arriba
            lineas.add(new GeneradorEtiquetas.LineaTexto(linea1, GeneradorEtiquetas.TAMANIO_AUTO, true, 180, Color.WHITE));

            // Renglón Inferior (Ajuste Automático)
            // Ajusté posY a 150 para que no choque con la línea de arriba si esta sale muy grande
            lineas.add(new GeneradorEtiquetas.LineaTexto(linea2, GeneradorEtiquetas.TAMANIO_AUTO, true, 300, Color.WHITE));
        } else {
            // Si solo había una palabra, la ponemos centrada y con ajuste automático también
            lineas.add(new GeneradorEtiquetas.LineaTexto(linea2, GeneradorEtiquetas.TAMANIO_AUTO, true, 120, Color.WHITE));
        }

        // Precio (Se mantiene tu configuración de color y posición que te gustó)
        // posY 220 suele funcionar bien para dejarlo al pie de la imagen
        lineas.add(new GeneradorEtiquetas.LineaTexto(precio, 145f, true, 480, new Color(255, 226, 153))); // Precio grande y azul

        // Llamada al generador
        GeneradorEtiquetas.crearImagenEtiqueta("recursos/plantilla.png", "recursos/fuentes/fuente.ttf", nombreCompleto, lineas);
    }

    private void generarCodigoAleatorio() {
        // Genera un código tipo: LUM-83921
        long numero = System.currentTimeMillis() % 100000; // Últimos 5 dígitos del tiempo
        String codigo = "LUM-" + numero;
        txtCodigo.setText(codigo);
    }

    private void generarCodigoBarrasInterno() {
        int row = tabla.getSelectedRow();
        if (row == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Selecciona un producto.", "ADVERTENCIA");
            return;
        }

        // Obtener datos de la tabla
        // Asumiendo columnas: 0=ID, 1=Nombre, 2=Marca, 3=Precio, 4=Stock
        // Necesitamos el código, pero en la tabla visual a veces no está o está oculto.
        // Lo más seguro es obtenerlo del modelo si está ahí, o consultarlo por ID.

        // Si tu tabla TIENE la columna código visible u oculta, úsala.
        // Si no, lo consultamos rápido a BD por el ID:
        int id = Integer.parseInt(modelo.getValueAt(row, 0).toString());
        String nombre = modelo.getValueAt(row, 1).toString();
        String codigo = obtenerCodigoPorId(id);

        if (codigo != null && !codigo.isEmpty()) {
            GeneradorCodigoBarras.crearEtiquetaBarras(codigo, nombre);
        } else {
            JOptionPanePro.mostrarMensaje(this, "Error", "Este producto no tiene código registrado.", "ERROR");
        }
    }

    // Helper rápido para asegurar que tenemos el código correcto desde la BD
    private String obtenerCodigoPorId(int id) {
        try (java.sql.Connection conn = ConexionBD.conectar()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT codigo_barras FROM productos WHERE id = ?");
            ps.setInt(1, id);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("codigo_barras");
        } catch (Exception e) {}
        return null;
    }
}
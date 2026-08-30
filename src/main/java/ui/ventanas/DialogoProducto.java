package ui.ventanas;

import config.ConexionBD;
import servicios.AlgoritmoPrecios; // Importar tu servicio
import ui.componentes.BotonPro;
import ui.componentes.JOptionPanePro;
import util.Estilos;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URI;
import java.sql.*;

public class DialogoProducto extends JDialog {

    // Campos del Formulario Izquierdo
    private JTextField txtId, txtCodigo, txtNombre, txtModelo;
    private JComboBox<String> cmbMarca, cmbCategoria, cmbProveedor;
    private BotonPro btnCodigosExtra;
    private BotonPro btnAjustarStock;

    // Campos del Formulario Derecho (Precios)
    private JTextField txtCosto, txtMercadoMin, txtMercadoMax, txtMercadoProm, txtPrecio, txtStock;
    private JLabel lblMargenCalc;
    private BotonPro btnGuardar;

    private final int idProductoActual;
    private final Runnable callbackRefrescarTabla;

    public DialogoProducto(Frame parent, int idProducto, Runnable callbackRefrescarTabla) {
        super(parent, idProducto == -1 ? "Nuevo Producto" : "Editar Detalles del Producto", true);
        this.idProductoActual = idProducto;
        this.callbackRefrescarTabla = callbackRefrescarTabla;

        initUI();

        // 1. Cargar Combos primero para que al setear el texto no falle
        cargarCombosDeBD();

        // 2. Cargar Datos si es edición
        if (idProductoActual != -1) {
            cargarDatosDesdeBD();
            configurarModoEdicion();
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Estilos.COLOR_FONDO);

        // Contenedor principal
        JPanel panelContenedorForm = new JPanel(new GridLayout(1, 2, 10, 0));
        panelContenedorForm.setBackground(Estilos.COLOR_FONDO);
        panelContenedorForm.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- LADO IZQUIERDO: DATOS ---
        JPanel panelIzquierdo = new JPanel(new GridBagLayout());
        panelIzquierdo.setBackground(Estilos.COLOR_PANEL);
        panelIzquierdo.setBorder(BorderFactory.createTitledBorder(null, "Datos del Producto", 0, 0, Estilos.FONT_BOLD, Color.WHITE));

        txtId = new JTextField(15); txtId.setEnabled(false); Estilos.estilizarInput(txtId);
        txtCodigo = new JTextField(15); Estilos.estilizarInput(txtCodigo);
        txtNombre = new JTextField(15); Estilos.estilizarInput(txtNombre);
        txtModelo = new JTextField(15); Estilos.estilizarInput(txtModelo);

        // --- NUEVO: PANEL COMBINADO PARA CÓDIGO + BOTÓN EXTRA ---
        JPanel pCodigoCombinado = new JPanel(new BorderLayout(5, 0));
        pCodigoCombinado.setOpaque(false);
        pCodigoCombinado.add(txtCodigo, BorderLayout.CENTER);

        // Botón azul de acento para abrir tu DialogoCodigos
        btnCodigosExtra = new BotonPro("+", Estilos.COLOR_ACCENT, this::abrirGestionCodigosExtra);
        btnCodigosExtra.setPreferredSize(new Dimension(45, 30));
        btnCodigosExtra.setToolTipText("Administrar códigos de barras adicionales (colores, lotes, variantes)");
        pCodigoCombinado.add(btnCodigosExtra, BorderLayout.EAST);

        String[] categorias = {"Cables y adaptadores", "Accesorios pequeños", "Periféricos", "Refacciones", "Hardware", "Equipos grandes"};
        cmbCategoria = new JComboBox<>(categorias); cmbCategoria.setFont(Estilos.FONT_PLAIN);
        cmbMarca = new JComboBox<>(); cmbMarca.setEditable(true); cmbMarca.setFont(Estilos.FONT_PLAIN);
        cmbProveedor = new JComboBox<>(); cmbProveedor.setEditable(true); cmbProveedor.setFont(Estilos.FONT_PLAIN);

        GridBagConstraints gIzq = new GridBagConstraints();
        gIzq.insets = new Insets(5, 5, 5, 5); gIzq.fill = GridBagConstraints.HORIZONTAL;

        // Si el ID es -1, mostramos "Auto", de lo contrario, mostramos el ID
        txtId.setText(idProductoActual == -1 ? "Auto" : String.valueOf(idProductoActual));

        addCampo(panelIzquierdo, gIzq, 0, "ID (Auto):", txtId);
        addCampo(panelIzquierdo, gIzq, 1, "Código:", pCodigoCombinado);
        addCampo(panelIzquierdo, gIzq, 2, "Nombre:", txtNombre);
        addCampo(panelIzquierdo, gIzq, 3, "Marca:", cmbMarca);
        addCampo(panelIzquierdo, gIzq, 4, "Modelo:", txtModelo);
        addCampo(panelIzquierdo, gIzq, 5, "Categoría:", cmbCategoria);
        addCampo(panelIzquierdo, gIzq, 6, "Proveedor:", cmbProveedor);

        // --- LADO DERECHO: ESTRATEGIA DE PRECIOS ---
        JPanel panelDerecho = new JPanel(new GridBagLayout());
        panelDerecho.setBackground(Estilos.COLOR_PANEL);
        panelDerecho.setBorder(BorderFactory.createTitledBorder(null, "Estrategia de Precios", 0, 0, Estilos.FONT_BOLD, Color.WHITE));

        txtCosto = new JTextField(10); Estilos.estilizarInput(txtCosto);
        txtMercadoMin = new JTextField(15); Estilos.estilizarInput(txtMercadoMin); txtMercadoMin.setText("0");
        txtMercadoMax = new JTextField(15); Estilos.estilizarInput(txtMercadoMax); txtMercadoMax.setText("0");
        txtMercadoProm = new JTextField(15); Estilos.estilizarInput(txtMercadoProm); txtMercadoProm.setText("0"); txtMercadoProm.setEnabled(false);
        txtPrecio = new JTextField(15); Estilos.estilizarInput(txtPrecio);
        txtStock = new JTextField(15); Estilos.estilizarInput(txtStock);



        lblMargenCalc = new JLabel("Margen: 0%");
        lblMargenCalc.setForeground(Color.YELLOW);

        JPanel pStockCombinado = new JPanel(new BorderLayout(5, 0));
        pStockCombinado.setOpaque(false);
        pStockCombinado.add(txtStock, BorderLayout.CENTER);

        btnAjustarStock = new BotonPro("✎", new Color(255, 140, 0), this::abrirAjusteStockAdmin);
        btnAjustarStock.setPreferredSize(new Dimension(45, 30));
        btnAjustarStock.setToolTipText("Ajuste manual de inventario (Requiere PIN)");
        btnAjustarStock.setEnabled(false); // Apagado por defecto para productos nuevos
        pStockCombinado.add(btnAjustarStock, BorderLayout.EAST);

        // Panel combinado para Costo + IVA
        JPanel pCosto = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pCosto.setBackground(Estilos.COLOR_PANEL);
        pCosto.add(txtCosto);
        pCosto.add(Box.createHorizontalStrut(5));
        BotonPro btnIVA = new BotonPro("+16%", Estilos.COLOR_INPUT, this::calcIVA);
        btnIVA.setPreferredSize(new Dimension(60, 30));
        pCosto.add(btnIVA);

        // Botones Funcionales de Precios
        BotonPro btnInvestigar = new BotonPro("Web", "lupa.png", new Color(255, 140, 0), this::investigarWeb);
        BotonPro btnCalcular = new BotonPro("Calcular Precio", "rayo.png", new Color(46, 204, 113), this::ejecutarAlgoritmoPrecio);

        // Listener Promedio
        DocumentListener calculadorPromedio = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calcularPromedioAuto(); }
            public void removeUpdate(DocumentEvent e) { calcularPromedioAuto(); }
            public void changedUpdate(DocumentEvent e) { calcularPromedioAuto(); }
        };
        txtMercadoMin.getDocument().addDocumentListener(calculadorPromedio);
        txtMercadoMax.getDocument().addDocumentListener(calculadorPromedio);

        GridBagConstraints gDer = new GridBagConstraints();
        gDer.insets = new Insets(5, 5, 5, 5); gDer.fill = GridBagConstraints.HORIZONTAL;

        addCampo(panelDerecho, gDer, 0, "Costo:", pCosto);
        addCampo(panelDerecho, gDer, 1, "Mínimo Web:", txtMercadoMin);
        addCampo(panelDerecho, gDer, 2, "Máximo Web:", txtMercadoMax);
        addCampo(panelDerecho, gDer, 3, "Promedio (Auto):", txtMercadoProm);


        gDer.gridx = 1; gDer.gridy = 4; panelDerecho.add(btnInvestigar, gDer);

        gDer.gridwidth = 2; gDer.gridx = 0; gDer.gridy = 5; panelDerecho.add(new JSeparator(), gDer);
        gDer.gridwidth = 1; gDer.gridx = 1; gDer.gridy = 6; panelDerecho.add(btnCalcular, gDer);

        addCampo(panelDerecho, gDer, 7, "Precio VENTA:", txtPrecio);

        gDer.gridx = 1; gDer.gridy = 8; panelDerecho.add(lblMargenCalc, gDer);
        addCampo(panelDerecho, gDer, 9, "Stock Inicial:", pStockCombinado);

        panelContenedorForm.add(panelIzquierdo);
        panelContenedorForm.add(panelDerecho);

        // --- BOTONES INFERIORES ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelBotones.setBackground(Estilos.COLOR_PANEL);
        panelBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Estilos.COLOR_BORDER));

        String textoBoton = idProductoActual == -1 ? "GUARDAR NUEVO PRODUCTO" : "ACTUALIZAR CAMBIOS";
        btnGuardar = new BotonPro(textoBoton, Estilos.COLOR_ACCENT, this::guardarOActualizar);
        panelBotones.add(new BotonPro("Cancelar", Color.GRAY, this::dispose));
        panelBotones.add(btnGuardar);

        add(panelContenedorForm, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void addCampo(JPanel panel, GridBagConstraints g, int y, String labelText, Component comp) {
        g.gridx = 0; g.gridy = y; g.gridwidth = 1; g.weightx = 0.0;
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(Estilos.FONT_BOLD);
        panel.add(label, g);
        g.gridx = 1; g.weightx = 1.0;
        panel.add(comp, g);
    }

    // =========================================================================
    // LÓGICA DE NEGOCIO Y BASE DE DATOS
    // =========================================================================

    private void configurarModoEdicion() {
        txtId.setEnabled(false);

        // Aquí bloqueamos el stock manual para evitar errores de dedo o robos
        txtStock.setEnabled(false);
        txtStock.setToolTipText("Stock bloqueado. Usa el botón naranja para ajustes de Admin.");

        // Encendemos las herramientas exclusivas del modo edición
        btnCodigosExtra.setEnabled(true);
        btnAjustarStock.setEnabled(true); // <-- Encendemos el botón de Admin
    }

    private void abrirGestionCodigosExtra() {
        // Buscamos el JFrame principal que está hasta arriba del diálogo actual
        Window ancestro = SwingUtilities.getWindowAncestor(this);
        if (ancestro instanceof JFrame) {
            JFrame framePrincipal = (JFrame) ancestro;

            // Instanciamos tu clase con los datos actuales
            DialogoCodigos dialogoBarras = new DialogoCodigos(framePrincipal, idProductoActual, txtNombre.getText().trim());
            dialogoBarras.setVisible(true);
        } else {
            // Protección por si las dudas
            JOptionPanePro.mostrarMensaje(this, "Error", "No se pudo detectar la ventana principal.", "ERROR");
        }
    }

    private void cargarDatosDesdeBD() {
        String sql = "SELECT * FROM productos WHERE id = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProductoActual);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Mapeo exacto con tu esquema de BD
                    txtCodigo.setText(rs.getString("codigo_barras") != null ? rs.getString("codigo_barras") : "");
                    txtNombre.setText(rs.getString("nombre"));
                    txtModelo.setText(rs.getString("modelo"));

                    cmbMarca.setSelectedItem(rs.getString("marca"));
                    cmbCategoria.setSelectedItem(rs.getString("categoria"));
                    cmbProveedor.setSelectedItem(rs.getString("proveedor"));

                    txtCosto.setText(String.valueOf(rs.getDouble("costo")));
                    txtPrecio.setText(String.valueOf(rs.getDouble("precio")));
                    txtStock.setText(String.valueOf(rs.getInt("stock")));

                    txtMercadoMin.setText(String.valueOf(rs.getDouble("precio_mercado_min")));
                    txtMercadoProm.setText(String.valueOf(rs.getDouble("precio_mercado_prom")));
                    txtMercadoMax.setText(String.valueOf(rs.getDouble("precio_mercado_max")));

                    // Actualizar visualmente el margen y la explicación del algoritmo
                    if (rs.getDouble("costo") > 0) {
                        double costo = rs.getDouble("costo");
                        double venta = rs.getDouble("precio");

                        double mercadoMin = rs.getDouble("precio_mercado_min");
                        double mercadoProm = rs.getDouble("precio_mercado_prom");
                        double mercadoMax = rs.getDouble("precio_mercado_max");
                        String categoria = rs.getString("categoria");

                        // Usar el margen guardado en BD o calcularlo
                        double margen = rs.getDouble("margen_ganancia");
                        lblMargenCalc.setText("Margen: " + String.format("%.2f", margen) + "%");

                        try {
                            String catSegura = (categoria != null && !categoria.isEmpty()) ? categoria : "Hardware";
                            servicios.AlgoritmoPrecios.ResultadoPrecio res = servicios.AlgoritmoPrecios.calcular(catSegura, costo, mercadoMin, mercadoProm, mercadoMax);
                            lblMargenCalc.setToolTipText("Estrategia: " + res.explicacion);
                        } catch (Exception e) {
                            lblMargenCalc.setToolTipText("Datos cargados desde BD.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en DialogoProducto.cargarDatosDesdeBD: " + e.getMessage());
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error de Carga", "Fallo al leer la BD: " + e.getMessage(), "ERROR");
        }
    }

    private void guardarOActualizar() {
        if (txtNombre.getText().trim().isEmpty() || txtPrecio.getText().trim().isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Datos Faltantes", "El Nombre y el Precio Venta son obligatorios.", "ADVERTENCIA");
            return;
        }

        try (Connection conn = ConexionBD.conectar()) {

            // Pre-calcular variables numéricas
            double costoVal = parseDoubleSafe(txtCosto.getText());
            double precioVal = parseDoubleSafe(txtPrecio.getText());
            double minVal = parseDoubleSafe(txtMercadoMin.getText());
            double promVal = parseDoubleSafe(txtMercadoProm.getText());
            double maxVal = parseDoubleSafe(txtMercadoMax.getText());
            double margenCalculado = costoVal > 0 ? ((precioVal - costoVal) / costoVal) * 100 : 0;

            if (idProductoActual == -1) {
                // INSERT - Mapeado a tus columnas exactas (13 parámetros)
                String sql = "INSERT INTO productos (codigo_barras, nombre, marca, modelo, categoria, proveedor, costo, precio, stock, precio_mercado_min, precio_mercado_prom, precio_mercado_max, margen_ganancia) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, txtCodigo.getText().trim());
                    ps.setString(2, txtNombre.getText().trim().toUpperCase());
                    ps.setString(3, cmbMarca.getSelectedItem() != null ? cmbMarca.getSelectedItem().toString() : "");
                    ps.setString(4, txtModelo.getText().trim());
                    ps.setString(5, cmbCategoria.getSelectedItem() != null ? cmbCategoria.getSelectedItem().toString() : "");
                    ps.setString(6, cmbProveedor.getSelectedItem() != null ? cmbProveedor.getSelectedItem().toString() : "");
                    ps.setDouble(7, costoVal);
                    ps.setDouble(8, precioVal);
                    ps.setInt(9, parseIntSafe(txtStock.getText()));

                    ps.setDouble(10, minVal);
                    ps.setDouble(11, promVal);
                    ps.setDouble(12, maxVal);
                    ps.setDouble(13, margenCalculado);

                    ps.executeUpdate();
                }
            } else {
                // UPDATE - Mapeado a tus columnas exactas (Respetamos la regla de NO editar stock aquí)
                String sql = "UPDATE productos SET codigo_barras=?, nombre=?, marca=?, modelo=?, categoria=?, proveedor=?, costo=?, precio=?, precio_mercado_min=?, precio_mercado_prom=?, precio_mercado_max=?, margen_ganancia=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, txtCodigo.getText().trim());
                    ps.setString(2, txtNombre.getText().trim().toUpperCase());
                    ps.setString(3, cmbMarca.getSelectedItem() != null ? cmbMarca.getSelectedItem().toString() : "");
                    ps.setString(4, txtModelo.getText().trim());
                    ps.setString(5, cmbCategoria.getSelectedItem() != null ? cmbCategoria.getSelectedItem().toString() : "");
                    ps.setString(6, cmbProveedor.getSelectedItem() != null ? cmbProveedor.getSelectedItem().toString() : "");
                    ps.setDouble(7, costoVal);
                    ps.setDouble(8, precioVal);

                    ps.setDouble(9, minVal);
                    ps.setDouble(10, promVal);
                    ps.setDouble(11, maxVal);
                    ps.setDouble(12, margenCalculado);

                    ps.setInt(13, idProductoActual);
                    ps.executeUpdate();
                }
            }

            JOptionPanePro.mostrarMensaje(this, "Éxito", "Producto guardado correctamente.", "INFO");
            callbackRefrescarTabla.run();
            dispose();

        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en DialogoProducto.guardarOActualizar: " + e.getMessage());
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error BD", "Fallo al guardar: " + e.getMessage(), "ERROR");
        }
    }

    // =========================================================================
    // MÉTODOS DE LA CALCULADORA DE PRECIOS
    // =========================================================================

    private void calcIVA() {
        try {
            double c = Double.parseDouble(txtCosto.getText());
            txtCosto.setText(String.format("%.2f", c * 1.16));
        } catch (Exception ignore) {}
    }

    private void calcularPromedioAuto() {
        try {
            double min = Double.parseDouble(txtMercadoMin.getText().isEmpty() ? "0" : txtMercadoMin.getText());
            double max = Double.parseDouble(txtMercadoMax.getText().isEmpty() ? "0" : txtMercadoMax.getText());
            if (min > 0 && max > 0) {
                txtMercadoProm.setText(String.format("%.2f", (min + max) / 2));
            }
        } catch (Exception ignore) {}
    }

    private void investigarWeb() {
        String termino = txtNombre.getText().trim() + " " + txtModelo.getText().trim();
        if (termino.trim().isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Escribe un nombre o modelo para buscar.", "INFO");
            return;
        }
        try {
            String url = "https://www.google.com/search?q=" + termino.replace(" ", "+") + "+precio+mexico";
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ejecutarAlgoritmoPrecio() {
        try {
            // 1. Extraer la categoría seleccionada
            String categoria = cmbCategoria.getSelectedItem() != null ?
                    cmbCategoria.getSelectedItem().toString() : "Hardware";

            // 2. Extraer los valores numéricos de forma segura
            double costo = parseDoubleSafe(txtCosto.getText());
            double mercadoMin = parseDoubleSafe(txtMercadoMin.getText());
            double mercadoProm = parseDoubleSafe(txtMercadoProm.getText());
            double mercadoMax = parseDoubleSafe(txtMercadoMax.getText());

            if (costo <= 0) {
                JOptionPanePro.mostrarMensaje(this, "Aviso", "Necesitas ingresar el costo del producto para calcular la estrategia.", "ADVERTENCIA");
                return;
            }

            // 3. ¡Llamar a tu algoritmo!
            // (Si ResultadoPrecio está dentro de AlgoritmoPrecios, usa AlgoritmoPrecios.ResultadoPrecio)
            AlgoritmoPrecios.ResultadoPrecio res = AlgoritmoPrecios.calcular(categoria, costo, mercadoMin, mercadoProm, mercadoMax);

            // 4. Inyectar los resultados en la interfaz
            txtPrecio.setText(String.format("%.2f", res.precioSugerido));
            lblMargenCalc.setText("Margen: " + String.format("%.2f", res.margenAplicado) + "%");

            // 5. UX: Aprovechar tu variable 'explicacion' para darle feedback al cajero/administrador
            lblMargenCalc.setToolTipText(res.explicacion); // El usuario podrá ver la razón si pasa el mouse

            // Como vi que usas ToastPro en otro lado, te recomiendo mostrar la explicación ahí,
            // así el usuario sabe exactamente qué estrategia tomó el algoritmo al instante:
            try {
                ui.componentes.ToastPro.show(res.explicacion, "INFO");
            } catch (Exception ignore) {
                // Si no tienes ToastPro importado aquí, usamos el mensaje clásico:
                // JOptionPanePro.mostrarMensaje(this, "Estrategia Aplicada", res.explicacion, "INFO");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error", "Verifica que los campos de precios tengan números válidos.", "ERROR");
        }
    }

    // Utilidades para evitar crashes por campos vacíos
    private double parseDoubleSafe(String val) {
        try { return Double.parseDouble(val.trim()); } catch (Exception e) { return 0.0; }
    }
    private int parseIntSafe(String val) {
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return 0; }
    }

    private void abrirAjusteStockAdmin() {
        if (idProductoActual == -1) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Primero debes guardar el producto para poder ajustar su stock.", "INFO");
            return;
        }

        // 1. Armamos el Micro-Panel para el Pop-up
        JPanel panelAdmin = new JPanel(new GridLayout(2, 2, 10, 10));
        panelAdmin.setOpaque(false);

        JLabel lblNuevoStock = new JLabel("Nueva Cantidad:");
        lblNuevoStock.setForeground(Color.WHITE);
        lblNuevoStock.setFont(Estilos.FONT_BOLD);
        JTextField txtNuevoStock = new JTextField(txtStock.getText());
        Estilos.estilizarInput(txtNuevoStock);

        JLabel lblPin = new JLabel("PIN de Autorización:");
        lblPin.setForeground(Color.WHITE);
        lblPin.setFont(Estilos.FONT_BOLD);
        JPasswordField txtPin = new JPasswordField();
        Estilos.estilizarInput(txtPin);

        panelAdmin.add(lblNuevoStock);
        panelAdmin.add(txtNuevoStock);
        panelAdmin.add(lblPin);
        panelAdmin.add(txtPin);

        // 2. Mostramos el Pop-up nativo
        int opcion = JOptionPane.showConfirmDialog(
                this,
                panelAdmin,
                "Ajuste de Inventario Admin",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        // 3. Evaluamos la respuesta
        if (opcion == JOptionPane.OK_OPTION) {
            String pinIngresado = new String(txtPin.getPassword());
            boolean esAdminValido = false;

            // --- VALIDACIÓN DE ADMIN EN BASE DE DATOS ---
            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM usuarios_sistema WHERE password = ? AND (rol = 'admin' OR rol = 'Administrador')")) {

                ps.setString(1, pinIngresado);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        esAdminValido = true; // El PIN existe y pertenece a un admin
                    }
                }
            } catch (Exception ex) {
                servicios.LoggerPro.registrar("ERROR_DB", "Fallo en DialogoProducto.abrirAjusteStockAdmin (validar admin): " + ex.getMessage());
                ex.printStackTrace();
                JOptionPanePro.mostrarMensaje(this, "Error BD", "Fallo al validar las credenciales.", "ERROR");
                return; // Salimos para no actualizar si hubo error de conexión
            }

            // --- EJECUCIÓN SI EL PIN ES VÁLIDO ---
            if (esAdminValido) {
                try {
                    int nuevaCantidad = Integer.parseInt(txtNuevoStock.getText().trim());

                    if(nuevaCantidad < 0) {
                        JOptionPanePro.mostrarMensaje(this, "Error", "El stock no puede ser negativo.", "ERROR");
                        return;
                    }

                    // Actualizamos directamente el inventario
                    try (Connection conn = ConexionBD.conectar();
                         PreparedStatement ps = conn.prepareStatement("UPDATE productos SET stock = ? WHERE id = ?")) {
                        ps.setInt(1, nuevaCantidad);
                        ps.setInt(2, idProductoActual);
                        ps.executeUpdate();

                        // Reflejamos el cambio en la pantalla y avisamos
                        txtStock.setText(String.valueOf(nuevaCantidad));
                        callbackRefrescarTabla.run();
                        ui.componentes.ToastPro.show("Stock actualizado", "EXITO");
                    }

                } catch (NumberFormatException ex) {
                    JOptionPanePro.mostrarMensaje(this, "Error numérico", "Ingresa una cantidad válida.", "ERROR");
                } catch (Exception ex) {
                    servicios.LoggerPro.registrar("ERROR_DB", "Fallo en DialogoProducto.abrirAjusteStockAdmin (update stock): " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPanePro.mostrarMensaje(this, "Error BD", "Fallo al actualizar el stock.", "ERROR");
                }
            } else {
                JOptionPanePro.mostrarMensaje(this, "Acceso Denegado", "PIN incorrecto o no tienes privilegios de Administrador.", "ERROR");
            }
        }
    }

    private void cargarCombosDeBD() {
        cmbProveedor.removeAllItems();
        cmbMarca.removeAllItems();

        try (Connection c = ConexionBD.conectar();
             java.sql.Statement s = c.createStatement()) {

            // 1. Cargar Proveedores (Evitando vacíos o nulos)
            try (ResultSet rsProv = s.executeQuery("SELECT DISTINCT proveedor FROM productos WHERE proveedor IS NOT NULL AND proveedor != ''")) {
                while (rsProv.next()) {
                    cmbProveedor.addItem(rsProv.getString(1));
                }
            }

            // 2. Cargar Marcas (Evitando vacíos o nulos)
            try (ResultSet rsMarca = s.executeQuery("SELECT DISTINCT marca FROM productos WHERE marca IS NOT NULL AND marca != ''")) {
                while (rsMarca.next()) {
                    cmbMarca.addItem(rsMarca.getString(1));
                }
            }

        } catch (Exception e) {
            servicios.LoggerPro.registrar("ERROR_DB", "Fallo en DialogoProducto.cargarCombosDeBD: " + e.getMessage());
            e.printStackTrace();
        }

        // Dejamos la selección en blanco por defecto (Para cuando es Producto Nuevo)
        cmbProveedor.setSelectedItem(null);
        cmbMarca.setSelectedItem(null);
    }
}
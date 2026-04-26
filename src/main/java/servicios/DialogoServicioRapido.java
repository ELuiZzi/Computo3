package servicios;

import config.ConexionBD;
import ui.componentes.BotonPro;
import ui.componentes.JOptionPanePro;
import util.Estilos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

public class DialogoServicioRapido extends JDialog {

    private JTextField txtConcepto, txtMonto;
    private JCheckBox chkGarantia;

    public DialogoServicioRapido(JFrame parent) {
        super(parent, "Cobro de Servicio Rápido", true);
        setSize(400, 450);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Estilos.COLOR_FONDO);
        p.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(10, 0, 5, 0);
        g.gridx = 0;

        // --- CAMPOS ---
        JLabel lbl1 = new JLabel("Concepto del Servicio:");
        lbl1.setForeground(Color.GRAY);
        txtConcepto = new JTextField();
        Estilos.estilizarInput(txtConcepto);

        JLabel lbl2 = new JLabel("Monto a Cobrar ($):");
        lbl2.setForeground(Color.GRAY);
        txtMonto = new JTextField();
        Estilos.estilizarInput(txtMonto);
        txtMonto.setFont(new Font("Segoe UI", Font.BOLD, 20));
        txtMonto.setHorizontalAlignment(SwingConstants.CENTER);

        chkGarantia = new JCheckBox("Imprimir Póliza de Garantía (Refacción)");
        chkGarantia.setBackground(Estilos.COLOR_FONDO);
        chkGarantia.setForeground(Color.ORANGE);
        chkGarantia.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // --- BOTÓN COBRAR ---
        BotonPro btnCobrar = new BotonPro("GENERAR VENTA E IMPRIMIR", Estilos.COLOR_ACCENT, this::procesarCobro);
        btnCobrar.setPreferredSize(new Dimension(0, 50));

        // Agregar al layout
        g.gridy = 0; p.add(lbl1, g);
        g.gridy = 1; p.add(txtConcepto, g);
        g.gridy = 2; p.add(lbl2, g);
        g.gridy = 3; p.add(txtMonto, g);
        g.gridy = 4; p.add(new JLabel(" "), g); // Espaciador
        g.gridy = 5; p.add(chkGarantia, g);
        g.gridy = 6; p.add(Box.createVerticalStrut(20), g);
        g.gridy = 7; p.add(btnCobrar, g);

        add(p);
    }

    private void procesarCobro() {
        String concepto = txtConcepto.getText().trim();
        String montoStr = txtMonto.getText().trim();

        if (concepto.isEmpty() || montoStr.isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Error", "Llena todos los campos.", "ADVERTENCIA");
            return;
        }

        try {
            double monto = Double.parseDouble(montoStr);
            guardarEnBaseDeDatos(concepto, monto);
        } catch (NumberFormatException e) {
            JOptionPanePro.mostrarMensaje(this, "Error", "Monto inválido.", "ERROR");
        }
    }

    private void guardarEnBaseDeDatos(String concepto, double monto) {
        try (Connection conn = ConexionBD.conectar()) {
            conn.setAutoCommit(false);

            // 1. Insertar Venta Genérica
            String sqlV = "INSERT INTO ventas (total_venta, ganancia_total, tipo_venta) VALUES (?, ?, 'SERVICIO')";
            PreparedStatement psV = conn.prepareStatement(sqlV, Statement.RETURN_GENERATED_KEYS);
            psV.setDouble(1, monto);
            psV.setDouble(2, monto * 0.8); // Ganancia estimada del 80% en servicios
            psV.executeUpdate();

            ResultSet rs = psV.getGeneratedKeys();
            rs.next();
            int idVenta = rs.getInt(1);

            // 2. Insertar Detalle
            String sqlD = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, subtotal, descripcion) VALUES (?, NULL, 1, ?, ?)";
            PreparedStatement psD = conn.prepareStatement(sqlD);
            psD.setInt(1, idVenta);
            psD.setDouble(2, monto);
            psD.setString(3, concepto);
            psD.executeUpdate();

            conn.commit();

            // 3. Generar Ticket con la lógica de Garantía
            ArrayList<GeneradorTicket.ItemTicket> items = new ArrayList<>();
            items.add(new GeneradorTicket.ItemTicket(concepto, 1, monto));

            // Aquí usamos el método sobrecargado con el booleano del Checkbox
            String ticket = GeneradorTicket.crearTicket(idVenta, null, items, monto, chkGarantia.isSelected());
            ImpresoraTicket.imprimir(ticket);

            // 4. Notificar a Telegram
            NotificadorTelegram.notificarVentaNueva(monto);

            JOptionPanePro.mostrarMensaje(this, "Éxito", "Venta rápida de servicio finalizada.", "INFO");
            this.dispose();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error", "Error al guardar: " + e.getMessage(), "ERROR");
        }
    }
}
package ui.ventanas; // Ajusta a tu paquete

import config.ConexionBD;

import ui.componentes.*;
import util.Estilos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DialogoCodigos extends JDialog {

    private int idProducto;
    private JTextField txtNuevoCodigo;
    private TablaPro tabla;
    private DefaultTableModel modelo;

    public DialogoCodigos(JFrame parent, int idProducto, String nombreProducto) {
        super(parent, "Códigos Adicionales - " + nombreProducto, true);
        this.idProducto = idProducto;

        setSize(500, 400);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Estilos.COLOR_FONDO);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- FORMULARIO SUPERIOR ---
        JPanel pForm = new JPanel(new BorderLayout(10, 0));
        pForm.setOpaque(false);

        txtNuevoCodigo = new JTextField();
        Estilos.estilizarInput(txtNuevoCodigo);
        txtNuevoCodigo.addActionListener(e -> agregarCodigo()); // Enter para agregar

        BotonPro btnAgregar = new BotonPro("AGREGAR", "mas.png", Estilos.COLOR_ACCENT, this::agregarCodigo);

        pForm.add(new JLabel("<html><font color='white'>Nuevo Código:</font></html>"), BorderLayout.NORTH);
        pForm.add(txtNuevoCodigo, BorderLayout.CENTER);
        pForm.add(btnAgregar, BorderLayout.EAST);

        // --- LISTA DE CÓDIGOS ---
        modelo = new DefaultTableModel(new String[]{"ID", "Código Extra"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new TablaPro(modelo);

        // --- BOTÓN ELIMINAR ---
        BotonPro btnEliminar = new BotonPro("ELIMINAR SELECCIONADO", "menos.png", new Color(200, 50, 50), this::eliminarCodigo);

        panel.add(pForm, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        panel.add(btnEliminar, BorderLayout.SOUTH);

        setContentPane(panel);
        cargarCodigos();
    }

    private void cargarCodigos() {
        modelo.setRowCount(0);
        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT id, codigo FROM codigos_adicionales WHERE id_producto = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idProducto);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                modelo.addRow(new Object[]{rs.getInt("id"), rs.getString("codigo")});
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void agregarCodigo() {
        String codigo = txtNuevoCodigo.getText().trim();
        if(codigo.isEmpty()) return;

        try (Connection conn = config.ConexionBD.conectar()) {
            // Verificar que no exista ya (ni como principal ni como extra)
            String sqlCheck = "SELECT count(*) FROM productos WHERE codigo_barras = ? UNION SELECT count(*) FROM codigos_adicionales WHERE codigo = ?";
            PreparedStatement psCheck = conn.prepareStatement("SELECT (SELECT count(*) FROM productos WHERE codigo_barras = ?) + (SELECT count(*) FROM codigos_adicionales WHERE codigo = ?) as total");
            psCheck.setString(1, codigo);
            psCheck.setString(2, codigo);
            ResultSet rs = psCheck.executeQuery();
            rs.next();
            if(rs.getInt(1) > 0) {
                JOptionPanePro.mostrarMensaje(this, "Error", "Ese código ya está en uso.", "ERROR");
                return;
            }

            // Insertar
            String sql = "INSERT INTO codigos_adicionales (id_producto, codigo) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idProducto);
            ps.setString(2, codigo);
            ps.executeUpdate();

            txtNuevoCodigo.setText("");
            cargarCodigos();
            ToastPro.show("Código Agregado", "EXITO");

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void eliminarCodigo() {
        int row = tabla.getSelectedRow();
        if(row == -1) return;
        int id = Integer.parseInt(modelo.getValueAt(row, 0).toString());

        try (Connection conn = config.ConexionBD.conectar()) {
            conn.createStatement().executeUpdate("DELETE FROM codigos_adicionales WHERE id = " + id);
            cargarCodigos();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
package Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import ElementosPro.BotonPro;
import ElementosPro.JOptionPanePro;
import Utils.Estilos;
import Conexión.ConexionBD;

public class Login extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    public Login() {
        setTitle("Acceso - LUMTECH POS");
        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Fondo General
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(Estilos.COLOR_FONDO);
        panelPrincipal.setBorder(new EmptyBorder(40, 40, 40, 40));

        // --- 1. LOGO / TÍTULO ---
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(Estilos.COLOR_FONDO);

        JLabel lblTitulo = new JLabel("INICIAR SESIÓN", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setForeground(Color.WHITE);

        // Si tienes el logo, lo ponemos
        JLabel lblLogo = new JLabel();
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            ImageIcon icon = new ImageIcon("recursos/logo.png");
            // Redimensionar logo para que no sea gigante en el login
            Image img = icon.getImage().getScaledInstance(120, 80, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(img));
        } catch (Exception e) {}

        pHeader.add(lblLogo, BorderLayout.NORTH);
        pHeader.add(lblTitulo, BorderLayout.CENTER);

        // --- 2. FORMULARIO ---
        JPanel pForm = new JPanel(new GridLayout(4, 1, 10, 10));
        pForm.setBackground(Estilos.COLOR_FONDO);
        pForm.setBorder(new EmptyBorder(30, 0, 30, 0));

        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setForeground(Color.WHITE);
        lblUser.setFont(Estilos.FONT_BOLD);

        txtUsuario = new JTextField();
        Estilos.estilizarInput(txtUsuario);
        txtUsuario.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblPass = new JLabel("Contraseña:");
        lblPass.setForeground(Color.WHITE);
        lblPass.setFont(Estilos.FONT_BOLD);

        txtPassword = new JPasswordField();
        Estilos.estilizarInput(txtPassword);
        txtPassword.setHorizontalAlignment(SwingConstants.CENTER);
        // Hacer que al dar Enter en la contraseña se presione el botón
        txtPassword.addActionListener(e -> validarIngreso());

        pForm.add(lblUser);
        pForm.add(txtUsuario);
        pForm.add(lblPass);
        pForm.add(txtPassword);

        // --- 3. BOTÓN ---
        BotonPro btnEntrar = new BotonPro("INGRESAR", Estilos.COLOR_ACCENT, this::validarIngreso);
        btnEntrar.setPreferredSize(new Dimension(0, 50));

        panelPrincipal.add(pHeader, BorderLayout.NORTH);
        panelPrincipal.add(pForm, BorderLayout.CENTER);
        panelPrincipal.add(btnEntrar, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    private void validarIngreso() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Datos", "Ingresa usuario y contraseña.", "ADVERTENCIA");
            return;
        }

        try (Connection conn = ConexionBD.conectar()) {
            String sql = "SELECT rol FROM usuarios_sistema WHERE usuario = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, pass); // Nota: En producción usar HASH (MD5/SHA)

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String rol = rs.getString("rol");

                // --- AQUÍ OCURRE LA MAGIA ---
                // Abrimos el sistema pasándole el ROL detectado
                new SistemaPOS(rol).setVisible(true);

                this.dispose(); // Cerramos el Login
            } else {
                JOptionPanePro.mostrarMensaje(this, "Error", "Credenciales incorrectas.", "ERROR");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error", "Error de conexión: " + e.getMessage(), "ERROR");
        }
    }

    // Método main para probar solo el login (Opcional, usaremos el de Utils.SistemaPOS)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}
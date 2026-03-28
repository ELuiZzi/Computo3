package ui.ventanas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

import util.Estilos;
import ui.componentes.BotonPro;
import ui.componentes.JOptionPanePro;
import config.ConexionBD;
import config.Sesion;
import util.Recursos;

public class Login extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    public Login() {
        setTitle("Acceso - LUMTECH POS");
        setSize(400, 580);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Fondo General
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(Estilos.COLOR_FONDO);
        panelPrincipal.setBorder(new EmptyBorder(30, 40, 30, 40));

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
            Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(img));
        } catch (Exception e) {}

        pHeader.add(lblLogo, BorderLayout.NORTH);
        pHeader.add(lblTitulo, BorderLayout.CENTER);

        // --- 2. FORMULARIO (CORREGIDO CON GridBagLayout) ---
        JPanel pForm = new JPanel(new GridBagLayout());
        pForm.setBackground(Estilos.COLOR_FONDO);
        // Sin bordes extraños, centrado

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(10, 0, 5, 0); // Espacio entre elementos
        g.weightx = 1.0;

        // Usuario
        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setForeground(Color.LIGHT_GRAY);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtUsuario = new JTextField();
        Estilos.estilizarInput(txtUsuario);
        txtUsuario.setPreferredSize(new Dimension(0, 35)); // Altura fija cómoda
        txtUsuario.setHorizontalAlignment(SwingConstants.CENTER);

        // Contraseña
        JLabel lblPass = new JLabel("Contraseña:");
        lblPass.setForeground(Color.LIGHT_GRAY);
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtPassword = new JPasswordField();
        Estilos.estilizarInput(txtPassword);
        txtPassword.setPreferredSize(new Dimension(0, 35)); // Altura fija cómoda
        txtPassword.setHorizontalAlignment(SwingConstants.CENTER);
        txtPassword.addActionListener(e -> validarIngreso());

        // Agregar al panel
        g.gridy = 0; pForm.add(lblUser, g);
        g.gridy = 1; pForm.add(txtUsuario, g);
        g.gridy = 2; pForm.add(lblPass, g);
        g.gridy = 3; pForm.add(txtPassword, g);

        // --- 3. BOTONES Y FOOTER ---
        JPanel pSur = new JPanel(new GridLayout(3, 1, 0, 10)); // Grid vertical con espacio
        pSur.setBackground(Estilos.COLOR_FONDO);
        pSur.setBorder(new EmptyBorder(20, 0, 0, 0));

        BotonPro btnEntrar = new BotonPro("INGRESAR", Estilos.COLOR_ACCENT, this::validarIngreso);
        btnEntrar.setPreferredSize(new Dimension(0, 45));

        BotonPro btnRapido = new BotonPro("VENTA RÁPIDA (Solo Escanear)", new Color(255, 140, 0), this::abrirVentaRapida);
        btnRapido.setPreferredSize(new Dimension(0, 45));

        // Versión
        JPanel pVersion = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pVersion.setBackground(Estilos.COLOR_FONDO);
        JLabel lblVersion = new JLabel("v" + Recursos.getVersionActual());
        lblVersion.setForeground(Color.DARK_GRAY);
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pVersion.add(lblVersion);

        pSur.add(btnEntrar);
        pSur.add(btnRapido);
        pSur.add(pVersion);

        // ARMADO FINAL
        panelPrincipal.add(pHeader, BorderLayout.NORTH);
        panelPrincipal.add(pForm, BorderLayout.CENTER);
        panelPrincipal.add(pSur, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    private void abrirVentaRapida() {
        DialogoVentaRapida dialog = new DialogoVentaRapida(this);
        dialog.setVisible(true);
    }

    private void validarIngreso() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Datos", "Ingresa usuario y contraseña.", "ADVERTENCIA");
            return;
        }
        try (Connection conn = ConexionBD.conectar()) {
            // CAMBIO: Agregamos BINARY antes de usuario para forzar distinción mayus/minus
            // Y pedimos el campo 'genero'
            String sql = "SELECT rol, genero FROM usuarios_sistema WHERE BINARY usuario = ? AND password = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, pass);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String rol = rs.getString("rol");
                String genero = rs.getString("genero");

                // Guardar datos en Sesión Global
                Sesion.usuarioActual = user; // Guardamos tal cual lo escribió
                Sesion.rolActual = rol;
                Sesion.generoActual = (genero != null) ? genero : "M"; // Protección null

                new SistemaPOS(rol).setVisible(true);
                this.dispose();
            }else {
                JOptionPanePro.mostrarMensaje(this, "Error", "Credenciales incorrectas (Verifica mayúsculas).", "ERROR");
            }
        }catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(this, "Error", "Error de conexión: " + e.getMessage(), "ERROR");
        }
    }

    // Método main para probar solo el login (Opcional, usaremos el de ui.ventanas.SistemaPOS)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}
package ui.paneles;

import config.ConexionBD;
import config.Sesion;
import servicios.ImpresoraTicket;
import ui.componentes.*;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Properties;

public class PanelConfiguracion extends JPanel {

    // Componentes Impresora
    private JComboBox<String> cmbImpresora;
    private JCheckBox chkAutoImprimir;

    // Componentes Perfil
    private JTextField txtMiUsuario;
    private JPasswordField txtMiPass;
    private JComboBox<String> cmbMiGenero;

    // Componentes Base de Datos
    private JTextField txtDbIp, txtDbPuerto, txtDbUser;
    private JPasswordField txtDbPass;
    private JTextField txtRutaDump;

    // Componentes Nuevo Usuario (Admin)
    private JTextField txtNuevoUser;
    private JPasswordField txtNuevoPass;
    private JComboBox<String> cmbNuevoRol;
    private JComboBox<String> cmbNuevoGenero;

    public PanelConfiguracion(String rolUsuario) {
        setLayout(new BorderLayout());
        setBackground(Estilos.COLOR_FONDO);

        TabbedPanePro tabsConfig = new TabbedPanePro();

        tabsConfig.addTab("IMPRESIÓN", crearPanelImpresora());
        tabsConfig.addTab("MI PERFIL", crearPanelPerfil());

        if ("ADMIN".equals(rolUsuario)) {
            tabsConfig.addTab("BASE DE DATOS", crearPanelBD());
            tabsConfig.addTab("USUARIOS", crearPanelUsuarios()); // <-- NUEVA PESTAÑA
        }

        add(tabsConfig, BorderLayout.CENTER);
        cargarConfiguracionGlobal();
    }

    // --- TAB 1: IMPRESORA ---
    private JPanel crearPanelImpresora() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Estilos.COLOR_PANEL);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10,10,10,10); g.fill = GridBagConstraints.HORIZONTAL;

        addLbl(p, g, 0, 0, "Impresora Tickets:");
        cmbImpresora = new JComboBox<>();
        g.gridx=1; p.add(cmbImpresora, g);

        chkAutoImprimir = new JCheckBox("Imprimir Automáticamente al Vender");
        chkAutoImprimir.setBackground(Estilos.COLOR_PANEL);
        chkAutoImprimir.setForeground(Color.WHITE);
        g.gridx=0; g.gridy=1; g.gridwidth=2; p.add(chkAutoImprimir, g);

        BotonPro btnGuardarImp = new BotonPro("Guardar Impresora", Estilos.COLOR_ACCENT, this::guardarConfigImpresora);
        g.gridy=2; p.add(btnGuardarImp, g);

        return p;
    }

    // --- TAB 2: MI PERFIL ---
    private JPanel crearPanelPerfil() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Estilos.COLOR_PANEL);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10,10,10,10); g.fill = GridBagConstraints.HORIZONTAL;

        txtMiUsuario = input();
        txtMiPass = new JPasswordField(15); Estilos.estilizarInput(txtMiPass);
        cmbMiGenero = new JComboBox<>(new String[]{"Masculino", "Femenino"});

        txtMiUsuario.setText(Sesion.usuarioActual);
        cmbMiGenero.setSelectedIndex("F".equals(Sesion.generoActual) ? 1 : 0);

        addLbl(p, g, 0, 0, "Nuevo Usuario:"); g.gridx=1; p.add(txtMiUsuario, g);
        addLbl(p, g, 0, 1, "Nueva Contraseña:"); g.gridx=1; p.add(txtMiPass, g);
        addLbl(p, g, 0, 2, "Género:"); g.gridx=1; p.add(cmbMiGenero, g);

        BotonPro btnUpdatePerfil = new BotonPro("Actualizar Perfil", new Color(46, 204, 113), this::actualizarPerfil);
        g.gridx=0; g.gridy=3; g.gridwidth=2; p.add(btnUpdatePerfil, g);

        return p;
    }

    // --- TAB 3: BASE DE DATOS ---
    private JPanel crearPanelBD() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Estilos.COLOR_PANEL);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10,10,10,10); g.fill = GridBagConstraints.HORIZONTAL;

        txtDbIp = input(); txtDbPuerto = input(); txtDbUser = input();
        txtDbPass = new JPasswordField(15); Estilos.estilizarInput(txtDbPass);

        txtRutaDump = input();
        BotonPro btnBuscarDump = new BotonPro("", "folder.png", Estilos.COLOR_INPUT, this::buscarMysqlDump);
        btnBuscarDump.setPreferredSize(new Dimension(40, 30));

        JPanel pDump = new JPanel(new BorderLayout(5,0));
        pDump.setBackground(Estilos.COLOR_PANEL);
        pDump.add(txtRutaDump, BorderLayout.CENTER);
        pDump.add(btnBuscarDump, BorderLayout.EAST);

        addLbl(p, g, 0, 0, "IP Servidor:"); g.gridx=1; p.add(txtDbIp, g);
        addLbl(p, g, 0, 1, "Puerto (3306):"); g.gridx=1; p.add(txtDbPuerto, g);
        addLbl(p, g, 0, 2, "Usuario BD:"); g.gridx=1; p.add(txtDbUser, g);
        addLbl(p, g, 0, 3, "Contraseña BD:"); g.gridx=1; p.add(txtDbPass, g);
        addLbl(p, g, 0, 4, "Ruta mysqldump:"); g.gridx=1; p.add(pDump, g);

        BotonPro btnSaveBD = new BotonPro("Guardar Conexión", Color.ORANGE, this::guardarConfigBD);
        g.gridx=0; g.gridy=5; g.gridwidth=2; p.add(btnSaveBD, g);

        return p;
    }

    // --- TAB 4: NUEVO USUARIO (SOLO ADMIN) ---
    private JPanel crearPanelUsuarios() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Estilos.COLOR_PANEL);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10,10,10,10); g.fill = GridBagConstraints.HORIZONTAL;

        txtNuevoUser = input();
        txtNuevoPass = new JPasswordField(15); Estilos.estilizarInput(txtNuevoPass);
        cmbNuevoRol = new JComboBox<>(new String[]{"CAJERO", "ADMIN"});
        cmbNuevoGenero = new JComboBox<>(new String[]{"Masculino", "Femenino"});

        addLbl(p, g, 0, 0, "Usuario:"); g.gridx=1; p.add(txtNuevoUser, g);
        addLbl(p, g, 0, 1, "Contraseña:"); g.gridx=1; p.add(txtNuevoPass, g);
        addLbl(p, g, 0, 2, "Rol en Sistema:"); g.gridx=1; p.add(cmbNuevoRol, g);
        addLbl(p, g, 0, 3, "Género:"); g.gridx=1; p.add(cmbNuevoGenero, g);

        BotonPro btnCrear = new BotonPro("Crear Nuevo Usuario", new Color(41, 128, 185), this::guardarNuevoUsuario);
        g.gridx=0; g.gridy=4; g.gridwidth=2; p.add(btnCrear, g);

        return p;
    }

    // ==========================================
    // LÓGICA
    // ==========================================

    private void cargarConfiguracionGlobal() {
        List<String> impresoras = ImpresoraTicket.obtenerImpresorasDisponibles();
        cmbImpresora.removeAllItems();
        for (String imp : impresoras) cmbImpresora.addItem(imp);

        File configFile = new File("config.properties");
        if (configFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                String impGuardada = props.getProperty("ticket.impresora");
                if(impGuardada != null) cmbImpresora.setSelectedItem(impGuardada);
                chkAutoImprimir.setSelected(Boolean.parseBoolean(props.getProperty("ticket.auto_imprimir", "true")));

                txtDbIp.setText(props.getProperty("db.ip", "localhost"));
                txtDbPuerto.setText(props.getProperty("db.port", "3306"));
                txtDbUser.setText(props.getProperty("db.user", "root"));
                txtDbPass.setText(props.getProperty("db.password", ""));
                txtRutaDump.setText(props.getProperty("db.dump_path", ""));
            } catch (Exception e) {}
        }
    }

    private void guardarConfigImpresora() {
        actualizarProperties("ticket.impresora", (String) cmbImpresora.getSelectedItem());
        actualizarProperties("ticket.auto_imprimir", String.valueOf(chkAutoImprimir.isSelected()));
        ImpresoraTicket.setImpresora((String) cmbImpresora.getSelectedItem());
        ImpresoraTicket.setAutoImprimir(chkAutoImprimir.isSelected());
        JOptionPanePro.mostrarMensaje(this, "Guardado", "Configuración de impresión actualizada.", "INFO");
    }

    private void guardarConfigBD() {
        actualizarProperties("db.ip", txtDbIp.getText());
        actualizarProperties("db.port", txtDbPuerto.getText());
        actualizarProperties("db.user", txtDbUser.getText());
        actualizarProperties("db.password", new String(txtDbPass.getPassword()));
        actualizarProperties("db.dump_path", txtRutaDump.getText());
        JOptionPanePro.mostrarMensaje(this, "Guardado", "Datos de conexión guardados.\nReinicia el programa para aplicar.", "ADVERTENCIA");
    }

    private void actualizarPerfil() {
        String nuevoUser = txtMiUsuario.getText().trim();
        String nuevoPass = new String(txtMiPass.getPassword()).trim();
        String nuevoGenero = cmbMiGenero.getSelectedIndex() == 0 ? "M" : "F";

        if(nuevoUser.isEmpty() || nuevoPass.isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Error", "Usuario y contraseña no pueden estar vacíos.", "ERROR");
            return;
        }

        try (Connection conn = ConexionBD.conectar()) {
            String sql = "UPDATE usuarios_sistema SET usuario = ?, password = ?, genero = ? WHERE usuario = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nuevoUser);
            ps.setString(2, nuevoPass);
            ps.setString(3, nuevoGenero);
            ps.setString(4, Sesion.usuarioActual);

            if (ps.executeUpdate() > 0) {
                Sesion.usuarioActual = nuevoUser;
                Sesion.generoActual = nuevoGenero;

                // Si cambiamos el usuario actual, también actualizamos el properties para no romper el auto-login
                actualizarProperties("session.user", nuevoUser);
                actualizarProperties("session.pass", java.util.Base64.getEncoder().encodeToString(nuevoPass.getBytes()));

                JOptionPanePro.mostrarMensaje(this, "Éxito", "Perfil actualizado correctamente.", "INFO");
            } else {
                JOptionPanePro.mostrarMensaje(this, "Error", "No se pudo actualizar el perfil.", "ERROR");
            }
        } catch (Exception e) {
            JOptionPanePro.mostrarMensaje(this, "Error BD", e.getMessage(), "ERROR");
        }
    }

    private void guardarNuevoUsuario() {
        String u = txtNuevoUser.getText().trim();
        String p = new String(txtNuevoPass.getPassword()).trim();
        String r = cmbNuevoRol.getSelectedItem().toString();
        String gen = cmbNuevoGenero.getSelectedIndex() == 0 ? "M" : "F";

        if (u.isEmpty() || p.isEmpty()) {
            JOptionPanePro.mostrarMensaje(this, "Aviso", "Llene todos los campos para crear al usuario.", "ADVERTENCIA");
            return;
        }

        try (Connection conn = ConexionBD.conectar()) {
            String sql = "INSERT INTO usuarios_sistema (usuario, password, rol, genero) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, u);
            ps.setString(2, p);
            ps.setString(3, r);
            ps.setString(4, gen);
            ps.executeUpdate();

            JOptionPanePro.mostrarMensaje(this, "Éxito", "El usuario '" + u + "' ha sido creado.", "INFO");
            txtNuevoUser.setText("");
            txtNuevoPass.setText("");
        } catch (Exception e) {
            JOptionPanePro.mostrarMensaje(this, "Error", "Error al crear: Probablemente el usuario ya existe.\nDetalle: " + e.getMessage(), "ERROR");
        }
    }

    private void actualizarProperties(String key, String value) {
        Properties props = new Properties();
        File archivo = new File("config.properties");
        if (archivo.exists()) {
            try (FileInputStream fis = new FileInputStream(archivo)) { props.load(fis); } catch (Exception e) {}
        }
        props.setProperty(key, value);
        try (FileOutputStream fos = new FileOutputStream(archivo)) { props.store(fos, "Configuracion Sistema POS"); } catch (Exception e) {}
    }

    private void buscarMysqlDump() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Buscar mysqldump.exe (Carpeta bin de MySQL)");
        File rutaComun = new File("C:\\Program Files\\MySQL");
        if(rutaComun.exists()) fc.setCurrentDirectory(rutaComun);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtRutaDump.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private JTextField input() { JTextField t = new JTextField(15); Estilos.estilizarInput(t); return t; }
    private void addLbl(JPanel p, GridBagConstraints g, int x, int y, String t) { g.gridx=x; g.gridy=y; JLabel l=new JLabel(t); l.setForeground(Color.WHITE); p.add(l,g); }
}
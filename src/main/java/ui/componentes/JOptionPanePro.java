package ui.componentes;

import util.Estilos;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class JOptionPanePro {

    // 1. MENSAJE / NOTIFICACIÓN
    public static void mostrarMensaje(Component parent, String titulo, String mensaje, String tipo) {
        JDialog dialog = crearDialogoBase(parent, titulo);
        JPanel content = (JPanel) dialog.getContentPane();

        // Icono y Color según tipo
        Color colorBorde = Estilos.COLOR_BORDER;
        if (tipo.equalsIgnoreCase("ADVERTENCIA")) colorBorde = Color.YELLOW;
        else if (tipo.equalsIgnoreCase("ERROR")) colorBorde = Color.RED;
        else colorBorde = Estilos.COLOR_ACCENT;

        content.setBorder(new CompoundBorder(new LineBorder(colorBorde, 2), new EmptyBorder(20, 20, 20, 20)));

        // Mensaje
        JLabel lblMsg = new JLabel("<html><div style='width:250px; text-align:center'>" + mensaje + "</div></html>");
        lblMsg.setForeground(Color.WHITE);
        lblMsg.setFont(Estilos.FONT_BOLD);
        lblMsg.setHorizontalAlignment(SwingConstants.CENTER);

        // Botón Aceptar
        JPanel panelBtn = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBtn.setBackground(Estilos.COLOR_PANEL);
        BotonPro btnOk = new BotonPro("ENTENDIDO", colorBorde, dialog::dispose);
        panelBtn.add(btnOk);

        content.add(lblMsg, BorderLayout.CENTER);
        content.add(panelBtn, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    // 2. CONFIRMACIÓN (SI / NO)
    public static boolean mostrarConfirmacion(Component parent, String titulo, String mensaje) {
        JDialog dialog = crearDialogoBase(parent, titulo);
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBorder(new CompoundBorder(new LineBorder(Estilos.COLOR_ACCENT, 1), new EmptyBorder(20, 20, 20, 20)));

        // Mensaje
        JLabel lblMsg = new JLabel("<html><div style='width:250px; text-align:center'>" + mensaje + "</div></html>");
        lblMsg.setForeground(Color.WHITE);
        lblMsg.setFont(Estilos.FONT_PLAIN);
        lblMsg.setHorizontalAlignment(SwingConstants.CENTER);

        // Botones
        JPanel panelBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        panelBtn.setBackground(Estilos.COLOR_PANEL);

        // Variable para guardar respuesta (Truco array de 1 posición para ser final)
        final boolean[] respuesta = {false};

        BotonPro btnSi = new BotonPro("SÍ, CONTINUAR", new Color(46, 204, 113), () -> {
            respuesta[0] = true;
            dialog.dispose();
        });

        BotonPro btnNo = new BotonPro("CANCELAR", new Color(200, 80, 80), dialog::dispose);

        panelBtn.add(btnNo);
        panelBtn.add(btnSi);

        content.add(lblMsg, BorderLayout.CENTER);
        content.add(panelBtn, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return respuesta[0];
    }

    // 3. ENTRADA DE TEXTO
    public static String solicitarEntrada(Component parent, String titulo, String mensaje) {
        return solicitarEntrada(parent, titulo, mensaje, ""); // Llama al método completo con texto vacío
    }

    public static String solicitarEntrada(Component parent, String titulo, String mensaje, String valorDefecto) {
        JDialog dialog = crearDialogoBase(parent, titulo);
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBorder(new CompoundBorder(new LineBorder(Estilos.COLOR_ACCENT, 1), new EmptyBorder(20, 20, 20, 20)));

        JLabel lblMsg = new JLabel("<html><div style='text-align:center'>" + mensaje.replace("\n", "<br>") + "</div></html>");
        lblMsg.setForeground(Color.WHITE);
        lblMsg.setFont(Estilos.FONT_BOLD);
        lblMsg.setHorizontalAlignment(SwingConstants.CENTER);

        JTextField txtInput = new JTextField(20);
        Estilos.estilizarInput(txtInput);

        // AQUÍ ASIGNAMOS EL VALOR POR DEFECTO
        if (valorDefecto != null) {
            txtInput.setText(valorDefecto);
            // Seleccionar todo el texto para que sea fácil borrarlo si el usuario quiere cambiarlo
            txtInput.selectAll();
        }

        // Listener para que al dar ENTER se acepte
        JPanel panelBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBtn.setBackground(Estilos.COLOR_PANEL);

        final String[] respuesta = {null};

        Runnable accionAceptar = () -> {
            respuesta[0] = txtInput.getText();
            dialog.dispose();
        };

        txtInput.addActionListener(e -> accionAceptar.run());

        BotonPro btnAceptar = new BotonPro("ACEPTAR", Estilos.COLOR_ACCENT, accionAceptar);
        BotonPro btnCancelar = new BotonPro("CANCELAR", Color.GRAY, dialog::dispose);

        panelBtn.add(btnCancelar);
        panelBtn.add(btnAceptar);

        content.add(lblMsg, BorderLayout.NORTH);
        content.add(txtInput, BorderLayout.CENTER);
        content.add(panelBtn, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return respuesta[0];
    }

    // Base para evitar repetir código de configuración
    private static JDialog crearDialogoBase(Component parent, String titulo) {
        // Encontrar la ventana padre correcta
        Window window = null;
        if (parent != null) {
            window = SwingUtilities.getWindowAncestor(parent);
        }
        JDialog dialog = new JDialog(window, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true); // Sin bordes de ventana SO (Estilo Flat total)

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(Estilos.COLOR_PANEL);
        dialog.setContentPane(content);

        dialog.setLocationRelativeTo(parent);

        return dialog;
    }

    public static int mostrarOpciones(Component parent, String titulo, String mensaje, Object[] opciones) {
        JDialog dialog = crearDialogoBase(parent, titulo);
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBorder(new CompoundBorder(new LineBorder(Estilos.COLOR_ACCENT, 1), new EmptyBorder(20, 20, 20, 20)));

        // Procesar mensaje para saltos de línea HTML
        String msgHtml = "<html><div style='text-align:center'>" + mensaje.replace("\n", "<br>") + "</div></html>";

        JLabel lblMsg = new JLabel(msgHtml);
        lblMsg.setForeground(Color.WHITE);
        lblMsg.setFont(Estilos.FONT_PLAIN);
        lblMsg.setHorizontalAlignment(SwingConstants.CENTER);

        // Panel de Botones Dinámico
        JPanel panelBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panelBtn.setBackground(Estilos.COLOR_PANEL);

        // Variable para capturar la respuesta (Array de 1 posición para ser final efectivo)
        final int[] respuesta = {-1};

        for (int i = 0; i < opciones.length; i++) {
            String textoOpcion = opciones[i].toString();
            final int index = i;

            // --- Lógica de Colores Inteligente ---
            Color colorBtn = Estilos.COLOR_ACCENT; // Azul por defecto
            String textoLower = textoOpcion.toLowerCase();

            if (textoLower.contains("cancelar") || textoLower.contains("cerrar")) {
                colorBtn = Color.GRAY;
            } else if (textoLower.contains("borrar") || textoLower.contains("eliminar") || textoLower.contains("descontinuar")) {
                colorBtn = new Color(200, 50, 50); // Rojo
            } else if (textoLower.contains("pedido") || textoLower.contains("0")) {
                colorBtn = Color.ORANGE; // Naranja para acciones de modificación
            }

            // Crear Botón
            BotonPro btn = new BotonPro(textoOpcion.toUpperCase(), colorBtn, () -> {
                respuesta[0] = index;
                dialog.dispose();
            });
            // Hacerlos un poco más pequeños si son muchos
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 40));

            panelBtn.add(btn);
        }

        content.add(lblMsg, BorderLayout.CENTER);
        content.add(panelBtn, BorderLayout.SOUTH);

        dialog.pack();
        // Ajuste para que no quede muy angosta si el mensaje es corto pero hay muchos botones
        if (dialog.getWidth() < 400) dialog.setSize(400, dialog.getHeight());

        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return respuesta[0];
    }
}
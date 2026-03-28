package ui.componentes;

import javax.swing.*;
import java.awt.*;

public class ToastPro extends JWindow {

    public ToastPro(String mensaje, String tipo) {
        setAlwaysOnTop(true);
        setBackground(new Color(0,0,0,0)); // Transparente base

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Color según tipo
                Color c = new Color(40, 40, 40, 240); // Negro semitransparente default
                if(tipo.equals("EXITO")) c = new Color(39, 174, 96, 240); // Verde
                if(tipo.equals("ERROR")) c = new Color(192, 57, 43, 240); // Rojo

                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JLabel lbl = new JLabel(mensaje);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(lbl);

        add(panel);
        pack();

        // Posicionar abajo al centro de la pantalla
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - getWidth()) / 2;
        int y = dim.height - 100;
        setLocation(x, y);
    }

    public void mostrar() {
        setVisible(true);
        // Hilo para desvanecer y cerrar
        new Thread(() -> {
            try {
                Thread.sleep(2500); // Duración visible
                dispose();
            } catch (Exception e) {}
        }).start();
    }

    // Método estático rápido
    public static void show(String msg, String tipo) {
        new ToastPro(msg, tipo).mostrar();
    }
}
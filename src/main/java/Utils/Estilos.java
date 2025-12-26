package Utils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class Estilos {
    // Paleta de Colores basada en la imagen (Dark UI)
    public static final Color COLOR_FONDO = new Color(26, 29, 46);      // #1a1d2e (Fondo General)
    public static final Color COLOR_PANEL = new Color(36, 43, 66);      // #242b42 (Paneles/Tarjetas)
    public static final Color COLOR_ACCENT = new Color(41, 98, 255);    // #2962ff (Azul Botón)
    public static final Color COLOR_TEXTO = new Color(255, 255, 255);   // Blanco
    public static final Color COLOR_TEXTO_SEC = new Color(176, 184, 196); // Gris claro
    public static final Color COLOR_INPUT = new Color(48, 56, 82);      // Input fields
    public static final Color COLOR_BORDER = new Color(60, 70, 100);    // Bordes sutiles

    public static final Font FONT_TITULO = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_PLAIN = new Font("Segoe UI", Font.PLAIN, 14);

    // Método para aplicar tema oscuro a Tablas
    public static void estilizarTabla(JTable tabla) {
        tabla.setBackground(COLOR_PANEL);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setGridColor(COLOR_BORDER);
        tabla.setRowHeight(35);
        tabla.setFont(FONT_PLAIN);
        tabla.setSelectionBackground(COLOR_ACCENT);
        tabla.setSelectionForeground(Color.WHITE);

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(COLOR_INPUT);
        header.setForeground(Color.black);
        header.setFont(FONT_BOLD);
        header.setBorder(null);

        // Centrar celdas (Opcional)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        // tabla.setDefaultRenderer(Object.class, centerRenderer); // Descomentar si quieres todo centrado
    }

    // Método para inputs (TextFields)
    public static void estilizarInput(JTextField txt) {
        txt.setBackground(COLOR_INPUT);
        txt.setForeground(COLOR_TEXTO);
        txt.setCaretColor(COLOR_TEXTO); // Cursor blanco
        txt.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        txt.setFont(FONT_PLAIN);
    }

    // Botón Azul Principal
    public static JButton botonPrincipal(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(COLOR_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(FONT_BOLD);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
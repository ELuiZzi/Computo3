package ui.componentes;

import util.Estilos;
import util.Recursos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BotonPro extends JPanel {
    private JLabel lblContenido; // Cambiamos nombre a contenido
    private Color colorNormal;
    private Color colorHover;
    private Runnable accion;

    // CONSTRUCTOR ACTUALIZADO
    // Ahora recibe "nombreIcono" (String). Puede ser null si no quieres icono.
    public BotonPro(String texto, String nombreIcono, Color colorFondo, Runnable accionAEjecutar) {
        this.accion = accionAEjecutar;
        this.colorNormal = colorFondo;
        this.colorHover = brillarColor(colorFondo);

        setLayout(new GridBagLayout());
        setBackground(colorNormal);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Borde para dar espacio
        setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // Configurar Label con Texto e Icono
        lblContenido = new JLabel(texto);
        lblContenido.setFont(Estilos.FONT_BOLD);
        lblContenido.setForeground(Color.WHITE);

        // Si hay icono, lo cargamos
        if (nombreIcono != null) {
            ImageIcon icon = Recursos.getIcono(nombreIcono);
            if (icon != null) {
                lblContenido.setIcon(icon);
                lblContenido.setIconTextGap(9); // Espacio entre icono y texto
            }
        }

        add(lblContenido);

        // Eventos Mouse (Igual que antes)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { setBackground(colorHover); repaint(); }
            @Override
            public void mouseExited(MouseEvent e) { setBackground(colorNormal); repaint(); }
            @Override
            public void mouseClicked(MouseEvent e) { if(accion != null) accion.run(); }
        });
    }

    // Constructor de compatibilidad (Solo texto, sin icono) - Para no romper código viejo
    public BotonPro(String texto, Color colorFondo, Runnable accionAEjecutar) {
        this(texto, null, colorFondo, accionAEjecutar);
    }

    private Color brillarColor(Color c) {
        int r = Math.min(255, c.getRed() + 30);
        int g = Math.min(255, c.getGreen() + 30);
        int b = Math.min(255, c.getBlue() + 30);
        return new Color(r, g, b);
    }

    public void setTexto(String t) {
        lblContenido.setText(t);
    }
}
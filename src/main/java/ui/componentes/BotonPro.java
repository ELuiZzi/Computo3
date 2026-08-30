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
    private Color colorOriginal;
    private String textoOriginal;
    private Icon iconoOriginal;

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

        inicializarEfectos();

        // Eventos Mouse (Igual que antes)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { setBackground(colorHover); repaint(); }
            @Override
            public void mouseExited(MouseEvent e) { setBackground(colorNormal); repaint(); }
            @Override
            public void mouseClicked(MouseEvent e) { if(isEnabled() && accion != null) accion.run(); }
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

    private void inicializarEfectos() {
        // Guardamos el color que se le haya asignado al botón
        this.colorOriginal = getBackground();

        // Agregamos el efecto visual automático al hacer clic
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (isEnabled()) {
                    // Oscurecemos el color actual en un 20% matemáticamente
                    setBackground(oscurecerColor(getBackground(), 0.8));
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (isEnabled()) {
                    // Regresamos al color original al soltar el clic
                    setBackground(colorOriginal);
                }
            }
        });
    }

    // Método matemático para oscurecer CUALQUIER color dinámicamente
    private Color oscurecerColor(Color color, double factor) {
        int r = Math.max((int) (color.getRed() * factor), 0);
        int g = Math.max((int) (color.getGreen() * factor), 0);
        int b = Math.max((int) (color.getBlue() * factor), 0);
        return new Color(r, g, b, color.getAlpha());
    }

    // =======================================================
    // MÉTODO UNIVERSAL DE CARGA (ANTI-DOBLE CLIC)
    // =======================================================
    public void setEstadoCargando(boolean cargando, String textoTemporal) {
        if (cargando) {
            // Guardamos cómo se veía el label antes de procesar
            this.textoOriginal = lblContenido.getText();
            this.iconoOriginal = lblContenido.getIcon();

            // Cambiamos al modo procesamiento
            lblContenido.setText(textoTemporal);
            lblContenido.setIcon(null); // Ocultamos el icono normal
            setEnabled(false); // BLOQUEAMOS EL BOTÓN para evitar doble clic
            setCursor(new Cursor(Cursor.WAIT_CURSOR)); // Cursor de reloj de arena
        } else {
            // Restauramos el label a la normalidad
            lblContenido.setText(this.textoOriginal);
            lblContenido.setIcon(this.iconoOriginal);
            setEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR)); // Regresa la manita
        }
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        // Validamos que lblContenido ya exista, porque setFont se llama muy temprano en Java
        if (lblContenido != null) {
            lblContenido.setFont(font);
        }
    }
}
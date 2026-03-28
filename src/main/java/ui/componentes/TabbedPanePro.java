package ui.componentes;

import util.Estilos;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TabbedPanePro extends JTabbedPane {

    public TabbedPanePro() {
        // Quitar bordes y fondos por defecto
        setOpaque(true);
        setBackground(Estilos.COLOR_FONDO);
        setForeground(Color.WHITE);
        setFont(Estilos.FONT_BOLD);

        // Aplicar la UI personalizada
        setUI(new BrowserTabUI());
    }

    // --- CLASE INTERNA QUE DIBUJA LAS PESTAÑAS ---
    private class BrowserTabUI extends BasicTabbedPaneUI {

        private int hoverIndex = -1; // Para detectar el mouse

        @Override
        protected void installListeners() {
            super.installListeners();
            // Listener para efecto Hover
            tabPane.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoverIndex = tabForCoordinate(tabPane, e.getX(), e.getY());
                    tabPane.repaint();
                }
            });
            tabPane.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    hoverIndex = -1;
                    tabPane.repaint();
                }
            });
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            // Quitar el borde grueso alrededor del contenido
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(10, 5, 0, 0); // Espacio arriba de las pestañas
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            // No pintar borde de contenido estándar (lo haremos manual si queremos)
            // Opcional: Dibujar una línea delgada del color del panel arriba
            g.setColor(Estilos.COLOR_PANEL);
            g.fillRect(0, 0, tabPane.getWidth(), tabPane.getHeight());
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Colores
            Color colorSeleccionado = Estilos.COLOR_PANEL; // Se funde con el contenido
            Color colorNoSeleccionado = new Color(20, 23, 35); // Más oscuro que el fondo (Efecto pestaña inactiva)
            Color colorHover = new Color(45, 55, 80); // Un poco más claro al pasar mouse

            if (isSelected) {
                g2.setColor(colorSeleccionado);
            } else if (tabIndex == hoverIndex) {
                g2.setColor(colorHover);
            } else {
                g2.setColor(colorNoSeleccionado);
            }

            // DIBUJAR FORMA DE NAVEGADOR (Trapecio o Redondeado)
            // Aquí haremos estilo "Chrome Moderno" (Redondeado arriba)

            // Si está seleccionado, lo hacemos un poco más alto para tapar la línea base
            if (isSelected) {
                g2.fillRoundRect(x, y - 2, w, h + 5, 15, 15);
            } else {
                g2.fillRoundRect(x, y + 2, w, h, 10, 10); // Un poco más abajo y pequeño
            }

            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            // Sin bordes de línea para look "Flat"
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setFont(font);

            if (isSelected) {
                g2.setColor(Estilos.COLOR_ACCENT); // Texto Azul/Verde si está activo
            } else {
                g2.setColor(Color.GRAY); // Texto Gris si está inactivo
            }

            // Centrar texto mejorado
            int mnemIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);

            // Dibujar
            g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        }

        @Override
        protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
            return 40; // Altura fija más cómoda
        }

        @Override
        protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
            return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 20; // Más padding lateral
        }
    }
}
package ui.componentes;

import util.Estilos;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class TablaPro extends JTable {

    public TablaPro(DefaultTableModel model) {
        super(model);
        configurarEstilo();
    }

    private void configurarEstilo() {
        // Colores Base de la Tabla
        setBackground(Estilos.COLOR_PANEL);
        setForeground(Color.WHITE); // Texto de las celdas en BLANCO
        setGridColor(Estilos.COLOR_BORDER);
        setSelectionBackground(Estilos.COLOR_ACCENT);
        setSelectionForeground(Color.WHITE); // Texto al seleccionar en BLANCO

        // Fuentes y Altura
        setFont(Estilos.FONT_PLAIN);
        setRowHeight(35);

        // Quitar bordes feos del scroll al seleccionar
        setFillsViewportHeight(true);
        setShowVerticalLines(false);
        setIntercellSpacing(new Dimension(0, 1));

        // Estilo del Encabezado (Header)
        JTableHeader header = getTableHeader();
        header.setBackground(Estilos.COLOR_INPUT);
        header.setForeground(Color.WHITE); // Texto del Header en BLANCO
        header.setFont(Estilos.FONT_BOLD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Estilos.COLOR_BORDER));

        // Bloquear reordenamiento accidental de columnas por parte del usuario
        header.setReorderingAllowed(false);

        // Aplicar renderizador a todas las celdas por defecto
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Mantenemos el color BLANCO en todo momento
                c.setForeground(Color.WHITE);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Estilos.COLOR_PANEL : new Color(40, 48, 70)); // Efecto Cebra
                } else {
                    c.setBackground(Estilos.COLOR_ACCENT); // Color de selección
                }

                setHorizontalAlignment(SwingConstants.CENTER);

                return c;
            }
        });
    }

}
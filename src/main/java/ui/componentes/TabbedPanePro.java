package ui.componentes;

import util.Estilos;

import javax.swing.*;
import java.awt.*;

public class TabbedPanePro extends JTabbedPane {

    public TabbedPanePro() {
        // 1. Colores base y fuente
        setBackground(new Color(20, 23, 35)); // Color del fondo inactivo
        setForeground(Color.WHITE); // Color del texto inactivo
        setFont(Estilos.FONT_BOLD);

        // 2. MAGIA FLATLAF: Estilo "Card" (Como pestañas de Google Chrome)
        putClientProperty("JTabbedPane.tabType", "card");

        // 3. Ajustar márgenes y alturas
        putClientProperty("JTabbedPane.tabHeight", 40);
        putClientProperty("JTabbedPane.tabInsets", new Insets(5, 20, 5, 20));

        // 4. Colores para el tema FlatLaf (Hover y Seleccionado)
        // Usamos el formato HEX para que FlatLaf lo lea perfectamente
        putClientProperty("JTabbedPane.selectedBackground", Estilos.COLOR_PANEL);
        putClientProperty("JTabbedPane.hoverColor", new Color(45, 55, 80));

        // Ocultar la línea de separación fea debajo de las pestañas
        putClientProperty("JTabbedPane.showTabSeparators", false);
    }
}
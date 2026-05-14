package util;

import javax.swing.ImageIcon;
import java.awt.Font;
import java.awt.Image;
import java.io.File;

public class CacheRecursos {

    // Variables estáticas para mantenerlas en memoria RAM vivas
    private static Image logoTicket;
    private static Font fuenteCodigoBarras;

    // Bloque static: Se ejecuta UNA SOLA VEZ cuando arranca el sistema POS
    static {
        cargarRecursosDesdeDisco();
    }

    private static void cargarRecursosDesdeDisco() {
        System.out.println("[OPTIMIZACIÓN] Cargando recursos estáticos en RAM...");

        // 1. Cargar Logo
        try {
            ImageIcon icon = new ImageIcon("recursos/logo.png");
            logoTicket = icon.getImage();
        } catch (Exception e) {
            System.err.println("Error al cargar logo.png: " + e.getMessage());
        }

        // 2. Cargar Fuente Code39
        try {
            File fileFont = new File("recursos/fuentes/code39.ttf");
            if (fileFont.exists()) {
                fuenteCodigoBarras = Font.createFont(Font.TRUETYPE_FONT, fileFont).deriveFont(24f);
            } else {
                fuenteCodigoBarras = new Font("Serif", Font.PLAIN, 20); // Fallback
            }
        } catch (Exception e) {
            fuenteCodigoBarras = new Font("Serif", Font.PLAIN, 20);
        }
    }

    // Getters ultrarrápidos (Leen de la RAM, 0 milisegundos)
    public static Image getLogoTicket() { return logoTicket; }
    public static Font getFuenteCodigoBarras() { return fuenteCodigoBarras; }
}
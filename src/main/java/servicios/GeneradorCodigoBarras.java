package servicios;

import ui.componentes.JOptionPanePro;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GeneradorCodigoBarras {

    public static void crearEtiquetaBarras(String codigo, String nombreProducto) {
        // Tamaño de etiqueta estándar (ej. 300x150 px)
        int ancho = 300;
        int alto = 150;

        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imagen.createGraphics();

        // 1. Configuración de Calidad y Fondo
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, ancho, alto);
        g2d.setColor(Color.BLACK);

        try {
            // 2. Cargar Fuentes
            // Fuente Normal para textos
            Font fuenteTexto = new Font("Arial", Font.PLAIN, 12);
            // Fuente de Barras (Asegúrate de tener Code39.ttf en la raíz)
            Font fuenteBarras;
            try {
                File fileFont = new File("recursos/fuentes/code39.ttf");
                if (fileFont.exists()) {
                    fuenteBarras = Font.createFont(Font.TRUETYPE_FONT, fileFont).deriveFont(40f); // Tamaño barras
                } else {
                    System.err.println("No se encontró Code39.ttf, usando default");
                    fuenteBarras = new Font("Serif", Font.PLAIN, 30);
                }
            } catch (Exception e) {
                fuenteBarras = new Font("Serif", Font.PLAIN, 30);
            }

            // 3. Dibujar Nombre Producto (Arriba, centrado, cortado si es largo)
            g2d.setFont(fuenteTexto);
            if (nombreProducto.length() > 25) nombreProducto = nombreProducto.substring(0, 25) + "...";
            centrarTexto(g2d, nombreProducto, ancho, 25);

            // 4. Dibujar Código de Barras (Centro)
            // IMPORTANTE: Code39 requiere asteriscos al inicio y final (*CODIGO*)
            g2d.setFont(fuenteBarras);
            String codigoParaBarras = "*" + codigo.toUpperCase() + "*";
            centrarTexto(g2d, codigoParaBarras, ancho, 80);

            // 5. Dibujar Código Legible (Abajo)
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            centrarTexto(g2d, codigo, ancho, 120);

            g2d.dispose();

            // 6. Guardar Imagen
            File carpeta = new File("codigos_barras");
            if (!carpeta.exists()) carpeta.mkdirs();

            // Limpiar nombre de archivo
            String nombreArchivo = "Barra_" + codigo.replaceAll("[^a-zA-Z0-9.-]", "_") + ".png";
            File salida = new File(carpeta, nombreArchivo);
            ImageIO.write(imagen, "png", salida);

            JOptionPanePro.mostrarMensaje(null, "Código Generado", "Guardado en: codigos_barras/" + nombreArchivo, "INFO");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(null, "Error", "No se pudo crear el código: " + e.getMessage(), "ERROR");
        }
    }

    private static void centrarTexto(Graphics2D g, String texto, int anchoImg, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (anchoImg - fm.stringWidth(texto)) / 2;
        g.drawString(texto, x, y);
    }
}
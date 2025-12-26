package Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import ElementosPro.*;

public class GeneradorEtiquetas {

    // Constante mágica para indicar que queremos cálculo automático
    public static final float TAMANIO_AUTO = -1f;

    // Clase para configurar cada línea de texto
    public static class LineaTexto {
        public String texto;
        public float tamanioFuente; // Ej: 40f
        public boolean esNegrita;
        public Color color;
        public int posY; // Posición vertical (Y) en pixeles

        public LineaTexto(String texto, float tamanio, boolean esNegrita, int posY) {
            this.texto = texto;
            this.tamanioFuente = tamanio;
            this.esNegrita = esNegrita;
            this.posY = posY;
        }

        // Constructor con color personalizado
        public LineaTexto(String texto, float tamanio, boolean esNegrita, int posY, Color c) {
            this(texto, tamanio, esNegrita, posY);
            this.color = c;
        }
    }

    /**
     * @param rutaImagenBase Ruta del archivo JPG/PNG base (Plantilla)
     * @param rutaFuenteTTF Ruta del archivo .ttf (Fuente personalizada)
     * @param nombreProducto Nombre para el archivo de salida
     * @param lineas Lista de objetos LineaTexto con lo que se va a imprimir
     */
    public static void crearImagenEtiqueta(String rutaImagenBase, String rutaFuenteTTF, String nombreProducto, List<LineaTexto> lineas) {
        try {
            // 1. Cargar Imagen Base
            File fileImg = new File(rutaImagenBase);
            if (!fileImg.exists()) {
                System.err.println("Imagen base no encontrada: " + rutaImagenBase);
                return;
            }
            BufferedImage imagen = ImageIO.read(fileImg);

            // 2. Preparar Gráficos
            Graphics2D g2d = imagen.createGraphics();

            // Calidad Alta (Anti-aliasing)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 3. Cargar Fuente Personalizada
            Font fuenteBase;
            try {
                File fileFont = new File(rutaFuenteTTF);
                if (fileFont.exists()) {
                    fuenteBase = Font.createFont(Font.TRUETYPE_FONT, fileFont);
                } else {
                    System.err.println("Fuente no encontrada, usando Arial default.");
                    fuenteBase = new Font("Arial", Font.PLAIN, 12);
                }
            } catch (Exception e) {
                fuenteBase = new Font("Arial", Font.PLAIN, 12);
            }

            int anchoImagen = imagen.getWidth();
            // El límite es el ancho menos el 15% (7.5% de cada lado aprox)
            int anchoMaximo = (int) (anchoImagen * 0.85);

            for (LineaTexto linea : lineas) {
                int estilo = linea.esNegrita ? Font.BOLD : Font.PLAIN;
                Font fuenteFinal;

                if (linea.tamanioFuente == TAMANIO_AUTO) {
                    // --- ALGORITMO DE AJUSTE AUTOMÁTICO ---
                    float size = 150f; // Empezamos con un tamaño muy grande
                    fuenteFinal = fuenteBase.deriveFont(estilo, size);
                    FontMetrics fm = g2d.getFontMetrics(fuenteFinal);

                    // Reducimos la fuente hasta que el texto quepa en el ancho máximo
                    // O hasta que lleguemos a un tamaño mínimo legible (ej: 10)
                    while (fm.stringWidth(linea.texto) > anchoMaximo && size > 10) {
                        size -= 2f; // Bajamos de 2 en 2 para ser rápidos
                        fuenteFinal = fuenteBase.deriveFont(estilo, size);
                        fm = g2d.getFontMetrics(fuenteFinal);
                    }
                } else {
                    // Tamaño fijo manual
                    fuenteFinal = fuenteBase.deriveFont(estilo, linea.tamanioFuente);
                }

                g2d.setFont(fuenteFinal);
                g2d.setColor(linea.color);

                // Centrar Horizontalmente
                FontMetrics fm = g2d.getFontMetrics();
                int x = (anchoImagen - fm.stringWidth(linea.texto)) / 2;

                g2d.drawString(linea.texto, x, linea.posY);
            }

            g2d.dispose();

            // 5. Exportar Imagen
            File carpeta = new File("etiquetas_generadas");
            if (!carpeta.exists()) carpeta.mkdirs(); // Crear carpeta si no existe

            // Limpiar nombre archivo (quitar caracteres raros)
            String nombreArchivo = "Etiqueta_" + nombreProducto.replaceAll("[^a-zA-Z0-9.-]", "_") + ".png";
            File salida = new File(carpeta, nombreArchivo);

            ImageIO.write(imagen, "png", salida);

            System.out.println("Imagen creada exitosamente en: " + salida.getAbsolutePath());
            JOptionPanePro.mostrarMensaje(null, "", "Etiqueta Creada", "INFO");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(null, "Error", "No se pudo crear la imagen: " + e.getMessage(), "ERROR");
        }
    }
}
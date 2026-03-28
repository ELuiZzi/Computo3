package servicios;


import util.Recursos;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;

public class GeneradorTicketDigital {

    private static final Color COLOR_AZUL = new Color(0, 158, 227);
    private static final Color COLOR_FONDO = Color.WHITE;
    private static final Color COLOR_TEXTO = new Color(51, 51, 51);
    private static final Color COLOR_GRIS_CLARO = new Color(240, 240, 240);

    /**
     * Método Unificado para generar comprobantes digitales.
     * @param folio Folio de la venta/orden
     * @param cliente Nombre del cliente
     * @param concepto Lista de productos o nombre del servicio
     * @param detalle (Opcional) Detalles extra o notas. Si es null o vacío, se oculta.
     * @param total Monto total
     * @param esRecoleccion Si es true, añade el sello verde de "PAGADO" y cambia el subtítulo.
     */
    public static void generarComprobanteUniversal(String folio, String cliente, String concepto, String detalle, double total, boolean esRecoleccion) {
        int ancho = 500;
        int alto = 850;

        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = imagen.createGraphics();

        // Configuración de Calidad
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Fondo
        g2.setColor(COLOR_FONDO);
        g2.fillRect(0, 0, ancho, alto);

        // 2. Marca de Agua
        try {
            Image logo = Recursos.getImagenApp();
            if (logo != null) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
                int logoSize = 400;
                g2.drawImage(logo, (ancho - logoSize)/2, (alto - logoSize)/2, logoSize, logoSize, null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        } catch (Exception e) {}

        int y = 50;

        // 3. Encabezado
        try {
            Image logoHeader = Recursos.getImagenApp();
            if (logoHeader != null) g2.drawImage(logoHeader, 30, y, 60, 60, null);
        } catch(Exception e){}

        g2.setColor(COLOR_AZUL);
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.drawString("LUMTECH", 100, y + 25);
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("COMPROBANTE", 100, y + 45);
        g2.drawLine(215, y+30, 215, y+50);

        y += 100;

        // 4. Título
        g2.setColor(COLOR_TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString(esRecoleccion ? "Comprobante de Recolección" : "Comprobante de Operación", 30, y);

        y += 25;
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy, HH:mm");
        g2.drawString(sdf.format(new Date()), 30, y);

        y += 40;
        g2.setColor(COLOR_GRIS_CLARO);
        g2.fillRect(30, y, ancho - 60, 2);
        y += 50;

        // 5. Sello PAGADO (Solo si es recolección)
        if (esRecoleccion) {
            g2.setColor(new Color(39, 174, 96));
            g2.fillRoundRect(ancho/2 - 60, y - 30, 120, 35, 35, 35);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("PAGADO", ancho/2 - 32, y - 7);
            y += 50;
        }

        // 6. Monto
        g2.setColor(COLOR_TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        String precioStr = String.format("$ %.2f MXN", total);
        int wPrecio = g2.getFontMetrics().stringWidth(precioStr);
        g2.drawString(precioStr, (ancho - wPrecio)/2, y);

        y += 30;
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.drawString("Folio: " + folio, 30, y);

        y += 50;
        // Caja de Detalles
        g2.setColor(new Color(248, 249, 250));
        g2.fillRoundRect(20, y, ancho - 40, 220, 20, 20); // Caja más alta por si hay detalles

        int yDet = y + 40;
        int xDet = 40;

        dibujarPuntoAzul(g2, xDet, yDet);
        dibujarCampo(g2, "Cliente", cliente, xDet + 20, yDet);
        yDet += 60;

        dibujarPuntoAzul(g2, xDet, yDet);
        // Cortar concepto si es muy largo
        if(concepto.length() > 38) concepto = concepto.substring(0, 38) + "...";
        dibujarCampo(g2, "Concepto", concepto, xDet + 20, yDet);
        yDet += 60;

        // Solo dibujar detalle si existe
        if (detalle != null && !detalle.isEmpty()) {
            dibujarPuntoAzul(g2, xDet, yDet);
            if(detalle.length() > 38) detalle = detalle.substring(0, 38) + "...";
            dibujarCampo(g2, "Notas / Detalles", detalle, xDet + 20, yDet);
        }

        // 7. Código de Barras
        y = alto - 120;
        try {
            File fileFont = new File("recursos/fuentes/code39.ttf");
            Font fontBarra = Font.createFont(Font.TRUETYPE_FONT, fileFont).deriveFont(50f);
            g2.setColor(Color.BLACK);
            g2.setFont(fontBarra);

            String codigo = "*" + folio + "*";
            int wBarra = g2.getFontMetrics().stringWidth(codigo);
            g2.drawString(codigo, (ancho - wBarra)/2, y);

            y += 25;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String textoHumano = "Escanea este código en caja";
            int wTexto = g2.getFontMetrics().stringWidth(textoHumano);
            g2.drawString(textoHumano, (ancho - wTexto)/2, y);

        } catch (Exception e) {
            g2.setColor(Color.BLACK);
            g2.drawString("Folio: " + folio, 150, y);
        }

        g2.dispose();

        // Guardar
        try {
            File carpeta = new File("comprobantes");
            if (!carpeta.exists()) carpeta.mkdirs();
            String nombreArchivo = "Comprobante_" + folio + ".png";
            File salida = new File(carpeta, nombreArchivo);
            ImageIO.write(imagen, "png", salida);
            Desktop.getDesktop().open(salida);
        } catch (Exception e) {}
    }

    private static void dibujarPuntoAzul(Graphics2D g2, int x, int y) {
        g2.setColor(COLOR_AZUL);
        g2.fill(new Ellipse2D.Double(x, y - 5, 8, 8));
        g2.setColor(new Color(200, 200, 200));
        g2.drawLine(x + 4, y + 5, x + 4, y + 35);
    }

    private static void dibujarCampo(Graphics2D g2, String titulo, String valor, int x, int y) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(titulo, x, y - 5);
        g2.setColor(COLOR_TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(valor, x, y + 12);
    }
}
package servicios;

import javax.print.PrintService;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import ui.componentes.JOptionPanePro;
import javax.swing.ImageIcon;

public class ImpresoraTicket {

    private static String impresoraSeleccionada = null;
    private static boolean autoImprimir = true;

    // --- CONFIGURACIÓN ---
    public static void setImpresora(String impresora) { impresoraSeleccionada = impresora; }
    public static void setAutoImprimir(boolean valor) { autoImprimir = valor; }
    public static boolean isAutoImprimir() { return autoImprimir; }

    public static void cargarConfiguracionInicial() {
        try {
            File configFile = new File("config.properties");
            if (configFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                    impresoraSeleccionada = props.getProperty("ticket.impresora");
                    autoImprimir = Boolean.parseBoolean(props.getProperty("ticket.auto_imprimir", "true"));
                    System.out.println("Imp: " + impresoraSeleccionada);
                }
            }
        } catch (Exception e) {}
    }

    public static List<String> obtenerImpresorasDisponibles() {
        List<String> lista = new ArrayList<>();
        PrintService[] services = PrinterJob.lookupPrintServices(); // Usamos PrinterJob ahora
        for (PrintService s : services) lista.add(s.getName());
        Collections.sort(lista);
        return lista;
    }

    // --- MÉTODO PRINCIPAL DE IMPRESIÓN ---
    public static void imprimir(String textoTicket) {
        if (impresoraSeleccionada == null || impresoraSeleccionada.isEmpty()) {
            JOptionPanePro.mostrarMensaje(null, "Aviso", "Configura la impresora primero.", "ADVERTENCIA");
            return;
        }

        try {
            // 1. Buscar servicio
            PrintService servicio = null;
            PrintService[] services = PrinterJob.lookupPrintServices();
            for (PrintService s : services) {
                if (s.getName().equalsIgnoreCase(impresoraSeleccionada)) {
                    servicio = s;
                    break;
                }
            }

            if (servicio == null) {
                JOptionPanePro.mostrarMensaje(null, "Error", "Impresora no encontrada.", "ERROR");
                return;
            }

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(servicio);

            // 2. CONFIGURACIÓN EXACTA DEL PAPEL (55mm)
            PageFormat pf = new PageFormat();
            Paper paper = new Paper();

            // Conversión: 55mm / 25.4 * 72 DPI = ~156 puntos.
            // Le damos 155 para asegurar que entre.
            // Altura 3000 para simular rollo continuo.
            double width = 155;
            double height = 3000;

            paper.setSize(width, height);

            // Márgenes en 0 absoluto para que el driver controle el resto
            paper.setImageableArea(0, 0, width, height);

            pf.setPaper(paper);
            pf.setOrientation(PageFormat.PORTRAIT);

            // Validar el PageFormat con el trabajo actual (Esto ayuda a que el driver lo acepte)
            PageFormat validatePage = job.validatePage(pf);

            // 3. Asignar contenido
            job.setPrintable(new TicketPrintable(textoTicket), validatePage);

            // 4. Imprimir sin diálogo
            job.print();

        } catch (PrinterException e) {
            e.printStackTrace();
            JOptionPanePro.mostrarMensaje(null, "Error Impresión", e.getMessage(), "ERROR");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- CLASE INTERNA QUE DIBUJA EL TICKET ---
    static class TicketPrintable implements Printable {
        private String contenido;

        public TicketPrintable(String texto) {
            this.contenido = texto;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) return NO_SUCH_PAGE;

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int y = 0;

            // 1. LOGO (Igual que antes)
            try {
                ImageIcon icon = new ImageIcon("recursos/logo.png");
                Image img = icon.getImage();
                int logoAncho = 60;
                int logoAlto = 60;
                int xLogo = (138 - logoAncho) / 2;
                g2d.drawImage(img, xLogo, 0, logoAncho, logoAlto, null);
                y = logoAlto + 10;
            } catch (Exception e) {}

            // 2. PREPARAR FUENTES
            Font fontTexto = new Font("Consolas", Font.PLAIN, 9);
            Font fontNegrita = new Font("Consolas", Font.BOLD, 10);
            Font fontBarra = null;

            // Cargar Fuente Code39
            try {
                // Ajusta la ruta si es necesario. "recursos/fuentes/code39.ttf"
                File fileFont = new File("recursos/fuentes/code39.ttf");
                if (fileFont.exists()) {
                    fontBarra = Font.createFont(Font.TRUETYPE_FONT, fileFont).deriveFont(24f); // Tamaño grande para el código
                } else {
                    fontBarra = new Font("Serif", Font.PLAIN, 20); // Fallback
                }
            } catch (Exception e) {
                fontBarra = new Font("Serif", Font.PLAIN, 20);
            }

            g2d.setColor(Color.BLACK);
            int lineHeight = g2d.getFontMetrics(fontTexto).getHeight();

            // 3. DIBUJAR LÍNEAS
            String[] lineas = contenido.split("\n");

            for (String linea : lineas) {

                // DETECTAR CÓDIGO DE BARRAS
                if (linea.startsWith("<<<BARCODE:") && linea.endsWith(">>>")) {
                    // Extraer dato: <<<BARCODE:12345>>> -> 12345
                    String data = linea.substring(11, linea.length() - 3);
                    String codigoFinal = "*" + data + "*"; // Code39 necesita asteriscos

                    g2d.setFont(fontBarra);

                    // Centrar código
                    int anchoBarra = g2d.getFontMetrics().stringWidth(codigoFinal);
                    int xBarra = (138 - anchoBarra) / 2;

                    // Dibujar Barras
                    g2d.drawString(codigoFinal, xBarra, y + 20); // Un poco más de espacio arriba

                    // Dibujar Texto Humano debajo
                    g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                    String textoHumano = data;
                    int anchoTextoH = g2d.getFontMetrics().stringWidth(textoHumano);
                    g2d.drawString(textoHumano, (138 - anchoTextoH) / 2, y + 35);

                    y += 50; // Espacio que ocupa el código
                    continue; // Saltar al siguiente ciclo
                }

                // DIBUJAR TEXTO NORMAL
                if (linea.contains("TOTAL:")) {
                    g2d.setFont(fontNegrita);
                } else {
                    g2d.setFont(fontTexto);
                }

                g2d.drawString(linea, 0, y);
                y += lineHeight;
            }

            g2d.setFont(fontTexto);
            g2d.drawString(".", 0, y + 10); // Corte

            return PAGE_EXISTS;
        }
    }
}
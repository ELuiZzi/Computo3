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
    // --- MÉTODO PRINCIPAL DE IMPRESIÓN ---
    public static void imprimir(String textoTicket) {
        if (impresoraSeleccionada == null || impresoraSeleccionada.isEmpty()) {
            JOptionPanePro.mostrarMensaje(null, "Aviso", "Configura la impresora primero.", "ADVERTENCIA");
            return;
        }

        try {
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

            // ==========================================
            // CÁLCULO DINÁMICO DE LA ALTURA DEL TICKET
            // ==========================================
            int alturaCalculada = 0;

            // 1. Sumar altura del logo (si existe)
            java.io.File logoFile = new java.io.File("recursos/logo.png");
            if (logoFile.exists()) {
                alturaCalculada += 70; // 60 de alto + 10 de margen
            }

            // 2. Sumar altura del texto y códigos de barras
            String[] lineas = textoTicket.split("\n");
            int altoLineaTexto = 11; // Altura promedio de fuente Consolas a 9pt

            for (String linea : lineas) {
                if (linea.startsWith("<<<BARCODE:") && linea.endsWith(">>>")) {
                    alturaCalculada += 50; // Espacio que ocupa el bloque del código de barras
                } else {
                    alturaCalculada += altoLineaTexto; // Espacio de una línea de texto normal
                }
            }

            // 3. Añadir margen inferior de seguridad para el corte (evita cortar letras a la mitad)
            alturaCalculada += 40;

            // ==========================================

            PageFormat pf = new PageFormat();
            Paper paper = new Paper();

            double width = 155; // 55mm exactos
            double height = alturaCalculada; // ALTURA DINÁMICA APLICADA

            paper.setSize(width, height);
            paper.setImageableArea(0, 0, width, height);

            pf.setPaper(paper);
            pf.setOrientation(PageFormat.PORTRAIT);

            PageFormat validatePage = job.validatePage(pf);

            job.setPrintable(new TicketPrintable(textoTicket), validatePage);
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
                Image img = util.CacheRecursos.getLogoTicket();
                if (img != null) {
                    int logoAncho = 60;
                    int logoAlto = 60;
                    int xLogo = (138 - logoAncho) / 2;
                    g2d.drawImage(img, xLogo, 0, logoAncho, logoAlto, null);
                    y = logoAlto + 10;
                }
            } catch (Exception e) {}

            // 2. PREPARAR FUENTES
            Font fontTexto = new Font("Consolas", Font.PLAIN, 9);
            Font fontNegrita = new Font("Consolas", Font.BOLD, 10);

            // Cargar Fuente Code39
            // FUENTE DE BARRAS (DESDE CACHÉ RAM)
            Font fontBarra = util.CacheRecursos.getFuenteCodigoBarras();

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
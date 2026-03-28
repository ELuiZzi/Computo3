package servicios;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GeneradorPDF {

    public static void crearOrdenServicio(int folio, String cliente, String telefono,
  String equipo, String falla, String notas, String tintas, String pathDestino) {
        Document document = new Document(PageSize.LETTER); // Tamaño Carta

        // --- 1. CONVERSIÓN A MAYÚSCULAS AUTOMÁTICA ---
        // Protegemos contra null usando (s != null ? s.toUpperCase() : "")
        String clienteUp = (cliente != null) ? cliente.toUpperCase() : "";
        String equipoUp = (equipo != null) ? equipo.toUpperCase() : "";
        String fallaUp = (falla != null) ? falla.toUpperCase() : "";
        String notasUp = (notas != null) ? notas.toUpperCase() : "";
        // El teléfono no suele necesitar mayúsculas, pero por si acaso
        String telUp = (telefono != null) ? telefono.toUpperCase() : "";

        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pathDestino));
            document.open();

            // ==========================================
            // PÁGINA 1: ORDEN DE TRABAJO (GENERADA)
            // ==========================================

            // 1. LOGO (Esquina superior izquierda)
            try {
                // Asume que logo.png está en la raíz o carpeta recursos
                Image logo = Image.getInstance("recursos/logo2.png");
                logo.scaleToFit(120, 100); // Ajustar tamaño
                logo.setAlignment(Element.ALIGN_LEFT);
                document.add(logo);
            } catch (Exception e) {
                // Si no hay logo, no pasa nada
            }

            // 2. Encabezado
            Paragraph empresa = new Paragraph("LUMTECH\nOrden de Servicio", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
            empresa.setAlignment(Element.ALIGN_CENTER);
            document.add(empresa);

            document.add(new Paragraph("\n")); // Espacio

            // 3. Tabla de Datos Principales
            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(10f);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            // Fila 1
            agregarCelda(tabla, "FOLIO: " + folio, true);
            agregarCelda(tabla, "FECHA: " + sdf.format(new Date()), true);

            // Fila 2 (Cliente y Teléfono)
            agregarCelda(tabla, "CLIENTE: " + clienteUp, false);
            agregarCelda(tabla, "TEL: " + telUp, false);

            // Fila 3 (Equipo ocupa las 2 columnas para que quepa bien)
            PdfPCell celdaEquipo = new PdfPCell(new Phrase("EQUIPO: " + equipoUp, FontFactory.getFont(FontFactory.HELVETICA, 10)));
            celdaEquipo.setColspan(2);
            celdaEquipo.setPadding(6);
            tabla.addCell(celdaEquipo);

            document.add(tabla);

            // 4. Detalles
            document.add(new Paragraph("\nFALLA REPORTADA:",
        FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            document.add(new Paragraph(fallaUp));

            document.add(new Paragraph("\nNOTAS / ACCESORIOS:",
        FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            document.add(new Paragraph(notasUp));

            // 5. NIVELES DE TINTA (Solo si es Impresora y hay datos)
            if (tintas != null && !tintas.isEmpty() && tintas.contains(",")) {
                document.add(new Paragraph("\nNIVELES DE TINTA (Recepción):", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));

                PdfPTable tTintas = new PdfPTable(4);
                tTintas.setWidthPercentage(60);
                tTintas.setHorizontalAlignment(Element.ALIGN_LEFT);
                tTintas.setSpacingBefore(5);

                String[] vals = tintas.split(","); // C, M, Y, K
                // Cyan
                agregarCeldaColor(tTintas, "C: " + vals[0] + "%", BaseColor.CYAN);
                // Magenta
                agregarCeldaColor(tTintas, "M: " + vals[1] + "%", BaseColor.MAGENTA);
                // Yellow
                agregarCeldaColor(tTintas, "Y: " + vals[2] + "%", BaseColor.YELLOW);
                // Key (Black)
                agregarCeldaColor(tTintas, "K: " + vals[3] + "%", BaseColor.LIGHT_GRAY);

                document.add(tTintas);
            }

            // 6. Espacio para Firma
            document.add(new Paragraph("\n\n\n\n\n"));
            LineSeparator linea = new LineSeparator();
            linea.setPercentage(40);
            document.add(linea);

            Paragraph firma = new Paragraph("FIRMA DE CONFORMIDAD DEL CLIENTE", FontFactory.getFont(FontFactory.HELVETICA, 8));
            firma.setAlignment(Element.ALIGN_CENTER);
            document.add(firma);

            // ==========================================
            // PÁGINA 2: TÉRMINOS Y CONDICIONES (ADJUNTA)
            // ==========================================
            try {
                PdfReader reader = new PdfReader("recursos/pdf/terminos.pdf"); // Archivo existente en raíz
                int n = reader.getNumberOfPages();
                PdfContentByte cb = writer.getDirectContent();

                for (int i = 1; i <= n; i++) {
                    document.newPage(); // Crear página en blanco en el documento destino
                    PdfImportedPage page = writer.getImportedPage(reader, i);
                    // Ajustar al tamaño carta si es necesario, o usar el tamaño original
                    cb.addTemplate(page, 0, 0);
                }
            } catch (Exception e) {
                // Si no encuentra el archivo, agrega un texto de aviso
                document.newPage();
                document.add(new Paragraph("Nota: Archivo 'terminos.pdf' no encontrado en la carpeta del sistema."));
            }

            document.close();
            java.awt.Desktop.getDesktop().open(new java.io.File(pathDestino));

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Error PDF: " + e.getMessage());
        }
    }

    private static void agregarCelda(PdfPTable tabla, String texto, boolean bold) {
        Font fuente = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10) : FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuente));
        cell.setPadding(6);
        tabla.addCell(cell);
    }

    private static void agregarCeldaColor(PdfPTable tabla, String texto, BaseColor color) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cell.setBackgroundColor(color);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        tabla.addCell(cell);
    }
}
package servicios;

import java.awt.print.PrinterJob;
import java.io.File;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Sides;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import javax.print.attribute.standard.Chromaticity;

public class ImpresorOrdenes {

    public static void imprimirOrdenSilenciosa(String rutaFicheroPDF) {
        try {
            // 1. Cargamos el PDF que acabas de generar
            File file = new File(rutaFicheroPDF);
            PDDocument document = PDDocument.load(file);

            // 2. Preparamos el trabajo de impresión
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(document));

            // ==========================================
            // 3. LA MAGIA: Configuramos los atributos físicos
            // ==========================================
            PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();

            // Forzar 2 copias exactas
            atributos.add(new Copies(2));

            // Forzar impresión a doble cara (dando vuelta por el lado largo de la hoja)
            atributos.add(Sides.TWO_SIDED_LONG_EDGE);

            // ¡NUEVO! Forzar impresión en escala de grises / Blanco y Negro
            atributos.add(Chromaticity.MONOCHROME);

            // 4. Mandamos a la impresora predeterminada de Windows (false = sin ventanas)
            System.out.println("Enviando orden a la impresora en segundo plano...");
            job.print(atributos);

            // 5. Cerramos el documento para liberar memoria
            document.close();
            System.out.println("¡Impresión automática exitosa!");

        } catch (Exception e) {
            e.printStackTrace();
            ui.componentes.JOptionPanePro.mostrarMensaje(null, "Error de Impresión",
                    "No se pudo imprimir la orden automáticamente: " + e.getMessage(), "ERROR");
        }
    }
}
package servicios;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import config.ConexionBD;


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

    // =========================================================================
    // NUEVO MÓDULO: INTELIGENCIA DE NEGOCIOS (ROTACIÓN Y DEAD STOCK)
    // =========================================================================

    public static void generarReporteRotacion(String rutaDestino, int mes, int anio) throws Exception {
        Document documento = new Document(PageSize.A4);

        // Array simple para obtener el nombre del mes
        String[] nombresMeses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        String nombreMes = nombresMeses[mes - 1];

        try (Connection conn = ConexionBD.conectar()) {
            PdfWriter.getInstance(documento, new FileOutputStream(rutaDestino));
            documento.open();

            // =========================================================
            // PÁGINA 1: REPORTE MENSUAL (Mes seleccionado)
            // =========================================================
            Font fuenteTitulo = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph tituloMensual = new Paragraph("Análisis de Rotación - " + nombreMes + " " + anio, fuenteTitulo);
            tituloMensual.setAlignment(Element.ALIGN_CENTER);
            tituloMensual.setSpacingAfter(20);
            documento.add(tituloMensual);

            // 1. MÁS VENDIDOS (MES)
            documento.add(crearSubtitulo("Top 15 - Productos Más Vendidos del Mes", new BaseColor(46, 204, 113)));
            documento.add(generarTablaRotacion(conn,
                    "SELECT p.id, p.nombre, SUM(d.cantidad) as unidades " +
                            "FROM productos p " +
                            "JOIN detalle_venta d ON p.id = d.id_producto " +
                            "JOIN ventas v ON d.id_venta = v.id " +
                            "WHERE MONTH(v.fecha) = ? AND YEAR(v.fecha) = ? " +
                            "GROUP BY p.id ORDER BY unidades DESC LIMIT 15",
                    "Unidades Vendidas", mes, anio));

            // 2. MENOS VENDIDOS (MES)
            documento.add(crearSubtitulo("Top 15 - Baja Rotación en el Mes", new BaseColor(255, 140, 0)));
            documento.add(generarTablaRotacion(conn,
                    "SELECT p.id, p.nombre, SUM(d.cantidad) as unidades " +
                            "FROM productos p " +
                            "JOIN detalle_venta d ON p.id = d.id_producto " +
                            "JOIN ventas v ON d.id_venta = v.id " +
                            "WHERE MONTH(v.fecha) = ? AND YEAR(v.fecha) = ? " +
                            "GROUP BY p.id ORDER BY unidades ASC LIMIT 15",
                    "Unidades Vendidas", mes, anio));

            // Nota: Se eliminó el "Dead Stock" mensual por ser redundante.

            // =========================================================
            // PÁGINA 2: REPORTE HISTÓRICO (Global)
            // =========================================================
            documento.newPage(); // Salto de página

            Paragraph tituloHistorico = new Paragraph("Análisis Histórico Global (Todo el tiempo)", fuenteTitulo);
            tituloHistorico.setAlignment(Element.ALIGN_CENTER);
            tituloHistorico.setSpacingAfter(20);
            documento.add(tituloHistorico);

            // 1. MÁS VENDIDOS (HISTÓRICO)
            documento.add(crearSubtitulo("Top 15 - Productos Más Vendidos", new BaseColor(46, 204, 113)));
            documento.add(generarTablaRotacion(conn,
                    "SELECT p.id, p.nombre, SUM(d.cantidad) as unidades " +
                            "FROM productos p JOIN detalle_venta d ON p.id = d.id_producto " +
                            "GROUP BY p.id ORDER BY unidades DESC LIMIT 15",
                    "Unidades Totales"));

            // 2. MENOS VENDIDOS (HISTÓRICO)
            documento.add(crearSubtitulo("Top 15 - Baja Rotación (Al menos 1 venta)", new BaseColor(255, 140, 0)));
            documento.add(generarTablaRotacion(conn,
                    "SELECT p.id, p.nombre, SUM(d.cantidad) as unidades " +
                            "FROM productos p JOIN detalle_venta d ON p.id = d.id_producto " +
                            "GROUP BY p.id ORDER BY unidades ASC LIMIT 15",
                    "Unidades Totales"));

            // 3. DEAD STOCK (HISTÓRICO - ESTE ES EL QUE IMPORTA)
            documento.add(crearSubtitulo("Dead Stock Histórico (NUNCA VENDIDOS)", new BaseColor(231, 76, 60)));
            documento.add(generarTablaRotacion(conn,
                    "SELECT p.id, p.nombre, p.stock as unidades " +
                            "FROM productos p LEFT JOIN detalle_venta d ON p.id = d.id_producto " +
                            "WHERE d.id_producto IS NULL ORDER BY p.nombre ASC",
                    "Stock Estancado"));

            documento.close();
        }
    }
    private static Paragraph crearSubtitulo(String texto, BaseColor color) {
        Font fuente = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, color);
        Paragraph p = new Paragraph(texto, fuente);
        p.setSpacingBefore(15);
        p.setSpacingAfter(10);
        return p;
    }

    // ACTUALIZACIÓN MAGISTRAL: Agregamos "Object... parametros" para inyectar variables de forma segura
    private static PdfPTable generarTablaRotacion(Connection conn, String sql, String tituloColumna3, Object... parametros) throws Exception {
        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 6f, 2.5f});

        Font fontHeader = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        String[] headers = {"ID / Código", "Nombre del Producto", tituloColumna3};

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(new BaseColor(41, 98, 255)); // Azul Lumtech
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(cell);
        }

        Font fontData = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            // Inyectar los parámetros dinámicos (Mes y Año) si existen
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tabla.addCell(new PdfPCell(new Phrase(String.valueOf(rs.getInt(1)), fontData)));
                    tabla.addCell(new PdfPCell(new Phrase(rs.getString(2), fontData)));

                    PdfPCell celdaUnidades = new PdfPCell(new Phrase(String.valueOf(rs.getInt(3)), fontData));
                    celdaUnidades.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabla.addCell(celdaUnidades);
                }
            }
        }
        return tabla;
    }
}
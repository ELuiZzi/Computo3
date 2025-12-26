package Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GeneradorTicket {

    // Clase auxiliar para pasar datos del producto al ticket sin depender de JTable
    public static class ItemTicket {
        String nombre;
        int cantidad;
        double subtotal;

        public ItemTicket(String n, int c, double s) {
            this.nombre = n; this.cantidad = c; this.subtotal = s;
        }
    }

    public static String crearTicket(int idVenta, String fechaStr, List<ItemTicket> items, double totalVenta) {
        StringBuilder sb = new StringBuilder();

        // Formato de fecha para el texto visible
        if (fechaStr == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            fechaStr = sdf.format(new Date());
        }

        double baseImponible = totalVenta / 1.16;
        double iva = totalVenta - baseImponible;

        // --- ENCABEZADO ---
        sb.append("===========LUMTECH==========\n");
        sb.append("Folio: ").append(idVenta).append("\n");
        sb.append("Fecha: ").append(fechaStr).append("\n");
        sb.append("----------------------------\n");
        sb.append(String.format("%-13s %3s %9s\n", "PROD", "CAN", "TOTAL"));
        sb.append("----------------------------\n");

        // --- PRODUCTOS CON SALTO DE LÍNEA ---
        for (ItemTicket item : items) {
            // Ancho máximo para la columna nombre (13 caracteres)
            List<String> lineasNombre = dividirTexto(item.nombre, 13);

            // Primera línea: Lleva Nombre, Cantidad y Subtotal
            sb.append(String.format("%-13s %3d %9.2f\n",
                    lineasNombre.get(0),
                    item.cantidad,
                    item.subtotal));

            // Líneas siguientes: Solo llevan el resto del nombre (Indentado)
            for (int i = 1; i < lineasNombre.size(); i++) {
                sb.append(String.format("%-13s\n", lineasNombre.get(i)));
            }
        }

        // --- TOTALES ---
        sb.append("----------------------------\n");
        sb.append(String.format("Subtotal:      $%8.2f\n", baseImponible));
        sb.append(String.format("IVA (16%%):      $%8.2f\n", iva));
        sb.append("============================\n");
        sb.append(String.format("TOTAL:         $%8.2f\n", totalVenta));
        sb.append("============================\n");

        sb.append("   ¡Gracias por su compra!  \n");
        sb.append("\n");
        sb.append("     ¿REQUIERE FACTURA?     \n");
        sb.append("Canjee este ticket en caja\n");
        sb.append("con su Constancia de Situ-\n");
        sb.append("acion Fiscal (CSF).\n");
        sb.append("\n"); // Espacio antes del código

        // --- GENERAR DATOS PARA CÓDIGO DE BARRAS ---
        // Formato: Venta + Fecha compacta (ddMM) + Hora (HHmm)
        // Ejemplo: 5722121030 (Folio 57, 22 Dic, 10:30 am)
        // Code39 se hace muy ancho rápido, mantenlo lo más corto posible.
        SimpleDateFormat sdfBarra = new SimpleDateFormat("ddMMHHmm");
        String fechaBarra = sdfBarra.format(new Date());
        String dataBarra = idVenta + "-" + fechaBarra;

        // ETIQUETA ESPECIAL PARA INTERCEPTAR EN IMPRESORA
        sb.append("<<<BARCODE:").append(dataBarra).append(">>>");

        return sb.toString();
    }

    private static List<String> dividirTexto(String texto, int largoMax) {
        List<String> lineas = new ArrayList<>();
        // Si cabe, retornamos directo
        if (texto.length() <= largoMax) {
            lineas.add(texto);
            return lineas;
        }

        // Lógica de división
        String[] palabras = texto.split(" ");
        StringBuilder lineaActual = new StringBuilder();

        for (String palabra : palabras) {
            if (lineaActual.length() + palabra.length() + 1 <= largoMax) {
                if (lineaActual.length() > 0) lineaActual.append(" ");
                lineaActual.append(palabra);
            } else {
                // La línea se llenó
                if (lineaActual.length() > 0) {
                    lineas.add(lineaActual.toString());
                    lineaActual = new StringBuilder();
                }
                // Si la palabra sola es más larga que el máximo, la cortamos a la fuerza
                if (palabra.length() > largoMax) {
                    String restante = palabra;
                    while (restante.length() > largoMax) {
                        lineas.add(restante.substring(0, largoMax));
                        restante = restante.substring(largoMax);
                    }
                    lineaActual.append(restante);
                } else {
                    lineaActual.append(palabra);
                }
            }
        }
        if (lineaActual.length() > 0) lineas.add(lineaActual.toString());

        return lineas;
    }

}
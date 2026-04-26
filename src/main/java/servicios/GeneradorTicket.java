package servicios;

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

    // 1. MÉTODO ORIGINAL (Se queda igual para no romper tus ventas normales)
    // Llama al método nuevo, indicando que NO es refacción por defecto (false)
    public static String crearTicket(int idVenta, String fechaStr, List<ItemTicket> items, double totalVenta) {
        return crearTicket(idVenta, fechaStr, items, totalVenta, false);
    }

    // 2. NUEVO MÉTODO MAESTRO (Acepta el booleano 'esRefaccion')
    public static String crearTicket(int idVenta, String fechaStr, List<ItemTicket> items, double totalVenta, boolean esRefaccion) {
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
            List<String> lineasNombre = dividirTexto(item.nombre, 13);

            // Primera línea
            sb.append(String.format("%-13s %3d %9.2f\n",
                    lineasNombre.get(0),
                    item.cantidad,
                    item.subtotal));

            // Líneas siguientes
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

        // --- NUEVO: LÓGICA CONDICIONAL PARA TÉRMINOS ---
        if (esRefaccion) {
            sb.append("\n");
            sb.append("   TÉRMINOS Y CONDICIONES   \n");
            sb.append("1. Esta refacción cuenta con\n");
            sb.append("   30 días de garantía por  \n");
            sb.append("   defectos de fábrica.     \n");
            sb.append("2. Indispensable presentar  \n");
            sb.append("   este ticket para hacer   \n");
            sb.append("   válida la garantía.      \n");
            sb.append("----------------------------\n");
        }

        sb.append("   ¡Gracias por su compra!  \n");
        sb.append("\n");
        sb.append("     ¿REQUIERE FACTURA?     \n");
        sb.append("Canjee este ticket en caja\n");
        sb.append("con su Constancia de Situ-\n");
        sb.append("acion Fiscal (CSF).\n");
        sb.append("\n"); // Espacio antes del código

        // --- GENERAR DATOS PARA CÓDIGO DE BARRAS ---
        SimpleDateFormat sdfBarra = new SimpleDateFormat("ddMMHHmm");
        String fechaBarra = sdfBarra.format(new Date());
        String dataBarra = idVenta + "-" + fechaBarra;

        sb.append("<<<BARCODE:").append(dataBarra).append(">>>");

        return sb.toString();
    }

    // 3. MÉTODO PARA TICKETS DE ANTICIPOS (Para no inflar las cuentas de finanzas)
    public static String crearTicketAnticipo(int idOrden, String cliente, double totalPieza, double anticipoDejado) {
        StringBuilder sb = new StringBuilder();
        double saldoRestante = totalPieza - anticipoDejado;

        sb.append("===========LUMTECH==========\n");
        sb.append("COMPROBANTE DE ANTICIPO\n");
        sb.append("Orden Serv: ").append(idOrden).append("\n");
        sb.append("Cliente: ").append(cliente).append("\n");
        sb.append("----------------------------\n");
        sb.append(String.format("Costo Total:   $%8.2f\n", totalPieza));
        sb.append(String.format("Anticipo:      $%8.2f\n", anticipoDejado));
        sb.append("============================\n");
        sb.append(String.format("RESTA A PAGAR: $%8.2f\n", saldoRestante));
        sb.append("============================\n");
        sb.append("\nConserve este ticket para\n");
        sb.append("liquidar y recoger su equipo.\n\n");
        return sb.toString();
    }

    private static List<String> dividirTexto(String texto, int largoMax) {
        List<String> lineas = new ArrayList<>();
        if (texto.length() <= largoMax) {
            lineas.add(texto);
            return lineas;
        }

        String[] palabras = texto.split(" ");
        StringBuilder lineaActual = new StringBuilder();

        for (String palabra : palabras) {
            if (lineaActual.length() + palabra.length() + 1 <= largoMax) {
                if (lineaActual.length() > 0) lineaActual.append(" ");
                lineaActual.append(palabra);
            } else {
                if (lineaActual.length() > 0) {
                    lineas.add(lineaActual.toString());
                    lineaActual = new StringBuilder();
                }
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
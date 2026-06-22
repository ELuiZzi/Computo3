package servicios;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class ImpresoraTicket {

    // Ahora la variable maestra es la IP
    public static String ipImpresora = "192.168.100.85"; // IP por defecto por si falla el config
    private static boolean autoImprimir = true;

    // --- MÉTODOS DE CONFIGURACIÓN ---
    public static boolean isAutoImprimir() { return autoImprimir; }
    public static void setAutoImprimir(boolean auto) { autoImprimir = auto; }
    public static void setIpImpresora(String ip) { ipImpresora = ip; }

    public static void cargarConfiguracionInicial() {
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);

                // Leemos la IP desde el archivo
                String ipGuardada = props.getProperty("ticket.ip_impresora");
                if (ipGuardada != null && !ipGuardada.trim().isEmpty()) {
                    ipImpresora = ipGuardada;
                }

                String autoImp = props.getProperty("ticket.auto_imprimir");
                if (autoImp != null) {
                    autoImprimir = Boolean.parseBoolean(autoImp);
                }
            } catch (Exception e) {
                System.err.println("Error al cargar configuración de red: " + e.getMessage());
            }
        }
    }

    // --- EL MOTOR DE IMPRESIÓN POR RED (SOCKETS TCP) ---
    // Ya no recibe la IP como parámetro, la toma de la variable global cargada
    public static void imprimir(String textoTicket) {
        if (!autoImprimir) return;

        if (ipImpresora == null || ipImpresora.isEmpty()) {
            System.out.println("Aviso: Configura la IP de la impresora primero.");
            return;
        }

        int puerto = 9100;
        boolean imprimirLogo = true;

        try (java.net.Socket socket = new java.net.Socket(ipImpresora, puerto);
             java.io.OutputStream out = socket.getOutputStream()) {

            String[] lineas = textoTicket.split("\n");

            int margenSuperiorMm = 6;
            int margenInferiorMm = 11;
            int yDibujo = margenSuperiorMm * 8;

            int alturaSimulada = yDibujo;
            java.awt.Image imgLogo = util.CacheRecursos.getLogoTicket();

            if (imgLogo != null && imprimirLogo) alturaSimulada += 180;

            for (String linea : lineas) {
                if (linea.startsWith("<<<BARCODE:")) alturaSimulada += 80;
                else alturaSimulada += 30;
            }

            int heightMm = (alturaSimulada / 8) + margenInferiorMm;

            StringBuilder tspl = new StringBuilder();
            tspl.append("SIZE 58 mm, ").append(heightMm).append(" mm\r\n");
            tspl.append("GAP 0 mm, 0 mm\r\n");
            tspl.append("SET TEAR ON\r\n");
            tspl.append("REFERENCE 0,0\r\n");
            tspl.append("OFFSET 0 mm, 0 mm\r\n");
            tspl.append("DIRECTION 1\r\n");
            tspl.append("CLS\r\n");

            out.write(tspl.toString().getBytes("ISO-8859-1"));

            if (imgLogo != null && imprimirLogo) {
                byte[] logoBytes = generarComandoLogoTSPL(110, yDibujo, imgLogo);
                out.write(logoBytes);
                yDibujo += 180;
            }

            StringBuilder body = new StringBuilder();
            for (String linea : lineas) {
                if (linea.startsWith("<<<BARCODE:") && linea.endsWith(">>>")) {
                    String data = linea.substring(11, linea.length() - 3);
                    body.append("BARCODE 40,").append(yDibujo).append(",\"128\",50,1,0,2,2,\"").append(data).append("\"\r\n");
                    yDibujo += 80;
                    continue;
                }

                String textoSeguro = linea.replace("\"", "'");
                String font = textoSeguro.contains("TOTAL:") ? "3" : "2";

                body.append("TEXT 24,").append(yDibujo).append(",\"").append(font).append("\",0,1,1,\"").append(textoSeguro).append("\"\r\n");
                yDibujo += 30;
            }

            body.append("PRINT 1\r\n");
            out.write(body.toString().getBytes("ISO-8859-1"));
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
            ui.componentes.JOptionPanePro.mostrarMensaje(null, "Error de Red",
                    "Fallo al conectar con la impresora en IP: " + ipImpresora + "\nVerifica el cable Ethernet.", "ERROR");
        }
    }

    // --- CONVERTIDOR DE IMAGEN A TSPL BITMAP ---
    private static byte[] generarComandoLogoTSPL(int x, int y, java.awt.Image logo) {
        if (logo == null) return new byte[0];

        int width = 160;
        int height = 160;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = img.createGraphics();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.drawImage(logo, 0, 0, width, height, null);
        g2d.dispose();

        int widthBytes = width / 8;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        try {
            String header = "BITMAP " + x + "," + y + "," + widthBytes + "," + height + ",0,";
            baos.write(header.getBytes("ISO-8859-1"));

            for (int r = 0; r < height; r++) {
                for (int c = 0; c < widthBytes; c++) {
                    int b = 0;
                    for (int bit = 0; bit < 8; bit++) {
                        int rgb = img.getRGB(c * 8 + bit, r);
                        int luminancia = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
                        if (luminancia < 128) b |= (1 << (7 - bit));
                    }
                    baos.write(b);
                }
            }
            baos.write("\r\n".getBytes("ISO-8859-1"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }
}
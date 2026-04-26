package servicios;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.io.FileInputStream;

public class NotificadorTelegram {
    public static void notificarVentaNueva(double total) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream("config.properties")) {
                    props.load(fis);
                }

                String tokenCod = props.getProperty("telegram.token", "");
                String chatCod = props.getProperty("telegram.chat_id", "");

                if (tokenCod.isEmpty() || chatCod.isEmpty()) return;

// Decodificar en tiempo real
                String token = new String(java.util.Base64.getDecoder().decode(tokenCod));
                String chatId = new String(java.util.Base64.getDecoder().decode(chatCod));

                String mensaje = "✅ *Nueva Venta en Taller*\nTotal ingresado: $" + String.format("%.2f", total);
                // Codificar espacios para la URL
                mensaje = mensaje.replace(" ", "%20").replace("\n", "%0A");

                String urlStr = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&parse_mode=Markdown&text=" + mensaje;

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.getResponseCode(); // Ejecutar la petición
            } catch (Exception e) {
                System.out.println("No se pudo notificar a Telegram.");
            }
        }).start();
    }
}
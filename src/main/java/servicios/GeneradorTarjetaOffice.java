package servicios;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GeneradorTarjetaOffice {
    
    public static String generarTarjeta(String clienteNombre, String correo, String password, LocalDate fechaInicio, LocalDate fechaFin) {
        int width = 800;
        int height = 500;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // Anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Background
        g.setColor(new Color(245, 245, 250));
        g.fillRect(0, 0, width, height);
        
        // Header
        g.setColor(new Color(216, 59, 1)); // Office Red/Orange
        g.fillRect(0, 0, width, 100);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("¡Bienvenido a Office 365!", 50, 60);
        
        // Try to draw logos
        try {
            BufferedImage lumtechLogo = ImageIO.read(new File("recursos/logo2.png"));
            g.drawImage(lumtechLogo, width - 150, 10, 130, 80, null);
        } catch (Exception e) {
            // Ignorar si no carga logo lumtech
        }
        
        try {
            BufferedImage officeLogo = ImageIO.read(new File("recursos/office.png"));
            g.drawImage(officeLogo, 600, 150, 150, 150, null);
        } catch (Exception e) {
            // Ignorar si no carga logo office
        }
        
        // Body text
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        int startY = 160;
        
        g.drawString("Hola " + (clienteNombre != null && !clienteNombre.isEmpty() ? clienteNombre : "Cliente") + ",", 50, startY);
        g.drawString("Gracias por tu compra. Aquí tienes los detalles de tu suscripción:", 50, startY + 40);
        
        g.setColor(new Color(0, 102, 204));
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Cuenta de Correo:", 50, startY + 100);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        g.drawString(correo, 250, startY + 100);
        
        if (password != null && !password.isEmpty()) {
            g.setColor(new Color(0, 102, 204));
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.drawString("Contraseña:", 50, startY + 140);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 22));
            g.drawString(password, 250, startY + 140);
        }
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        g.setColor(new Color(0, 102, 204));
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Fecha de Inicio:", 50, startY + 200);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        g.drawString(fechaInicio.format(dtf), 250, startY + 200);
        
        g.setColor(new Color(216, 59, 1));
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Fecha de Vencimiento:", 50, startY + 240);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        g.drawString(fechaFin.format(dtf), 300, startY + 240);
        
        // Footer
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Arial", Font.ITALIC, 18));
        g.drawString("¡Lumtech agradece tu preferencia! Te recordaremos antes de que expire.", 50, height - 30);
        
        g.dispose();
        
        // Save
        try {
            File dir = new File("recursos/tarjetas");
            if (!dir.exists()) dir.mkdirs();
            String prefix = correo.contains("@") ? correo.split("@")[0] : "usuario";
            String filename = "Office_" + prefix + "_" + System.currentTimeMillis() + ".png";
            File output = new File(dir, filename);
            ImageIO.write(image, "png", output);
            return output.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

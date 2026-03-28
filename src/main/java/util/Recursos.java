package util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;

public class Recursos {

    // Ruta base donde guardaste las imágenes
    private static final String RUTA_ICONOS = "recursos/";

    public static ImageIcon getIcono(String nombreArchivo) {
        // Intentamos cargar desde el sistema de archivos
        String rutaCompleta = RUTA_ICONOS + nombreArchivo;
        File archivo = new File(rutaCompleta);

        if (archivo.exists()) {
            return new ImageIcon(rutaCompleta);
        } else {
            // Si no existe, retornamos null (no se mostrará icono, pero no crashea)
            System.err.println("Icono no encontrado: " + rutaCompleta);
            return null;
        }
    }

    public static Image getImagenApp() {
        ImageIcon icon = getIcono("logo.png"); // Asegúrate de tener logo.png
        return (icon != null) ? icon.getImage() : null;
    }

    public static String getVersionActual() {
        try {
            // Intenta leer el archivo que genera el Launcher
            File archivo = new File("version.dat");
            if (archivo.exists()) {
                return new String(Files.readAllBytes(archivo.toPath())).trim();
            }
        } catch (Exception e) {
            // Ignorar error
        }
        return "Dev / 1.0.0"; // Valor por defecto si no hay launcher (modo pruebas)
    }

    public static String capitalizarTexto(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        String[] palabras = texto.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                sb.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
package Utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

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
}
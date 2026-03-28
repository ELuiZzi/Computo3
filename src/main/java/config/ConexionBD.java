package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import ui.componentes.JOptionPanePro;

public class ConexionBD {

    // Nombre del archivo de configuración
    private static final String CONFIG_FILE = "config.properties";

    public static Connection conectar() {
        Properties props = new Properties();
        String ip = "localhost";
        String puerto = "3306";
        String db = "punto_venta";
        String user = "root";
        String pass = "";

        File archivoConfig = new File(CONFIG_FILE);

        // 1. VERIFICAR SI EXISTE EL ARCHIVO DE CONFIGURACIÓN
        if (archivoConfig.exists()) {
            try (FileInputStream input = new FileInputStream(archivoConfig)) {
                props.load(input);
                // Leemos los valores del archivo
                ip = props.getProperty("db.ip", "localhost");
                puerto = props.getProperty("db.port", "3306");
                db = props.getProperty("db.name", "punto_venta");
                user = props.getProperty("db.user", "root");
                pass = props.getProperty("db.password", "");
            } catch (IOException e) {
                JOptionPanePro.mostrarMensaje(null, "Error Config", "No se pudo leer config.properties", "ERROR");
            }
        } else {
            // 2. SI NO EXISTE, LO CREAMOS POR DEFECTO
            crearArchivoPorDefecto(props, archivoConfig);
            JOptionPanePro.mostrarMensaje(null, "Configuración Inicial",
                    "Se ha creado el archivo 'config.properties'.\nPor favor, configura la IP del servidor si no es local.", "INFO");
        }

        // 3. CONSTRUIR LA URL DINÁMICA
        String url = "jdbc:mysql://" + ip + ":" + puerto + "/" + db;

        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            System.err.println("Error conexión: " + e.getMessage());
            JOptionPanePro.mostrarMensaje(null, "Error de Conexión",
                    "No se pudo conectar al servidor: " + ip + "\nVerifique IP, Firewall o Credenciales en config.properties", "ERROR");
            return null;
        }
    }

    private static void crearArchivoPorDefecto(Properties props, File archivo) {
        try (FileOutputStream output = new FileOutputStream(archivo)) {
            // Establecer valores predeterminados
            props.setProperty("db.ip", "localhost"); // Aquí pondrás la 192.168.x.x
            props.setProperty("db.port", "3306");
            props.setProperty("db.name", "punto_venta");
            props.setProperty("db.user", "root");
            props.setProperty("db.password", "");

            // Guardar archivo con comentarios
            props.store(output, "Configuracion de Base de Datos - Sistema POS");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
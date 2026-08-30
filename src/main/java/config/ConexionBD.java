package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    private static final String CONFIG_FILE = "config.properties";
    private static HikariDataSource dataSource;

    // Bloque estático: Se ejecuta UNA SOLA VEZ cuando el programa arranca
    static {
        inicializarPool();
    }

    private static void inicializarPool() {
        Properties props = new Properties();
        String ip = "localhost", puerto = "3306", db = "punto_venta", user = "root", pass = "";
        File archivoConfig = new File(CONFIG_FILE);

        // 1. LEER CONFIGURACIÓN
        if (archivoConfig.exists()) {
            try (FileInputStream input = new FileInputStream(archivoConfig)) {
                props.load(input);
                ip = props.getProperty("db.ip", "localhost");
                puerto = props.getProperty("db.port", "3306");
                db = props.getProperty("db.name", "punto_venta");
                user = props.getProperty("db.user", "root");
                pass = props.getProperty("db.password", "");
            } catch (IOException e) {
                System.err.println("Error Config: No se pudo leer config.properties - " + e.getMessage());
            }
        } else {
            crearArchivoPorDefecto(props, archivoConfig);
            System.out.println("Configuración: Se creó 'config.properties'. Configura la IP si es necesario.");
        }

        // 2. CONFIGURAR HIKARICP
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + ip + ":" + puerto + "/" + db + "?serverTimezone=America/Mexico_City");
            config.setUsername(user);
            config.setPassword(pass);

            // Optimizaciones de rendimiento para MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            // Configuración del Pool
            config.setMaximumPoolSize(10); // 10 conexiones simultáneas es más que suficiente para un POS local
            config.setMinimumIdle(2);      // Mantiene 2 conexiones siempre listas
            config.setIdleTimeout(30000);  // 30 segundos de inactividad
            config.setConnectionTimeout(10000); // 10 segundos máximo esperando conexión

            dataSource = new HikariDataSource(config);

            // 3. REGISTRAR SHUTDOWN HOOK PARA CERRAR EL POOL
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (dataSource != null && !dataSource.isClosed()) {
                    dataSource.close();
                    System.out.println("Pool de conexiones de HikariCP cerrado correctamente.");
                }
            }));
        } catch (Exception e) {
            System.err.println("Error crítico al inicializar HikariCP: " + e.getMessage());
        }
    }

    // 4. RETORNAR CONEXIÓN O LANZAR EXCEPCIÓN EN LUGAR DE NULL
    public static Connection conectar() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("El Pool de BD no se inicializó correctamente. Revisa los logs de arranque.");
        }
        return dataSource.getConnection();
    }

    private static void crearArchivoPorDefecto(Properties props, File archivo) {
        try (FileOutputStream output = new FileOutputStream(archivo)) {
            props.setProperty("db.ip", "localhost");
            props.setProperty("db.port", "3306");
            props.setProperty("db.name", "punto_venta");
            props.setProperty("db.user", "root");
            props.setProperty("db.password", "");
            props.store(output, "Configuracion de Base de Datos - Sistema POS");
        } catch (IOException e) {
            System.err.println("Error al crear archivo por defecto: " + e.getMessage());
        }
    }
}
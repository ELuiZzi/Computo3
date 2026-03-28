package servicios;

import ui.componentes.ToastPro;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupManager {

    public static void iniciarScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Revisar cada minuto si es hora de respaldar
        scheduler.scheduleAtFixedRate(() -> {
            SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm");
            String horaActual = sdfHora.format(new Date());

            // Hora objetivo: 17:00 (5 PM)
            if (horaActual.equals("17:00")) {
                ejecutarBackup();
                // Dormir 1 minuto para no repetir el backup varias veces en el mismo minuto
                try { Thread.sleep(61000); } catch (Exception e) {}
            }
        }, 0, 1, TimeUnit.MINUTES);
    }


    public static void ejecutarBackup() {
        try {
            // 1. Leer Configuración
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                props.load(fis);
            }

            String dbUser = props.getProperty("db.user", "root");
            String dbPass = props.getProperty("db.password", "");
            String dbName = props.getProperty("db.name", "punto_venta");
            // LEER TAMBIÉN LA IP Y EL PUERTO
            String dbIp   = props.getProperty("db.ip", "localhost");
            String dbPort = props.getProperty("db.port", "3306");

            String dumpPath = props.getProperty("db.dump_path", "mysqldump");

            // 2. Preparar Carpeta
            File carpeta = new File("dumps");
            if (!carpeta.exists()) carpeta.mkdirs();

            String fecha = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
            String archivoSQL = new File(carpeta, "Respaldo_" + fecha + ".sql").getAbsolutePath();

            // 3. CONSTRUCCIÓN DEL COMANDO
            List<String> comandos = new ArrayList<>();
            comandos.add(dumpPath);

            // AGREGAR IP Y PUERTO (Crucial para que coincida con tu conexión)
            comandos.add("-h" + dbIp);
            comandos.add("-P" + dbPort);

            comandos.add("-u" + dbUser);

            if (!dbPass.isEmpty()) {
                comandos.add("-p" + dbPass);
            }

            comandos.add("--column-statistics=0");
            // Opcional: Agregar --column-statistics=0 si te da error en versiones nuevas de MySQL 8
            // comandos.add("--column-statistics=0");

            comandos.add(dbName);
            comandos.add("-r");
            comandos.add(archivoSQL);

            // 4. Ejecutar
            ProcessBuilder pb = new ProcessBuilder(comandos);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 5. Capturar salida
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder outputMsg = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignorar el warning de contraseña insegura para que no parezca error
                if (!line.contains("Using a password")) {
                    outputMsg.append(line).append("\n");
                }
            }

            int processComplete = process.waitFor();

            if (processComplete == 0) {
                File f = new File(archivoSQL);
                if (f.exists() && f.length() > 0) {
                    LoggerPro.registrar("SISTEMA", "Respaldo automático OK: " + f.getName());
                    System.out.println("Respaldo creado con éxito.");
                } else {
                    LoggerPro.registrar("ERROR", "El archivo SQL se creó vacío.");
                }
            } else {
                String errorLog = outputMsg.toString();
                if (errorLog.trim().isEmpty()) errorLog = "Código: " + processComplete;
                System.err.println("Error Backup: " + errorLog);
                LoggerPro.registrar("ERROR", "Fallo Backup: " + errorLog);
            }

        } catch (Exception e) {
            e.printStackTrace();
            LoggerPro.registrar("ERROR", "Excepción Java Backup: " + e.getMessage());
        }
    }
}
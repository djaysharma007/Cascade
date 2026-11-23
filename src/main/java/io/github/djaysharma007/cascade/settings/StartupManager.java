package io.github.djaysharma007.cascade.settings;

import io.github.djaysharma007.cascade.app.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages Windows per-user startup registration (HKCU\Software\Microsoft\Windows\CurrentVersion\Run)
 * to launch Cascade with the --background flag on login.
 */
public final class StartupManager {

    private static final Logger log = LoggerFactory.getLogger(StartupManager.class);
    private static final String REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "Cascade";

    private StartupManager() {
    }

    public static boolean isDevMode() {
        return findRunnableJar().isEmpty();
    }

    public static Optional<Path> findRunnableJar() {
        try {
            URL codeSource = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeSource != null) {
                File file = new File(codeSource.toURI());
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    return Optional.of(file.toPath().toAbsolutePath());
                }
            }
        } catch (Exception ignored) {}

        // Fallback: check target folder
        Path targetJar = Path.of("target", "cascade-1.0.0-SNAPSHOT.jar").toAbsolutePath();
        if (Files.exists(targetJar)) {
            return Optional.of(targetJar);
        }

        return Optional.empty();
    }

    public static boolean registerStartup(Path jarPath) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return false;
        }

        Path javaHome = Path.of(System.getProperty("java.home", "C:\\Program Files\\Java\\jdk-25.0.2"));
        Path javaw = javaHome.resolve("bin").resolve("javaw.exe");
        if (!Files.exists(javaw)) {
            javaw = javaHome.resolve("bin").resolve("java.exe");
        }

        String commandValue = String.format("\"%s\" -jar \"%s\" --background",
                javaw.toAbsolutePath(), jarPath.toAbsolutePath());

        try {
            Process process = new ProcessBuilder("reg.exe", "add", REG_KEY, "/v", APP_NAME, "/t", "REG_SZ", "/d", commandValue, "/f")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                log.info("Registered Cascade in Windows startup: {}", commandValue);
                return true;
            } else {
                log.warn("Failed to register Windows startup: exit code {}", process.exitValue());
                return false;
            }
        } catch (Exception e) {
            log.error("Error registering Windows startup", e);
            return false;
        }
    }

    public static boolean unregisterStartup() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return false;
        }

        try {
            Process process = new ProcessBuilder("reg.exe", "delete", REG_KEY, "/v", APP_NAME, "/f")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                log.info("Unregistered Cascade from Windows startup");
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("Error unregistering Windows startup", e);
            return false;
        }
    }

    public static boolean isRegistered() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return false;
        }

        try {
            Process process = new ProcessBuilder("reg.exe", "query", REG_KEY, "/v", APP_NAME)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

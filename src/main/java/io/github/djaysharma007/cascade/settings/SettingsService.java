package io.github.djaysharma007.cascade.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Service managing persistence, listeners, and system effects of user settings.
 */
public final class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Path settingsFile;
    private final List<Consumer<Settings>> listeners = new CopyOnWriteArrayList<>();
    private volatile Settings currentSettings;

    public SettingsService() {
        this(Path.of(System.getProperty("user.home"), ".cascade", "settings.json"));
    }

    public SettingsService(Path settingsFile) {
        this.settingsFile = Objects.requireNonNull(settingsFile, "settingsFile must not be null");
        this.currentSettings = load();
        applySideEffects(this.currentSettings);
    }

    public Settings getSettings() {
        return currentSettings;
    }

    public synchronized void updateSettings(Settings newSettings) {
        Objects.requireNonNull(newSettings, "newSettings must not be null");
        Settings oldSettings = this.currentSettings;
        this.currentSettings = newSettings;
        save(newSettings);
        applySideEffects(newSettings);

        for (Consumer<Settings> listener : listeners) {
            try {
                listener.accept(newSettings);
            } catch (Exception e) {
                log.error("Error notifying settings listener", e);
            }
        }
    }

    public void addListener(Consumer<Settings> listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(Consumer<Settings> listener) {
        listeners.remove(listener);
    }

    private void applySideEffects(Settings settings) {
        UiMotion.setReduceMotion(settings.reduceMotion());

        if (settings.startOnWindowsStartup()) {
            StartupManager.findRunnableJar().ifPresent(StartupManager::registerStartup);
        } else {
            StartupManager.unregisterStartup();
        }
    }

    private Settings load() {
        if (!Files.exists(settingsFile)) {
            return Settings.defaults();
        }
        try {
            return MAPPER.readValue(settingsFile.toFile(), Settings.class);
        } catch (Exception e) {
            log.warn("Could not load settings from {}, using defaults: {}", settingsFile, e.getMessage());
            return Settings.defaults();
        }
    }

    private void save(Settings settings) {
        try {
            Path parent = settingsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(parent != null ? parent : Path.of("."), "settings_", ".tmp");
            MAPPER.writeValue(temp.toFile(), settings);
            try {
                Files.move(temp, settingsFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, settingsFile, StandardCopyOption.REPLACE_EXISTING);
            }
            log.debug("Saved settings to {}", settingsFile);
        } catch (IOException e) {
            log.error("Failed to save settings to {}", settingsFile, e);
        }
    }
}

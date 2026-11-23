package io.github.djaysharma007.cascade.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsServiceTest {

    @Test
    void testDefaultsAndPersistence(@TempDir Path tempDir) {
        Path settingsFile = tempDir.resolve("settings.json");
        SettingsService service = new SettingsService(settingsFile);

        Settings defaults = service.getSettings();
        assertThat(defaults.keepRunningInTray()).isTrue();
        assertThat(defaults.startOnWindowsStartup()).isFalse();
        assertThat(defaults.reduceMotion()).isFalse();

        AtomicReference<Settings> updatedRef = new AtomicReference<>();
        service.addListener(updatedRef::set);

        Settings modified = defaults.withKeepRunningInTray(false).withReduceMotion(true);
        service.updateSettings(modified);

        assertThat(updatedRef.get()).isEqualTo(modified);
        assertThat(settingsFile).exists();

        // Reload from file
        SettingsService reloaded = new SettingsService(settingsFile);
        assertThat(reloaded.getSettings().keepRunningInTray()).isFalse();
        assertThat(reloaded.getSettings().reduceMotion()).isTrue();
    }
}

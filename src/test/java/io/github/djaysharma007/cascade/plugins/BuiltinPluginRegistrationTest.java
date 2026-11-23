package io.github.djaysharma007.cascade.plugins;

import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.PluginId;
import io.github.djaysharma007.cascade.registry.PluginDescriptor;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinPluginRegistrationTest {

    private PluginRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
    }

    @Test
    void allRegisteredPluginsCarryPluginAnnotation() {
        Collection<PluginDescriptor<?>> all = registry.allDescriptors();
        assertThat(all).isNotEmpty();

        for (PluginDescriptor<?> desc : all) {
            Class<?> clazz = desc.pluginClass();
            Plugin annotation = clazz.getAnnotation(Plugin.class);
            assertThat(annotation)
                    .withFailMessage("Class %s must carry @Plugin annotation", clazz.getName())
                    .isNotNull();
            assertThat(annotation.id()).isEqualTo(desc.id().value());
            assertThat(annotation.category()).isEqualTo(desc.category());
        }
    }

    @Test
    void expectedPluginsArePresent() {
        List<String> expectedIds = List.of(
                "manual", "timer", "file-created", "file-modified", "schedule",
                "extension-is", "name-contains", "file-size-over", "all-of", "any-of", "not",
                "move-file", "print-message", "copy-file", "rename-file", "delete-file",
                "run-program", "desktop-notification", "http-webhook", "send-email"
        );

        for (String id : expectedIds) {
            assertThat(registry.findDescriptor(id))
                    .withFailMessage("Expected plugin %s is not registered", id)
                    .isPresent();
        }

        assertThat(registry.findByCategory(PluginCategory.TRIGGER)).hasSize(5);
        assertThat(registry.findByCategory(PluginCategory.CONDITION)).hasSize(6);
        assertThat(registry.findByCategory(PluginCategory.ACTION)).hasSize(9);
    }
}

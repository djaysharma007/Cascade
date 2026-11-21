package io.github.djaysharma007.cascade.registry;

import io.github.djaysharma007.cascade.core.ParamSpec;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.PluginId;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata descriptor for a registered plugin.
 *
 * @param id unique plugin id, never null
 * @param displayName human readable name, never null
 * @param category plugin category (TRIGGER, CONDITION, ACTION), never null
 * @param pluginClass class implementing the plugin, never null
 * @param parameters list of parameter specifications, never null
 * @param providedKeys keys provided in event payload (for triggers), never null
 * @param requiredKeys keys required from event payload (for conditions and actions), never null
 * @param factory instantiation factory, never null
 * @param <T> plugin interface type
 */
public record PluginDescriptor<T>(
        PluginId id,
        String displayName,
        PluginCategory category,
        Class<? extends T> pluginClass,
        List<ParamSpec> parameters,
        Set<String> providedKeys,
        Set<String> requiredKeys,
        PluginFactory<T> factory
) {
    public PluginDescriptor {
        Objects.requireNonNull(id, "PluginId must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(pluginClass, "pluginClass must not be null");
        Objects.requireNonNull(factory, "factory must not be null");
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        providedKeys = providedKeys == null ? Set.of() : Set.copyOf(providedKeys);
        requiredKeys = requiredKeys == null ? Set.of() : Set.copyOf(requiredKeys);
    }
}

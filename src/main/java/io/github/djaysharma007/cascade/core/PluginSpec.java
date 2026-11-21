package io.github.djaysharma007.cascade.core;

import java.util.Map;
import java.util.Objects;

/**
 * A spec names a plugin id and holds an unmodifiable copy of its parameters.
 *
 * @param pluginId the plugin identifier, never null
 * @param params unmodifiable map of parameter key-values, never null
 */
public record PluginSpec(PluginId pluginId, Map<String, Object> params) {

    public PluginSpec {
        Objects.requireNonNull(pluginId, "PluginId must not be null");
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public static PluginSpec of(String pluginIdStr, Map<String, Object> params) {
        return new PluginSpec(PluginId.of(pluginIdStr), params);
    }

    public static PluginSpec of(PluginId pluginId) {
        return new PluginSpec(pluginId, Map.of());
    }
}

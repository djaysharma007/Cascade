package io.github.djaysharma007.cascade.registry;

import java.util.Map;

/**
 * Functional factory producing plugin instances from configuration parameters.
 *
 * @param <T> plugin interface type (Trigger, Condition, or Action)
 */
@FunctionalInterface
public interface PluginFactory<T> {

    /**
     * Instantiates and configures a plugin with the given parameters.
     *
     * @param params configuration parameter map, never null
     * @return configured plugin instance, never null
     */
    T create(Map<String, Object> params);
}

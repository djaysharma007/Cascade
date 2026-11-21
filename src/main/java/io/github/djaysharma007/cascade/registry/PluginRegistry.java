package io.github.djaysharma007.cascade.registry;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.PluginId;
import io.github.djaysharma007.cascade.core.Trigger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all Trigger, Condition, and Action plugins.
 */
public final class PluginRegistry {

    private final Map<PluginId, PluginDescriptor<?>> descriptors = new ConcurrentHashMap<>();

    public PluginRegistry() {
    }

    public <T> void register(PluginDescriptor<T> descriptor) {
        Objects.requireNonNull(descriptor, "PluginDescriptor must not be null");
        descriptors.put(descriptor.id(), descriptor);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<PluginDescriptor<T>> findDescriptor(PluginId id) {
        Objects.requireNonNull(id, "PluginId must not be null");
        return Optional.ofNullable((PluginDescriptor<T>) descriptors.get(id));
    }

    public Optional<PluginDescriptor<?>> findDescriptor(String idStr) {
        return Optional.ofNullable(descriptors.get(PluginId.of(idStr)));
    }

    @SuppressWarnings("unchecked")
    public <T> List<PluginDescriptor<T>> findByCategory(PluginCategory category) {
        Objects.requireNonNull(category, "PluginCategory must not be null");
        return descriptors.values().stream()
                .filter(d -> d.category() == category)
                .map(d -> (PluginDescriptor<T>) d)
                .toList();
    }

    public Collection<PluginDescriptor<?>> allDescriptors() {
        return Collections.unmodifiableCollection(descriptors.values());
    }

    public Trigger createTrigger(PluginId id, Map<String, Object> params) {
        PluginDescriptor<Trigger> desc = this.<Trigger>findDescriptor(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown trigger plugin: " + id));
        return desc.factory().create(params);
    }

    public Condition createCondition(PluginId id, Map<String, Object> params) {
        PluginDescriptor<Condition> desc = this.<Condition>findDescriptor(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown condition plugin: " + id));
        return desc.factory().create(params);
    }

    public Action createAction(PluginId id, Map<String, Object> params) {
        PluginDescriptor<Action> desc = this.<Action>findDescriptor(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown action plugin: " + id));
        return desc.factory().create(params);
    }
}

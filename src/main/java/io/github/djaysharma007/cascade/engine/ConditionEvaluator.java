package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.PluginId;
import io.github.djaysharma007.cascade.registry.PluginRegistry;

import java.util.Objects;

/**
 * Recursively evaluates condition specification trees against incoming events.
 */
public final class ConditionEvaluator {

    private final PluginRegistry registry;

    public ConditionEvaluator(PluginRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    public boolean evaluate(ConditionSpec spec, Event event) {
        if (spec == null) {
            return true;
        }
        Objects.requireNonNull(event, "event must not be null");

        String type = spec.type();
        if (ConditionSpec.TYPE_ALL_OF.equals(type)) {
            for (ConditionSpec child : spec.children()) {
                if (!evaluate(child, event)) {
                    return false;
                }
            }
            return true;
        }

        if (ConditionSpec.TYPE_ANY_OF.equals(type)) {
            for (ConditionSpec child : spec.children()) {
                if (evaluate(child, event)) {
                    return true;
                }
            }
            return false;
        }

        if (ConditionSpec.TYPE_NOT.equals(type)) {
            if (spec.children().isEmpty()) {
                return true;
            }
            return !evaluate(spec.children().get(0), event);
        }

        // Leaf condition
        Condition condition = registry.createCondition(PluginId.of(type), spec.params());
        return condition.test(event);
    }
}

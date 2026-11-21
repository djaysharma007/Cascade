package io.github.djaysharma007.cascade.plugins.conditions;

import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;

import java.util.Objects;

/**
 * Composite condition inverting the result of its child condition.
 */
@Plugin(id = "not", displayName = "Not", category = PluginCategory.CONDITION)
public final class NotCondition implements Condition {

    private final Condition child;

    public NotCondition(Condition child) {
        this.child = Objects.requireNonNull(child, "child must not be null");
    }

    @Override
    public boolean test(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        return !child.test(event);
    }
}

package io.github.djaysharma007.cascade.plugins.conditions;

import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;

import java.util.List;
import java.util.Objects;

/**
 * Composite condition matching if all child conditions evaluate to true.
 */
@Plugin(id = "all-of", displayName = "All of", category = PluginCategory.CONDITION)
public final class AllOfCondition implements Condition {

    private final List<Condition> children;

    public AllOfCondition(List<Condition> children) {
        Objects.requireNonNull(children, "children must not be null");
        if (children.isEmpty()) {
            throw new IllegalArgumentException("AllOfCondition must have at least one child");
        }
        this.children = List.copyOf(children);
    }

    @Override
    public boolean test(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        for (Condition child : children) {
            if (!child.test(event)) {
                return false;
            }
        }
        return true;
    }
}

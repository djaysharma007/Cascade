package io.github.djaysharma007.cascade.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Condition specification tree node.
 * Invariant: A composite has at least one child; Not has exactly one.
 *
 * @param type plugin id or composite type ("all-of", "any-of", "not"), never null
 * @param params parameters for leaf condition, never null
 * @param children nested condition specs for composites, never null
 */
public record ConditionSpec(
        String type,
        Map<String, Object> params,
        List<ConditionSpec> children
) {
    public static final String TYPE_ALL_OF = "all-of";
    public static final String TYPE_ANY_OF = "any-of";
    public static final String TYPE_NOT = "not";

    public ConditionSpec {
        Objects.requireNonNull(type, "type must not be null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        params = params == null ? Map.of() : Map.copyOf(params);
        children = children == null ? List.of() : List.copyOf(children);

        if (TYPE_ALL_OF.equals(type) || TYPE_ANY_OF.equals(type)) {
            if (children.isEmpty()) {
                throw new IllegalArgumentException("Composite condition '" + type + "' must have at least one child");
            }
        } else if (TYPE_NOT.equals(type)) {
            if (children.size() != 1) {
                throw new IllegalArgumentException("Not condition must have exactly one child");
            }
        }
    }

    public static ConditionSpec leaf(String pluginId, Map<String, Object> params) {
        return new ConditionSpec(pluginId, params, List.of());
    }

    public static ConditionSpec allOf(List<ConditionSpec> children) {
        return new ConditionSpec(TYPE_ALL_OF, Map.of(), children);
    }

    public static ConditionSpec anyOf(List<ConditionSpec> children) {
        return new ConditionSpec(TYPE_ANY_OF, Map.of(), children);
    }

    public static ConditionSpec not(ConditionSpec child) {
        Objects.requireNonNull(child, "child must not be null");
        return new ConditionSpec(TYPE_NOT, Map.of(), List.of(child));
    }

    public boolean isComposite() {
        return TYPE_ALL_OF.equals(type) || TYPE_ANY_OF.equals(type) || TYPE_NOT.equals(type);
    }
}

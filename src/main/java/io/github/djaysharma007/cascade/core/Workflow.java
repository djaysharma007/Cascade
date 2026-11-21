package io.github.djaysharma007.cascade.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A workflow has a non-blank name of at most 80 characters, exactly one trigger spec
 * and at least one action spec, and none of its lists can change after construction.
 *
 * @param id unique workflow identifier, never null
 * @param name non-blank name of up to 80 characters, never null
 * @param enabled whether workflow is enabled to run
 * @param trigger trigger specification, never null
 * @param conditions optional root condition tree specification
 * @param actions list of action specifications, non-empty and immutable
 */
public record Workflow(
        WorkflowId id,
        String name,
        boolean enabled,
        PluginSpec trigger,
        ConditionSpec conditions,
        List<PluginSpec> actions
) {
    public static final int MAX_NAME_LENGTH = 80;

    public Workflow {
        Objects.requireNonNull(id, "Workflow id must not be null");
        Objects.requireNonNull(name, "Workflow name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Workflow name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Workflow name must be at most " + MAX_NAME_LENGTH + " characters: " + name);
        }
        Objects.requireNonNull(trigger, "Workflow trigger must not be null");
        Objects.requireNonNull(actions, "Workflow actions must not be null");
        actions = List.copyOf(actions);
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one action spec");
        }
    }

    public Optional<ConditionSpec> conditionSpec() {
        return Optional.ofNullable(conditions);
    }

    public static WorkflowBuilder builder() {
        return new WorkflowBuilder();
    }
}

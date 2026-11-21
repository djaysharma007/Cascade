package io.github.djaysharma007.cascade.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for immutable {@link Workflow} instances.
 */
public final class WorkflowBuilder {

    private WorkflowId id = WorkflowId.random();
    private String name;
    private boolean enabled = true;
    private PluginSpec trigger;
    private ConditionSpec conditions;
    private final List<PluginSpec> actions = new ArrayList<>();

    WorkflowBuilder() {
    }

    public WorkflowBuilder id(WorkflowId id) {
        this.id = Objects.requireNonNull(id, "Workflow id must not be null");
        return this;
    }

    public WorkflowBuilder name(String name) {
        this.name = name;
        return this;
    }

    public WorkflowBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public WorkflowBuilder trigger(PluginSpec trigger) {
        this.trigger = trigger;
        return this;
    }

    public WorkflowBuilder conditions(ConditionSpec conditions) {
        this.conditions = conditions;
        return this;
    }

    public WorkflowBuilder addAction(PluginSpec action) {
        Objects.requireNonNull(action, "action must not be null");
        this.actions.add(action);
        return this;
    }

    public WorkflowBuilder actions(List<PluginSpec> actions) {
        Objects.requireNonNull(actions, "actions must not be null");
        this.actions.clear();
        this.actions.addAll(actions);
        return this;
    }

    public Workflow build() {
        return new Workflow(id, name, enabled, trigger, conditions, actions);
    }
}

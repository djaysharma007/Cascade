package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;

import java.util.Objects;

/**
 * Pre-configured workflow template that can be instantiated into an editable workflow draft.
 */
public record WorkflowTemplate(
        String id,
        String title,
        String description,
        String category,
        String iconName,
        Workflow templateWorkflow
) {
    public WorkflowTemplate {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(iconName, "iconName must not be null");
        Objects.requireNonNull(templateWorkflow, "templateWorkflow must not be null");
    }

    /**
     * Creates a new independent workflow draft instance with a fresh random WorkflowId.
     */
    public Workflow instantiate() {
        return Workflow.builder()
                .id(WorkflowId.random())
                .name(templateWorkflow.name())
                .enabled(templateWorkflow.enabled())
                .trigger(templateWorkflow.trigger())
                .conditions(templateWorkflow.conditionSpec().orElse(null))
                .actions(templateWorkflow.actions())
                .build();
    }
}

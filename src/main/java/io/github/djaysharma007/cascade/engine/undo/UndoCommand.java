package io.github.djaysharma007.cascade.engine.undo;

import java.io.IOException;

/**
 * Command representing a reversible mutation from a workflow run step.
 */
public interface UndoCommand {

    /**
     * Executes the reversal of the action.
     *
     * @throws IOException if a filesystem error occurs
     */
    void execute() throws IOException;

    /**
     * Human-readable description of what will be undone.
     */
    String description();
}

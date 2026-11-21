package io.github.djaysharma007.cascade.core;

/**
 * Contract for workflow execution steps.
 *
 * <p><b>Precondition:</b> Event and context not null; parameters validated.
 * <br><b>Postcondition:</b> Returns an {@link ExecutionResult}; expected failures are returned, not thrown.
 * <br><b>Invariant:</b> Does not mutate the event; safe to call from several threads;
 * repeating a call with the same idempotency key has no extra effect.
 */
@FunctionalInterface
public interface Action {

    /**
     * Executes the action for the given event and execution context.
     *
     * @param event incoming trigger event, never null
     * @param context workflow and run context, never null
     * @return result of execution (Success, Skipped, or Failed)
     */
    ExecutionResult execute(Event event, ExecutionContext context);
}

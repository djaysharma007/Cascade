package io.github.djaysharma007.cascade.core;

/**
 * Contract for workflow filter conditions.
 *
 * <p><b>Precondition:</b> Event not null.
 * <br><b>Postcondition:</b> Returns true or false.
 * <br><b>Invariant:</b> Pure: no side effects, no blocking I/O, deterministic for the same event.
 */
@FunctionalInterface
public interface Condition {

    /**
     * Tests whether the given event satisfies this condition.
     *
     * @param event the event to evaluate, never null
     * @return true if the event satisfies the condition, false otherwise
     */
    boolean test(Event event);
}

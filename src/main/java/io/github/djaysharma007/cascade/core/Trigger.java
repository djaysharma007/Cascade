package io.github.djaysharma007.cascade.core;

/**
 * Contract for workflow triggers.
 *
 * <p><b>Precondition:</b> Not already started; parameters validated; listener not null.
 * <br><b>Postcondition:</b> Events reach the listener on a non-UI thread until {@link #stop()}.
 * <br><b>Invariant:</b> After {@link #stop()} returns, the listener is never called again.
 * {@link #stop()} is idempotent and never throws.
 */
public interface Trigger {

    /**
     * Starts listening for external occurrences and sending events to the given listener.
     *
     * @param listener callback invoked when events fire, must not be null
     * @throws IllegalStateException if already started
     */
    void start(TriggerListener listener);

    /**
     * Stops the trigger and releases all held threads and resources.
     * Must be idempotent and never throw.
     */
    void stop();
}

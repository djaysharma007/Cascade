package io.github.djaysharma007.cascade.core;

/**
 * Listener for events produced by a {@link Trigger}.
 */
@FunctionalInterface
public interface TriggerListener {

    /**
     * Handles an event fired by a trigger.
     *
     * @param event the event, never null
     */
    void onEvent(Event event);
}

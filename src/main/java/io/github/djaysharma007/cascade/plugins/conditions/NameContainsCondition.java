package io.github.djaysharma007.cascade.plugins.conditions;

import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;

import java.util.Objects;

/**
 * Checks whether the event file name contains the specified substring.
 */
@Plugin(id = "name-contains", displayName = "Name contains", category = PluginCategory.CONDITION)
public final class NameContainsCondition implements Condition {

    private final String text;
    private final boolean caseSensitive;

    public NameContainsCondition(String text, boolean caseSensitive) {
        this.text = Objects.requireNonNull(text, "text must not be null");
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean test(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        Object nameObj = event.payload().get("fileName");
        if (nameObj == null) {
            return false;
        }
        String fileName = nameObj.toString();
        if (caseSensitive) {
            return fileName.contains(text);
        } else {
            return fileName.toLowerCase().contains(text.toLowerCase());
        }
    }
}

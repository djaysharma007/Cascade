package io.github.djaysharma007.cascade.plugins.conditions;

import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;

import java.util.Objects;

/**
 * Checks whether the event file has the specified extension (case-insensitive).
 */
@Plugin(id = "extension-is", displayName = "Extension is", category = PluginCategory.CONDITION)
public final class ExtensionIsCondition implements Condition {

    private final String expectedExtension;

    public ExtensionIsCondition(String expectedExtension) {
        Objects.requireNonNull(expectedExtension, "expectedExtension must not be null");
        this.expectedExtension = expectedExtension.startsWith(".")
                ? expectedExtension.toLowerCase()
                : "." + expectedExtension.toLowerCase();
    }

    @Override
    public boolean test(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        Object nameObj = event.payload().get("fileName");
        if (nameObj == null) {
            return false;
        }
        String fileName = nameObj.toString().toLowerCase();
        return fileName.endsWith(expectedExtension);
    }
}

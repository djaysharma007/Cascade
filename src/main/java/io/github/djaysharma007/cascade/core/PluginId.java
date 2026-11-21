package io.github.djaysharma007.cascade.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A plugin id is 3 to 40 lowercase letters, digits or hyphens and starts with a letter.
 *
 * @param value the validated string identifier, never null
 */
public record PluginId(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{2,39}$");

    public PluginId {
        Objects.requireNonNull(value, "PluginId value must not be null");
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "PluginId must be 3-40 lowercase letters, digits, or hyphens starting with a letter: " + value);
        }
    }

    public static PluginId of(String value) {
        return new PluginId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

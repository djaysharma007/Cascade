package io.github.djaysharma007.cascade.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A parameter has a name, a type, a required flag and a default that matches its type.
 *
 * @param name parameter identifier, never null or blank
 * @param type data type of parameter, never null
 * @param required whether this parameter must be supplied
 * @param defaultValue default value, or null if none
 * @param options valid choices if type is CHOICE, never null
 */
public record ParamSpec(
        String name,
        ParamType type,
        boolean required,
        Object defaultValue,
        List<String> options
) {
    public ParamSpec {
        Objects.requireNonNull(name, "ParamSpec name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("ParamSpec name must not be blank");
        }
        Objects.requireNonNull(type, "ParamSpec type must not be null");
        options = options == null ? List.of() : List.copyOf(options);
    }

    public static ParamSpec text(String name, boolean required, String defaultValue) {
        return new ParamSpec(name, ParamType.TEXT, required, defaultValue, List.of());
    }

    public static ParamSpec template(String name, boolean required, String defaultValue) {
        return new ParamSpec(name, ParamType.TEMPLATE, required, defaultValue, List.of());
    }

    public static ParamSpec number(String name, boolean required, Number defaultValue) {
        return new ParamSpec(name, ParamType.NUMBER, required, defaultValue, List.of());
    }

    public static ParamSpec bool(String name, boolean required, Boolean defaultValue) {
        return new ParamSpec(name, ParamType.BOOLEAN, required, defaultValue, List.of());
    }

    public static ParamSpec folder(String name, boolean required, String defaultValue) {
        return new ParamSpec(name, ParamType.FOLDER, required, defaultValue, List.of());
    }

    public static ParamSpec choice(String name, boolean required, String defaultValue, List<String> options) {
        return new ParamSpec(name, ParamType.CHOICE, required, defaultValue, options);
    }

    public Optional<Object> defaultVal() {
        return Optional.ofNullable(defaultValue);
    }
}

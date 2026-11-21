package io.github.djaysharma007.cascade.plugins;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.ParamSpec;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.PluginId;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.plugins.actions.MoveFileAction;
import io.github.djaysharma007.cascade.plugins.actions.PrintMessageAction;
import io.github.djaysharma007.cascade.plugins.conditions.AllOfCondition;
import io.github.djaysharma007.cascade.plugins.conditions.AnyOfCondition;
import io.github.djaysharma007.cascade.plugins.conditions.ExtensionIsCondition;
import io.github.djaysharma007.cascade.plugins.conditions.FileSizeOverCondition;
import io.github.djaysharma007.cascade.plugins.conditions.NameContainsCondition;
import io.github.djaysharma007.cascade.plugins.conditions.NotCondition;
import io.github.djaysharma007.cascade.plugins.triggers.FileCreatedTrigger;
import io.github.djaysharma007.cascade.plugins.triggers.ManualTrigger;
import io.github.djaysharma007.cascade.plugins.triggers.TimerTrigger;
import io.github.djaysharma007.cascade.registry.PluginDescriptor;
import io.github.djaysharma007.cascade.registry.PluginRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Registers all built-in triggers, conditions, and actions into a {@link PluginRegistry}.
 */
public final class BuiltinPlugins {

    private BuiltinPlugins() {
    }

    public static void registerAll(PluginRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");

        // Triggers
        registry.register(new PluginDescriptor<>(
                PluginId.of("manual"),
                "Manual",
                PluginCategory.TRIGGER,
                ManualTrigger.class,
                List.of(ParamSpec.text("label", false, "Test run")),
                Set.of(),
                Set.of(),
                params -> new ManualTrigger((String) params.getOrDefault("label", "Test run"))
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("timer"),
                "Timer",
                PluginCategory.TRIGGER,
                TimerTrigger.class,
                List.of(
                        ParamSpec.number("intervalSeconds", true, 60),
                        ParamSpec.bool("fireImmediately", false, false)
                ),
                Set.of("firedAt"),
                Set.of(),
                params -> {
                    Number interval = (Number) params.getOrDefault("intervalSeconds", 60);
                    Boolean immediate = (Boolean) params.getOrDefault("fireImmediately", false);
                    return new TimerTrigger(interval.longValue(), immediate);
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("file-created"),
                "File created",
                PluginCategory.TRIGGER,
                FileCreatedTrigger.class,
                List.of(
                        ParamSpec.folder("folder", true, null),
                        ParamSpec.bool("recursive", false, false)
                ),
                Set.of("fileName", "filePath", "fileSize"),
                Set.of(),
                params -> {
                    String folder = (String) params.get("folder");
                    Boolean rec = (Boolean) params.getOrDefault("recursive", false);
                    return new FileCreatedTrigger(folder != null ? folder : ".", rec);
                }
        ));

        // Conditions
        registry.register(new PluginDescriptor<>(
                PluginId.of("extension-is"),
                "Extension is",
                PluginCategory.CONDITION,
                ExtensionIsCondition.class,
                List.of(ParamSpec.text("value", true, ".pdf")),
                Set.of(),
                Set.of("fileName"),
                params -> new ExtensionIsCondition((String) params.getOrDefault("value", ""))
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("name-contains"),
                "Name contains",
                PluginCategory.CONDITION,
                NameContainsCondition.class,
                List.of(
                        ParamSpec.text("text", true, ""),
                        ParamSpec.bool("caseSensitive", false, false)
                ),
                Set.of(),
                Set.of("fileName"),
                params -> {
                    String text = (String) params.getOrDefault("text", "");
                    Boolean cs = (Boolean) params.getOrDefault("caseSensitive", false);
                    return new NameContainsCondition(text, cs);
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("file-size-over"),
                "File size over",
                PluginCategory.CONDITION,
                FileSizeOverCondition.class,
                List.of(ParamSpec.number("megabytes", true, 1.0)),
                Set.of(),
                Set.of("fileSize"),
                params -> {
                    Number mb = (Number) params.getOrDefault("megabytes", 1.0);
                    return new FileSizeOverCondition(mb.doubleValue());
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("all-of"),
                "All of",
                PluginCategory.CONDITION,
                AllOfCondition.class,
                List.of(),
                Set.of(),
                Set.of(),
                params -> new AllOfCondition(List.of())
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("any-of"),
                "Any of",
                PluginCategory.CONDITION,
                AnyOfCondition.class,
                List.of(),
                Set.of(),
                Set.of(),
                params -> new AnyOfCondition(List.of())
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("not"),
                "Not",
                PluginCategory.CONDITION,
                NotCondition.class,
                List.of(),
                Set.of(),
                Set.of(),
                params -> new NotCondition(e -> true)
        ));

        // Actions
        registry.register(new PluginDescriptor<>(
                PluginId.of("move-file"),
                "Move file",
                PluginCategory.ACTION,
                MoveFileAction.class,
                List.of(
                        ParamSpec.folder("destinationFolder", true, null),
                        ParamSpec.choice("onConflict", false, "rename", List.of("rename", "skip", "overwrite"))
                ),
                Set.of(),
                Set.of("filePath"),
                params -> {
                    String dest = (String) params.getOrDefault("destinationFolder", ".");
                    String conflict = (String) params.getOrDefault("onConflict", "rename");
                    return new MoveFileAction(dest, conflict);
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("print-message"),
                "Print message",
                PluginCategory.ACTION,
                PrintMessageAction.class,
                List.of(ParamSpec.template("text", true, "Executed workflow")),
                Set.of(),
                Set.of(),
                params -> new PrintMessageAction((String) params.getOrDefault("text", ""))
        ));
    }
}

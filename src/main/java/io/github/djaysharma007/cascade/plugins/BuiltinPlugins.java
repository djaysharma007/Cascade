package io.github.djaysharma007.cascade.plugins;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.ParamSpec;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.PluginId;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.plugins.actions.CopyFileAction;
import io.github.djaysharma007.cascade.plugins.actions.DeleteFileAction;
import io.github.djaysharma007.cascade.plugins.actions.DesktopNotificationAction;
import io.github.djaysharma007.cascade.plugins.actions.HttpWebhookAction;
import io.github.djaysharma007.cascade.plugins.actions.MoveFileAction;
import io.github.djaysharma007.cascade.plugins.actions.PrintMessageAction;
import io.github.djaysharma007.cascade.plugins.actions.RenameFileAction;
import io.github.djaysharma007.cascade.plugins.actions.RunProgramAction;
import io.github.djaysharma007.cascade.plugins.actions.SendEmailAction;
import io.github.djaysharma007.cascade.plugins.conditions.AllOfCondition;
import io.github.djaysharma007.cascade.plugins.conditions.AnyOfCondition;
import io.github.djaysharma007.cascade.plugins.conditions.ExtensionIsCondition;
import io.github.djaysharma007.cascade.plugins.conditions.FileSizeOverCondition;
import io.github.djaysharma007.cascade.plugins.conditions.NameContainsCondition;
import io.github.djaysharma007.cascade.plugins.conditions.NotCondition;
import io.github.djaysharma007.cascade.plugins.triggers.FileCreatedTrigger;
import io.github.djaysharma007.cascade.plugins.triggers.FileModifiedTrigger;
import io.github.djaysharma007.cascade.plugins.triggers.ManualTrigger;
import io.github.djaysharma007.cascade.plugins.triggers.ScheduleTrigger;
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

        registry.register(new PluginDescriptor<>(
                PluginId.of("file-modified"),
                "File modified",
                PluginCategory.TRIGGER,
                FileModifiedTrigger.class,
                List.of(
                        ParamSpec.folder("folder", true, null),
                        ParamSpec.bool("recursive", false, false)
                ),
                Set.of("fileName", "filePath", "fileSize", "modifiedAt"),
                Set.of(),
                params -> {
                    String folder = (String) params.get("folder");
                    Boolean rec = (Boolean) params.getOrDefault("recursive", false);
                    return new FileModifiedTrigger(folder != null ? folder : ".", rec);
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("schedule"),
                "Schedule",
                PluginCategory.TRIGGER,
                ScheduleTrigger.class,
                List.of(
                        ParamSpec.text("time", true, "09:00"),
                        ParamSpec.choice("days", false, "daily", List.of(
                                "daily", "weekdays", "weekends", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
                        ))
                ),
                Set.of("firedAt", "scheduledTime", "schedulePattern"),
                Set.of(),
                params -> {
                    String time = (String) params.getOrDefault("time", "09:00");
                    String days = (String) params.getOrDefault("days", "daily");
                    return new ScheduleTrigger(time, days);
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

        registry.register(new PluginDescriptor<>(
                PluginId.of("copy-file"),
                "Copy file",
                PluginCategory.ACTION,
                CopyFileAction.class,
                List.of(
                        ParamSpec.folder("destinationFolder", true, null),
                        ParamSpec.choice("onConflict", false, "rename", List.of("rename", "skip", "overwrite"))
                ),
                Set.of(),
                Set.of("filePath"),
                params -> {
                    String dest = (String) params.getOrDefault("destinationFolder", ".");
                    String conflict = (String) params.getOrDefault("onConflict", "rename");
                    return new CopyFileAction(dest, conflict);
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("rename-file"),
                "Rename file",
                PluginCategory.ACTION,
                RenameFileAction.class,
                List.of(
                        ParamSpec.template("pattern", true, "{name}_{date}.{ext}"),
                        ParamSpec.choice("onConflict", false, "rename", List.of("rename", "skip", "overwrite"))
                ),
                Set.of(),
                Set.of("filePath"),
                params -> {
                    String pattern = (String) params.getOrDefault("pattern", "{name}_{date}.{ext}");
                    String conflict = (String) params.getOrDefault("onConflict", "rename");
                    return new RenameFileAction(pattern, conflict);
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("delete-file"),
                "Move to Recycle Bin",
                PluginCategory.ACTION,
                DeleteFileAction.class,
                List.of(ParamSpec.bool("moveToTrash", false, true)),
                Set.of(),
                Set.of("filePath"),
                params -> {
                    Boolean trash = (Boolean) params.getOrDefault("moveToTrash", true);
                    return new DeleteFileAction(trash != null ? trash : true);
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("run-program"),
                "Run program",
                PluginCategory.ACTION,
                RunProgramAction.class,
                List.of(
                        ParamSpec.text("command", true, "notepad.exe"),
                        ParamSpec.text("arguments", false, ""),
                        ParamSpec.folder("workingDirectory", false, null),
                        ParamSpec.number("timeoutSeconds", false, 30)
                ),
                Set.of(),
                Set.of(),
                params -> {
                    String cmd = (String) params.get("command");
                    String args = (String) params.get("arguments");
                    String wd = (String) params.get("workingDirectory");
                    Number to = (Number) params.getOrDefault("timeoutSeconds", 30);
                    return new RunProgramAction(cmd, args, wd, to.intValue());
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("desktop-notification"),
                "Desktop notification",
                PluginCategory.ACTION,
                DesktopNotificationAction.class,
                List.of(
                        ParamSpec.text("title", false, "Cascade Notification"),
                        ParamSpec.template("message", true, "Operation completed")
                ),
                Set.of(),
                Set.of(),
                params -> {
                    String title = (String) params.getOrDefault("title", "Cascade Notification");
                    String msg = (String) params.getOrDefault("message", "Operation completed");
                    return new DesktopNotificationAction(title, msg);
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("http-webhook"),
                "HTTP webhook",
                PluginCategory.ACTION,
                HttpWebhookAction.class,
                List.of(
                        ParamSpec.text("url", true, "https://httpbin.org/post"),
                        ParamSpec.choice("method", false, "POST", List.of("POST", "GET", "PUT")),
                        ParamSpec.template("payload", false, "{}"),
                        ParamSpec.text("contentType", false, "application/json"),
                        ParamSpec.number("timeoutSeconds", false, 10)
                ),
                Set.of(),
                Set.of(),
                params -> {
                    String url = (String) params.get("url");
                    String method = (String) params.getOrDefault("method", "POST");
                    String payload = (String) params.getOrDefault("payload", "");
                    String contentType = (String) params.getOrDefault("contentType", "application/json");
                    Number to = (Number) params.getOrDefault("timeoutSeconds", 10);
                    return new HttpWebhookAction(url, method, payload, contentType, to.intValue());
                }
        ));

        registry.register(new PluginDescriptor<>(
                PluginId.of("send-email"),
                "Send email",
                PluginCategory.ACTION,
                SendEmailAction.class,
                List.of(
                        ParamSpec.text("smtpHost", true, "smtp.example.com"),
                        ParamSpec.number("smtpPort", false, 587),
                        ParamSpec.text("username", false, ""),
                        ParamSpec.text("passwordEnvVar", false, "SMTP_PASSWORD"),
                        ParamSpec.text("to", true, "user@example.com"),
                        ParamSpec.template("subject", true, "Workflow alert: {workflowName}"),
                        ParamSpec.template("body", true, "Workflow {workflowName} ran at {time}")
                ),
                Set.of(),
                Set.of(),
                params -> {
                    String host = (String) params.get("smtpHost");
                    Number port = (Number) params.getOrDefault("smtpPort", 587);
                    String user = (String) params.get("username");
                    String passEnv = (String) params.get("passwordEnvVar");
                    String to = (String) params.get("to");
                    String sub = (String) params.get("subject");
                    String body = (String) params.get("body");
                    return new SendEmailAction(host, port.intValue(), user, passEnv, to, sub, body);
                }
        ));
    }
}

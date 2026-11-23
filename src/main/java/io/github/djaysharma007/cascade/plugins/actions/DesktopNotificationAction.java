package io.github.djaysharma007.cascade.plugins.actions;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.registry.TemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.Map;
import java.util.Objects;

/**
 * Displays a desktop notification to alert the user of workflow progress.
 */
@Plugin(id = "desktop-notification", displayName = "Desktop notification", category = PluginCategory.ACTION)
public final class DesktopNotificationAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(DesktopNotificationAction.class);

    private final String title;
    private final String message;

    public DesktopNotificationAction(String title, String message) {
        this.title = title != null && !title.isBlank() ? title.trim() : "Cascade Notification";
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    @Override
    public ExecutionResult execute(Event event, ExecutionContext context) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String resolvedTitle = TemplateResolver.resolve(title, event, context, null);
        String resolvedMessage = TemplateResolver.resolve(message, event, context, null);

        log.info("[Desktop Notification] {}: {}", resolvedTitle, resolvedMessage);

        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                TrayIcon[] icons = tray.getTrayIcons();
                if (icons.length > 0) {
                    icons[0].displayMessage(resolvedTitle, resolvedMessage, TrayIcon.MessageType.INFO);
                }
            }
        } catch (Exception e) {
            log.debug("Tray displayMessage skipped: {}", e.getMessage());
        }

        return new ExecutionResult.Success(Map.of(
                "title", resolvedTitle,
                "message", resolvedMessage
        ));
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}

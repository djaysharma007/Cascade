package io.github.djaysharma007.cascade.ui.components;

import java.util.Map;

/**
 * Maps raw plugin IDs and system codes to human-readable friendly names.
 * Ensures that identifiers like 'file-created' or 'move-file' are never exposed in UI copy.
 */
public final class PluginDisplayNameResolver {

    private static final Map<String, String> FRIENDLY_NAMES = Map.ofEntries(
            Map.entry("manual", "Manual trigger"),
            Map.entry("timer", "Interval timer"),
            Map.entry("file-created", "File created"),
            Map.entry("file-modified", "File modified"),
            Map.entry("schedule", "Scheduled time"),
            Map.entry("extension-is", "Extension matches"),
            Map.entry("name-contains", "File name contains"),
            Map.entry("file-size-over", "File size exceeds"),
            Map.entry("all-of", "All conditions match"),
            Map.entry("any-of", "Any condition matches"),
            Map.entry("not", "Invert condition"),
            Map.entry("move-file", "Move file"),
            Map.entry("copy-file", "Copy file"),
            Map.entry("rename-file", "Rename file"),
            Map.entry("delete-file", "Move to Recycle Bin"),
            Map.entry("print-message", "Print message"),
            Map.entry("desktop-notification", "Desktop notification"),
            Map.entry("run-program", "Run program"),
            Map.entry("http-webhook", "HTTP webhook"),
            Map.entry("send-email", "Send email")
    );

    private PluginDisplayNameResolver() {
    }

    public static String resolve(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return "Unknown";
        }
        String friendly = FRIENDLY_NAMES.get(rawId.toLowerCase().trim());
        if (friendly != null) {
            return friendly;
        }

        // Fallback: convert kebab-case or snake_case to Title Case
        String[] parts = rawId.replace('_', '-').split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}

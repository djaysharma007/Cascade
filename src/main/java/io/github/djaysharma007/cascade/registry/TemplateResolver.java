package io.github.djaysharma007.cascade.registry;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves template tokens such as {fileName}, {filePath}, {fileSize}, {time}, {workflowName}, {runId}.
 */
public final class TemplateResolver {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static final Set<String> KNOWN_PLACEHOLDERS = Set.of(
            "fileName", "filePath", "fileSize", "time", "workflowName", "runId"
    );

    private TemplateResolver() {
    }

    /**
     * Extracts all placeholder token names used in a template string.
     */
    public static Set<String> extractPlaceholders(String template) {
        if (template == null || template.isEmpty()) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        while (matcher.find()) {
            tokens.add(matcher.group(1));
        }
        return Set.copyOf(tokens);
    }

    /**
     * Resolves all placeholder tokens in template with event payload and execution context.
     */
    public static String resolve(String template, Event event, ExecutionContext context, String workflowName) {
        if (template == null) {
            return "";
        }
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1);
            String replacement = resolveToken(token, event, context, workflowName);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String resolveToken(String token, Event event, ExecutionContext context, String workflowName) {
        if ("time".equals(token)) {
            Instant t = event != null ? event.timestamp() : Instant.now();
            return TIME_FORMAT.format(t);
        }
        if ("workflowName".equals(token)) {
            return workflowName != null ? workflowName : (context != null ? context.workflowId().value().toString() : "");
        }
        if ("runId".equals(token)) {
            return context != null ? context.runId().value().toString() : "";
        }
        if (event != null && event.payload().containsKey(token)) {
            return Objects.toString(event.payload().get(token), "");
        }
        return "{" + token + "}";
    }
}

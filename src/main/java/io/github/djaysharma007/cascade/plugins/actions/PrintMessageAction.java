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

import java.util.Map;
import java.util.Objects;

/**
 * Prints a message resolved from template placeholders to the SLF4J application log.
 */
@Plugin(id = "print-message", displayName = "Print message", category = PluginCategory.ACTION)
public final class PrintMessageAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(PrintMessageAction.class);

    private final String template;

    public PrintMessageAction(String template) {
        this.template = Objects.requireNonNull(template, "template must not be null");
    }

    @Override
    public ExecutionResult execute(Event event, ExecutionContext context) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String resolved = TemplateResolver.resolve(template, event, context, null);
        log.info("[Cascade Action: Print Message] {}", resolved);

        return new ExecutionResult.Success(Map.of("message", resolved));
    }

    public String template() {
        return template;
    }
}

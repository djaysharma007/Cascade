package io.github.djaysharma007.cascade.plugins;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.registry.TemplateResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateResolverTest {

    @Test
    void extractsPlaceholders() {
        String template = "File {fileName} moved to {filePath} at {time} in {workflowName}";
        Set<String> placeholders = TemplateResolver.extractPlaceholders(template);
        assertThat(placeholders).containsExactlyInAnyOrder("fileName", "filePath", "time", "workflowName");
    }

    @Test
    void resolvesKnownPlaceholders() {
        WorkflowId wid = WorkflowId.random();
        RunId rid = RunId.random();
        ExecutionContext ctx = new ExecutionContext(wid, rid, 1);
        Event event = Event.of(Map.of("fileName", "report.pdf", "fileSize", 4096));

        String template = "Saved {fileName} ({fileSize} bytes) run={runId} wf={workflowName}";
        String resolved = TemplateResolver.resolve(template, event, ctx, "Sort Assignments");

        assertThat(resolved)
                .contains("Saved report.pdf (4096 bytes)")
                .contains("run=" + rid.value())
                .contains("wf=Sort Assignments");
    }
}

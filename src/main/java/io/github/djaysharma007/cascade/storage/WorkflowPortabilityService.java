package io.github.djaysharma007.cascade.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Handles exporting and importing workflows as human-readable JSON files.
 */
public final class WorkflowPortabilityService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowPortabilityService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private WorkflowPortabilityService() {
    }

    public static void exportWorkflow(Workflow workflow, Path targetFile) throws IOException {
        Objects.requireNonNull(workflow, "workflow must not be null");
        Objects.requireNonNull(targetFile, "targetFile must not be null");

        Path parent = targetFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        JsonWorkflowRepository.WorkflowDto dto = JsonWorkflowRepository.toDto(workflow);
        MAPPER.writeValue(targetFile.toFile(), dto);
        log.info("Exported workflow '{}' ({}) to {}", workflow.name(), workflow.id(), targetFile);
    }

    public static Workflow importWorkflow(Path sourceFile, WorkflowRepository repository, EngineFacade facade) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(facade, "facade must not be null");

        if (!Files.exists(sourceFile)) {
            throw new IOException("Workflow import file does not exist: " + sourceFile);
        }

        JsonWorkflowRepository.WorkflowDto dto = MAPPER.readValue(sourceFile.toFile(), JsonWorkflowRepository.WorkflowDto.class);
        Workflow imported = JsonWorkflowRepository.fromDto(dto);

        // Sanitize imported workflow
        WorkflowId id = imported.id() != null ? imported.id() : WorkflowId.random();
        String name = imported.name() != null && !imported.name().isBlank() ? imported.name() : "Imported Workflow";

        Workflow validated = Workflow.builder()
                .id(id)
                .name(name)
                .enabled(imported.enabled())
                .trigger(imported.trigger())
                .conditions(imported.conditionSpec().orElse(null))
                .actions(imported.actions())
                .build();

        repository.save(validated);
        facade.registerWorkflow(validated);
        log.info("Successfully imported workflow '{}' from {}", validated.name(), sourceFile);
        return validated;
    }

    public static int exportAllWorkflows(List<Workflow> workflows, Path targetFolder) throws IOException {
        Objects.requireNonNull(workflows, "workflows must not be null");
        Objects.requireNonNull(targetFolder, "targetFolder must not be null");

        Files.createDirectories(targetFolder);
        int count = 0;
        for (Workflow wf : workflows) {
            String safeName = wf.name().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            Path file = targetFolder.resolve(safeName + "_" + wf.id().value() + ".json");
            exportWorkflow(wf, file);
            count++;
        }
        return count;
    }
}

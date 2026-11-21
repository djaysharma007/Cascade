package io.github.djaysharma007.cascade.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.PluginId;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowBuilder;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Persists workflows as individual JSON files under a directory.
 * Writes are atomic using temporary files.
 */
public final class JsonWorkflowRepository implements WorkflowRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonWorkflowRepository.class);
    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final Path workflowsDir;
    private final ObjectMapper mapper;
    private final List<CorruptWorkflowFile> corruptFiles = new CopyOnWriteArrayList<>();

    public JsonWorkflowRepository() {
        this(Path.of(System.getProperty("user.home"), ".cascade", "workflows"));
    }

    public JsonWorkflowRepository(Path workflowsDir) {
        this.workflowsDir = Objects.requireNonNull(workflowsDir, "workflowsDir must not be null");
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(workflowsDir);
        } catch (IOException e) {
            log.error("Failed to create workflows directory: {}", workflowsDir, e);
        }
    }

    @Override
    public void save(Workflow workflow) {
        Objects.requireNonNull(workflow, "workflow must not be null");
        ensureDirectoryExists();

        Path targetPath = workflowsDir.resolve(workflow.id().value().toString() + ".json");
        Path tempPath = workflowsDir.resolve(workflow.id().value().toString() + ".tmp");

        WorkflowDto dto = toDto(workflow);

        try {
            // Write to temporary file
            mapper.writeValue(tempPath.toFile(), dto);
            // Move atomically to destination
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Saved workflow {} to {}", workflow.name(), targetPath);
        } catch (IOException e) {
            log.error("Failed to save workflow {}", workflow.name(), e);
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            throw new RuntimeException("Failed to save workflow " + workflow.id(), e);
        }
    }

    @Override
    public Optional<Workflow> findById(WorkflowId id) {
        Objects.requireNonNull(id, "WorkflowId must not be null");
        Path filePath = workflowsDir.resolve(id.value().toString() + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            WorkflowDto dto = mapper.readValue(filePath.toFile(), WorkflowDto.class);
            return Optional.of(fromDto(dto));
        } catch (Exception e) {
            log.warn("Failed to read workflow file: {}", filePath, e);
            corruptFiles.add(new CorruptWorkflowFile(filePath.getFileName().toString(), e.getMessage()));
            return Optional.empty();
        }
    }

    @Override
    public List<Workflow> findAll() {
        corruptFiles.clear();
        ensureDirectoryExists();
        List<Workflow> result = new ArrayList<>();

        try (Stream<Path> stream = Files.list(workflowsDir)) {
            List<Path> jsonFiles = stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .toList();

            for (Path path : jsonFiles) {
                try {
                    WorkflowDto dto = mapper.readValue(path.toFile(), WorkflowDto.class);
                    result.add(fromDto(dto));
                } catch (Exception e) {
                    log.warn("Skipping corrupt or unreadable workflow file: {}", path, e);
                    corruptFiles.add(new CorruptWorkflowFile(path.getFileName().toString(), e.getMessage()));
                }
            }
        } catch (IOException e) {
            log.error("Failed to list workflows directory: {}", workflowsDir, e);
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public void delete(WorkflowId id) {
        Objects.requireNonNull(id, "WorkflowId must not be null");
        Path filePath = workflowsDir.resolve(id.value().toString() + ".json");
        try {
            Files.deleteIfExists(filePath);
            log.info("Deleted workflow file: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete workflow file: {}", filePath, e);
            throw new RuntimeException("Failed to delete workflow " + id, e);
        }
    }

    @Override
    public List<CorruptWorkflowFile> getCorruptFiles() {
        return Collections.unmodifiableList(corruptFiles);
    }

    // DTO Mappings

    private static WorkflowDto toDto(Workflow wf) {
        WorkflowDto dto = new WorkflowDto();
        dto.schemaVersion = CURRENT_SCHEMA_VERSION;
        dto.id = wf.id().value().toString();
        dto.name = wf.name();
        dto.enabled = wf.enabled();
        dto.trigger = toPluginDto(wf.trigger());
        dto.conditions = wf.conditionSpec().map(JsonWorkflowRepository::toConditionDto).orElse(null);
        dto.actions = wf.actions().stream().map(JsonWorkflowRepository::toPluginDto).toList();
        return dto;
    }

    private static Workflow fromDto(WorkflowDto dto) {
        WorkflowBuilder builder = Workflow.builder()
                .id(WorkflowId.of(dto.id))
                .name(dto.name)
                .enabled(dto.enabled)
                .trigger(fromPluginDto(dto.trigger));

        if (dto.conditions != null) {
            builder.conditions(fromConditionDto(dto.conditions));
        }

        if (dto.actions != null) {
            builder.actions(dto.actions.stream().map(JsonWorkflowRepository::fromPluginDto).toList());
        }

        return builder.build();
    }

    private static PluginSpecDto toPluginDto(PluginSpec spec) {
        PluginSpecDto dto = new PluginSpecDto();
        dto.type = spec.pluginId().value();
        dto.params = spec.params();
        return dto;
    }

    private static PluginSpec fromPluginDto(PluginSpecDto dto) {
        return PluginSpec.of(dto.type, dto.params);
    }

    private static ConditionSpecDto toConditionDto(ConditionSpec spec) {
        ConditionSpecDto dto = new ConditionSpecDto();
        dto.type = spec.type();
        dto.params = spec.params();
        if (!spec.children().isEmpty()) {
            dto.children = spec.children().stream().map(JsonWorkflowRepository::toConditionDto).toList();
        }
        return dto;
    }

    private static ConditionSpec fromConditionDto(ConditionSpecDto dto) {
        List<ConditionSpec> children = dto.children == null
                ? List.of()
                : dto.children.stream().map(JsonWorkflowRepository::fromConditionDto).toList();
        return new ConditionSpec(dto.type, dto.params, children);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class WorkflowDto {
        @JsonProperty("schemaVersion")
        public int schemaVersion;
        @JsonProperty("id")
        public String id;
        @JsonProperty("name")
        public String name;
        @JsonProperty("enabled")
        public boolean enabled;
        @JsonProperty("trigger")
        public PluginSpecDto trigger;
        @JsonProperty("conditions")
        public ConditionSpecDto conditions;
        @JsonProperty("actions")
        public List<PluginSpecDto> actions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class PluginSpecDto {
        @JsonProperty("type")
        public String type;
        @JsonProperty("params")
        public Map<String, Object> params;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ConditionSpecDto {
        @JsonProperty("type")
        public String type;
        @JsonProperty("params")
        public Map<String, Object> params;
        @JsonProperty("children")
        public List<ConditionSpecDto> children;
    }
}

package io.github.djaysharma007.cascade.storage;

import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JsonWorkflowRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonWorkflowRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JsonWorkflowRepository(tempDir);
    }

    @Test
    void roundTripSaveAndFindById() {
        Workflow wf = Workflow.builder()
                .name("Sort assignments")
                .enabled(true)
                .trigger(PluginSpec.of("file-created", Map.of("folder", "C:/Downloads")))
                .conditions(ConditionSpec.allOf(List.of(
                        ConditionSpec.leaf("extension-is", Map.of("value", ".pdf")),
                        ConditionSpec.leaf("name-contains", Map.of("text", "assignment"))
                )))
                .addAction(PluginSpec.of("move-file", Map.of("destinationFolder", "C:/Assignments")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Saved {fileName}")))
                .build();

        repository.save(wf);

        Optional<Workflow> loaded = repository.findById(wf.id());
        assertThat(loaded).isPresent();
        Workflow restored = loaded.get();
        assertThat(restored.id()).isEqualTo(wf.id());
        assertThat(restored.name()).isEqualTo("Sort assignments");
        assertThat(restored.enabled()).isTrue();
        assertThat(restored.trigger().pluginId().value()).isEqualTo("file-created");
        assertThat(restored.conditionSpec()).isPresent();
        assertThat(restored.conditionSpec().get().children()).hasSize(2);
        assertThat(restored.actions()).hasSize(2);

        // Verify JSON contents have schemaVersion 1
        Path filePath = tempDir.resolve(wf.id().value().toString() + ".json");
        assertThat(filePath).exists();
        try {
            String json = Files.readString(filePath);
            assertThat(json).contains("\"schemaVersion\" : 1");
            assertThat(json).contains("\"Sort assignments\"");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findAllAndDeletion() {
        Workflow wf1 = Workflow.builder()
                .name("Workflow 1")
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("print-message", Map.of("text", "1")))
                .build();

        Workflow wf2 = Workflow.builder()
                .name("Workflow 2")
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("print-message", Map.of("text", "2")))
                .build();

        repository.save(wf1);
        repository.save(wf2);

        List<Workflow> all = repository.findAll();
        assertThat(all).hasSize(2);

        repository.delete(wf1.id());
        assertThat(repository.findById(wf1.id())).isEmpty();
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void corruptFileIsHandledGracefullyWithoutCrashing() throws IOException {
        // Create a corrupt non-JSON file in the repository folder
        Path corruptFile = tempDir.resolve("corrupt-workflow.json");
        Files.writeString(corruptFile, "{ this is not valid json !!!");

        List<Workflow> workflows = repository.findAll();
        assertThat(workflows).isEmpty();

        List<WorkflowRepository.CorruptWorkflowFile> corrupt = repository.getCorruptFiles();
        assertThat(corrupt).hasSize(1);
        assertThat(corrupt.get(0).fileName()).isEqualTo("corrupt-workflow.json");
        assertThat(corrupt.get(0).reason()).isNotBlank();
    }
}

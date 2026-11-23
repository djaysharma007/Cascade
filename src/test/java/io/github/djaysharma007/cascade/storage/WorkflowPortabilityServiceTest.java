package io.github.djaysharma007.cascade.storage;

import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowPortabilityServiceTest {

    @TempDir
    Path tempDir;

    private WorkflowRepository repository;
    private EngineFacade facade;

    @BeforeEach
    void setUp() {
        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        repository = new JsonWorkflowRepository(tempDir.resolve("repo"));
        WorkflowEngine engine = new WorkflowEngine(registry);
        facade = new EngineFacade(engine);
    }

    @Test
    void testExportAndImportWorkflowRoundTrip() throws IOException {
        Workflow wf = Workflow.builder()
                .id(WorkflowId.random())
                .name("Organize Invoices")
                .enabled(true)
                .trigger(PluginSpec.of("file-created", Map.of("directory", "C:\\Invoices")))
                .conditions(ConditionSpec.allOf(List.of(
                        ConditionSpec.leaf("extension-is", Map.of("extension", "pdf")),
                        ConditionSpec.leaf("name-contains", Map.of("substring", "Invoice"))
                )))
                .addAction(PluginSpec.of("move-file", Map.of("destination", "C:\\Archive")))
                .addAction(PluginSpec.of("desktop-notification", Map.of("title", "Invoice Saved", "message", "File organized")))
                .build();

        Path exportFile = tempDir.resolve("exported_invoice.json");
        WorkflowPortabilityService.exportWorkflow(wf, exportFile);

        assertThat(Files.exists(exportFile)).isTrue();
        String json = Files.readString(exportFile);
        assertThat(json).contains("Organize Invoices");
        assertThat(json).contains("schemaVersion");
        assertThat(json).contains("file-created");

        // Import into clean repo and facade
        Workflow imported = WorkflowPortabilityService.importWorkflow(exportFile, repository, facade);

        assertThat(imported).isNotNull();
        assertThat(imported.name()).isEqualTo("Organize Invoices");
        assertThat(imported.enabled()).isTrue();
        assertThat(imported.trigger().pluginId().value()).isEqualTo("file-created");
        assertThat(imported.conditionSpec()).isPresent();
        assertThat(imported.actions()).hasSize(2);
        assertThat(imported.actions().get(0).pluginId().value()).isEqualTo("move-file");
        assertThat(imported.actions().get(1).pluginId().value()).isEqualTo("desktop-notification");

        // Verify it was saved to repository and registered in facade
        assertThat(repository.findById(imported.id())).isPresent();
        assertThat(facade.findWorkflow(imported.id())).isPresent();
    }

    @Test
    void testExportAllWorkflows() throws IOException {
        Workflow wf1 = Workflow.builder()
                .name("Workflow 1")
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("print-message", Map.of("text", "One")))
                .build();

        Workflow wf2 = Workflow.builder()
                .name("Workflow 2")
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Two")))
                .build();

        Path exportDir = tempDir.resolve("export_all");
        int count = WorkflowPortabilityService.exportAllWorkflows(List.of(wf1, wf2), exportDir);

        assertThat(count).isEqualTo(2);
        try (var files = Files.list(exportDir)) {
            assertThat(files.count()).isEqualTo(2);
        }
    }

    @Test
    void testImportNonExistentFileThrows() {
        Path missing = tempDir.resolve("does_not_exist.json");
        assertThatThrownBy(() -> WorkflowPortabilityService.importWorkflow(missing, repository, facade))
                .isInstanceOf(IOException.class);
    }
}

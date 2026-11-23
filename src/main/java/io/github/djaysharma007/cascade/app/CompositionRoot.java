package io.github.djaysharma007.cascade.app;

import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.settings.SettingsService;
import io.github.djaysharma007.cascade.storage.JsonWorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Composition root wiring the dependencies: Registry, Storage, Engine, and Facade.
 */
public final class CompositionRoot {

    private static final Logger log = LoggerFactory.getLogger(CompositionRoot.class);

    private final PluginRegistry registry;
    private final WorkflowRepository repository;
    private final WorkflowEngine engine;
    private final EngineFacade facade;
    private final SettingsService settingsService;

    public CompositionRoot() {
        this(Path.of(System.getProperty("user.home"), ".cascade", "workflows"));
    }

    public CompositionRoot(Path workflowsDir) {
        this(workflowsDir, (workflowsDir.getParent() != null ? workflowsDir.getParent() : Path.of(".")).resolve("settings.json"));
    }

    public CompositionRoot(Path workflowsDir, Path settingsFile) {
        log.info("Initializing CompositionRoot with workflows dir: {}", workflowsDir);
        this.registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);

        this.repository = new JsonWorkflowRepository(workflowsDir);
        this.engine = new WorkflowEngine(registry);
        this.facade = new EngineFacade(engine);
        this.settingsService = new SettingsService(settingsFile);

        loadInitialWorkflows();
    }

    private void loadInitialWorkflows() {
        List<Workflow> saved = repository.findAll();
        if (saved.isEmpty()) {
            // Seed reference workflow per Section 3 of PRD
            String downloads = Path.of(System.getProperty("user.home"), "Downloads").toString();
            String assignments = Path.of(System.getProperty("user.home"), "Documents", "College", "Assignments").toString();

            Workflow demo = Workflow.builder()
                    .name("Sort assignments")
                    .enabled(true)
                    .trigger(PluginSpec.of("file-created", Map.of("folder", downloads, "recursive", false)))
                    .conditions(ConditionSpec.allOf(List.of(
                            ConditionSpec.leaf("extension-is", Map.of("value", ".pdf")),
                            ConditionSpec.leaf("name-contains", Map.of("text", "assignment", "caseSensitive", false))
                    )))
                    .addAction(PluginSpec.of("move-file", Map.of("destinationFolder", assignments, "onConflict", "rename")))
                    .addAction(PluginSpec.of("print-message", Map.of("text", "Saved {fileName}")))
                    .build();

            repository.save(demo);
            saved = List.of(demo);
            log.info("Created initial reference workflow: {}", demo.name());
        }

        for (Workflow wf : saved) {
            engine.registerWorkflow(wf);
        }
    }

    public PluginRegistry registry() {
        return registry;
    }

    public WorkflowRepository repository() {
        return repository;
    }

    public EngineFacade facade() {
        return facade;
    }

    public SettingsService settingsService() {
        return settingsService;
    }
}

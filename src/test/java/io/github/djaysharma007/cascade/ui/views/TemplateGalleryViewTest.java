package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.storage.JsonWorkflowRepository;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateGalleryViewTest {

    private static boolean platformStarted = false;

    @BeforeAll
    static synchronized void initPlatform() {
        if (!platformStarted) {
            try {
                Platform.startup(() -> {});
                platformStarted = true;
            } catch (IllegalStateException ignored) {
                platformStarted = true;
            }
        }
    }

    @Test
    void testTemplateGalleryInitialization() {
        AtomicReference<Workflow> usedWorkflow = new AtomicReference<>();
        TemplateGalleryView gallery = new TemplateGalleryView(usedWorkflow::set);

        assertThat(gallery.getNode()).isNotNull();
        assertThat(gallery.getTemplateCount()).isGreaterThanOrEqualTo(6);

        List<WorkflowTemplate> templates = gallery.getTemplates();
        for (WorkflowTemplate t : templates) {
            assertThat(t.id()).isNotBlank();
            assertThat(t.title()).isNotBlank();
            assertThat(t.description()).isNotBlank();
            assertThat(t.category()).isNotBlank();
            assertThat(t.iconName()).isNotBlank();
            assertThat(t.templateWorkflow()).isNotNull();
            assertThat(t.templateWorkflow().trigger()).isNotNull();
            assertThat(t.templateWorkflow().actions()).isNotEmpty();
        }
    }

    @Test
    void testSearchAndCategoryFiltering() {
        AtomicReference<Workflow> usedWorkflow = new AtomicReference<>();
        TemplateGalleryView gallery = new TemplateGalleryView(usedWorkflow::set);

        gallery.search("screenshot");
        // Verify filtering does not throw and maintains integrity
        assertThat(gallery.getNode()).isNotNull();

        gallery.search("");
        gallery.selectCategory("Monitoring");
        assertThat(gallery.getNode()).isNotNull();

        gallery.selectCategory("All");
        assertThat(gallery.getTemplateCount()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void testUseTemplateInstantiatesWorkflow() {
        AtomicReference<Workflow> usedWorkflow = new AtomicReference<>();
        TemplateGalleryView gallery = new TemplateGalleryView(usedWorkflow::set);

        gallery.useTemplate("sort-downloads-by-type");

        Workflow instantiated = usedWorkflow.get();
        assertThat(instantiated).isNotNull();
        assertThat(instantiated.name()).isEqualTo("Sort downloads by file type");
        assertThat(instantiated.enabled()).isTrue();
        assertThat(instantiated.trigger().pluginId().value()).isEqualTo("file-created");
        assertThat(instantiated.conditionSpec()).isPresent();
        assertThat(instantiated.actions()).hasSize(1);
        assertThat(instantiated.actions().get(0).pluginId().value()).isEqualTo("move-file");
    }

    @Test
    void testMainShellNavigationToTemplates(@TempDir Path tempDir) {
        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        WorkflowRepository repo = new JsonWorkflowRepository(tempDir);
        WorkflowEngine engine = new WorkflowEngine(registry);
        EngineFacade facade = new EngineFacade(engine);

        MainShellView shell = new MainShellView(facade, repo, registry);
        assertThat(shell.getNode()).isNotNull();

        shell.showTemplateGalleryView();
        assertThat(shell.getTemplateGalleryView()).isNotNull();
    }
}

package io.github.djaysharma007.cascade.app;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.storage.JsonWorkflowRepository;
import io.github.djaysharma007.cascade.ui.views.ActivityView;
import io.github.djaysharma007.cascade.ui.views.MainShellView;
import io.github.djaysharma007.cascade.ui.views.WorkflowEditorView;
import io.github.djaysharma007.cascade.ui.views.WorkflowsView;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CascadeApplicationTest {

    private static boolean platformStarted = false;

    @BeforeAll
    static synchronized void initPlatform() {
        System.setProperty("cascade.disable.lock", "true");
        System.setProperty("cascade.disable.tray", "true");
        if (!platformStarted) {
            try {
                Platform.startup(() -> {});
                platformStarted = true;
            } catch (IllegalStateException ignored) {
                platformStarted = true;
            }
        }
    }

    private static void saveSnapshot(Scene scene, String filename) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<WritableImage> imageRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                WritableImage image = scene.snapshot(null);
                imageRef.set(image);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        WritableImage fxImage = imageRef.get();
        assertThat(fxImage).isNotNull();

        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = fxImage.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, reader.getArgb(x, y));
            }
        }

        Path screenshotDir = Path.of("docs", "screenshots");
        Files.createDirectories(screenshotDir);
        File file = screenshotDir.resolve(filename).toFile();
        ImageIO.write(bufferedImage, "png", file);
        assertThat(file).exists();
        assertThat(file.length()).isGreaterThan(0);
    }

    @Test
    void applicationFullUiWorkflowAndScreenshots(@TempDir Path tempWorkflowsDir) throws Exception {
        // Initialize CompositionRoot with temporary directory
        CompositionRoot compRoot = new CompositionRoot(tempWorkflowsDir);
        PluginRegistry registry = compRoot.registry();
        WorkflowRepository repository = compRoot.repository();
        EngineFacade facade = compRoot.facade();

        CountDownLatch stageLatch = new CountDownLatch(1);
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicReference<MainShellView> shellRef = new AtomicReference<>();
        AtomicReference<Scene> sceneRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                MainShellView shell = new MainShellView(facade, repository, registry);
                Scene scene = new Scene((Parent) shell.getNode(), 1280, 800);
                String cssUrl = getClass().getResource("/theme/cascade.css") != null
                        ? getClass().getResource("/theme/cascade.css").toExternalForm()
                        : null;
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl);
                }
                stage.setScene(scene);
                stage.show();

                stageRef.set(stage);
                shellRef.set(shell);
                sceneRef.set(scene);
            } finally {
                stageLatch.countDown();
            }
        });

        assertThat(stageLatch.await(10, TimeUnit.SECONDS)).isTrue();
        Stage stage = stageRef.get();
        MainShellView shell = shellRef.get();
        Scene scene = sceneRef.get();

        // 0. Capture default screen: Dashboard View
        saveSnapshot(scene, "v1_1_dashboard.png");

        // 1. Switch to Workflows View and capture
        CountDownLatch wfLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            shell.showWorkflowsView();
            wfLatch.countDown();
        });
        assertThat(wfLatch.await(5, TimeUnit.SECONDS)).isTrue();
        saveSnapshot(scene, "phase5_workflows_populated.png");

        // 1b. Switch to Template Gallery View and capture
        CountDownLatch tplLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            shell.showTemplateGalleryView();
            tplLatch.countDown();
        });
        assertThat(tplLatch.await(5, TimeUnit.SECONDS)).isTrue();
        saveSnapshot(scene, "v1_1_template_gallery.png");

        // 2. Open Editor view and capture
        CountDownLatch editorLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            shell.openNewWorkflowEditor();
            editorLatch.countDown();
        });
        assertThat(editorLatch.await(5, TimeUnit.SECONDS)).isTrue();
        saveSnapshot(scene, "phase5_editor.png");

        // 3. Switch to Activity View (empty state) and capture
        CountDownLatch actEmptyLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            shell.showActivityView();
            actEmptyLatch.countDown();
        });
        assertThat(actEmptyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        saveSnapshot(scene, "phase5_activity_empty.png");

        // 4. Run an execution and capture populated Activity View
        Workflow wf = facade.allWorkflows().get(0);
        Event sampleEvent = Event.of(Map.of(
                "fileName", "assignment_final.pdf",
                "filePath", tempWorkflowsDir.resolve("assignment_final.pdf").toString(),
                "fileSize", 1024 * 500
        ));
        facade.executeWorkflowManually(wf.id(), sampleEvent);

        CountDownLatch actPopLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            shell.showActivityView();
            actPopLatch.countDown();
        });
        assertThat(actPopLatch.await(5, TimeUnit.SECONDS)).isTrue();
        saveSnapshot(scene, "phase5_activity_populated.png");

        // 4b. Select run in table and capture detail panel with failure cause banner & timeline
        CountDownLatch actDetailLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            ActivityView actView = shell.getActivityView();
            BorderPane pane = (BorderPane) actView.getNode();
            if (pane.getCenter() instanceof TableView<?> tv && !tv.getItems().isEmpty()) {
                tv.getSelectionModel().select(0);
            }
            actDetailLatch.countDown();
        });
        assertThat(actDetailLatch.await(5, TimeUnit.SECONDS)).isTrue();
        saveSnapshot(scene, "phase5_activity_detail.png");

        // 5. Test engine start/stop toggling
        CountDownLatch engineLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            facade.start();
            assertThat(facade.isRunning()).isTrue();
            facade.stop();
            assertThat(facade.isRunning()).isFalse();
            engineLatch.countDown();
        });
        assertThat(engineLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // 6. Delete workflow to capture empty workflows view
        CountDownLatch deleteLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            facade.unregisterWorkflow(wf.id());
            repository.delete(wf.id());
            shell.showWorkflowsView();
            deleteLatch.countDown();
        });
        assertThat(deleteLatch.await(5, TimeUnit.SECONDS)).isTrue();
        saveSnapshot(scene, "phase5_workflows_empty.png");

        // Close stage
        Platform.runLater(stage::close);
    }
}

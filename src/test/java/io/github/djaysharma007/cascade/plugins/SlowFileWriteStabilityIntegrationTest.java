package io.github.djaysharma007.cascade.plugins;

import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunOutcome;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SlowFileWriteStabilityIntegrationTest {

    @TempDir
    Path tempFolder;

    private Path sourceDir;
    private Path destDir;
    private PluginRegistry registry;
    private WorkflowEngine engine;
    private EngineFacade facade;

    @BeforeEach
    void setUp() throws IOException {
        sourceDir = tempFolder.resolve("source_testcascade");
        destDir = tempFolder.resolve("dest_testcascade2");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        engine = new WorkflowEngine(registry);
        facade = new EngineFacade(engine);
    }

    @AfterEach
    void tearDown() {
        if (facade.isRunning()) {
            facade.stop();
        }
    }

    @Test
    void slowChunkedWriteStabilizesAndMovesSuccessfullyWithSingleRow() throws Exception {
        Workflow wf = Workflow.builder()
                .name("first test")
                .enabled(true)
                .trigger(PluginSpec.of("file-created", Map.of("folder", sourceDir.toString())))
                .conditions(ConditionSpec.leaf("extension-is", Map.of("value", ".pdf")))
                .addAction(PluginSpec.of("move-file", Map.of("destinationFolder", destDir.toString(), "onConflict", "rename")))
                .build();

        CountDownLatch completedLatch = new CountDownLatch(1);
        facade.addListener(new io.github.djaysharma007.cascade.engine.EngineListener() {
            @Override
            public void onEngineStateChanged(boolean running) {}

            @Override
            public void onWorkflowStateChanged(io.github.djaysharma007.cascade.core.WorkflowId workflowId, io.github.djaysharma007.cascade.core.WorkflowState state) {}

            @Override
            public void onRunUpdated(RunRecord record) {
                if (record.outcome() == RunOutcome.SUCCEEDED || record.outcome() == RunOutcome.FAILED) {
                    completedLatch.countDown();
                }
            }
        });

        facade.registerWorkflow(wf);
        facade.start();

        // Give the WatchService a brief moment to register
        Thread.sleep(200);

        Path testPdf = sourceDir.resolve("sample-document.pdf");

        // Simulate pasting / slow network copy: write in chunks over ~750ms keeping the file locked
        try (FileOutputStream out = new FileOutputStream(testPdf.toFile())) {
            byte[] chunk = "PDF-HEADER-CHUNK-DATA-".getBytes();
            for (int i = 0; i < 5; i++) {
                out.write(chunk);
                out.flush();
                Thread.sleep(150);
            }
        }

        // Wait for stability and move execution
        boolean finished = completedLatch.await(10, TimeUnit.SECONDS);
        assertThat(finished).as("Workflow execution did not finish in time").isTrue();

        // Give any in-flight persistence a split second
        Thread.sleep(200);

        // Verify exactly 1 run record exists (Requirement 2: One row per run)
        assertThat(facade.getRuns()).hasSize(1);
        RunRecord run = facade.getRuns().get(0);
        assertThat(run.outcome()).isEqualTo(RunOutcome.SUCCEEDED);

        // Verify source file is moved
        assertThat(Files.exists(testPdf)).as("Source file should have been moved").isFalse();

        // Verify destination file exists with the complete content
        Path movedFile = destDir.resolve("sample-document.pdf");
        assertThat(Files.exists(movedFile)).as("Moved file should exist in destination").isTrue();
        assertThat(Files.size(movedFile)).isEqualTo("PDF-HEADER-CHUNK-DATA-".getBytes().length * 5);
    }
}

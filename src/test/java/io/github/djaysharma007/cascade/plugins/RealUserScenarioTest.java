package io.github.djaysharma007.cascade.plugins;

import io.github.djaysharma007.cascade.app.CompositionRoot;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunOutcome;
import io.github.djaysharma007.cascade.engine.RunRecord;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RealUserScenarioTest {

    @Test
    void testRealUserScenarioWithActualFolders() throws Exception {
        Path sourceDir = Path.of("D:", "Second_Year", "testcascade");
        Path destDir = Path.of("D:", "Second_Year", "testcascade2");

        if (!Files.exists(sourceDir) || !Files.exists(destDir)) {
            return;
        }

        CompositionRoot compRoot = new CompositionRoot();
        EngineFacade facade = compRoot.facade();

        Workflow firstTest = facade.allWorkflows().stream()
                .filter(w -> w.name().equals("first test"))
                .findFirst()
                .orElse(null);

        if (firstTest == null) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        facade.addListener(new io.github.djaysharma007.cascade.engine.EngineListener() {
            @Override
            public void onEngineStateChanged(boolean running) {}

            @Override
            public void onWorkflowStateChanged(io.github.djaysharma007.cascade.core.WorkflowId workflowId, io.github.djaysharma007.cascade.core.WorkflowState state) {}

            @Override
            public void onRunUpdated(RunRecord record) {
                if (record.workflowName().equals("first test") &&
                        (record.outcome() == RunOutcome.SUCCEEDED || record.outcome() == RunOutcome.FAILED)) {
                    latch.countDown();
                }
            }
        });

        facade.start();
        Thread.sleep(500);

        Path testPdf = sourceDir.resolve("real-test-document.pdf");
        // Simulate pasting file into the folder
        try (FileOutputStream fos = new FileOutputStream(testPdf.toFile())) {
            fos.write("%PDF-1.4 Real Test Content for Cascade Automation Engine".getBytes());
            fos.flush();
        }

        boolean done = latch.await(10, TimeUnit.SECONDS);
        facade.stop();

        assertThat(done).as("Run should have completed within 10 seconds").isTrue();

        RunRecord lastRun = facade.getRuns().stream()
                .filter(r -> r.workflowName().equals("first test"))
                .findFirst()
                .orElseThrow();

        assertThat(lastRun.outcome()).isEqualTo(RunOutcome.SUCCEEDED);
        assertThat(Files.exists(testPdf)).isFalse();
        assertThat(Files.exists(destDir.resolve("real-test-document.pdf"))).isTrue();

        // Clean up test file from destDir
        Files.deleteIfExists(destDir.resolve("real-test-document.pdf"));
    }
}

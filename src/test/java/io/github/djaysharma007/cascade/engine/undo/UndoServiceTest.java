package io.github.djaysharma007.cascade.engine.undo;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.PluginId;
import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.engine.RunOutcome;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.engine.StepRecord;
import io.github.djaysharma007.cascade.plugins.actions.CopyFileAction;
import io.github.djaysharma007.cascade.plugins.actions.MoveFileAction;
import io.github.djaysharma007.cascade.plugins.actions.RenameFileAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UndoServiceTest {

    private UndoService undoService;

    @BeforeEach
    void setUp() {
        undoService = new UndoService();
    }

    @Test
    void testUndoMoveFile(@TempDir Path tempDir) throws IOException {
        Path srcFile = tempDir.resolve("original.txt");
        Files.writeString(srcFile, "Original content");

        Path destDir = tempDir.resolve("moved_here");
        MoveFileAction action = new MoveFileAction(destDir.toString(), "rename");
        ExecutionContext ctx = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
        Event event = Event.of(Map.of("filePath", srcFile.toAbsolutePath().toString()));

        ExecutionResult result = action.execute(event, ctx);
        assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        assertThat(srcFile).doesNotExist();
        Path movedFile = destDir.resolve("original.txt");
        assertThat(movedFile).exists();

        StepRecord step = new StepRecord(0, PluginId.of("move-file"), "Move file", result, 10, 1);
        RunRecord run = new RunRecord(
                RunId.random(),
                WorkflowId.random(),
                "Test Move Flow",
                Instant.now(),
                Instant.now(),
                RunOutcome.SUCCEEDED,
                "Manual",
                List.of(step)
        );

        assertThat(undoService.canUndo(run)).isTrue();
        undoService.undoRun(run);

        assertThat(undoService.isUndone(run.id())).isTrue();
        assertThat(undoService.canUndo(run)).isFalse();
        assertThat(srcFile).exists();
        assertThat(movedFile).doesNotExist();

        // Cannot undo again
        assertThatThrownBy(() -> undoService.undoRun(run))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testUndoCopyFile(@TempDir Path tempDir) throws IOException {
        Path srcFile = tempDir.resolve("photo.jpg");
        Files.writeString(srcFile, "Photo binary bytes");

        Path destDir = tempDir.resolve("backup");
        CopyFileAction action = new CopyFileAction(destDir.toString(), "rename");
        ExecutionContext ctx = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
        Event event = Event.of(Map.of("filePath", srcFile.toAbsolutePath().toString()));

        ExecutionResult result = action.execute(event, ctx);
        assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        Path copiedFile = destDir.resolve("photo.jpg");
        assertThat(copiedFile).exists();
        assertThat(srcFile).exists();

        StepRecord step = new StepRecord(0, PluginId.of("copy-file"), "Copy file", result, 10, 1);
        RunRecord run = new RunRecord(
                RunId.random(),
                WorkflowId.random(),
                "Test Copy Flow",
                Instant.now(),
                Instant.now(),
                RunOutcome.SUCCEEDED,
                "Manual",
                List.of(step)
        );

        assertThat(undoService.canUndo(run)).isTrue();
        undoService.undoRun(run);

        assertThat(undoService.isUndone(run.id())).isTrue();
        assertThat(copiedFile).doesNotExist();
        assertThat(srcFile).exists();
    }

    @Test
    void testUndoRenameFile(@TempDir Path tempDir) throws IOException {
        Path srcFile = tempDir.resolve("report.docx");
        Files.writeString(srcFile, "Report content");

        RenameFileAction action = new RenameFileAction("{name}_archived.{ext}", "rename");
        ExecutionContext ctx = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
        Event event = Event.of(Map.of("filePath", srcFile.toAbsolutePath().toString()));

        ExecutionResult result = action.execute(event, ctx);
        assertThat(result).isInstanceOf(ExecutionResult.Success.class);
        Path renamedFile = tempDir.resolve("report_archived.docx");
        assertThat(renamedFile).exists();
        assertThat(srcFile).doesNotExist();

        StepRecord step = new StepRecord(0, PluginId.of("rename-file"), "Rename file", result, 10, 1);
        RunRecord run = new RunRecord(
                RunId.random(),
                WorkflowId.random(),
                "Test Rename Flow",
                Instant.now(),
                Instant.now(),
                RunOutcome.SUCCEEDED,
                "Manual",
                List.of(step)
        );

        assertThat(undoService.canUndo(run)).isTrue();
        undoService.undoRun(run);

        assertThat(undoService.isUndone(run.id())).isTrue();
        assertThat(srcFile).exists();
        assertThat(renamedFile).doesNotExist();
    }

    @Test
    void testFailedRunCannotBeUndone() {
        StepRecord step = new StepRecord(
                0,
                PluginId.of("move-file"),
                "Move file",
                new ExecutionResult.Failed(new io.github.djaysharma007.cascade.core.Failure.Expected("File missing")),
                5,
                1
        );
        RunRecord run = new RunRecord(
                RunId.random(),
                WorkflowId.random(),
                "Failed Flow",
                Instant.now(),
                Instant.now(),
                RunOutcome.FAILED,
                "Manual",
                List.of(step)
        );

        assertThat(undoService.canUndo(run)).isFalse();
    }
}

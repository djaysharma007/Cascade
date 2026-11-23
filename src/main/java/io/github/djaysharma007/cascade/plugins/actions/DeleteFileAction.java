package io.github.djaysharma007.cascade.plugins.actions;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.Failure;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Safely removes a file by moving it to the system Recycle Bin/Trash only (never permanent deletion).
 */
@Plugin(id = "delete-file", displayName = "Move to Recycle Bin", category = PluginCategory.ACTION)
public final class DeleteFileAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(DeleteFileAction.class);

    private final boolean moveToTrash;

    public DeleteFileAction(boolean moveToTrash) {
        this.moveToTrash = moveToTrash;
    }

    @Override
    public ExecutionResult execute(Event event, ExecutionContext context) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Object pathObj = event.payload().get("filePath");
        if (pathObj == null) {
            return new ExecutionResult.Failed(new Failure.Defect(null, "Missing required payload key: filePath"));
        }

        Path sourcePath = Path.of(pathObj.toString());
        if (!Files.exists(sourcePath)) {
            return new ExecutionResult.Failed(new Failure.Expected("File to delete does not exist: " + sourcePath));
        }

        boolean success = false;

        // Route 1: Desktop API moveToTrash
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                try {
                    success = desktop.moveToTrash(sourcePath.toFile());
                } catch (Exception e) {
                    log.debug("Desktop moveToTrash threw exception, attempting fallback: {}", e.getMessage());
                }
            }
        }

        // Route 2: Windows PowerShell Microsoft.VisualBasic RecycleBin
        if (!success && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                String cmd = String.format(
                        "Add-Type -AssemblyName Microsoft.VisualBasic; [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile('%s', 'OnlyErrorDialogs', 'SendToRecycleBin')",
                        sourcePath.toAbsolutePath().toString().replace("'", "''")
                );
                Process proc = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", cmd)
                        .redirectErrorStream(true)
                        .start();
                boolean finished = proc.waitFor(10, TimeUnit.SECONDS);
                if (finished && proc.exitValue() == 0 && !Files.exists(sourcePath)) {
                    success = true;
                }
            } catch (Exception e) {
                log.debug("PowerShell SendToRecycleBin failed: {}", e.getMessage());
            }
        }

        if (!success && Files.exists(sourcePath)) {
            return new ExecutionResult.Failed(new Failure.Transient("Could not move file to Recycle Bin: " + sourcePath));
        }

        log.info("Moved file to Recycle Bin: {}", sourcePath);
        return new ExecutionResult.Success(Map.of(
                "deletedPath", sourcePath.toAbsolutePath().toString(),
                "movedToTrash", true
        ));
    }

    public boolean isMoveToTrash() {
        return moveToTrash;
    }
}

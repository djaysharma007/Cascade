package io.github.djaysharma007.cascade.engine.undo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Reverses a Rename file action by restoring the file to its original name.
 */
public record UndoRenameCommand(Path sourcePath, Path renamedTo) implements UndoCommand {

    public UndoRenameCommand {
        Objects.requireNonNull(sourcePath, "sourcePath must not be null");
        Objects.requireNonNull(renamedTo, "renamedTo must not be null");
    }

    @Override
    public void execute() throws IOException {
        if (!Files.exists(renamedTo)) {
            throw new IOException("Cannot undo rename: renamed file does not exist: " + renamedTo);
        }
        Files.move(renamedTo, sourcePath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public String description() {
        return "Rename " + renamedTo.getFileName() + " back to " + sourcePath.getFileName();
    }
}

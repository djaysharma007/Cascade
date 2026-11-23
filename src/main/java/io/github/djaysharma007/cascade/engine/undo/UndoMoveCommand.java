package io.github.djaysharma007.cascade.engine.undo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Reverses a Move file action by moving the target file back to its original location.
 */
public record UndoMoveCommand(Path sourcePath, Path movedTo) implements UndoCommand {

    public UndoMoveCommand {
        Objects.requireNonNull(sourcePath, "sourcePath must not be null");
        Objects.requireNonNull(movedTo, "movedTo must not be null");
    }

    @Override
    public void execute() throws IOException {
        if (!Files.exists(movedTo)) {
            throw new IOException("Cannot undo move: destination file does not exist: " + movedTo);
        }
        if (sourcePath.getParent() != null) {
            Files.createDirectories(sourcePath.getParent());
        }
        Files.move(movedTo, sourcePath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public String description() {
        return "Move " + movedTo.getFileName() + " back to " + sourcePath;
    }
}

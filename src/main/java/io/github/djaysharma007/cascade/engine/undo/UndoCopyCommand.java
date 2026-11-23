package io.github.djaysharma007.cascade.engine.undo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Reverses a Copy file action by safely deleting the created copy.
 */
public record UndoCopyCommand(Path copiedTo) implements UndoCommand {

    public UndoCopyCommand {
        Objects.requireNonNull(copiedTo, "copiedTo must not be null");
    }

    @Override
    public void execute() throws IOException {
        if (Files.exists(copiedTo)) {
            Files.delete(copiedTo);
        }
    }

    @Override
    public String description() {
        return "Delete copied file " + copiedTo;
    }
}

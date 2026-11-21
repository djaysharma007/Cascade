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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

/**
 * Moves a file from event payload to a destination directory with conflict resolution.
 */
@Plugin(id = "move-file", displayName = "Move file", category = PluginCategory.ACTION)
public final class MoveFileAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(MoveFileAction.class);

    private final Path destinationFolder;
    private final String onConflict;

    public MoveFileAction(String destinationFolder, String onConflict) {
        Objects.requireNonNull(destinationFolder, "destinationFolder must not be null");
        this.destinationFolder = Path.of(destinationFolder);
        this.onConflict = onConflict != null ? onConflict.toLowerCase() : "rename";
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
            return new ExecutionResult.Failed(new Failure.Expected("Source file does not exist: " + sourcePath));
        }

        try {
            Files.createDirectories(destinationFolder);
            Path targetPath = destinationFolder.resolve(sourcePath.getFileName());

            if (Files.exists(targetPath)) {
                switch (onConflict) {
                    case "skip" -> {
                        return new ExecutionResult.Failed(
                                new Failure.Expected("Destination file already exists and conflict policy is skip: " + targetPath)
                        );
                    }
                    case "overwrite" -> Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    case "rename" -> {
                        targetPath = resolveUniqueName(destinationFolder, sourcePath.getFileName().toString());
                        Files.move(sourcePath, targetPath);
                    }
                    default -> throw new IllegalArgumentException("Unknown onConflict policy: " + onConflict);
                }
            } else {
                Files.move(sourcePath, targetPath);
            }

            log.info("Moved file {} -> {}", sourcePath, targetPath);
            return new ExecutionResult.Success(Map.of("movedTo", targetPath.toAbsolutePath().toString()));
        } catch (IOException e) {
            log.warn("IO error moving file {}", sourcePath, e);
            return new ExecutionResult.Failed(new Failure.Transient("Failed to move file: " + e.getMessage(), e));
        }
    }

    private Path resolveUniqueName(Path dir, String fileName) {
        String baseName = fileName;
        String extension = "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            baseName = fileName.substring(0, dotIdx);
            extension = fileName.substring(dotIdx);
        }

        int count = 1;
        Path candidate;
        do {
            candidate = dir.resolve(baseName + " (" + count + ")" + extension);
            count++;
        } while (Files.exists(candidate));

        return candidate;
    }
}

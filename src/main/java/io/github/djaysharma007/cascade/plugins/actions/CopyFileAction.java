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
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

/**
 * Copies a file from event payload to a destination directory with conflict resolution.
 */
@Plugin(id = "copy-file", displayName = "Copy file", category = PluginCategory.ACTION)
public final class CopyFileAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(CopyFileAction.class);

    private final Path destinationFolder;
    private final String onConflict;

    public CopyFileAction(String destinationFolder, String onConflict) {
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

            boolean isSame;
            try {
                isSame = sourcePath.toAbsolutePath().normalize().equals(targetPath.toAbsolutePath().normalize())
                        || (Files.exists(targetPath) && Files.isSameFile(sourcePath, targetPath));
            } catch (IOException e) {
                isSame = sourcePath.toAbsolutePath().normalize().equals(targetPath.toAbsolutePath().normalize());
            }

            if (isSame) {
                return new ExecutionResult.Failed(new Failure.Expected(
                        "Source and destination are the same file: " + sourcePath.toAbsolutePath()
                ));
            }

            if (Files.exists(targetPath)) {
                switch (onConflict) {
                    case "skip" -> {
                        return new ExecutionResult.Failed(
                                new Failure.Expected("Destination file already exists and conflict policy is skip: " + targetPath)
                        );
                    }
                    case "overwrite" -> Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    case "rename" -> {
                        targetPath = resolveUniqueName(destinationFolder, sourcePath.getFileName().toString());
                        Files.copy(sourcePath, targetPath);
                    }
                    default -> {
                        return new ExecutionResult.Failed(
                                new Failure.Defect(null, "Unknown onConflict policy: " + onConflict)
                        );
                    }
                }
            } else {
                Files.copy(sourcePath, targetPath);
            }

            log.info("Copied file {} -> {}", sourcePath, targetPath);
            return new ExecutionResult.Success(Map.of(
                    "copiedTo", targetPath.toAbsolutePath().toString(),
                    "sourcePath", sourcePath.toAbsolutePath().toString()
            ));
        } catch (FileSystemException e) {
            log.warn("File system lock copying file {} -> {}: {}", sourcePath, destinationFolder, e.getMessage());
            return new ExecutionResult.Failed(new Failure.Transient("File is locked or in use: " + e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to copy file {} -> {}", sourcePath, destinationFolder, e);
            return new ExecutionResult.Failed(new Failure.Defect(e, "I/O error copying file: " + e.getMessage()));
        }
    }

    private Path resolveUniqueName(Path folder, String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        String base = dotIndex > 0 ? originalName.substring(0, dotIndex) : originalName;
        String ext = dotIndex > 0 ? originalName.substring(dotIndex) : "";

        int counter = 1;
        Path candidate;
        do {
            candidate = folder.resolve(base + " (" + counter + ")" + ext);
            counter++;
        } while (Files.exists(candidate));

        return candidate;
    }

    public Path getDestinationFolder() {
        return destinationFolder;
    }

    public String getOnConflict() {
        return onConflict;
    }
}

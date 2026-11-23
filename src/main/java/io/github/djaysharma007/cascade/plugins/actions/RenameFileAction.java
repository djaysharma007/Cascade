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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

/**
 * Renames a file using a pattern with placeholders ({name}, {fileName}, {ext}, {date}).
 */
@Plugin(id = "rename-file", displayName = "Rename file", category = PluginCategory.ACTION)
public final class RenameFileAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(RenameFileAction.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String pattern;
    private final String onConflict;

    public RenameFileAction(String pattern, String onConflict) {
        Objects.requireNonNull(pattern, "pattern must not be null");
        this.pattern = pattern.trim();
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

        Path parentDir = sourcePath.getParent();
        if (parentDir == null) {
            return new ExecutionResult.Failed(new Failure.Defect(null, "Cannot rename root path: " + sourcePath));
        }

        String originalName = sourcePath.getFileName().toString();
        int dotIdx = originalName.lastIndexOf('.');
        String baseName = dotIdx > 0 ? originalName.substring(0, dotIdx) : originalName;
        String ext = dotIdx > 0 ? originalName.substring(dotIdx + 1) : "";

        String newName = pattern
                .replace("{fileName}", originalName)
                .replace("{name}", baseName)
                .replace("{baseName}", baseName)
                .replace("{ext}", ext)
                .replace("{extension}", ext)
                .replace("{date}", DATE_FMT.format(LocalDate.now()));

        // If the pattern didn't supply an extension and the original had one, preserve it
        if (!ext.isEmpty() && !newName.contains(".")) {
            newName = newName + "." + ext;
        }

        if (originalName.equals(newName)) {
            return new ExecutionResult.Failed(new Failure.Expected(
                    "Source and destination names are identical: " + originalName
            ));
        }

        Path targetPath = parentDir.resolve(newName);

        try {
            if (Files.exists(targetPath)) {
                switch (onConflict) {
                    case "skip" -> {
                        return new ExecutionResult.Failed(
                                new Failure.Expected("Target name already exists and conflict policy is skip: " + targetPath)
                        );
                    }
                    case "overwrite" -> Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    case "rename" -> {
                        targetPath = resolveUniqueName(parentDir, newName);
                        Files.move(sourcePath, targetPath);
                    }
                    default -> {
                        return new ExecutionResult.Failed(
                                new Failure.Defect(null, "Unknown onConflict policy: " + onConflict)
                        );
                    }
                }
            } else {
                Files.move(sourcePath, targetPath);
            }

            log.info("Renamed file {} -> {}", sourcePath, targetPath);
            return new ExecutionResult.Success(Map.of(
                    "renamedTo", targetPath.toAbsolutePath().toString(),
                    "sourcePath", sourcePath.toAbsolutePath().toString()
            ));
        } catch (FileSystemException e) {
            log.warn("File system lock renaming file {} -> {}: {}", sourcePath, targetPath, e.getMessage());
            return new ExecutionResult.Failed(new Failure.Transient("File is locked or in use: " + e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to rename file {} -> {}", sourcePath, targetPath, e);
            return new ExecutionResult.Failed(new Failure.Defect(e, "I/O error renaming file: " + e.getMessage()));
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

    public String getPattern() {
        return pattern;
    }

    public String getOnConflict() {
        return onConflict;
    }
}

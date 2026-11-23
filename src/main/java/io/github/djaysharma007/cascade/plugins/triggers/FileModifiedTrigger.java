package io.github.djaysharma007.cascade.plugins.triggers;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.core.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches a folder for file modification events.
 * Waits for files to finish writing and stabilize before firing, and suppresses duplicate events.
 */
@Plugin(id = "file-modified", displayName = "File modified", category = PluginCategory.TRIGGER)
public final class FileModifiedTrigger implements Trigger {

    private static final Logger log = LoggerFactory.getLogger(FileModifiedTrigger.class);
    private static final long DUPLICATE_WINDOW_MS = 2000L;
    private static final long STABILITY_THRESHOLD_MS = 400L;
    private static final long STABILITY_TIMEOUT_MS = 15_000L;
    private static final long POLL_INTERVAL_MS = 100L;

    private final Path folder;
    private final boolean recursive;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ConcurrentHashMap<Path, Long> recentlyProcessed = new ConcurrentHashMap<>();
    private WatchService watchService;
    private Thread watcherThread;
    private volatile TriggerListener listener;

    public FileModifiedTrigger(String folderPath, boolean recursive) {
        Objects.requireNonNull(folderPath, "folderPath must not be null");
        this.folder = Path.of(folderPath);
        this.recursive = recursive;
    }

    @Override
    public synchronized void start(TriggerListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("FileModifiedTrigger already started");
        }
        this.listener = listener;

        try {
            Files.createDirectories(folder);
            this.watchService = FileSystems.getDefault().newWatchService();
            folder.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            started.set(false);
            throw new RuntimeException("Failed to register watch service on: " + folder, e);
        }

        watcherThread = new Thread(this::pollEvents, "cascade-file-modified-watcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
        log.info("FileModifiedTrigger started on {}", folder);
    }

    @Override
    public synchronized void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        listener = null;
        recentlyProcessed.clear();
        log.info("FileModifiedTrigger stopped");
    }

    private void pollEvents() {
        while (started.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (Exception e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                if (!started.get()) {
                    break;
                }
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    Path relPath = (Path) event.context();
                    Path fullPath = folder.resolve(relPath);
                    handleModifiedPath(fullPath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private void handleModifiedPath(Path fullPath) {
        String name = fullPath.getFileName().toString();
        if (name.startsWith(".") || name.endsWith(".tmp") || name.endsWith(".crdownload")) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastSeen = recentlyProcessed.get(fullPath);
        if (lastSeen != null && (now - lastSeen) < DUPLICATE_WINDOW_MS) {
            return;
        }

        if (!waitForFileStability(fullPath)) {
            return;
        }

        recentlyProcessed.put(fullPath, System.currentTimeMillis());

        try {
            long size = Files.size(fullPath);
            FileTime modified = Files.getLastModifiedTime(fullPath);
            Map<String, Object> payload = new HashMap<>();
            payload.put("fileName", name);
            payload.put("filePath", fullPath.toAbsolutePath().toString());
            payload.put("fileSize", size);
            payload.put("modifiedAt", modified.toString());

            TriggerListener l = this.listener;
            if (l != null) {
                l.onEvent(Event.of(payload));
            }
        } catch (IOException e) {
            log.debug("Could not read file details after modification: {}", fullPath, e);
        }
    }

    private boolean waitForFileStability(Path path) {
        long startTime = System.currentTimeMillis();
        long lastSize = -1;
        long lastModTime = -1;
        long stableSince = -1;

        while (started.get()) {
            if (!Files.exists(path)) {
                return false;
            }

            try {
                long currentSize = Files.size(path);
                long currentModTime = Files.getLastModifiedTime(path).toMillis();

                if (currentSize == lastSize && currentModTime == lastModTime) {
                    if (stableSince == -1) {
                        stableSince = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - stableSince >= STABILITY_THRESHOLD_MS) {
                        try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
                            return true;
                        } catch (IOException ignored) {}
                    }
                } else {
                    lastSize = currentSize;
                    lastModTime = currentModTime;
                    stableSince = -1;
                }
            } catch (IOException ignored) {}

            if (System.currentTimeMillis() - startTime > STABILITY_TIMEOUT_MS) {
                return false;
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public Path getFolder() {
        return folder;
    }

    public boolean isRecursive() {
        return recursive;
    }
}

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
 * Watches a filesystem folder and fires events when files are created.
 * Ensures files are stable (unchanged size and modification time for 500 ms,
 * and readable) before firing, and suppresses temporary files and duplicates.
 */
@Plugin(id = "file-created", displayName = "File created", category = PluginCategory.TRIGGER)
public final class FileCreatedTrigger implements Trigger {

    private static final Logger log = LoggerFactory.getLogger(FileCreatedTrigger.class);
    private static final long DUPLICATE_WINDOW_MS = 2500L;
    private static final long STABILITY_THRESHOLD_MS = 500L;
    private static final long STABILITY_TIMEOUT_MS = 15_000L;
    private static final long POLL_INTERVAL_MS = 100L;

    private final Path folder;
    private final boolean recursive;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ConcurrentHashMap<Path, Long> recentlyProcessed = new ConcurrentHashMap<>();
    private WatchService watchService;
    private Thread watcherThread;
    private volatile TriggerListener listener;

    public FileCreatedTrigger(String folderPath, boolean recursive) {
        Objects.requireNonNull(folderPath, "folderPath must not be null");
        this.folder = Path.of(folderPath);
        this.recursive = recursive;
    }

    @Override
    public synchronized void start(TriggerListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("FileCreatedTrigger already started");
        }
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Watch path is not an existing directory: " + folder);
        }
        this.listener = listener;

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException e) {
            started.set(false);
            throw new IllegalStateException("Failed to initialize WatchService for " + folder, e);
        }

        this.watcherThread = new Thread(this::pollEvents, "cascade-file-watcher");
        this.watcherThread.setDaemon(true);
        this.watcherThread.start();
        log.debug("FileCreatedTrigger watching {}", folder);
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
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    if (filename == null || isTemporaryOrIgnored(filename)) {
                        continue;
                    }

                    Path fullPath = folder.resolve(filename);
                    Path canonicalPath = fullPath.toAbsolutePath().normalize();
                    long now = System.currentTimeMillis();

                    Long lastTime = recentlyProcessed.get(canonicalPath);
                    if (lastTime != null && (now - lastTime) < DUPLICATE_WINDOW_MS) {
                        log.debug("Suppressing duplicate file event for {}", canonicalPath);
                        continue;
                    }
                    recentlyProcessed.put(canonicalPath, now);
                    pruneRecentCache(now);

                    // Wait for file stability on a virtual thread to prevent blocking the watch loop
                    Thread.ofVirtual().name("file-stabilizer-" + filename).start(() -> {
                        processCreatedFile(filename, fullPath);
                    });
                }
            }
            if (!key.reset()) {
                break;
            }
        }
    }

    private void processCreatedFile(Path filename, Path fullPath) {
        if (!waitForStability(fullPath)) {
            log.warn("File did not stabilize within {} ms or was removed: {}", STABILITY_TIMEOUT_MS, fullPath);
            return;
        }

        if (!started.get() || !Files.exists(fullPath)) {
            return;
        }

        long size = 0;
        try {
            size = Files.size(fullPath);
        } catch (IOException ignored) {
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("fileName", filename.toString());
        payload.put("filePath", fullPath.toAbsolutePath().toString());
        payload.put("fileSize", size);

        TriggerListener current = this.listener;
        if (current != null) {
            try {
                log.info("FileCreatedTrigger firing event for stable file: {} ({} bytes)", fullPath, size);
                current.onEvent(Event.of(payload));
            } catch (Exception ex) {
                log.warn("Error in listener on FileCreatedTrigger", ex);
            }
        }
    }

    private boolean waitForStability(Path path) {
        long start = System.currentTimeMillis();
        long lastSize = -1;
        FileTime lastModified = null;
        long stableSince = -1;

        while (started.get() && (System.currentTimeMillis() - start) < STABILITY_TIMEOUT_MS) {
            if (!Files.exists(path)) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                continue;
            }

            try {
                long currentSize = Files.size(path);
                FileTime currentModified = Files.getLastModifiedTime(path);

                boolean readable = false;
                try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
                    readable = true;
                } catch (IOException e) {
                    // File is still locked or being written by another process
                    readable = false;
                }

                long now = System.currentTimeMillis();
                if (readable && currentSize == lastSize && Objects.equals(currentModified, lastModified)) {
                    if (stableSince == -1) {
                        stableSince = now;
                    } else if (now - stableSince >= STABILITY_THRESHOLD_MS) {
                        return true;
                    }
                } else {
                    lastSize = currentSize;
                    lastModified = currentModified;
                    stableSince = readable ? now : -1;
                }

                Thread.sleep(POLL_INTERVAL_MS);
            } catch (IOException e) {
                stableSince = -1;
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isTemporaryOrIgnored(Path filename) {
        String name = filename.toString().toLowerCase();
        if (name.startsWith(".") || name.startsWith("~")) {
            return true;
        }
        if (name.endsWith(".tmp") || name.endsWith(".crdownload") || name.endsWith(".part") || name.endsWith("~")) {
            return true;
        }
        return name.equals("thumbs.db") || name.equals("desktop.ini");
    }

    private void pruneRecentCache(long now) {
        recentlyProcessed.entrySet().removeIf(entry -> (now - entry.getValue()) > 30_000L);
    }

    @Override
    public synchronized void stop() {
        started.set(false);
        this.listener = null;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
            watchService = null;
        }
        if (watcherThread != null) {
            watcherThread.interrupt();
            watcherThread = null;
        }
    }
}

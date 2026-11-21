package io.github.djaysharma007.cascade.plugins.triggers;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.core.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches a filesystem folder and fires events when files are created.
 */
@Plugin(id = "file-created", displayName = "File created", category = PluginCategory.TRIGGER)
public final class FileCreatedTrigger implements Trigger {

    private static final Logger log = LoggerFactory.getLogger(FileCreatedTrigger.class);

    private final Path folder;
    private final boolean recursive;
    private final AtomicBoolean started = new AtomicBoolean(false);
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
                    Path fullPath = folder.resolve(filename);

                    long size = 0;
                    try {
                        size = Files.exists(fullPath) ? Files.size(fullPath) : 0;
                    } catch (IOException ignored) {
                    }

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("fileName", filename.toString());
                    payload.put("filePath", fullPath.toAbsolutePath().toString());
                    payload.put("fileSize", size);

                    TriggerListener current = this.listener;
                    if (current != null) {
                        try {
                            current.onEvent(Event.of(payload));
                        } catch (Exception ex) {
                            log.warn("Error in listener on FileCreatedTrigger", ex);
                        }
                    }
                }
            }
            if (!key.reset()) {
                break;
            }
        }
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

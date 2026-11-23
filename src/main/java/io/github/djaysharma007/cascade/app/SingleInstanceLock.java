package io.github.djaysharma007.cascade.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enforces single-instance execution via a loopback port.
 * When a second instance launches, it notifies the primary instance to bring its window
 * to the front and exits cleanly.
 */
public final class SingleInstanceLock {

    private static final Logger log = LoggerFactory.getLogger(SingleInstanceLock.class);
    private static final int PORT = 49285;
    private static final String SIGNAL_SHOW = "SHOW";

    private final Runnable onShowRequest;
    private final AtomicBoolean acquired = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread listenerThread;

    public SingleInstanceLock(Runnable onShowRequest) {
        this.onShowRequest = Objects.requireNonNull(onShowRequest, "onShowRequest must not be null");
    }

    /**
     * Attempts to acquire the single-instance lock.
     * If another instance is running, sends the show signal to it and returns false.
     * If this is the primary instance, begins listening for second-instance signals and returns true.
     */
    public boolean tryAcquire() {
        try {
            serverSocket = new ServerSocket(PORT, 10, InetAddress.getByName("127.0.0.1"));
            acquired.set(true);

            listenerThread = new Thread(this::listenForSignals, "cascade-single-instance-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            log.info("Single instance lock acquired on port {}", PORT);
            return true;
        } catch (Exception bindEx) {
            log.info("Another instance is already running on port {}. Sending activation signal.", PORT);
            notifyRunningInstance();
            return false;
        }
    }

    private void notifyRunningInstance() {
        try (Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), PORT);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(SIGNAL_SHOW + "\n");
            writer.flush();
            log.info("Activation signal successfully delivered to running Cascade instance.");
        } catch (Exception e) {
            log.warn("Could not deliver activation signal to running instance: {}", e.getMessage());
        }
    }

    private void listenForSignals() {
        while (acquired.get() && serverSocket != null && !serverSocket.isClosed()) {
            try (Socket socket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (SIGNAL_SHOW.equalsIgnoreCase(line)) {
                    log.info("Received show activation signal from secondary instance.");
                    onShowRequest.run();
                }
            } catch (Exception e) {
                if (!acquired.get()) {
                    break;
                }
            }
        }
    }

    public synchronized void release() {
        if (acquired.compareAndSet(true, false)) {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception ignored) {}
                serverSocket = null;
            }
            if (listenerThread != null) {
                listenerThread.interrupt();
                listenerThread = null;
            }
            log.info("Single instance lock released.");
        }
    }
}

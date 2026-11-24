package io.github.djaysharma007.cascade.app;

import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowState;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.EngineListener;
import io.github.djaysharma007.cascade.engine.RunRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the Windows system tray icon, context menu, and background execution status.
 */
public final class SystemTrayManager implements EngineListener {

    private static final Logger log = LoggerFactory.getLogger(SystemTrayManager.class);

    private final EngineFacade facade;
    private final Runnable onOpenWindow;
    private final Runnable onQuit;
    private final AtomicBoolean firstTimeNotified = new AtomicBoolean(false);

    private TrayIcon trayIcon;
    private MenuItem engineToggleItem;

    public SystemTrayManager(EngineFacade facade, Runnable onOpenWindow, Runnable onQuit) {
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.onOpenWindow = Objects.requireNonNull(onOpenWindow, "onOpenWindow must not be null");
        this.onQuit = Objects.requireNonNull(onQuit, "onQuit must not be null");

        facade.addListener(this);
    }

    public boolean initTray() {
        if (!SystemTray.isSupported()) {
            log.info("SystemTray is not supported on this platform.");
            return false;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image iconImage = createTrayIconImage();

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Open Cascade");
            openItem.addActionListener(e -> onOpenWindow.run());

            engineToggleItem = new MenuItem(facade.isRunning() ? "Stop engine" : "Start engine");
            engineToggleItem.addActionListener(e -> {
                if (facade.isRunning()) {
                    facade.stop();
                } else {
                    facade.start();
                }
            });

            MenuItem quitItem = new MenuItem("Quit");
            quitItem.addActionListener(e -> onQuit.run());

            popup.add(openItem);
            popup.addSeparator();
            popup.add(engineToggleItem);
            popup.addSeparator();
            popup.add(quitItem);

            trayIcon = new TrayIcon(iconImage, "Cascade Automation", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 1) {
                        onOpenWindow.run();
                    }
                }
            });

            tray.add(trayIcon);
            log.info("Cascade system tray initialized.");
            return true;
        } catch (Exception e) {
            log.warn("Could not initialize system tray: {}", e.getMessage());
            return false;
        }
    }

    public void showFirstTimeNotification() {
        if (trayIcon != null && firstTimeNotified.compareAndSet(false, true)) {
            trayIcon.displayMessage(
                    "Cascade",
                    "Cascade is still running in the background. Use the tray icon to open or quit.",
                    TrayIcon.MessageType.INFO
            );
        }
    }

    public void removeTray() {
        if (trayIcon != null && SystemTray.isSupported()) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIcon = null;
                log.info("Cascade system tray icon removed.");
            } catch (Exception ignored) {}
        }
    }

    private Image createTrayIconImage() {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background rounded box
        g.setColor(new Color(0x10, 0x27, 0x30)); // ink #102730
        g.fillRoundRect(0, 0, size, size, 4, 4);

        // Cascade mark: 3 descending vertical bars in warm copper (#B77457)
        g.setColor(new Color(0xB7, 0x74, 0x57)); // copper #B77457
        g.fillRoundRect(2, 2, 3, 12, 2, 2);
        g.fillRoundRect(6, 4, 3, 10, 2, 2);
        g.fillRoundRect(10, 6, 3, 8, 2, 2);

        g.dispose();
        return img;
    }

    @Override
    public void onEngineStateChanged(boolean running) {
        if (engineToggleItem != null) {
            engineToggleItem.setLabel(running ? "Stop engine" : "Start engine");
        }
    }

    @Override
    public void onWorkflowStateChanged(WorkflowId workflowId, WorkflowState state) {
        // No-op for tray
    }

    @Override
    public void onRunUpdated(RunRecord record) {
        // No-op for tray
    }
}

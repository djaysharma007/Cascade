package io.github.djaysharma007.cascade.app;

import io.github.djaysharma007.cascade.ui.views.MainShellView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Main JavaFX Application class for Cascade desktop automation app.
 */
public class CascadeApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(CascadeApplication.class);

    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 800;
    private static final int MIN_WIDTH = 1024;
    private static final int MIN_HEIGHT = 680;

    private CompositionRoot compositionRoot;
    private MainShellView mainShell;
    private SingleInstanceLock singleInstanceLock;
    private SystemTrayManager trayManager;
    private Stage primaryStage;
    private boolean backgroundMode = false;

    @Override
    public void init() {
        log.info("Initializing Cascade application composition root");
        this.compositionRoot = new CompositionRoot();

        Parameters params = getParameters();
        if (params != null && (params.getRaw().contains("--background") || params.getUnnamed().contains("--background"))) {
            this.backgroundMode = true;
        }

        if (!Boolean.getBoolean("cascade.disable.lock")) {
            this.singleInstanceLock = new SingleInstanceLock(() -> {
                Platform.runLater(() -> {
                    if (primaryStage != null) {
                        primaryStage.show();
                        primaryStage.toFront();
                    }
                });
            });

            if (!singleInstanceLock.tryAcquire()) {
                log.info("Secondary instance exiting as running instance was activated.");
                Platform.exit();
                System.exit(0);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(false);

        log.info("Starting Cascade application window (backgroundMode={})", backgroundMode);
        loadBundledFonts();

        this.mainShell = new MainShellView(
                compositionRoot.facade(),
                compositionRoot.repository(),
                compositionRoot.registry(),
                compositionRoot.settingsService()
        );

        Scene scene = new Scene((javafx.scene.Parent) mainShell.getNode(), INITIAL_WIDTH, INITIAL_HEIGHT);
        String cssUrl = getClass().getResource("/theme/cascade.css") != null
                ? getClass().getResource("/theme/cascade.css").toExternalForm()
                : null;
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl);
        }

        primaryStage.setTitle("Cascade");
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setScene(scene);

        if (!Boolean.getBoolean("cascade.disable.tray")) {
            this.trayManager = new SystemTrayManager(
                    compositionRoot.facade(),
                    () -> Platform.runLater(() -> {
                        primaryStage.show();
                        primaryStage.toFront();
                    }),
                    this::shutdownAndQuit
            );
            trayManager.initTray();
        }

        primaryStage.setOnCloseRequest(e -> {
            boolean keepRunning = compositionRoot.settingsService().getSettings().keepRunningInTray();
            if (keepRunning && trayManager != null) {
                e.consume();
                primaryStage.hide();
                trayManager.showFirstTimeNotification();
            } else {
                shutdownAndQuit();
            }
        });

        if (backgroundMode) {
            compositionRoot.facade().start();
            log.info("Cascade started in background tray mode");
        } else {
            primaryStage.show();
            log.info("Cascade application window displayed");
        }

        boolean isTour = Boolean.getBoolean("cascade.ui.tour");
        Parameters params = getParameters();
        if (params != null && (params.getRaw().contains("--ui-tour") || params.getUnnamed().contains("--ui-tour"))) {
            isTour = true;
        }
        if (isTour) {
            runUiTourAndExit();
        }
    }

    private void runUiTourAndExit() {
        Thread tourThread = new Thread(() -> {
            try {
                log.info("[UI Tour] Starting automated UI screen tour");
                Thread.sleep(600);

                log.info("[UI Tour] Screen 1/7: Overview");
                Platform.runLater(() -> mainShell.showDashboardView());
                Thread.sleep(500);

                log.info("[UI Tour] Screen 2/7: Workflows");
                Platform.runLater(() -> mainShell.showWorkflowsView());
                Thread.sleep(500);

                log.info("[UI Tour] Screen 3/7: Templates");
                Platform.runLater(() -> mainShell.showTemplateGalleryView());
                Thread.sleep(500);

                log.info("[UI Tour] Screen 4/7: Activity");
                Platform.runLater(() -> mainShell.showActivityView());
                Thread.sleep(500);

                log.info("[UI Tour] Screen 5/7: Settings");
                Platform.runLater(() -> mainShell.showSettingsView());
                Thread.sleep(500);

                log.info("[UI Tour] Screen 6/7: Workflow Editor");
                Platform.runLater(() -> mainShell.openNewWorkflowEditor());
                Thread.sleep(500);

                log.info("[UI Tour] Screen 7/7: Command Palette (Ctrl+K)");
                Platform.runLater(() -> mainShell.toggleCommandPalette());
                Thread.sleep(400);
                Platform.runLater(() -> mainShell.toggleCommandPalette());
                Thread.sleep(400);

                log.info("[UI Tour] UI screen tour completed successfully with 0 errors");
                Platform.runLater(() -> {
                    shutdownAndQuit();
                    System.exit(0);
                });
            } catch (Exception e) {
                log.error("[UI Tour] Failed during UI tour: ", e);
                System.exit(1);
            }
        }, "ui-tour-runner");
        tourThread.setDaemon(true);
        tourThread.start();
    }

    public void shutdownAndQuit() {
        log.info("Shutting down Cascade application cleanly");
        if (trayManager != null) {
            trayManager.removeTray();
        }
        if (singleInstanceLock != null) {
            singleInstanceLock.release();
        }
        if (compositionRoot != null && compositionRoot.facade().isRunning()) {
            compositionRoot.facade().stop();
        }
        Platform.exit();
    }

    @Override
    public void stop() {
        shutdownAndQuit();
    }

    public MainShellView getMainShell() {
        return mainShell;
    }

    public CompositionRoot getCompositionRoot() {
        return compositionRoot;
    }

    public SystemTrayManager getTrayManager() {
        return trayManager;
    }

    public boolean isBackgroundMode() {
        return backgroundMode;
    }

    private void loadBundledFonts() {
        String[] fontPaths = {
                "/fonts/IBMPlexSans-Regular.ttf",
                "/fonts/IBMPlexSans-Medium.ttf",
                "/fonts/IBMPlexSans-SemiBold.ttf",
                "/fonts/IBMPlexSans-Bold.ttf",
                "/fonts/IBMPlexSerif-Regular.ttf",
                "/fonts/IBMPlexSerif-SemiBold.ttf",
                "/fonts/IBMPlexSerif-Bold.ttf",
                "/fonts/IBMPlexMono-Regular.ttf",
                "/fonts/IBMPlexMono-Medium.ttf",
                "/fonts/IBMPlexMono-SemiBold.ttf"
        };
        for (String path : fontPaths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    Font font = Font.loadFont(is, 14);
                    if (font != null) {
                        log.info("Loaded font: {} -> family='{}', name='{}'", path, font.getFamily(), font.getName());
                    } else {
                        log.warn("Failed to load font from {}", path);
                    }
                } else {
                    log.warn("Font resource not found: {}", path);
                }
            } catch (Exception e) {
                log.warn("Could not load font from {}", path, e);
            }
        }
    }
}

package io.github.djaysharma007.cascade.app;

import io.github.djaysharma007.cascade.ui.views.MainShellView;
import javafx.application.Application;
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

    @Override
    public void init() {
        log.info("Initializing Cascade application composition root");
        this.compositionRoot = new CompositionRoot();
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Cascade application window");
        loadBundledFonts();

        this.mainShell = new MainShellView(
                compositionRoot.facade(),
                compositionRoot.repository(),
                compositionRoot.registry()
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
        primaryStage.show();
        log.info("Cascade application window displayed");
    }

    @Override
    public void stop() {
        log.info("Stopping Cascade application");
        if (compositionRoot != null && compositionRoot.facade().isRunning()) {
            compositionRoot.facade().stop();
        }
    }

    public MainShellView getMainShell() {
        return mainShell;
    }

    public CompositionRoot getCompositionRoot() {
        return compositionRoot;
    }

    private void loadBundledFonts() {
        String[] fontPaths = {
                "/fonts/Inter-Regular.ttf",
                "/fonts/Inter-Medium.ttf",
                "/fonts/Inter-SemiBold.ttf",
                "/fonts/JetBrainsMono-Regular.ttf"
        };
        for (String path : fontPaths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    Font.loadFont(is, 14);
                    log.debug("Loaded font: {}", path);
                }
            } catch (Exception e) {
                log.warn("Could not load font from {}", path, e);
            }
        }
    }
}

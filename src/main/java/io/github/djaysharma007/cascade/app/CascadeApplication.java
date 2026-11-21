package io.github.djaysharma007.cascade.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Main JavaFX Application class for Cascade.
 */
public class CascadeApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(CascadeApplication.class);

    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 800;
    private static final int MIN_WIDTH = 1024;
    private static final int MIN_HEIGHT = 680;

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Cascade application");
        loadBundledFonts();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top bar
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Center content - empty state for initial window
        VBox centerArea = createCenterContent();
        root.setCenter(centerArea);

        // Status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
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

    private HBox createTopBar() {
        HBox topBar = new HBox(12);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // App Mark: 3 descending rounded bars in accent-bright
        HBox appMark = new HBox(3);
        appMark.setAlignment(Pos.CENTER);
        for (int h : new int[]{18, 14, 10}) {
            Rectangle bar = new Rectangle(3, h);
            bar.setArcWidth(2);
            bar.setArcHeight(2);
            bar.setFill(Color.web("#64E9EE"));
            appMark.getChildren().add(bar);
        }

        Label brandLabel = new Label("Cascade");
        brandLabel.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label engineStatusChip = new Label("Stopped");
        engineStatusChip.getStyleClass().addAll("chip", "chip-stopped");

        topBar.getChildren().addAll(appMark, brandLabel, spacer, engineStatusChip);
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.getStyleClass().add("sidebar");

        Label navWorkflows = new Label("Workflows");
        navWorkflows.getStyleClass().add("section-title");

        Label navActivity = new Label("Activity");
        navActivity.getStyleClass().add("secondary-text");

        sidebar.getChildren().addAll(navWorkflows, navActivity);
        return sidebar;
    }

    private VBox createCenterContent() {
        VBox center = new VBox(16);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(32));

        Label emptyHeader = new Label("Workflows run automatically when triggered");
        emptyHeader.getStyleClass().add("section-title");

        Label emptySubtext = new Label("Select New workflow to create your first automation.");
        emptySubtext.getStyleClass().add("secondary-text");

        center.getChildren().addAll(emptyHeader, emptySubtext);
        return center;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(16);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        Label workflowsCount = new Label("0 workflows enabled");
        workflowsCount.getStyleClass().add("caption-text");

        Label engineState = new Label("Engine stopped");
        engineState.getStyleClass().add("caption-text");

        statusBar.getChildren().addAll(workflowsCount, engineState);
        return statusBar;
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

package io.github.djaysharma007.cascade.app;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CascadeApplicationTest {

    private static boolean platformStarted = false;

    private static synchronized void initPlatform() {
        if (!platformStarted) {
            try {
                Platform.startup(() -> {});
                platformStarted = true;
            } catch (IllegalStateException ignored) {
                platformStarted = true;
            }
        }
    }

    @Test
    void applicationLaunchesAndCapturesThemedEmptyWindow() throws Exception {
        initPlatform();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicReference<Scene> sceneRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                CascadeApplication app = new CascadeApplication();
                Stage stage = new Stage();
                app.start(stage);
                stageRef.set(stage);
                sceneRef.set(stage.getScene());
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        Scene scene = sceneRef.get();
        assertThat(scene).isNotNull();

        // Capture snapshot on FX thread
        CountDownLatch snapLatch = new CountDownLatch(1);
        AtomicReference<WritableImage> imageRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                WritableImage image = scene.snapshot(null);
                imageRef.set(image);
            } finally {
                snapLatch.countDown();
            }
        });

        assertThat(snapLatch.await(10, TimeUnit.SECONDS)).isTrue();
        WritableImage fxImage = imageRef.get();
        assertThat(fxImage).isNotNull();

        // Save screenshot
        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = fxImage.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, reader.getArgb(x, y));
            }
        }

        Path screenshotDir = Path.of("docs", "screenshots");
        Files.createDirectories(screenshotDir);
        File file = screenshotDir.resolve("phase0_empty_window.png").toFile();
        ImageIO.write(bufferedImage, "png", file);

        assertThat(file).exists();
        assertThat(file.length()).isGreaterThan(0);

        // Close stage
        Platform.runLater(() -> {
            Stage stage = stageRef.get();
            if (stage != null) {
                stage.close();
            }
        });
    }
}

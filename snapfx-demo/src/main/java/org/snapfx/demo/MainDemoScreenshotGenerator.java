package org.snapfx.demo;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility launcher that starts {@link MainDemo} and captures a fresh preview image for README.
 */
public class MainDemoScreenshotGenerator extends Application {
    private static final String DEFAULT_OUTPUT = "docs/images/main-demo.png";
    private static final int RENDER_DELAY_MS = 1200;

    private boolean captureFailed;

    @Override
    public void start(Stage stage) {
        MainDemo demo = new MainDemo();
        demo.start(stage);

        String outputArg = getParameters().getUnnamed().isEmpty()
            ? DEFAULT_OUTPUT
            : getParameters().getUnnamed().getFirst();
        Path outputPath = Paths.get(outputArg).toAbsolutePath();

        PauseTransition delay = new PauseTransition(Duration.millis(RENDER_DELAY_MS));
        delay.setOnFinished(event -> captureAndExit(stage, outputPath));
        delay.play();
    }

    @Override
    public void stop() {
        if (captureFailed) {
            System.exit(1);
        }
    }

    private void captureAndExit(Stage stage, Path outputPath) {
        try {
            Scene scene = stage.getScene();
            if (scene == null || scene.getRoot() == null) {
                throw new IllegalStateException("MainDemo scene is not available for snapshot.");
            }

            Parent root = scene.getRoot();
            WritableImage snapshot = root.snapshot(null, null);

            File outputFile = outputPath.toFile();
            File outputDir = outputFile.getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }

            boolean writeOk = ImageIO.write(toBufferedImage(snapshot), "png", outputFile);
            if (!writeOk) {
                throw new IOException("No ImageIO writer found for PNG.");
            }
            System.out.println("MainDemo preview updated: " + outputPath);  // NOSONAR - part of build process, no need for logging framework
        } catch (Exception ex) {
            captureFailed = true;
            ex.printStackTrace(System.err); // NOSONAR - part of build process, no need for logging framework
        } finally {
            Platform.exit();
        }
    }

    /**
     * Main entry point for the application.
     * Launches the JavaFX application.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var pixelReader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            }
        }
        return bufferedImage;
    }
}

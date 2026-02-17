package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.floating.DockFloatingSnapTarget;
import com.github.beowolve.snapfx.persistence.DockLayoutLoadException;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

class MainDemoTest {
    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX is already running.
        }
    }

    @Test
    void testAppIconResourceListContainsAllExpectedSizes() {
        List<String> resources = MainDemo.getAppIconResources();
        assertEquals(6, resources.size());
    }

    @Test
    void testAppIconResourcesExistInClasspath() {
        for (String resourcePath : MainDemo.getAppIconResources()) {
            assertNotNull(
                MainDemo.class.getResource(resourcePath),
                "Missing app icon resource: " + resourcePath
            );
        }
    }

    @Test
    void testCopyMenuIconReturnsIndependentImageViewCopy() {
        Image sourceImage = new WritableImage(16, 16);
        Node copiedNode = MainDemo.copyMenuIcon(sourceImage);

        ImageView copied = assertInstanceOf(ImageView.class, copiedNode);
        assertNotNull(copied);
        assertEquals(sourceImage, copied.getImage());
        assertEquals(16.0, copied.getFitWidth(), 0.0001);
        assertEquals(16.0, copied.getFitHeight(), 0.0001);
        assertTrue(copied.isPreserveRatio());
        assertTrue(copied.isSmooth());
    }

    @Test
    void testCopyMenuIconReturnsNullForMissingImage() {
        Node copiedNode = MainDemo.copyMenuIcon(null);
        assertNull(copiedNode);
    }

    @Test
    void testConfigureDemoShortcutsRegistersF11Accelerator() {
        Scene scene = new Scene(new StackPane(), 300, 200);
        AtomicInteger invocationCounter = new AtomicInteger(0);

        MainDemo.configureDemoShortcuts(scene, invocationCounter::incrementAndGet);

        KeyCodeCombination f11 = new KeyCodeCombination(KeyCode.F11);
        Runnable f11Action = scene.getAccelerators().get(f11);
        assertNotNull(f11Action);

        f11Action.run();
        assertEquals(1, invocationCounter.get());
        assertTrue(scene.getAccelerators().containsKey(f11));
    }

    @Test
    void testBuildLayoutLoadErrorMessageIncludesLocation() {
        DockLayoutLoadException exception = new DockLayoutLoadException("Missing required field.", "$.root.type");
        String message = MainDemo.buildLayoutLoadErrorMessage(exception);

        assertTrue(message.contains("Error while loading:"));
        assertTrue(message.contains("Missing required field."));
        assertTrue(message.contains("$.root.type"));
    }

    @Test
    void testBuildLayoutLoadErrorMessageHandlesNullException() {
        String message = MainDemo.buildLayoutLoadErrorMessage(null);
        assertTrue(message.contains("unknown error"));
    }

    @Test
    void testCreateErrorAlertUsesProvidedOwner() {
        runOnFxThreadAndWait(() -> {
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane(), 100, 100));
            Alert alert = MainDemo.createErrorAlert("Error message", owner);
            assertSame(owner, alert.getOwner());
        });
    }

    @Test
    void testCreateErrorAlertWithoutOwner() {
        runOnFxThreadAndWait(() -> {
            Alert alert = MainDemo.createErrorAlert("Error message", null);
            assertNull(alert.getOwner());
        });
    }

    @Test
    void testResolveFloatingWindowSnapTargetsAllEnabled() {
        EnumSet<DockFloatingSnapTarget> targets =
            MainDemo.resolveFloatingWindowSnapTargets(true, true, true);

        assertEquals(
            EnumSet.of(
                DockFloatingSnapTarget.SCREEN,
                DockFloatingSnapTarget.MAIN_WINDOW,
                DockFloatingSnapTarget.FLOATING_WINDOWS
            ),
            targets
        );
    }

    @Test
    void testResolveFloatingWindowSnapTargetsSubsetAndEmpty() {
        EnumSet<DockFloatingSnapTarget> subset =
            MainDemo.resolveFloatingWindowSnapTargets(true, false, true);
        EnumSet<DockFloatingSnapTarget> empty =
            MainDemo.resolveFloatingWindowSnapTargets(false, false, false);

        assertEquals(
            EnumSet.of(DockFloatingSnapTarget.SCREEN, DockFloatingSnapTarget.FLOATING_WINDOWS),
            subset
        );
        assertEquals(EnumSet.noneOf(DockFloatingSnapTarget.class), empty);
    }

    @Test
    void testCreateJsonFileExtensionFilterUsesJsonGlob() {
        FileChooser.ExtensionFilter filter = MainDemo.createJsonFileExtensionFilter();

        assertEquals("JSON files", filter.getDescription());
        assertEquals(List.of("*.json"), filter.getExtensions());
    }

    @Test
    void testCreateEditorFileExtensionFiltersContainTextAndAllFilters() {
        List<FileChooser.ExtensionFilter> filters = MainDemo.createEditorFileExtensionFilters();

        assertEquals(2, filters.size());
        assertEquals("Text files", filters.getFirst().getDescription());
        assertEquals(
            List.of("*.txt", "*.md", "*.java", "*.xml", "*.json", "*.properties"),
            filters.getFirst().getExtensions()
        );
        assertEquals("All files", filters.getLast().getDescription());
        assertEquals(List.of("*.*"), filters.getLast().getExtensions());
    }

    @Test
    void testCreateFileChooserAppliesTitleAndFilters() {
        FileChooser.ExtensionFilter jsonFilter = MainDemo.createJsonFileExtensionFilter();
        FileChooser chooser = MainDemo.createFileChooser("Custom chooser", List.of(jsonFilter));

        assertEquals("Custom chooser", chooser.getTitle());
        assertEquals(1, chooser.getExtensionFilters().size());
        assertEquals(List.of("*.json"), chooser.getExtensionFilters().getFirst().getExtensions());
    }

    @Test
    void testApplyEditorSaveChooserDefaultsUsesCurrentFilePath() throws IOException {
        Path tempDirectory = Files.createTempDirectory("snapfx-main-demo-test");
        try {
            Path filePath = tempDirectory.resolve("editor-content.txt");
            FileChooser chooser = new FileChooser();

            MainDemo.applyEditorSaveChooserDefaults(chooser, filePath, "fallback-name.txt");

            assertNotNull(chooser.getInitialDirectory());
            assertEquals(
                tempDirectory.toFile().getCanonicalFile(),
                chooser.getInitialDirectory().getCanonicalFile()
            );
            assertEquals("editor-content.txt", chooser.getInitialFileName());
        } finally {
            Files.deleteIfExists(tempDirectory);
        }
    }

    @Test
    void testApplyEditorSaveChooserDefaultsUsesBaseTitleWhenPathMissing() {
        FileChooser chooser = new FileChooser();

        MainDemo.applyEditorSaveChooserDefaults(chooser, null, "EditorTitle.md");

        assertNull(chooser.getInitialDirectory());
        assertEquals("EditorTitle.md", chooser.getInitialFileName());
    }

    private void runOnFxThreadAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for JavaFX thread", e);
        }
        if (error.get() != null) {
            throw new AssertionError("JavaFX action failed", error.get());
        }
    }
}

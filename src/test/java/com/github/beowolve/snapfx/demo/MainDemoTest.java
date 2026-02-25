package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.SnapFX;
import com.github.beowolve.snapfx.floating.DockFloatingSnapTarget;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.persistence.DockLayoutLoadException;
import com.github.beowolve.snapfx.sidebar.DockSideBarMode;
import javafx.application.Platform;
import javafx.geometry.Side;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        Platform.setImplicitExit(false);
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
    void testThemeStylesheetResourcesExistInClasspath() {
        for (String resourcePath : MainDemo.getThemeStylesheetResources()) {
            assertNotNull(
                MainDemo.class.getResource(resourcePath),
                "Missing theme stylesheet resource: " + resourcePath
            );
        }
    }

    @Test
    void testNamedThemeStylesheetsMirrorSnapFXCatalog() {
        assertEquals(SnapFX.getAvailableThemeStylesheets(), MainDemo.getNamedThemeStylesheets());
    }

    @Test
    void testDockDebugHudIsTemporarilyDisabledDuringSidebarWork() {
        assertFalse(MainDemo.isDockDebugHudEnabled());
    }

    @Test
    void testResolveThemeNameByStylesheetPathUsesCatalogAndFallback() {
        assertEquals("Dark", MainDemo.resolveThemeNameByStylesheetPath("/snapfx-dark.css"));
        assertEquals(SnapFX.getDefaultThemeName(), MainDemo.resolveThemeNameByStylesheetPath("/unknown-theme.css"));
        assertEquals(SnapFX.getDefaultThemeName(), MainDemo.resolveThemeNameByStylesheetPath(" "));
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
    void testBuildSideBarMenuTitleIncludesSideCountAndVisibility() {
        assertEquals("Left Sidebar (2, pinned)", MainDemo.buildSideBarMenuTitle(Side.LEFT, 2, true));
        assertEquals("Right Sidebar (0, collapsed)", MainDemo.buildSideBarMenuTitle(Side.RIGHT, 0, false));
    }

    @Test
    void testSideBarSettingsSectionIncludesPanelWidthControlsBoundToSnapFxApi() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            setPrivateField(demo, "snapFX", framework);

            invokePrivateMethod(demo, "createSideBarSettingsSection");

            Spinner<Double> leftSpinner = readPrivateField(demo, "leftSideBarPanelWidthSpinner", Spinner.class);
            Spinner<Double> rightSpinner = readPrivateField(demo, "rightSideBarPanelWidthSpinner", Spinner.class);
            assertNotNull(leftSpinner);
            assertNotNull(rightSpinner);
            assertNotNull(leftSpinner.getValueFactory());
            assertNotNull(rightSpinner.getValueFactory());

            assertEquals(framework.getSideBarPanelWidth(Side.LEFT), leftSpinner.getValueFactory().getValue(), 0.0001);
            assertEquals(framework.getSideBarPanelWidth(Side.RIGHT), rightSpinner.getValueFactory().getValue(), 0.0001);

            leftSpinner.getValueFactory().setValue(360.0);
            rightSpinner.getValueFactory().setValue(290.0);

            assertEquals(360.0, framework.getSideBarPanelWidth(Side.LEFT), 0.0001);
            assertEquals(290.0, framework.getSideBarPanelWidth(Side.RIGHT), 0.0001);
        });
    }

    @Test
    void testSideBarSettingsSectionIncludesSideBarModeControlBoundToSnapFxApi() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            setPrivateField(demo, "snapFX", framework);

            invokePrivateMethod(demo, "createSideBarSettingsSection");

            ComboBox<DockSideBarMode> sideBarModeComboBox =
                readPrivateField(demo, "sideBarModeComboBox", ComboBox.class);
            assertNotNull(sideBarModeComboBox);
            assertEquals(DockSideBarMode.AUTO, sideBarModeComboBox.getValue());

            sideBarModeComboBox.setValue(DockSideBarMode.ALWAYS);
            assertEquals(DockSideBarMode.ALWAYS, framework.getSideBarMode());

            sideBarModeComboBox.setValue(DockSideBarMode.NEVER);
            assertEquals(DockSideBarMode.NEVER, framework.getSideBarMode());
        });
    }

    @Test
    void testUpdateDockLayoutKeepsDebugSplitLeftItemStable() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            framework.getDockGraph().setRoot(new DockNode("root", new Label("Root"), "Root"));

            SplitPane splitPane = new SplitPane();
            StackPane dockLayoutHost = new StackPane(new Label("placeholder"));
            splitPane.getItems().addAll(dockLayoutHost, new StackPane(new Label("debug")));
            splitPane.setDividerPositions(0.63);

            setPrivateField(demo, "snapFX", framework);
            setPrivateField(demo, "mainLayout", new BorderPane());
            setPrivateField(demo, "mainSplit", splitPane);
            setPrivateField(demo, "dockLayoutHost", dockLayoutHost);

            Node originalLeftItem = splitPane.getItems().getFirst();
            double originalDividerPosition = splitPane.getDividerPositions()[0];

            invokePrivateMethod(demo, "updateDockLayout");

            assertSame(originalLeftItem, splitPane.getItems().getFirst());
            assertSame(dockLayoutHost, splitPane.getItems().getFirst());
            assertEquals(originalDividerPosition, splitPane.getDividerPositions()[0], 0.0001);
            assertEquals(1, dockLayoutHost.getChildren().size());
            assertFalse(dockLayoutHost.getChildren().getFirst() instanceof Label);
        });
    }

    @Test
    void testFormatDockNodeListLabelUsesTitleAndNodeIdFallbacks() {
        DockNode node = new DockNode("editor", new Label("Editor"), "Main Editor");
        assertEquals("Main Editor [editor]", MainDemo.formatDockNodeListLabel(node));

        DockNode untitled = new DockNode("", new Label("Empty"), "");
        assertEquals("Untitled [unknown]", MainDemo.formatDockNodeListLabel(untitled));
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

    private void invokePrivateMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke private method: " + methodName, e);
        }
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to set private field: " + fieldName, e);
        }
    }

    private <T> T readPrivateField(Object target, String fieldName, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return type.cast(value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read private field: " + fieldName, e);
        }
    }
}

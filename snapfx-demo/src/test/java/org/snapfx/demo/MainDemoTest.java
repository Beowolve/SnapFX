package org.snapfx.demo;

import org.snapfx.SnapFX;
import org.snapfx.DockUserAgentThemeMode;
import org.snapfx.dnd.DockDragService;
import org.snapfx.floating.DockFloatingSnapTarget;
import org.snapfx.model.DockNode;
import org.snapfx.persistence.DockLayoutLoadException;
import org.snapfx.sidebar.DockSideBarMode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Side;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
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
    void testResolveThemeNameByStylesheetPathUsesCatalogAndFallback() {
        assertEquals("Dark", MainDemo.resolveThemeNameByStylesheetPath("/snapfx-dark.css"));
        assertEquals(SnapFX.getDefaultThemeName(), MainDemo.resolveThemeNameByStylesheetPath("/unknown-theme.css"));
        assertEquals(SnapFX.getDefaultThemeName(), MainDemo.resolveThemeNameByStylesheetPath(" "));
    }

    @Test
    void testAtlantaFxThemeStylesheetsAreExposed() {
        Map<String, String> atlantaFxThemeStylesheets = MainDemo.getAtlantaFxThemeStylesheets();
        assertFalse(atlantaFxThemeStylesheets.isEmpty());
        assertTrue(atlantaFxThemeStylesheets.containsKey("Primer Light"));
        assertTrue(
            atlantaFxThemeStylesheets.values().stream().allMatch(stylesheet -> stylesheet != null && stylesheet.contains("atlantafx")),
            "Expected all AtlantaFX stylesheets to point to AtlantaFX resources"
        );
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
    void testSideBarSettingsSectionIncludesCollapsePinnedOnActiveIconClickControlBoundToSnapFxApi() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            setPrivateField(demo, "snapFX", framework);

            invokePrivateMethod(demo, "createSideBarSettingsSection");

            CheckBox collapsePinnedOnActiveIconClickCheckBox =
                readPrivateField(demo, "collapsePinnedOnActiveIconClickCheckBox", CheckBox.class);
            assertNotNull(collapsePinnedOnActiveIconClickCheckBox);
            assertEquals(
                framework.isCollapsePinnedSideBarOnActiveIconClick(),
                collapsePinnedOnActiveIconClickCheckBox.isSelected()
            );

            collapsePinnedOnActiveIconClickCheckBox.setSelected(false);
            assertFalse(framework.isCollapsePinnedSideBarOnActiveIconClick());

            collapsePinnedOnActiveIconClickCheckBox.setSelected(true);
            assertTrue(framework.isCollapsePinnedSideBarOnActiveIconClick());
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
    void testCreateSettingsPanelIncludesLocalizationControlsBoundToSnapFxApi() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            setPrivateField(demo, "snapFX", framework);

            invokePrivateMethod(demo, "createSettingsPanel");

            ComboBox<Locale> localeComboBox = readPrivateField(demo, "localeComboBox", ComboBox.class);
            assertNotNull(localeComboBox);
            assertTrue(localeComboBox.getItems().contains(Locale.ENGLISH));
            assertTrue(localeComboBox.getItems().contains(Locale.GERMAN));
            assertTrue(localeComboBox.getItems().contains(Locale.FRENCH));

            localeComboBox.setValue(Locale.GERMAN);
            assertEquals(Locale.GERMAN, framework.getLocale());
            assertNull(framework.getLocalizationProvider());

            localeComboBox.setValue(Locale.FRENCH);
            assertEquals(Locale.FRENCH, framework.getLocale());
            assertNotNull(framework.getLocalizationProvider());

            localeComboBox.setValue(Locale.ENGLISH);
            assertEquals(Locale.ENGLISH, framework.getLocale());
            assertNull(framework.getLocalizationProvider());
        });
    }

    @Test
    void testCreateSettingsPanelInternalThemeSelectionUpdatesSnapFxThemeStylesheet() {
        runOnFxThreadAndWait(() -> {
            String previousUserAgentStylesheet = Application.getUserAgentStylesheet();
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            try {
                setPrivateField(demo, "snapFX", framework);
                invokePrivateMethod(demo, "createSettingsPanel");

                ComboBox<?> themeSourceComboBox = readPrivateField(demo, "themeSourceComboBox", ComboBox.class);
                ComboBox<String> themeComboBox = readPrivateField(demo, "themeComboBox", ComboBox.class);
                assertNotNull(themeSourceComboBox);
                assertNotNull(themeComboBox);
                themeSourceComboBox.getSelectionModel().selectFirst();

                String targetTheme = SnapFX.getAvailableThemeNames().stream()
                    .filter(themeName -> {
                        String themePath = SnapFX.getAvailableThemeStylesheets().get(themeName);
                        return !themePath.equals(framework.getThemeStylesheetResourcePath());
                    })
                    .findFirst()
                    .orElse(SnapFX.getDefaultThemeName());
                Map<String, String> namedThemes = SnapFX.getAvailableThemeStylesheets();
                String expectedStylesheet = namedThemes.get(targetTheme);
                themeComboBox.setValue(targetTheme);

                assertNotNull(expectedStylesheet);
                assertEquals(expectedStylesheet, framework.getThemeStylesheetResourcePath());
                assertEquals(Application.STYLESHEET_MODENA, Application.getUserAgentStylesheet());
                assertEquals(DockUserAgentThemeMode.MODENA, framework.getUserAgentThemeMode());
            } finally {
                Application.setUserAgentStylesheet(previousUserAgentStylesheet);
            }
        });
    }

    @Test
    void testCreateSettingsPanelAtlantaFxSelectionUpdatesUserAgentAndCompatMode() {
        runOnFxThreadAndWait(() -> {
            String previousUserAgentStylesheet = Application.getUserAgentStylesheet();
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            Stage stage = new Stage();
            try {
                DockNode node = new DockNode("node", new Label("Node"), "Node");
                framework.dock(node, null, org.snapfx.model.DockPosition.CENTER);
                Scene scene = new Scene(framework.buildLayout(), 640, 480);
                stage.setScene(scene);
                framework.initialize(stage);

                setPrivateField(demo, "snapFX", framework);
                invokePrivateMethod(demo, "createSettingsPanel");

                ComboBox<?> themeSourceComboBox = readPrivateField(demo, "themeSourceComboBox", ComboBox.class);
                ComboBox<String> themeComboBox = readPrivateField(demo, "themeComboBox", ComboBox.class);
                assertNotNull(themeSourceComboBox);
                assertNotNull(themeComboBox);
                themeSourceComboBox.getSelectionModel().select(1);
                themeComboBox.getSelectionModel().selectFirst();
                assertNotNull(Application.getUserAgentStylesheet());
                assertTrue(Application.getUserAgentStylesheet().contains("atlantafx"));
                assertEquals(DockUserAgentThemeMode.ATLANTAFX_COMPAT, framework.getUserAgentThemeMode());

                String compatStylesheetUrl = SnapFX.class.getResource("/snapfx-atlantafx-compat.css").toExternalForm();
                assertTrue(scene.getStylesheets().contains(compatStylesheetUrl));

                themeSourceComboBox.getSelectionModel().selectFirst();
                assertEquals(Application.STYLESHEET_MODENA, Application.getUserAgentStylesheet());
                assertEquals(DockUserAgentThemeMode.MODENA, framework.getUserAgentThemeMode());
                assertFalse(scene.getStylesheets().contains(compatStylesheetUrl));
            } finally {
                Application.setUserAgentStylesheet(previousUserAgentStylesheet);
                stage.close();
                closeGhostStage(framework);
            }
        });
    }

    @Test
    void testCreateSettingsPanelUsesScrollPaneAndShowsAppearanceControlsBeforeLayoutControls() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            setPrivateField(demo, "snapFX", framework);

            Parent settingsPanel = invokePrivateMethodWithResult(demo, "createSettingsPanel", Parent.class);

            ScrollPane scrollPane = assertInstanceOf(ScrollPane.class, settingsPanel);
            assertTrue(scrollPane.isFitToWidth());
            assertNotNull(scrollPane.getContent());
            VBox panelContent = assertInstanceOf(VBox.class, scrollPane.getContent());

            List<String> labeledTexts = collectLabeledTexts(panelContent);
            int themeIndex = labeledTexts.indexOf("Theme");
            int localeIndex = labeledTexts.indexOf("Framework Locale");
            int titleBarModeIndex = labeledTexts.indexOf("Title Bar Mode");

            assertTrue(themeIndex >= 0, "Theme setting label not found");
            assertTrue(localeIndex >= 0, "Framework locale setting label not found");
            assertTrue(titleBarModeIndex >= 0, "Title bar mode setting label not found");
            assertTrue(themeIndex < titleBarModeIndex, "Theme setting should be placed before layout settings");
            assertTrue(localeIndex < titleBarModeIndex, "Framework locale setting should be placed before layout settings");
        });
    }

    @Test
    void testLocaleSwitchUpdatesVisibleDemoTextsOutsideFrameworkContextMenus() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            setPrivateField(demo, "snapFX", framework);

            Parent settingsPanel = invokePrivateMethodWithResult(demo, "createSettingsPanel", Parent.class);
            ToolBar toolbar = invokePrivateMethodWithResult(demo, "createToolbar", ToolBar.class);

            ComboBox<Locale> localeSelector = readPrivateField(demo, "localeComboBox", ComboBox.class);
            assertNotNull(localeSelector);

            localeSelector.setValue(Locale.GERMAN);

            ScrollPane settingsScrollPane = assertInstanceOf(ScrollPane.class, settingsPanel);
            VBox settingsContent = assertInstanceOf(VBox.class, settingsScrollPane.getContent());
            List<String> settingsTexts = collectLabeledTexts(settingsContent);
            List<String> toolbarTexts = toolbar.getItems().stream()
                .filter(Labeled.class::isInstance)
                .map(Labeled.class::cast)
                .map(Labeled::getText)
                .toList();

            assertTrue(
                settingsTexts.contains("Darstellung und Lokalisierung"),
                "Localized appearance section title should be visible immediately after locale change"
            );
            assertTrue(
                toolbarTexts.contains("Hinzufügen:"),
                "Localized toolbar label should be visible immediately after locale change"
            );
            assertTrue(
                toolbarTexts.contains("+ Eigenschaften"),
                "Localized demo node action should be visible immediately after locale change"
            );
        });
    }

    @Test
    void testCreateSettingsPanelAppliesMinimumControlWidthsForReadability() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            setPrivateField(demo, "snapFX", framework);

            invokePrivateMethod(demo, "createSettingsPanel");

            ComboBox<Locale> localeSettingsComboBox = readPrivateField(demo, "localeComboBox", ComboBox.class);
            ComboBox<DockSideBarMode> sideBarModeSettingsComboBox =
                readPrivateField(demo, "sideBarModeComboBox", ComboBox.class);
            CheckBox collapsePinnedOnActiveIconClickCheckBox =
                readPrivateField(demo, "collapsePinnedOnActiveIconClickCheckBox", CheckBox.class);

            assertNotNull(localeSettingsComboBox);
            assertNotNull(sideBarModeSettingsComboBox);
            assertNotNull(collapsePinnedOnActiveIconClickCheckBox);
            assertTrue(localeSettingsComboBox.getMinWidth() > 0.0);
            assertTrue(sideBarModeSettingsComboBox.getMinWidth() > 0.0);
            assertTrue(collapsePinnedOnActiveIconClickCheckBox.getMinWidth() > 0.0);
            assertEquals(localeSettingsComboBox.getMinWidth(), sideBarModeSettingsComboBox.getMinWidth(), 0.0001);
        });
    }

    @Test
    void testSideBarSettingsSectionContainsOnlyModeAndCollapseSettings() {
        runOnFxThreadAndWait(() -> {
            MainDemo demo = new MainDemo();
            SnapFX framework = new SnapFX();
            setPrivateField(demo, "snapFX", framework);

            VBox sideBarSection = invokePrivateMethodWithResult(demo, "createSideBarSettingsSection", VBox.class);
            List<String> labeledTexts = collectLabeledTexts(sideBarSection);
            List<String> nonBlankTexts = labeledTexts.stream()
                .filter(text -> text != null && !text.isBlank())
                .toList();

            assertTrue(nonBlankTexts.contains("Sidebar Mode"));
            assertTrue(nonBlankTexts.contains("Collapse pinned"));
            assertTrue(nonBlankTexts.contains("Collapse pinned panel on active icon click"));
            assertFalse(nonBlankTexts.stream().anyMatch(text -> text.contains("Phase 2")));
            assertFalse(nonBlankTexts.stream().anyMatch(text -> text.contains("Move Selected")));
            assertFalse(nonBlankTexts.stream().anyMatch(text -> text.contains("Restore Selected")));
            assertFalse(nonBlankTexts.stream().anyMatch(text -> text.contains("Panel Width")));
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

    private <T> T invokePrivateMethodWithResult(Object target, String methodName, Class<T> type) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object result = method.invoke(target);
            return type.cast(result);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke private method with result: " + methodName, e);
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

    private void closeGhostStage(SnapFX framework) {
        if (framework == null) {
            return;
        }
        try {
            Field dragServiceField = SnapFX.class.getDeclaredField("dragService");
            dragServiceField.setAccessible(true);
            Object dragService = dragServiceField.get(framework);
            if (dragService == null) {
                return;
            }
            Field ghostStageField = DockDragService.class.getDeclaredField("ghostStage");
            ghostStageField.setAccessible(true);
            Object ghostStage = ghostStageField.get(dragService);
            if (ghostStage instanceof Stage ghostStageInstance) {
                ghostStageInstance.close();
            }
        } catch (ReflectiveOperationException ignored) {
            // Best-effort cleanup for JavaFX drag ghost stage.
        }
    }

    private List<String> collectLabeledTexts(Node root) {
        List<String> texts = new ArrayList<>();
        collectLabeledTexts(root, texts);
        return texts;
    }

    private void collectLabeledTexts(Node node, List<String> collector) {
        if (node instanceof Labeled labeled) {
            collector.add(labeled.getText());
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectLabeledTexts(child, collector);
            }
        }
    }

}

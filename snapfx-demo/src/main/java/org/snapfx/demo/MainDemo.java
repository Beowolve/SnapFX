package org.snapfx.demo;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import org.snapfx.SnapFX;
import org.snapfx.DockUserAgentThemeMode;
import org.snapfx.close.DockCloseBehavior;
import org.snapfx.close.DockCloseDecision;
import org.snapfx.close.DockCloseRequest;
import org.snapfx.close.DockCloseResult;
import org.snapfx.debug.DockDebugOverlay;
import org.snapfx.debug.DockGraphDebugView;
import org.snapfx.demo.dialog.AboutDialog;
import org.snapfx.demo.editor.EditorCloseDecisionPolicy;
import org.snapfx.demo.editor.SerializableEditor;
import org.snapfx.demo.factory.DemoNodeFactory;
import org.snapfx.demo.factory.DockNodeType;
import org.snapfx.demo.i18n.DemoLocalizationService;
import org.snapfx.demo.util.IconUtil;
import org.snapfx.dnd.DockDropVisualizationMode;
import org.snapfx.floating.DockFloatingPinButtonMode;
import org.snapfx.floating.DockFloatingPinChangeEvent;
import org.snapfx.floating.DockFloatingPinLockedBehavior;
import org.snapfx.floating.DockFloatingSnapTarget;
import org.snapfx.floating.DockFloatingWindow;
import org.snapfx.localization.DockLocalizationProvider;
import org.snapfx.localization.DockResourceBundleLocalizationProvider;
import org.snapfx.model.DockContainer;
import org.snapfx.model.DockElement;
import org.snapfx.model.DockNode;
import org.snapfx.model.DockPosition;
import org.snapfx.persistence.DockLayoutLoadException;
import org.snapfx.sidebar.DockSideBarMode;
import org.snapfx.view.DockCloseButtonMode;
import org.snapfx.view.DockTitleBarMode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Demo application for the SnapFX framework.
 * Shows a typical IDE-like layout with sidebar, editor, and console.
 */
public class MainDemo extends Application {
    private static final System.Logger LOGGER = System.getLogger(MainDemo.class.getName());
    public static final String FX_FONT_WEIGHT_BOLD = "-fx-font-weight: bold;"; // Used for bold text in the demo
    private static final String DIRTY_TITLE_SUFFIX = " *";
    private static final String JSON_FILE_GLOB = "*.json";
    private static final String ALL_FILES_GLOB = "*.*";
    private static final String[] TEXT_FILES_GLOBS = {"*.txt", "*.md", "*.java", "*.xml", JSON_FILE_GLOB, "*.properties"};
    private static final String JSON_FILES_FILTER_LABEL = "JSON files";
    private static final String TEXT_FILES_FILTER_LABEL = "Text files";
    private static final String ALL_FILES_FILTER_LABEL = "All files";
    private static final String SAVE_LAYOUT_CHOOSER_TITLE = "Save layout";
    private static final String LOAD_LAYOUT_CHOOSER_TITLE = "Load layout";
    private static final String OPEN_TEXT_FILE_CHOOSER_TITLE = "Open text file";
    private static final String SAVE_EDITOR_CHOOSER_TITLE = "Save editor content";
    private static final String DEFAULT_LAYOUT_FILE_NAME = "snapfx-layout.json";
    private static final String DOCUMENTS_DIRECTORY_NAME = "Documents";
    private static final boolean ENABLE_DOCK_DEBUG_HUD = false;
    private static final double SETTINGS_LABEL_MIN_WIDTH = 120.0;
    private static final double SETTINGS_CONTROL_MIN_WIDTH = 120.0;
    private static final double SETTINGS_SECTION_MIN_WIDTH = 250.0;
    private static final Map<String, Theme> ATLANTAFX_THEMES = createAtlantaFxThemes();
    private static final Locale EXTENDED_DEMO_LOCALE = Locale.FRENCH;
    private static final List<String> APP_ICON_RESOURCES = List.of(
        "/images/16/snapfx.png",
        "/images/24/snapfx.png",
        "/images/32/snapfx.png",
        "/images/48/snapfx.png",
        "/images/64/snapfx.png",
        "/images/128/snapfx.png"
    );
    private SnapFX snapFX;
    private Stage primaryStage;
    private BorderPane mainLayout;
    private SplitPane mainSplit;
    private StackPane dockLayoutHost;

    // Menu for hidden windows
    private Menu hiddenWindowsMenu;
    private Menu floatNodeMenu;
    private Menu floatingWindowsMenu;
    private Menu pinLeftSideBarMenu;
    private Menu pinRightSideBarMenu;
    private Menu leftSideBarMenu;
    private Menu rightSideBarMenu;

    // Shared lock state property
    private final BooleanProperty lockLayoutProperty = new SimpleBooleanProperty(false);

    // Node factory for creating demo nodes
    private DemoNodeFactory demoNodeFactory;
    private final BooleanProperty promptOnEditorCloseProperty = new SimpleBooleanProperty(true);
    private final Map<DockNode, EditorDocumentState> editorDocumentStates = new HashMap<>();
    private EditorCloseDecisionPolicy editorCloseDecisionPolicy;
    private ComboBox<DockSideBarMode> sideBarModeComboBox;
    private ComboBox<DemoThemeSource> themeSourceComboBox;
    private ComboBox<String> themeComboBox;
    private boolean updatingThemeSettingsControls;
    private CheckBox collapsePinnedOnActiveIconClickCheckBox;
    private boolean updatingSideBarSettingsControls;
    private ComboBox<Locale> localeComboBox;
    private DockGraphDebugView debugView;
    private DockDebugOverlay debugOverlay;
    private final DemoLocalizationService demoTextLocalization = new DemoLocalizationService(MainDemo.class.getModule());
    private final DockLocalizationProvider demoLocalizationProvider = new DockResourceBundleLocalizationProvider(
        "org.snapfx.demo.i18n.snapfx",
        MainDemo.class.getModule()
    );

    private static final class EditorDocumentState {
        private String baseTitle;
        private Path filePath;
        private boolean dirty;
        private boolean suppressDirtyTracking;
        private boolean usesGeneratedUntitledTitle;
        private ChangeListener<String> textListener;
    }

    private enum DemoThemeSource {
        INTERNAL,
        ATLANTAFX
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.snapFX = new SnapFX();

        // Main layout
        mainLayout = new BorderPane();

        // Create menu bar and toolbar
        VBox topContainer = new VBox();
        ToolBar toolbar = createToolbar();  // Create toolbar first (initializes lockCheckBox)
        MenuBar menuBar = createMenuBar();  // Then create menuBar (uses lockCheckBox)
        topContainer.getChildren().addAll(menuBar, toolbar);
        mainLayout.setTop(topContainer);

        stage.setScene(new Scene(mainLayout, 1400, 900));
        demoTextLocalization.bind(stage, "demo.stage.title");
        configureDemoShortcuts(stage.getScene(), this::toggleFullscreen);
        applyApplicationIcons(stage);

        // Create demo node factory
        demoNodeFactory = new DemoNodeFactory(demoTextLocalization);
        editorCloseDecisionPolicy = createEditorCloseDecisionPolicy();

        // Setup node factory for proper save/load across sessions
        setupNodeFactory();

        // Keep default close behavior explicit for demo clarity
        snapFX.setDefaultCloseBehavior(DockCloseBehavior.HIDE);
        snapFX.setOnCloseRequest(this::handleCloseRequest);
        snapFX.setOnCloseHandled(this::handleCloseHandled);

        // Set drop visualization mode
        snapFX.setDropVisualizationMode(DockDropVisualizationMode.DEFAULT);

        // Set close button mode to show only on active tab for cleaner look
        snapFX.setCloseButtonMode(DockCloseButtonMode.BOTH);

        // Set title bar mode to auto (show only when needed)
        snapFX.setTitleBarMode(DockTitleBarMode.AUTO);
        snapFX.setFloatingPinButtonMode(DockFloatingPinButtonMode.AUTO);
        snapFX.setAllowFloatingPinToggle(true);
        snapFX.setDefaultFloatingAlwaysOnTop(true);
        snapFX.setFloatingPinLockedBehavior(DockFloatingPinLockedBehavior.ALLOW);
        snapFX.setOnFloatingPinChanged(this::handleFloatingPinChanged);

        // Create dock layout (after handler is set)
        createDemoLayout();

        // Put dock layout into the center
        updateDockLayout();

        // Install debug panel (right side)
        installDebugPanel();

        // Keep demo localization controls and runtime state synchronized.
        applyLocalizationSelection();

        // Initialize SnapFX AFTER scene is set (needed for ghost overlay)
        snapFX.initialize(stage);

        // Listen to lock state changes
        lockLayoutProperty.addListener((obs, oldVal, newVal) -> onLockLayoutChanged(Boolean.TRUE.equals(newVal)));

        stage.show();

        // Bring window to front
        stage.toFront();
        stage.requestFocus();
    }

    private void toggleFullscreen() {
        if (primaryStage == null) {
            return;
        }
        primaryStage.setFullScreen(!primaryStage.isFullScreen());
    }

    private void onLockLayoutChanged(boolean locked) {
        snapFX.setLocked(locked);
        updateDockLayout();
        updateSideBarMenus();
        refreshSideBarSettingsViews();
    }

    static List<String> getAppIconResources() {
        return APP_ICON_RESOURCES;
    }

    static Map<String, String> getNamedThemeStylesheets() {
        return SnapFX.getAvailableThemeStylesheets();
    }

    static List<String> getThemeStylesheetResources() {
        return List.copyOf(getNamedThemeStylesheets().values());
    }

    SnapFX getSnapFXForAutomation() {
        return snapFX;
    }

    static boolean isDockDebugHudEnabled() {
        return ENABLE_DOCK_DEBUG_HUD;
    }

    static String resolveThemeNameByStylesheetPath(String stylesheetResourcePath) {
        if (stylesheetResourcePath == null || stylesheetResourcePath.isBlank()) {
            return SnapFX.getDefaultThemeName();
        }
        for (Map.Entry<String, String> entry : getNamedThemeStylesheets().entrySet()) {
            if (entry.getValue().equals(stylesheetResourcePath)) {
                return entry.getKey();
            }
        }
        return SnapFX.getDefaultThemeName();
    }

    static Map<String, String> getAtlantaFxThemeStylesheets() {
        LinkedHashMap<String, String> stylesheets = new LinkedHashMap<>();
        ATLANTAFX_THEMES.forEach((name, theme) -> stylesheets.put(name, theme.getUserAgentStylesheet()));
        return Collections.unmodifiableMap(stylesheets);
    }

    private static Map<String, Theme> createAtlantaFxThemes() {
        LinkedHashMap<String, Theme> themes = new LinkedHashMap<>();
        for (Theme theme : List.of(
            new PrimerLight(),
            new PrimerDark(),
            new CupertinoLight(),
            new CupertinoDark(),
            new NordLight(),
            new NordDark(),
            new Dracula()
        )) {
            themes.put(theme.getName(), theme);
        }
        return Collections.unmodifiableMap(themes);
    }

    private static String resolveAtlantaFxThemeNameByUserAgentStylesheet(String userAgentStylesheet) {
        if (userAgentStylesheet == null || userAgentStylesheet.isBlank()) {
            return null;
        }
        for (Map.Entry<String, Theme> entry : ATLANTAFX_THEMES.entrySet()) {
            if (userAgentStylesheet.equals(entry.getValue().getUserAgentStylesheet())) {
                return entry.getKey();
            }
        }
        return null;
    }

    static void configureDemoShortcuts(Scene scene, Runnable toggleFullscreenAction) {
        if (scene == null || toggleFullscreenAction == null) {
            return;
        }
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F11), toggleFullscreenAction);
    }

    private void applyApplicationIcons(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.getIcons().clear();
        for (String resourcePath : APP_ICON_RESOURCES) {
            var iconUrl = MainDemo.class.getResource(resourcePath);
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        }
    }

    private String tr(String key, Object... args) {
        return demoTextLocalization.text(key, args);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu();
        demoTextLocalization.bind(fileMenu, "demo.menu.file");

        MenuItem openEditorItem = new MenuItem();
        demoTextLocalization.bind(openEditorItem, "demo.menu.file.openTextFile");
        openEditorItem.setGraphic(IconUtil.loadIcon("folder-open-document-text.png"));
        openEditorItem.setOnAction(e -> openTextFileInEditor());

        MenuItem saveEditorItem = new MenuItem();
        demoTextLocalization.bind(saveEditorItem, "demo.menu.file.saveActiveEditor");
        saveEditorItem.setGraphic(IconUtil.loadIcon("disk.png"));
        saveEditorItem.setOnAction(e -> saveActiveEditor(false));

        MenuItem saveEditorAsItem = new MenuItem();
        demoTextLocalization.bind(saveEditorAsItem, "demo.menu.file.saveActiveEditorAs");
        saveEditorAsItem.setGraphic(IconUtil.loadIcon("disk--pencil.png"));
        saveEditorAsItem.setOnAction(e -> saveActiveEditor(true));

        SeparatorMenuItem separatorEditor = new SeparatorMenuItem();

        MenuItem saveLayoutItem = new MenuItem();
        demoTextLocalization.bind(saveLayoutItem, "demo.menu.file.saveLayout");
        saveLayoutItem.setGraphic(IconUtil.loadIcon("disk-black.png"));
        saveLayoutItem.setOnAction(e -> saveLayout());

        MenuItem loadLayoutItem = new MenuItem();
        demoTextLocalization.bind(loadLayoutItem, "demo.menu.file.loadLayout");
        loadLayoutItem.setGraphic(IconUtil.loadIcon("folder-open-document.png"));
        loadLayoutItem.setOnAction(e -> loadLayout());

        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem exitItem = new MenuItem();
        demoTextLocalization.bind(exitItem, "demo.menu.file.exit");
        exitItem.setGraphic(IconUtil.loadIcon("logout.png"));
        exitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(
            openEditorItem,
            saveEditorItem,
            saveEditorAsItem,
            separatorEditor,
            saveLayoutItem,
            loadLayoutItem,
            separator1,
            exitItem
        );

        Menu layoutMenu = new Menu();
        demoTextLocalization.bind(layoutMenu, "demo.menu.layout");

        MenuItem resetItem = new MenuItem();
        demoTextLocalization.bind(resetItem, "demo.menu.layout.resetToDefault");
        resetItem.setGraphic(IconUtil.loadIcon("arrow-circle.png"));
        resetItem.setOnAction(e -> resetLayoutToDefault());

        CheckMenuItem lockItem = new CheckMenuItem();
        demoTextLocalization.bind(lockItem, "demo.menu.layout.lock");
        lockItem.setGraphic(IconUtil.loadIcon("lock.png"));
        lockItem.selectedProperty().bindBidirectional(lockLayoutProperty);
        lockItem.selectedProperty().addListener((obs, oldVal, newVal) ->
            lockItem.setGraphic(IconUtil.loadIcon(Boolean.TRUE.equals(newVal) ? "lock.png" : "lock-unlock.png"))
        );
        lockItem.selectedProperty().bindBidirectional(lockLayoutProperty);

        SeparatorMenuItem sep2 = new SeparatorMenuItem();

        hiddenWindowsMenu = new Menu();
        demoTextLocalization.bind(hiddenWindowsMenu, "demo.menu.layout.hiddenWindows");
        updateHiddenWindowsMenu();

        pinLeftSideBarMenu = new Menu();
        demoTextLocalization.bind(pinLeftSideBarMenu, "demo.menu.layout.moveToLeftSidebar");
        pinRightSideBarMenu = new Menu();
        demoTextLocalization.bind(pinRightSideBarMenu, "demo.menu.layout.moveToRightSidebar");
        leftSideBarMenu = new Menu();
        rightSideBarMenu = new Menu();
        updateSideBarMenus();

        floatNodeMenu = new Menu();
        demoTextLocalization.bind(floatNodeMenu, "demo.menu.layout.floatNode");
        floatingWindowsMenu = new Menu();
        demoTextLocalization.bind(floatingWindowsMenu, "demo.menu.layout.floatingWindows");
        updateFloatingMenus();

        // Listen to hidden nodes changes
        snapFX.getHiddenNodes().addListener((ListChangeListener<DockNode>) c ->
            updateHiddenWindowsMenu()
        );

        snapFX.getDockGraph().revisionProperty().addListener((obs, oldVal, newVal) ->
            onDockGraphRevisionChanged()
        );
        snapFX.getFloatingWindows().addListener((ListChangeListener<DockFloatingWindow>) c ->
            updateFloatingMenus()
        );

        layoutMenu.getItems().addAll(
            resetItem,
            lockItem,
            sep2,
            hiddenWindowsMenu,
            new SeparatorMenuItem(),
            pinLeftSideBarMenu,
            pinRightSideBarMenu,
            leftSideBarMenu,
            rightSideBarMenu,
            new SeparatorMenuItem(),
            floatNodeMenu,
            floatingWindowsMenu
        );

        Menu helpMenu = new Menu();
        demoTextLocalization.bind(helpMenu, "demo.menu.help");

        MenuItem aboutItem = new MenuItem();
        demoTextLocalization.bind(aboutItem, "demo.menu.help.about");
        aboutItem.setGraphic(IconUtil.loadIcon("question.png"));
        aboutItem.setOnAction(e -> AboutDialog.show(primaryStage, getHostServices()::showDocument, demoTextLocalization));

        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, layoutMenu, helpMenu);
        return menuBar;
    }


    private void updateHiddenWindowsMenu() {
        hiddenWindowsMenu.getItems().clear();

        if (snapFX.getHiddenNodes().isEmpty()) {
            MenuItem emptyItem = new MenuItem(tr("demo.menu.layout.emptyHiddenWindows"));
            emptyItem.setDisable(true);
            hiddenWindowsMenu.getItems().add(emptyItem);
        } else {
            for (DockNode node : snapFX.getHiddenNodes()) {
                MenuItem item = new MenuItem(node.getTitle());
                item.setGraphic(createMenuItemIcon(node));
                item.setOnAction(e -> snapFX.restore(node));
                hiddenWindowsMenu.getItems().add(item);
            }
        }
    }

    private void onDockGraphRevisionChanged() {
        updateDockLayout();
        updateSideBarMenus();
        updateFloatingMenus();
        refreshSideBarSettingsViews();
    }

    private void updateSideBarMenus() {
        if (pinLeftSideBarMenu != null) {
            updatePinToSideBarMenu(pinLeftSideBarMenu, Side.LEFT);
        }
        if (pinRightSideBarMenu != null) {
            updatePinToSideBarMenu(pinRightSideBarMenu, Side.RIGHT);
        }
        if (leftSideBarMenu != null) {
            updatePinnedSideBarMenu(leftSideBarMenu, Side.LEFT);
        }
        if (rightSideBarMenu != null) {
            updatePinnedSideBarMenu(rightSideBarMenu, Side.RIGHT);
        }
    }

    private void updatePinToSideBarMenu(Menu menu, Side side) {
        if (menu == null || side == null) {
            return;
        }
        menu.getItems().clear();

        List<DockNode> dockedNodes = collectDockedNodesInMainLayout();
        if (dockedNodes.isEmpty()) {
            MenuItem emptyItem = new MenuItem(tr("demo.menu.layout.emptyDockedNodes"));
            emptyItem.setDisable(true);
            menu.getItems().add(emptyItem);
            return;
        }

        for (DockNode node : dockedNodes) {
            MenuItem item = new MenuItem(node.getTitle());
            item.setGraphic(createMenuItemIcon(node));
            item.setDisable(lockLayoutProperty.get());
            item.setOnAction(e -> pinNodeToSideBar(node, side));
            menu.getItems().add(item);
        }
    }

    private void updatePinnedSideBarMenu(Menu menu, Side side) {
        if (menu == null || side == null) {
            return;
        }

        List<DockNode> pinnedNodes = new ArrayList<>(snapFX.getSideBarNodes(side));
        boolean pinnedOpen = snapFX.isSideBarPinnedOpen(side);
        menu.setText(buildLocalizedSideBarMenuTitle(side, pinnedNodes.size(), pinnedOpen));
        menu.getItems().clear();

        CheckMenuItem pinnedOpenItem = new CheckMenuItem(tr("demo.menu.layout.pinnedOpen"));
        pinnedOpenItem.setSelected(pinnedOpen);
        pinnedOpenItem.setDisable(lockLayoutProperty.get());
        pinnedOpenItem.setOnAction(e -> onSideBarPinnedOpenMenuToggled(side, pinnedOpenItem.isSelected()));
        menu.getItems().add(pinnedOpenItem);

        if (pinnedNodes.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem emptyItem = new MenuItem(tr("demo.menu.layout.emptyPinnedNodes"));
            emptyItem.setDisable(true);
            menu.getItems().add(emptyItem);
            return;
        }

        MenuItem restoreAllItem = new MenuItem(tr("demo.menu.layout.restoreAll"));
        restoreAllItem.setDisable(lockLayoutProperty.get());
        restoreAllItem.setOnAction(e -> restoreAllPinnedNodesFromSideBar(side));
        menu.getItems().add(restoreAllItem);
        menu.getItems().add(new SeparatorMenuItem());

        for (DockNode node : pinnedNodes) {
            MenuItem item = new MenuItem(node.getTitle());
            item.setGraphic(createMenuItemIcon(node));
            item.setDisable(lockLayoutProperty.get());
            item.setOnAction(e -> restorePinnedNodeFromSideBar(node));
            menu.getItems().add(item);
        }
    }

    static String buildSideBarMenuTitle(Side side, int pinnedCount, boolean pinnedOpen) {
        if (side == null) {
            return "Sidebar";
        }
        String sideLabel = switch (side) {
            case LEFT -> "Left";
            case RIGHT -> "Right";
            case TOP -> "Top";
            case BOTTOM -> "Bottom";
        };
        String state = pinnedOpen ? "pinned" : "collapsed";
        return sideLabel + " Sidebar (" + pinnedCount + ", " + state + ")";
    }

    private String buildLocalizedSideBarMenuTitle(Side side, int pinnedCount, boolean pinnedOpen) {
        if (side == null) {
            return tr("demo.settings.label.sidebarMode");
        }
        String state = pinnedOpen ? tr("demo.sidebar.state.pinned") : tr("demo.sidebar.state.collapsed");
        return switch (side) {
            case LEFT -> tr("demo.sidebar.title.left", pinnedCount, state);
            case RIGHT -> tr("demo.sidebar.title.right", pinnedCount, state);
            case TOP -> tr("demo.sidebar.title.top", pinnedCount, state);
            case BOTTOM -> tr("demo.sidebar.title.bottom", pinnedCount, state);
        };
    }

    static String formatDockNodeListLabel(DockNode node) {
        if (node == null) {
            return "(none)";
        }
        String title = node.getTitle() == null || node.getTitle().isBlank() ? "Untitled" : node.getTitle();
        String nodeId = node.getDockNodeId() == null || node.getDockNodeId().isBlank() ? "unknown" : node.getDockNodeId();
        return title + " [" + nodeId + "]";
    }

    private void pinNodeToSideBar(DockNode node, Side side) {
        if (node == null || side == null) {
            return;
        }
        if (snapFX.getSideBarMode() == DockSideBarMode.NEVER) {
            return;
        }
        snapFX.pinToSideBar(node, side);
    }

    private void restorePinnedNodeFromSideBar(DockNode node) {
        if (node == null) {
            return;
        }
        snapFX.restoreFromSideBar(node);
    }

    private void restoreAllPinnedNodesFromSideBar(Side side) {
        if (side == null) {
            return;
        }
        List<DockNode> pinnedNodes = new ArrayList<>(snapFX.getSideBarNodes(side));
        for (DockNode node : pinnedNodes) {
            snapFX.restoreFromSideBar(node);
        }
    }

    private void onSideBarPinnedOpenMenuToggled(Side side, boolean pinnedOpen) {
        if (pinnedOpen) {
            snapFX.pinOpenSideBar(side);
        } else {
            snapFX.collapsePinnedSideBar(side);
        }
    }

    private List<DockNode> collectDockedNodesInMainLayout() {
        List<DockNode> nodes = new ArrayList<>();
        collectDockNodes(snapFX.getDockGraph().getRoot(), nodes);
        return nodes;
    }

    private void refreshSideBarSettingsViews() {
        if (sideBarModeComboBox == null && collapsePinnedOnActiveIconClickCheckBox == null) {
            return;
        }

        updatingSideBarSettingsControls = true;
        try {
            if (sideBarModeComboBox != null) {
                sideBarModeComboBox.setValue(snapFX.getSideBarMode());
            }
            if (collapsePinnedOnActiveIconClickCheckBox != null) {
                collapsePinnedOnActiveIconClickCheckBox.setSelected(snapFX.isCollapsePinnedSideBarOnActiveIconClick());
            }
        } finally {
            updatingSideBarSettingsControls = false;
        }
    }

    private void updateFloatingMenus() {
        updateFloatNodeMenu();
        updateFloatingWindowsMenu();
    }

    private void updateFloatNodeMenu() {
        floatNodeMenu.getItems().clear();
        List<DockNode> nodes = new ArrayList<>();
        collectDockNodes(snapFX.getDockGraph().getRoot(), nodes);

        if (nodes.isEmpty()) {
            MenuItem emptyItem = new MenuItem(tr("demo.menu.layout.emptyDockedNodes"));
            emptyItem.setDisable(true);
            floatNodeMenu.getItems().add(emptyItem);
            return;
        }

        for (DockNode node : nodes) {
            MenuItem item = new MenuItem(node.getTitle());
            item.setGraphic(createMenuItemIcon(node));
            item.setOnAction(e -> snapFX.floatNode(node));
            floatNodeMenu.getItems().add(item);
        }
    }

    private void updateFloatingWindowsMenu() {
        floatingWindowsMenu.getItems().clear();

        if (snapFX.getFloatingWindows().isEmpty()) {
            MenuItem emptyItem = new MenuItem(tr("demo.menu.layout.emptyFloatingWindows"));
            emptyItem.setDisable(true);
            floatingWindowsMenu.getItems().add(emptyItem);
            return;
        }

        MenuItem attachAllItem = new MenuItem(tr("demo.menu.layout.attachAll"));
        attachAllItem.setOnAction(e -> attachAllFloatingWindows());
        floatingWindowsMenu.getItems().add(attachAllItem);
        floatingWindowsMenu.getItems().add(new SeparatorMenuItem());

        for (DockFloatingWindow window : snapFX.getFloatingWindows()) {
            List<DockNode> nodes = window.getDockNodes();
            String label;
            if (nodes.isEmpty()) {
                label = tr("demo.menu.layout.attachFloatingWindow");
            } else if (nodes.size() == 1) {
                label = tr("demo.menu.layout.attachSingle", nodes.getFirst().getTitle());
            } else {
                label = tr("demo.menu.layout.attachMultiple", nodes.getFirst().getTitle(), nodes.size() - 1);
            }
            MenuItem attachItem = new MenuItem(label);
            if (!nodes.isEmpty()) {
                attachItem.setGraphic(createMenuItemIcon(nodes.getFirst()));
            }
            attachItem.setOnAction(e -> snapFX.attachFloatingWindow(window));
            floatingWindowsMenu.getItems().add(attachItem);
        }
    }

    /**
     * Creates a menu icon for the given DockNode by copying its original icon.
     * This ensures that the menu uses a consistent and optimized version of the icon.
     *
     * @param node the DockNode whose icon should be copied for menu use
     * @return a Node containing the copied icon, or null if the node or its icon is null
     */
    private Node createMenuItemIcon(DockNode node) {
        if (node == null) {
            return null;
        }
        return copyMenuIcon(node.getIcon());
    }

    /**
     * Creates a copy of the given image optimized for menu icons.
     * This ensures consistent sizing and performance when used in menu items.
     *
     * @param image the original image to copy
     * @return a new ImageView Node containing the copied image, or null if the input image is null
     */
    static Node copyMenuIcon(Image image) {
        if (image == null) {
            return null;
        }
        ImageView copy = new ImageView(image);
        copy.setFitWidth(16);
        copy.setFitHeight(16);
        copy.setPreserveRatio(true);
        copy.setSmooth(true);
        copy.setCache(true);
        return copy;
    }

    /**
     * Recursively collects all DockNodes in the given element and its children.
     * @param element the root element to start collecting from
     * @param nodes the list to populate with found DockNodes
     */
    private void collectDockNodes(DockElement element, List<DockNode> nodes) {
        if (element instanceof DockNode node) {
            nodes.add(node);
        } else if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                collectDockNodes(child, nodes);
            }
        }
    }

    private void refreshLocalizedNodeTitles() {
        List<DockNode> nodes = collectAllDemoNodes();
        for (DockNode node : nodes) {
            refreshLocalizedNodeTitle(node);
        }
    }

    private List<DockNode> collectAllDemoNodes() {
        List<DockNode> nodes = new ArrayList<>();
        collectDockNodes(snapFX.getDockGraph().getRoot(), nodes);
        for (DockFloatingWindow floatingWindow : snapFX.getFloatingWindows()) {
            nodes.addAll(floatingWindow.getDockNodes());
        }
        nodes.addAll(snapFX.getHiddenNodes());
        nodes.addAll(snapFX.getSideBarNodes(Side.LEFT));
        nodes.addAll(snapFX.getSideBarNodes(Side.RIGHT));
        return nodes;
    }

    private void refreshLocalizedNodeTitle(DockNode node) {
        if (node == null) {
            return;
        }
        DockNodeType type = DockNodeType.fromId(node.getDockNodeId());
        if (type == null) {
            return;
        }
        String titleKey = type.getDefaultTitleKey();
        if (titleKey == null || titleKey.isBlank()) {
            return;
        }
        if (type == DockNodeType.GENERIC_PANEL
            && !demoTextLocalization.matchesInSupportedLocales(titleKey, stripDirtySuffix(node.getTitle()))) {
            return;
        }
        node.setTitle(tr(titleKey));
    }

    /**
     * Creates the toolbar with lock toggle and add-node buttons.
     */
    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar();

        CheckBox lockCheckBox = new CheckBox();
        demoTextLocalization.bind(lockCheckBox, "demo.toolbar.lock");
        lockCheckBox.selectedProperty().bindBidirectional(lockLayoutProperty);

        Separator sep1 = new Separator();

        Label addLabel = new Label();
        demoTextLocalization.bind(addLabel, "demo.toolbar.add");
        addLabel.setStyle(FX_FONT_WEIGHT_BOLD);

        Button addEditorBtn = new Button();
        demoTextLocalization.bind(addEditorBtn, "demo.toolbar.addEditor");
        addEditorBtn.setGraphic(IconUtil.loadIcon("document--pencil.png"));
        addEditorBtn.setOnAction(e -> addNewEditorNode());

        Button addPropsBtn = new Button();
        demoTextLocalization.bind(addPropsBtn, "demo.toolbar.addProperties");
        addPropsBtn.setGraphic(IconUtil.loadIcon("property.png"));
        addPropsBtn.setOnAction(e -> addNewPropertiesNode());

        Button addConsoleBtn = new Button();
        demoTextLocalization.bind(addConsoleBtn, "demo.toolbar.addConsole");
        addConsoleBtn.setGraphic(IconUtil.loadIcon("terminal.png"));
        addConsoleBtn.setOnAction(e -> addNewConsoleNode());

        Button addGenericBtn = new Button();
        demoTextLocalization.bind(addGenericBtn, "demo.toolbar.addPanel");
        addGenericBtn.setGraphic(IconUtil.loadIcon("plus.png"));
        addGenericBtn.setOnAction(e -> addNewGenericPanelNode());

        toolbar.getItems().addAll(
            lockCheckBox,
            sep1,
            addLabel,
            addEditorBtn,
            addPropsBtn,
            addConsoleBtn,
            addGenericBtn
        );

        return toolbar;
    }

    /**
     * Adds a DockNode to the right side of the current layout.
     */
    private void addNewEditorNode() {
        DockNode node = demoNodeFactory.createUntitledEditorNode();
        addDockNode(node);
    }

    /**
     * Adds a new Properties panel node to the right side of the current layout.
     */
    private void addNewPropertiesNode() {
        DockNode node = demoNodeFactory.createPropertiesPanelNode();
        addDockNode(node);
    }

    /**
     * Adds a new Console panel node to the right side of the current layout.
     */
    private void addNewConsoleNode() {
        DockNode node = demoNodeFactory.createConsolePanelNode();
        addDockNode(node);
    }

    /**
     * Adds a new generic panel node with an auto-generated name to the right side of the current layout.
     */
    private void addNewGenericPanelNode() {
        String name = tr("demo.node.genericPanel.title") + "_" + (snapFX.getDockNodeCount(DockNodeType.GENERIC_PANEL.getId()) + 1);
        DockNode node = demoNodeFactory.createGenericPanelNode(name);
        addDockNode(node);
    }

    /**
     * Adds the given DockNode to the right side of the current layout and registers it if it's an editor.
     * If there is no existing layout, sets the node as the root.
     *
     * @param node the DockNode to add
     */
    private void addDockNode(DockNode node) {
        registerEditorNode(node);
        if (snapFX.getDockGraph().getRoot() == null) {
            snapFX.getDockGraph().setRoot(node);
        } else {
            snapFX.dock(node, snapFX.getDockGraph().getRoot(), DockPosition.RIGHT);
        }
    }

    /**
     * Set up the node factory for proper save/load support across sessions.
     * The factory creates nodes based on their ID.
     */
    private void setupNodeFactory() {
        snapFX.setNodeFactory(demoNodeFactory);
    }

    private void attachAllFloatingWindows() {
        List<DockFloatingWindow> windows = new ArrayList<>(snapFX.getFloatingWindows());
        for (DockFloatingWindow window : windows) {
            snapFX.attachFloatingWindow(window);
        }
    }

    private void onTitleBarModeChanged(DockTitleBarMode mode) {
        if (mode != null) {
            snapFX.setTitleBarMode(mode);
        }
    }

    private void onCloseButtonModeChanged(DockCloseButtonMode mode) {
        if (mode != null) {
            snapFX.setCloseButtonMode(mode);
        }
    }

    private void onSideBarModeChanged(DockSideBarMode mode) {
        if (mode == null || updatingSideBarSettingsControls) {
            return;
        }
        snapFX.setSideBarMode(mode);
        updateSideBarMenus();
        refreshSideBarSettingsViews();
    }

    private void onDropVisualizationModeChanged(DockDropVisualizationMode mode) {
        if (mode != null) {
            snapFX.setDropVisualizationMode(mode);
        }
    }

    private void onFloatingPinButtonModeChanged(DockFloatingPinButtonMode mode) {
        if (mode != null) {
            snapFX.setFloatingPinButtonMode(mode);
        }
    }

    private void onFloatingPinLockedBehaviorChanged(DockFloatingPinLockedBehavior behavior) {
        if (behavior != null) {
            snapFX.setFloatingPinLockedBehavior(behavior);
        }
    }

    private void onFloatingWindowSnappingEnabledChanged(Boolean enabled) {
        if (enabled != null) {
            snapFX.setFloatingWindowSnappingEnabled(enabled);
        }
    }

    private void onFloatingWindowSnapDistanceChanged(Double distance) {
        if (distance != null) {
            snapFX.setFloatingWindowSnapDistance(distance);
        }
    }

    private void onFloatingWindowSnapTargetsChanged(boolean screenEnabled, boolean mainWindowEnabled, boolean floatingWindowsEnabled) {
        snapFX.setFloatingWindowSnapTargets(
            resolveFloatingWindowSnapTargets(screenEnabled, mainWindowEnabled, floatingWindowsEnabled)
        );
    }

    private void onThemeChanged(String themeName) {
        if (themeName == null || themeName.isBlank() || updatingThemeSettingsControls) {
            return;
        }
        applyThemeSelection();
    }

    private void onThemeSourceChanged(DemoThemeSource source) {
        if (source == null || themeComboBox == null || updatingThemeSettingsControls) {
            return;
        }
        updatingThemeSettingsControls = true;
        try {
            if (source == DemoThemeSource.INTERNAL) {
                themeComboBox.getItems().setAll(SnapFX.getAvailableThemeNames());
                themeComboBox.setValue(resolveInternalThemeSelection());
            } else {
                themeComboBox.getItems().setAll(getAtlantaFxThemeStylesheets().keySet());
                themeComboBox.setValue(resolveAtlantaFxThemeSelection());
            }
        } finally {
            updatingThemeSettingsControls = false;
        }
        applyThemeSelection();
    }

    private void applyThemeSelection() {
        if (snapFX == null || themeSourceComboBox == null || themeComboBox == null) {
            return;
        }
        DemoThemeSource source = themeSourceComboBox.getValue();
        String selectedTheme = themeComboBox.getValue();
        if (source == null || selectedTheme == null || selectedTheme.isBlank()) {
            return;
        }
        if (source == DemoThemeSource.INTERNAL) {
            String stylesheetPath = getNamedThemeStylesheets().get(selectedTheme);
            if (stylesheetPath == null || stylesheetPath.isBlank()) {
                return;
            }
            snapFX.setThemeStylesheet(stylesheetPath);
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
            snapFX.setUserAgentThemeMode(DockUserAgentThemeMode.MODENA);
            snapFX.refreshUserAgentThemeIntegration();
            return;
        }
        Theme atlantaFxTheme = ATLANTAFX_THEMES.get(selectedTheme);
        if (atlantaFxTheme == null) {
            return;
        }
        String defaultThemeStylesheet = getNamedThemeStylesheets().get(SnapFX.getDefaultThemeName());
        if (defaultThemeStylesheet != null && !defaultThemeStylesheet.isBlank()) {
            snapFX.setThemeStylesheet(defaultThemeStylesheet);
        }
        Application.setUserAgentStylesheet(atlantaFxTheme.getUserAgentStylesheet());
        snapFX.setUserAgentThemeMode(DockUserAgentThemeMode.ATLANTAFX_COMPAT);
        snapFX.refreshUserAgentThemeIntegration();
    }

    private String resolveInternalThemeSelection() {
        String resolvedThemeName = resolveThemeNameByStylesheetPath(snapFX.getThemeStylesheetResourcePath());
        if (SnapFX.getAvailableThemeNames().contains(resolvedThemeName)) {
            return resolvedThemeName;
        }
        if (SnapFX.getAvailableThemeNames().isEmpty()) {
            return null;
        }
        return SnapFX.getAvailableThemeNames().getFirst();
    }

    private String resolveAtlantaFxThemeSelection() {
        String resolvedThemeName = resolveAtlantaFxThemeNameByUserAgentStylesheet(Application.getUserAgentStylesheet());
        if (resolvedThemeName != null && ATLANTAFX_THEMES.containsKey(resolvedThemeName)) {
            return resolvedThemeName;
        }
        if (ATLANTAFX_THEMES.isEmpty()) {
            return null;
        }
        return ATLANTAFX_THEMES.keySet().stream().findFirst().orElse(null);
    }

    private DemoThemeSource resolveThemeSourceFromRuntime() {
        return resolveAtlantaFxThemeNameByUserAgentStylesheet(Application.getUserAgentStylesheet()) == null
            ? DemoThemeSource.INTERNAL
            : DemoThemeSource.ATLANTAFX;
    }

    private void syncThemeSettingsControlsFromRuntime() {
        if (themeSourceComboBox == null || themeComboBox == null) {
            return;
        }
        updatingThemeSettingsControls = true;
        try {
            DemoThemeSource source = resolveThemeSourceFromRuntime();
            themeSourceComboBox.setValue(source);
            if (source == DemoThemeSource.INTERNAL) {
                themeComboBox.getItems().setAll(SnapFX.getAvailableThemeNames());
                themeComboBox.setValue(resolveInternalThemeSelection());
                return;
            }
            themeComboBox.getItems().setAll(getAtlantaFxThemeStylesheets().keySet());
            themeComboBox.setValue(resolveAtlantaFxThemeSelection());
        } finally {
            updatingThemeSettingsControls = false;
        }
    }

    private StringConverter<DemoThemeSource> createThemeSourceDisplayConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(DemoThemeSource source) {
                if (source == null) {
                    return "";
                }
                return switch (source) {
                    case INTERNAL -> tr("demo.settings.value.themeSourceInternal");
                    case ATLANTAFX -> tr("demo.settings.value.themeSourceAtlantaFx");
                };
            }

            @Override
            public DemoThemeSource fromString(String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }
                String internalLabel = tr("demo.settings.value.themeSourceInternal");
                if (internalLabel.equals(value)) {
                    return DemoThemeSource.INTERNAL;
                }
                String atlantaFxLabel = tr("demo.settings.value.themeSourceAtlantaFx");
                if (atlantaFxLabel.equals(value)) {
                    return DemoThemeSource.ATLANTAFX;
                }
                return themeSourceComboBox == null ? null : themeSourceComboBox.getValue();
            }
        };
    }

    private void onLocaleSelectionChanged(Locale locale) {
        if (locale == null) {
            return;
        }
        applyLocalizationSelection();
    }

    private void applyLocalizationSelection() {
        if (snapFX == null) {
            return;
        }
        Locale selectedLocale = localeComboBox == null || localeComboBox.getValue() == null
            ? SnapFX.getDefaultLocale()
            : localeComboBox.getValue();
        DockLocalizationProvider selectedProvider = selectedLocale != null
            && EXTENDED_DEMO_LOCALE.getLanguage().equals(selectedLocale.getLanguage())
            ? demoLocalizationProvider
            : null;

        demoTextLocalization.setLocale(selectedLocale);
        snapFX.setLocale(selectedLocale);
        snapFX.setLocalizationProvider(selectedProvider);

        if (debugView != null) {
            debugView.setLocale(selectedLocale);
            debugView.setLocalizationProvider(selectedProvider);
        }
        if (debugOverlay != null) {
            debugOverlay.setLocale(selectedLocale);
            debugOverlay.setLocalizationProvider(selectedProvider);
        }
        refreshDemoLocalization();
    }

    private List<Locale> buildDemoLocaleOptions() {
        List<Locale> locales = new ArrayList<>(SnapFX.getAvailableLocales());
        if (!locales.contains(EXTENDED_DEMO_LOCALE)) {
            locales.add(EXTENDED_DEMO_LOCALE);
        }
        return locales;
    }

    private StringConverter<Locale> createLocaleDisplayConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Locale locale) {
                if (locale == null) {
                    return "";
                }
                String displayLanguage = locale.getDisplayLanguage(locale);
                if (displayLanguage == null || displayLanguage.isBlank()) {
                    displayLanguage = locale.toLanguageTag();
                }
                return displayLanguage + " (" + locale.toLanguageTag() + ")";
            }

            @Override
            public Locale fromString(String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }
                return localeComboBox == null ? null : localeComboBox.getValue();
            }
        };
    }

    private void refreshDemoLocalization() {
        if (themeSourceComboBox != null) {
            boolean previousUpdatingThemeSettingsControls = updatingThemeSettingsControls;
            updatingThemeSettingsControls = true;
            try {
                DemoThemeSource selectedThemeSource = themeSourceComboBox.getValue();
                themeSourceComboBox.setConverter(createThemeSourceDisplayConverter());
                if (selectedThemeSource != null) {
                    themeSourceComboBox.getSelectionModel().select(selectedThemeSource);
                }
            } finally {
                updatingThemeSettingsControls = previousUpdatingThemeSettingsControls;
            }
        }
        refreshLocalizedNodeTitles();
        updateUntitledEditorTitles();
        if (hiddenWindowsMenu != null) {
            updateHiddenWindowsMenu();
        }
        if (pinLeftSideBarMenu != null || pinRightSideBarMenu != null || leftSideBarMenu != null || rightSideBarMenu != null) {
            updateSideBarMenus();
        }
        if (floatNodeMenu != null && floatingWindowsMenu != null) {
            updateFloatingMenus();
        }
    }

    static EnumSet<DockFloatingSnapTarget> resolveFloatingWindowSnapTargets(
        boolean screenEnabled,
        boolean mainWindowEnabled,
        boolean floatingWindowsEnabled
    ) {
        EnumSet<DockFloatingSnapTarget> targets = EnumSet.noneOf(DockFloatingSnapTarget.class);
        if (screenEnabled) {
            targets.add(DockFloatingSnapTarget.SCREEN);
        }
        if (mainWindowEnabled) {
            targets.add(DockFloatingSnapTarget.MAIN_WINDOW);
        }
        if (floatingWindowsEnabled) {
            targets.add(DockFloatingSnapTarget.FLOATING_WINDOWS);
        }
        return targets;
    }

    /**
     * Creates the demo layout with fixed node IDs for persistence.
     */
    private void createDemoLayout() {
        clearEditorRegistry();

        // 1. Project Explorer (left)
        DockNode projectNode = demoNodeFactory.createProjectExplorerNode();
        snapFX.getDockGraph().setRoot(projectNode);

        // 2. Main Editor (center)
        DockNode editorNode = demoNodeFactory.createMainEditorNode();
        snapFX.getDockGraph().dock(editorNode, projectNode, DockPosition.RIGHT);

        // 3. Properties (right)
        DockNode propertiesNode = demoNodeFactory.createPropertiesNode();
        snapFX.getDockGraph().dock(propertiesNode, editorNode, DockPosition.RIGHT);

        // 4. Console (bottom)
        DockNode consoleNode = demoNodeFactory.createConsoleNode();
        snapFX.getDockGraph().dock(consoleNode, editorNode, DockPosition.BOTTOM);

        // 5. Tasks (as tab next to Console)
        DockNode tasksNode = demoNodeFactory.createTasksNode();
        snapFX.getDockGraph().dock(tasksNode, consoleNode, DockPosition.CENTER);
        snapFX.setRootSplitRatios(25, 50, 25);

        registerEditorNode(editorNode);
    }

    private void resetLayoutToDefault() {
        boolean wasLocked = snapFX.isLocked();
        if (wasLocked) {
            snapFX.setLocked(false);
        }
        snapFX.closeFloatingWindows(false);
        snapFX.getHiddenNodes().clear();
        snapFX.getDockGraph().clearSideBars();
        createDemoLayout();
        updateDockLayout();
        updateHiddenWindowsMenu();
        updateSideBarMenus();
        refreshSideBarSettingsViews();
        updateFloatingMenus();
        if (wasLocked) {
            snapFX.setLocked(true);
        }
    }

    private void installDebugPanel() {
        // Get the current dock layout from mainLayout
        Node dockLayout = mainLayout.getCenter();
        dockLayoutHost = new StackPane();
        if (dockLayout != null) {
            dockLayoutHost.getChildren().setAll(dockLayout);
        }

        debugView = new DockGraphDebugView(snapFX.getDockGraph(), snapFX.getDragService());
        debugView.setPrefWidth(420);

        // Enable auto-export by default
        debugView.setAutoExportOnDrop(true);

        TabPane debugTabs = new TabPane();
        debugTabs.setPrefWidth(420);
        debugTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab debugTab = new Tab(null, debugView);
        demoTextLocalization.bind(debugTab, "demo.tab.debug");
        debugTab.setGraphic(IconUtil.loadIcon("bug.png"));
        Tab settingsTab = new Tab(null, createSettingsPanel());
        demoTextLocalization.bind(settingsTab, "demo.tab.settings");
        settingsTab.setGraphic(IconUtil.loadIcon("hammer-screwdriver.png"));
        debugTabs.getTabs().addAll(settingsTab, debugTab);

        // Create split pane with dock layout on left and debug view on right
        mainSplit = new SplitPane();
        mainSplit.getItems().addAll(dockLayoutHost, debugTabs);
        mainSplit.setDividerPositions(0.72);

        if (ENABLE_DOCK_DEBUG_HUD) {
            // Local demo HUD for D&D diagnostics.
            debugOverlay = new DockDebugOverlay(snapFX.getDockGraph(), snapFX.getDragService());
            StackPane stack = new StackPane(mainSplit, debugOverlay);
            StackPane.setAlignment(debugOverlay, Pos.TOP_LEFT);
            StackPane.setMargin(debugOverlay, new Insets(10));
            mainLayout.setCenter(stack);
        } else {
            debugOverlay = null;
            mainLayout.setCenter(mainSplit);
        }
        // Rebuild debug tree when layout is rebuilt
        debugView.rebuildTree();

        // Expand debug tree by default
        debugView.expandAll();
    }

    private Parent createSettingsPanel() {
        GridPane appearanceGrid = createSettingsGrid();
        themeSourceComboBox = new ComboBox<>();
        themeSourceComboBox.getItems().setAll(DemoThemeSource.values());
        themeSourceComboBox.setConverter(createThemeSourceDisplayConverter());
        configureSettingsValueNode(themeSourceComboBox);
        themeSourceComboBox.valueProperty().addListener((obs, oldVal, newVal) -> onThemeSourceChanged(newVal));
        addLocalizedSettingsRow(appearanceGrid, 0, "demo.settings.label.themeSource", themeSourceComboBox);

        themeComboBox = new ComboBox<>();
        configureSettingsValueNode(themeComboBox);
        themeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> onThemeChanged(newVal));
        addLocalizedSettingsRow(appearanceGrid, 1, "demo.settings.label.theme", themeComboBox);

        localeComboBox = new ComboBox<>();
        localeComboBox.getItems().setAll(buildDemoLocaleOptions());
        localeComboBox.setConverter(createLocaleDisplayConverter());
        configureSettingsValueNode(localeComboBox);
        localeComboBox.setValue(snapFX.getLocale());
        localeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> onLocaleSelectionChanged(newVal));
        addLocalizedSettingsRow(appearanceGrid, 2, "demo.settings.label.frameworkLocale", localeComboBox);
        syncThemeSettingsControlsFromRuntime();
        VBox appearanceSection = createLocalizedSettingsSection("demo.settings.section.appearance", appearanceGrid);

        GridPane layoutGrid = createSettingsGrid();
        ComboBox<DockTitleBarMode> titleBarMode = new ComboBox<>();
        titleBarMode.getItems().setAll(DockTitleBarMode.values());
        configureSettingsValueNode(titleBarMode);
        titleBarMode.setValue(snapFX.getTitleBarMode());
        titleBarMode.valueProperty().addListener((obs, oldVal, newVal) -> onTitleBarModeChanged(newVal));
        addLocalizedSettingsRow(layoutGrid, 0, "demo.settings.label.titleBarMode", titleBarMode);

        ComboBox<DockCloseButtonMode> closeButtonMode = new ComboBox<>();
        closeButtonMode.getItems().setAll(DockCloseButtonMode.values());
        configureSettingsValueNode(closeButtonMode);
        closeButtonMode.setValue(snapFX.getCloseButtonMode());
        closeButtonMode.valueProperty().addListener((obs, oldVal, newVal) -> onCloseButtonModeChanged(newVal));
        addLocalizedSettingsRow(layoutGrid, 1, "demo.settings.label.closeButtonMode", closeButtonMode);

        ComboBox<DockDropVisualizationMode> dropMode = new ComboBox<>();
        dropMode.getItems().setAll(DockDropVisualizationMode.values());
        configureSettingsValueNode(dropMode);
        dropMode.setValue(snapFX.getDropVisualizationMode());
        dropMode.valueProperty().addListener((obs, oldVal, newVal) -> onDropVisualizationModeChanged(newVal));
        addLocalizedSettingsRow(layoutGrid, 2, "demo.settings.label.dropVisualization", dropMode);

        CheckBox lockCheckBox = new CheckBox();
        demoTextLocalization.bind(lockCheckBox, "demo.settings.value.locked");
        configureSettingsValueNode(lockCheckBox);
        lockCheckBox.selectedProperty().bindBidirectional(lockLayoutProperty);
        addLocalizedSettingsRow(layoutGrid, 3, "demo.settings.label.layoutLock", lockCheckBox);
        VBox layoutSection = createLocalizedSettingsSection("demo.settings.section.layout", layoutGrid);

        GridPane floatingGrid = createSettingsGrid();
        ComboBox<DockFloatingPinButtonMode> pinButtonMode = new ComboBox<>();
        pinButtonMode.getItems().setAll(DockFloatingPinButtonMode.values());
        configureSettingsValueNode(pinButtonMode);
        pinButtonMode.setValue(snapFX.getFloatingPinButtonMode());
        pinButtonMode.valueProperty().addListener((obs, oldVal, newVal) -> onFloatingPinButtonModeChanged(newVal));
        addLocalizedSettingsRow(floatingGrid, 0, "demo.settings.label.floatingPinButton", pinButtonMode);

        CheckBox allowPinToggleCheckBox = new CheckBox();
        demoTextLocalization.bind(allowPinToggleCheckBox, "demo.settings.value.allowPinToggle");
        configureSettingsValueNode(allowPinToggleCheckBox);
        allowPinToggleCheckBox.setSelected(snapFX.isAllowFloatingPinToggle());
        allowPinToggleCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            snapFX.setAllowFloatingPinToggle(Boolean.TRUE.equals(newVal))
        );
        addLocalizedSettingsRow(floatingGrid, 1, "demo.settings.label.floatingPinToggle", allowPinToggleCheckBox);

        CheckBox defaultPinnedCheckBox = new CheckBox();
        demoTextLocalization.bind(defaultPinnedCheckBox, "demo.settings.value.defaultPinned");
        configureSettingsValueNode(defaultPinnedCheckBox);
        defaultPinnedCheckBox.setSelected(snapFX.isDefaultFloatingAlwaysOnTop());
        defaultPinnedCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            snapFX.setDefaultFloatingAlwaysOnTop(Boolean.TRUE.equals(newVal))
        );
        addLocalizedSettingsRow(floatingGrid, 2, "demo.settings.label.defaultPinned", defaultPinnedCheckBox);

        ComboBox<DockFloatingPinLockedBehavior> pinLockedBehavior = new ComboBox<>();
        pinLockedBehavior.getItems().setAll(DockFloatingPinLockedBehavior.values());
        configureSettingsValueNode(pinLockedBehavior);
        pinLockedBehavior.setValue(snapFX.getFloatingPinLockedBehavior());
        pinLockedBehavior.valueProperty().addListener((obs, oldVal, newVal) -> onFloatingPinLockedBehaviorChanged(newVal));
        addLocalizedSettingsRow(floatingGrid, 3, "demo.settings.label.pinInLockMode", pinLockedBehavior);

        CheckBox floatingSnappingCheckBox = new CheckBox();
        demoTextLocalization.bind(floatingSnappingCheckBox, "demo.settings.value.enableFloatingSnapping");
        configureSettingsValueNode(floatingSnappingCheckBox);
        floatingSnappingCheckBox.setSelected(snapFX.isFloatingWindowSnappingEnabled());
        floatingSnappingCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            onFloatingWindowSnappingEnabledChanged(newVal)
        );
        addLocalizedSettingsRow(floatingGrid, 4, "demo.settings.label.floatingSnapping", floatingSnappingCheckBox);

        Spinner<Double> snapDistanceSpinner = new Spinner<>();
        snapDistanceSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 64.0, snapFX.getFloatingWindowSnapDistance(), 1.0)
        );
        snapDistanceSpinner.setEditable(true);
        configureSettingsValueNode(snapDistanceSpinner);
        snapDistanceSpinner.valueProperty().addListener((obs, oldVal, newVal) -> onFloatingWindowSnapDistanceChanged(newVal));
        addLocalizedSettingsRow(floatingGrid, 5, "demo.settings.label.snapDistance", snapDistanceSpinner);

        CheckBox screenSnapTargetCheckBox = new CheckBox();
        demoTextLocalization.bind(screenSnapTargetCheckBox, "demo.settings.value.snapTargetScreen");
        CheckBox mainWindowSnapTargetCheckBox = new CheckBox();
        demoTextLocalization.bind(mainWindowSnapTargetCheckBox, "demo.settings.value.snapTargetMainWindow");
        CheckBox floatingWindowsSnapTargetCheckBox = new CheckBox();
        demoTextLocalization.bind(floatingWindowsSnapTargetCheckBox, "demo.settings.value.snapTargetFloating");
        Tooltip floatingWindowsTooltip = new Tooltip();
        demoTextLocalization.bind(floatingWindowsTooltip, "demo.settings.tooltip.snapTargetFloating");
        floatingWindowsSnapTargetCheckBox.setTooltip(floatingWindowsTooltip);

        var configuredSnapTargets = snapFX.getFloatingWindowSnapTargets();
        screenSnapTargetCheckBox.setSelected(configuredSnapTargets.contains(DockFloatingSnapTarget.SCREEN));
        mainWindowSnapTargetCheckBox.setSelected(configuredSnapTargets.contains(DockFloatingSnapTarget.MAIN_WINDOW));
        floatingWindowsSnapTargetCheckBox.setSelected(configuredSnapTargets.contains(DockFloatingSnapTarget.FLOATING_WINDOWS));
        screenSnapTargetCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            onFloatingWindowSnapTargetsChanged(
                screenSnapTargetCheckBox.isSelected(),
                mainWindowSnapTargetCheckBox.isSelected(),
                floatingWindowsSnapTargetCheckBox.isSelected()
            )
        );
        mainWindowSnapTargetCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            onFloatingWindowSnapTargetsChanged(
                screenSnapTargetCheckBox.isSelected(),
                mainWindowSnapTargetCheckBox.isSelected(),
                floatingWindowsSnapTargetCheckBox.isSelected()
            )
        );
        floatingWindowsSnapTargetCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            onFloatingWindowSnapTargetsChanged(
                screenSnapTargetCheckBox.isSelected(),
                mainWindowSnapTargetCheckBox.isSelected(),
                floatingWindowsSnapTargetCheckBox.isSelected()
            )
        );
        HBox snapTargetsBox = new HBox(8, screenSnapTargetCheckBox, mainWindowSnapTargetCheckBox, floatingWindowsSnapTargetCheckBox);
        snapTargetsBox.setAlignment(Pos.CENTER_LEFT);
        configureSettingsValueNode(snapTargetsBox);
        addLocalizedSettingsRow(floatingGrid, 6, "demo.settings.label.snapTargets", snapTargetsBox);
        VBox floatingSection = createLocalizedSettingsSection("demo.settings.section.floating", floatingGrid);

        GridPane editorGrid = createSettingsGrid();
        CheckBox promptEditorCloseCheckBox = new CheckBox();
        demoTextLocalization.bind(promptEditorCloseCheckBox, "demo.settings.value.promptUnsavedEditors");
        configureSettingsValueNode(promptEditorCloseCheckBox);
        promptEditorCloseCheckBox.selectedProperty().bindBidirectional(promptOnEditorCloseProperty);
        addLocalizedSettingsRow(editorGrid, 0, "demo.settings.label.closeHook", promptEditorCloseCheckBox);
        VBox editorSection = createLocalizedSettingsSection("demo.settings.section.editors", editorGrid);

        VBox sideBarSection = createSideBarSettingsSection();
        Label hint = new Label();
        demoTextLocalization.bind(hint, "demo.settings.hint");
        hint.setWrapText(true);

        VBox panelContent = new VBox(12,  appearanceSection, layoutSection, floatingSection, sideBarSection, editorSection, hint);
        panelContent.setPadding(new Insets(10));
        panelContent.setFillWidth(true);
        panelContent.setMinWidth(SETTINGS_SECTION_MIN_WIDTH);

        ScrollPane settingsScrollPane = new ScrollPane(panelContent);
        settingsScrollPane.setFitToWidth(true);
        settingsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        settingsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return settingsScrollPane;
    }

    private VBox createSettingsSection(String sectionTitle, Node sectionContent) {
        Label sectionHeader = new Label(sectionTitle);
        sectionHeader.setStyle(FX_FONT_WEIGHT_BOLD);
        VBox section = new VBox(8, sectionHeader, sectionContent);
        section.setMinWidth(SETTINGS_SECTION_MIN_WIDTH);
        return section;
    }

    private VBox createLocalizedSettingsSection(String sectionTitleKey, Node sectionContent) {
        Label sectionHeader = new Label();
        demoTextLocalization.bind(sectionHeader, sectionTitleKey);
        sectionHeader.setStyle(FX_FONT_WEIGHT_BOLD);
        VBox section = new VBox(8, sectionHeader, sectionContent);
        section.setMinWidth(SETTINGS_SECTION_MIN_WIDTH);
        return section;
    }

    private GridPane createSettingsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(SETTINGS_LABEL_MIN_WIDTH);
        ColumnConstraints controlColumn = new ColumnConstraints();
        controlColumn.setMinWidth(SETTINGS_CONTROL_MIN_WIDTH);
        controlColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, controlColumn);
        grid.setMinWidth(SETTINGS_SECTION_MIN_WIDTH);
        return grid;
    }

    private void addSettingsRow(GridPane grid, int rowIndex, String labelText, Node valueNode) {
        Label label = new Label(labelText);
        label.setWrapText(true);
        label.setMinWidth(SETTINGS_LABEL_MIN_WIDTH);
        grid.addRow(rowIndex, label, valueNode);
        if (valueNode instanceof Region region) {
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
    }

    private void addLocalizedSettingsRow(GridPane grid, int rowIndex, String labelKey, Node valueNode) {
        Label label = new Label();
        demoTextLocalization.bind(label, labelKey);
        label.setWrapText(true);
        label.setMinWidth(SETTINGS_LABEL_MIN_WIDTH);
        grid.addRow(rowIndex, label, valueNode);
        if (valueNode instanceof Region region) {
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
    }

    private void configureSettingsValueNode(Region valueNode) {
        valueNode.setMinWidth(SETTINGS_CONTROL_MIN_WIDTH);
        valueNode.setMaxWidth(Double.MAX_VALUE);
    }

    private VBox createSideBarSettingsSection() {

        GridPane sideBarGrid = createSettingsGrid();

        sideBarModeComboBox = new ComboBox<>();
        sideBarModeComboBox.getItems().setAll(DockSideBarMode.values());
        sideBarModeComboBox.setValue(snapFX.getSideBarMode());
        configureSettingsValueNode(sideBarModeComboBox);
        sideBarModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> onSideBarModeChanged(newVal));

        addLocalizedSettingsRow(sideBarGrid, 0, "demo.settings.label.sidebarMode", sideBarModeComboBox);

        collapsePinnedOnActiveIconClickCheckBox = new CheckBox();
        demoTextLocalization.bind(collapsePinnedOnActiveIconClickCheckBox, "demo.settings.value.collapsePinned");
        configureSettingsValueNode(collapsePinnedOnActiveIconClickCheckBox);
        collapsePinnedOnActiveIconClickCheckBox.setSelected(snapFX.isCollapsePinnedSideBarOnActiveIconClick());
        collapsePinnedOnActiveIconClickCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingSideBarSettingsControls || newVal == null) {
                return;
            }
            snapFX.setCollapsePinnedSideBarOnActiveIconClick(Boolean.TRUE.equals(newVal));
        });

        addLocalizedSettingsRow(sideBarGrid, 1, "demo.settings.label.collapsePinned", collapsePinnedOnActiveIconClickCheckBox);

        refreshSideBarSettingsViews();
        return createLocalizedSettingsSection("demo.settings.section.sideBars", sideBarGrid);
    }

    private void updateDockLayout() {
        Parent dockLayout = snapFX.buildLayout();

        // If we have a split pane (with debug panel), update only the dock layout part
        if (mainSplit != null && !mainSplit.getItems().isEmpty()) {
            if (dockLayoutHost == null) {
                dockLayoutHost = new StackPane();
                if (!mainSplit.getItems().isEmpty()) {
                    mainSplit.getItems().set(0, dockLayoutHost);
                }
            }
            dockLayoutHost.getChildren().setAll(dockLayout);
        } else {
            // No debug panel, just set dock layout directly
            mainLayout.setCenter(dockLayout);
        }
    }

    private void saveLayout() {
        FileChooser fileChooser = createLayoutSaveFileChooser();

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                String json = snapFX.saveLayout();
                Files.writeString(file.toPath(), json);
                // Success - no popup needed for better UX
            } catch (IOException e) {
                showError(tr("demo.error.saveFile", e.getMessage()));
            }
        }
    }

    private void loadLayout() {
        FileChooser fileChooser = createLayoutLoadFileChooser();

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                snapFX.loadLayout(json);
                updateDockLayout();
                rebuildEditorRegistryFromCurrentLayout();
                refreshDemoLocalization();

                // Synchronize lock state from loaded layout
                lockLayoutProperty.set(snapFX.isLocked());
                updateSideBarMenus();
                refreshSideBarSettingsViews();

                // Success - no popup needed for better UX
            } catch (DockLayoutLoadException e) {
                LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Layout load failed: {0}",
                    e.toDisplayMessage()
                );
                showError(buildLocalizedLayoutLoadErrorMessage(e));
            } catch (IOException e) {
                showError(tr("demo.dialog.layoutLoadError", e.getMessage()));
            }
        }
    }

    private EditorCloseDecisionPolicy createEditorCloseDecisionPolicy() {
        return new EditorCloseDecisionPolicy(
            this::shouldPromptBeforeClose,
            this::promptSaveBeforeClose,
            this::saveEditorNodeForClose
        );
    }

    private DockCloseDecision handleCloseRequest(DockCloseRequest request) {
        if (editorCloseDecisionPolicy == null) {
            return DockCloseDecision.DEFAULT;
        }
        return editorCloseDecisionPolicy.resolve(request);
    }

    private void handleCloseHandled(DockCloseResult result) {
        if (result == null || result.request() == null || result.canceled()) {
            return;
        }
        if (result.appliedBehavior() != DockCloseBehavior.REMOVE) {
            return;
        }
        for (DockNode node : result.request().nodes()) {
            removeEditorNodeState(node);
        }
    }

    private void handleFloatingPinChanged(DockFloatingPinChangeEvent event) {
        if (event == null) {
            return;
        }
        LOGGER.log(
            System.Logger.Level.INFO,
            "Floating pin changed: window={0}, source={1}, alwaysOnTop={2}",
            event.window().getId(),
            event.source(),
            event.alwaysOnTop()
        );
    }

    private boolean shouldPromptBeforeClose(DockNode node) {
        if (!promptOnEditorCloseProperty.get()) {
            return false;
        }
        EditorDocumentState state = editorDocumentStates.get(node);
        return state != null && state.dirty;
    }

    private EditorCloseDecisionPolicy.SavePromptResult promptSaveBeforeClose(DockNode node) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(primaryStage);
        alert.setTitle(tr("demo.dialog.unsaved.title"));
        alert.setHeaderText(tr("demo.dialog.unsaved.header", node.getTitle().replace(DIRTY_TITLE_SUFFIX, "")));
        alert.setContentText(tr("demo.dialog.unsaved.content"));

        ButtonType saveButton = new ButtonType(tr("demo.dialog.unsaved.save"));
        ButtonType discardButton = new ButtonType(tr("demo.dialog.unsaved.discard"));
        ButtonType cancelButton = ButtonType.CANCEL;
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelButton) {
            return EditorCloseDecisionPolicy.SavePromptResult.CANCEL;
        }
        if (result.get() == saveButton) {
            return EditorCloseDecisionPolicy.SavePromptResult.SAVE;
        }
        return EditorCloseDecisionPolicy.SavePromptResult.DONT_SAVE;
    }

    private boolean saveEditorNodeForClose(DockNode node) {
        return saveEditorNode(node, false);
    }

    private void openTextFileInEditor() {
        FileChooser fileChooser = createEditorOpenFileChooser();
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file == null) {
            return;
        }

        try {
            String content = Files.readString(file.toPath());
            DockNode editorNode = demoNodeFactory.createEditorNode(file.getName());
            registerEditorNode(editorNode);
            setEditorContentWithoutDirtyTracking(editorNode, content);
            setEditorFilePath(editorNode, file.toPath());
            markEditorDirty(editorNode, false);
            addDockNode(editorNode);
        } catch (IOException e) {
            showError(tr("demo.error.openFile", e.getMessage()));
        }
    }

    private void saveActiveEditor(boolean forceSaveAs) {
        DockNode activeEditorNode = findActiveEditorNode();
        if (activeEditorNode == null) {
            showError(tr("demo.error.noActiveEditor"));
            return;
        }

        saveEditorNode(activeEditorNode, forceSaveAs);
    }

    private boolean saveEditorNode(DockNode node, boolean forceSaveAs) {
        SerializableEditor editor = extractEditor(node);
        EditorDocumentState state = editorDocumentStates.get(node);
        if (editor == null || state == null) {
            return false;
        }

        Path targetPath = state.filePath;
        if (forceSaveAs || targetPath == null) {
            File chosenFile = chooseEditorSaveTargetFile(state);
            if (chosenFile == null) {
                return false;
            }
            targetPath = chosenFile.toPath();
        }

        try {
            Files.writeString(targetPath, editor.getText());
            setEditorFilePath(node, targetPath);
            markEditorDirty(node, false);
            return true;
        } catch (IOException e) {
            showError(tr("demo.error.saveFile", e.getMessage()));
            return false;
        }
    }

    private DockNode findActiveEditorNode() {
        List<DockNode> nodes = new ArrayList<>();
        collectDockNodes(snapFX.getDockGraph().getRoot(), nodes);
        for (DockFloatingWindow floatingWindow : snapFX.getFloatingWindows()) {
            nodes.addAll(floatingWindow.getDockNodes());
        }

        DockNode firstEditorNode = null;
        DockNode mainEditorNode = null;
        for (DockNode node : nodes) {
            SerializableEditor editor = extractEditor(node);
            if (editor == null) {
                continue;
            }
            if (editor.isFocused()) {
                return node;
            }
            if (firstEditorNode == null) {
                firstEditorNode = node;
            }
            if (DockNodeType.MAIN_EDITOR.getId().equals(node.getDockNodeId())) {
                mainEditorNode = node;
            }
        }

        return mainEditorNode != null ? mainEditorNode : firstEditorNode;
    }

    private File chooseEditorSaveTargetFile(EditorDocumentState state) {
        FileChooser fileChooser = createEditorSaveFileChooser(state);
        return fileChooser.showSaveDialog(primaryStage);
    }

    private FileChooser createLayoutSaveFileChooser() {
        FileChooser fileChooser = createFileChooser(
            tr("demo.fileChooser.saveLayout"),
            List.of(new FileChooser.ExtensionFilter(tr("demo.fileChooser.filter.json"), JSON_FILE_GLOB))
        );
        fileChooser.setInitialFileName(DEFAULT_LAYOUT_FILE_NAME);
        applyDocumentsInitialDirectory(fileChooser);
        return fileChooser;
    }

    private FileChooser createLayoutLoadFileChooser() {
        FileChooser fileChooser = createFileChooser(
            tr("demo.fileChooser.loadLayout"),
            List.of(new FileChooser.ExtensionFilter(tr("demo.fileChooser.filter.json"), JSON_FILE_GLOB))
        );
        applyDocumentsInitialDirectory(fileChooser);
        return fileChooser;
    }

    private FileChooser createEditorOpenFileChooser() {
        return createFileChooser(tr("demo.fileChooser.openTextFile"), createLocalizedEditorFileExtensionFilters());
    }

    private FileChooser createEditorSaveFileChooser(EditorDocumentState state) {
        FileChooser fileChooser = createFileChooser(tr("demo.fileChooser.saveEditor"), createLocalizedEditorFileExtensionFilters());
        Path currentFilePath = state == null ? null : state.filePath;
        String baseTitle = state == null ? null : state.baseTitle;
        applyEditorSaveChooserDefaults(fileChooser, currentFilePath, baseTitle);
        return fileChooser;
    }

    static FileChooser createFileChooser(String title, List<FileChooser.ExtensionFilter> extensionFilters) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        if (extensionFilters != null) {
            fileChooser.getExtensionFilters().addAll(extensionFilters);
        }
        return fileChooser;
    }

    static FileChooser.ExtensionFilter createJsonFileExtensionFilter() {
        return new FileChooser.ExtensionFilter(JSON_FILES_FILTER_LABEL, JSON_FILE_GLOB);
    }

    static List<FileChooser.ExtensionFilter> createEditorFileExtensionFilters() {
        return List.of(
            new FileChooser.ExtensionFilter(TEXT_FILES_FILTER_LABEL, TEXT_FILES_GLOBS),
            new FileChooser.ExtensionFilter(ALL_FILES_FILTER_LABEL, ALL_FILES_GLOB)
        );
    }

    private List<FileChooser.ExtensionFilter> createLocalizedEditorFileExtensionFilters() {
        return List.of(
            new FileChooser.ExtensionFilter(tr("demo.fileChooser.filter.text"), TEXT_FILES_GLOBS),
            new FileChooser.ExtensionFilter(tr("demo.fileChooser.filter.all"), ALL_FILES_GLOB)
        );
    }

    static void applyEditorSaveChooserDefaults(FileChooser fileChooser, Path currentFilePath, String baseTitle) {
        if (fileChooser == null) {
            return;
        }
        if (currentFilePath != null) {
            File currentFile = currentFilePath.toFile();
            File parentFile = currentFile.getParentFile();
            applyInitialDirectoryIfDirectory(fileChooser, parentFile);
            fileChooser.setInitialFileName(currentFile.getName());
            return;
        }
        if (baseTitle != null && !baseTitle.isBlank()) {
            fileChooser.setInitialFileName(baseTitle);
        }
    }

    private static void applyDocumentsInitialDirectory(FileChooser fileChooser) {
        if (fileChooser == null) {
            return;
        }
        applyInitialDirectoryIfDirectory(fileChooser, resolveDocumentsDirectory());
    }

    private static void applyInitialDirectoryIfDirectory(FileChooser fileChooser, File directory) {
        if (fileChooser == null || directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        fileChooser.setInitialDirectory(directory);
    }

    private static File resolveDocumentsDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            return null;
        }
        return new File(userHome + File.separator + DOCUMENTS_DIRECTORY_NAME);
    }

    private void registerEditorNode(DockNode node) {
        SerializableEditor editor = extractEditor(node);
        if (editor == null || editorDocumentStates.containsKey(node)) {
            return;
        }

        EditorDocumentState state = new EditorDocumentState();
        state.baseTitle = stripDirtySuffix(node.getTitle());
        state.filePath = null;
        state.dirty = false;
        state.suppressDirtyTracking = false;
        state.usesGeneratedUntitledTitle = demoTextLocalization.matchesInSupportedLocales(
            "demo.node.editor.untitled",
            state.baseTitle
        );
        state.textListener = (obs, oldVal, newVal) -> {
            if (!state.suppressDirtyTracking) {
                markEditorDirty(node, true);
            }
        };
        editor.textProperty().addListener(state.textListener);
        editorDocumentStates.put(node, state);
        updateEditorTitle(node, state);
    }

    private void removeEditorNodeState(DockNode node) {
        if (node == null) {
            return;
        }
        EditorDocumentState state = editorDocumentStates.remove(node);
        SerializableEditor editor = extractEditor(node);
        if (state != null && editor != null && state.textListener != null) {
            editor.textProperty().removeListener(state.textListener);
        }
    }

    private void clearEditorRegistry() {
        List<DockNode> nodes = new ArrayList<>(editorDocumentStates.keySet());
        for (DockNode node : nodes) {
            removeEditorNodeState(node);
        }
    }

    private void rebuildEditorRegistryFromCurrentLayout() {
        clearEditorRegistry();

        List<DockNode> nodes = new ArrayList<>();
        collectDockNodes(snapFX.getDockGraph().getRoot(), nodes);
        for (DockFloatingWindow floatingWindow : snapFX.getFloatingWindows()) {
            nodes.addAll(floatingWindow.getDockNodes());
        }
        nodes.addAll(snapFX.getHiddenNodes());
        nodes.addAll(snapFX.getSideBarNodes(Side.LEFT));
        nodes.addAll(snapFX.getSideBarNodes(Side.RIGHT));
        for (DockNode node : nodes) {
            registerEditorNode(node);
        }
    }

    private SerializableEditor extractEditor(DockNode node) {
        if (node == null || !(node.getContent() instanceof SerializableEditor editor)) {
            return null;
        }
        return editor;
    }

    private void setEditorFilePath(DockNode node, Path filePath) {
        EditorDocumentState state = editorDocumentStates.get(node);
        if (state == null) {
            return;
        }
        state.filePath = filePath;
        if (filePath != null && filePath.getFileName() != null) {
            state.baseTitle = filePath.getFileName().toString();
            state.usesGeneratedUntitledTitle = false;
        }
        updateEditorTitle(node, state);
    }

    private void setEditorContentWithoutDirtyTracking(DockNode node, String content) {
        SerializableEditor editor = extractEditor(node);
        EditorDocumentState state = editorDocumentStates.get(node);
        if (editor == null || state == null) {
            return;
        }
        state.suppressDirtyTracking = true;
        try {
            editor.setText(content == null ? "" : content);
            editor.positionCaret(0);
        } finally {
            state.suppressDirtyTracking = false;
        }
    }

    private void markEditorDirty(DockNode node, boolean dirty) {
        EditorDocumentState state = editorDocumentStates.get(node);
        if (state == null) {
            return;
        }
        state.dirty = dirty;
        updateEditorTitle(node, state);
    }

    private void updateEditorTitle(DockNode node, EditorDocumentState state) {
        if (node == null || state == null) {
            return;
        }
        String baseTitle = stripDirtySuffix(state.baseTitle);
        state.baseTitle = baseTitle;
        node.setTitle(state.dirty ? baseTitle + DIRTY_TITLE_SUFFIX : baseTitle);
    }

    private void updateUntitledEditorTitles() {
        for (Map.Entry<DockNode, EditorDocumentState> entry : editorDocumentStates.entrySet()) {
            EditorDocumentState state = entry.getValue();
            if (state == null || !state.usesGeneratedUntitledTitle || state.filePath != null) {
                continue;
            }
            state.baseTitle = tr("demo.node.editor.untitled");
            updateEditorTitle(entry.getKey(), state);
        }
    }

    private String stripDirtySuffix(String title) {
        if (title == null || title.isBlank()) {
            return tr("demo.node.editor.untitled");
        }
        if (title.endsWith(DIRTY_TITLE_SUFFIX)) {
            return title.substring(0, title.length() - DIRTY_TITLE_SUFFIX.length());
        }
        return title;
    }


    static String buildLayoutLoadErrorMessage(DockLayoutLoadException exception) {
        if (exception == null) {
            return "Error while loading:\nLayout could not be loaded due to an unknown error.";
        }
        return "Error while loading:\n" + exception.toDisplayMessage();
    }

    private String buildLocalizedLayoutLoadErrorMessage(DockLayoutLoadException exception) {
        String details = exception == null
            ? tr("demo.dialog.layoutLoadErrorUnknown")
            : exception.toDisplayMessage();
        return tr("demo.dialog.layoutLoadError", details);
    }

    static Alert createErrorAlert(String message, Window owner) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (owner != null && owner.getScene() != null) {
            alert.initOwner(owner);
        }
        return alert;
    }

    private void showError(String message) {
        Alert alert = createErrorAlert(tr("demo.dialog.error.title"), message, primaryStage);
        alert.showAndWait();
    }

    private static Alert createErrorAlert(String title, String message, Window owner) {
        Alert alert = createErrorAlert(message, owner);
        alert.setTitle(title);
        return alert;
    }

    /**
     * Main entry point for the application.
     * Launches the JavaFX application.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}

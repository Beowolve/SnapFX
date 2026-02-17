package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.SnapFX;
import com.github.beowolve.snapfx.close.DockCloseBehavior;
import com.github.beowolve.snapfx.close.DockCloseDecision;
import com.github.beowolve.snapfx.close.DockCloseRequest;
import com.github.beowolve.snapfx.close.DockCloseResult;
import com.github.beowolve.snapfx.debug.DockDebugOverlay;
import com.github.beowolve.snapfx.debug.DockGraphDebugView;
import com.github.beowolve.snapfx.dnd.DockDropVisualizationMode;
import com.github.beowolve.snapfx.floating.DockFloatingPinButtonMode;
import com.github.beowolve.snapfx.floating.DockFloatingPinChangeEvent;
import com.github.beowolve.snapfx.floating.DockFloatingPinLockedBehavior;
import com.github.beowolve.snapfx.floating.DockFloatingSnapTarget;
import com.github.beowolve.snapfx.floating.DockFloatingWindow;
import com.github.beowolve.snapfx.model.DockContainer;
import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import com.github.beowolve.snapfx.persistence.DockLayoutLoadException;
import com.github.beowolve.snapfx.view.DockCloseButtonMode;
import com.github.beowolve.snapfx.view.DockTitleBarMode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Demo application for the SnapFX framework.
 * Shows a typical IDE-like layout with sidebar, editor, and console.
 */
public class MainDemo extends Application {
    private static final System.Logger LOGGER = System.getLogger(MainDemo.class.getName());
    public static final String FX_FONT_WEIGHT_BOLD = "-fx-font-weight: bold;";
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

    // Menu for hidden windows
    private Menu hiddenWindowsMenu;
    private Menu floatNodeMenu;
    private Menu floatingWindowsMenu;

    // Shared lock state property
    private final BooleanProperty lockLayoutProperty = new SimpleBooleanProperty(false);

    // Node factory for creating demo nodes
    private DemoNodeFactory demoNodeFactory;
    private final BooleanProperty promptOnEditorCloseProperty = new SimpleBooleanProperty(true);
    private final Map<DockNode, EditorDocumentState> editorDocumentStates = new HashMap<>();
    private EditorCloseDecisionPolicy editorCloseDecisionPolicy;

    private static final class EditorDocumentState {
        private String baseTitle;
        private Path filePath;
        private boolean dirty;
        private boolean suppressDirtyTracking;
        private ChangeListener<String> textListener;
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

        stage.setTitle("SnapFX Demo - Docking Framework");
        stage.setScene(new Scene(mainLayout, 1200, 800));
        configureDemoShortcuts(stage.getScene(), this::toggleFullscreen);
        applyApplicationIcons(stage);

        // Create demo node factory
        demoNodeFactory = new DemoNodeFactory();
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

        // Load CSS
        var cssResource = getClass().getResource("/snapfx.css");
        if (cssResource != null) {
            stage.getScene().getStylesheets().add(cssResource.toExternalForm());
        }

        // Initialize SnapFX AFTER scene is set (needed for ghost overlay)
        snapFX.initialize(stage);

        // Listen to lock state changes
        lockLayoutProperty.addListener((obs, oldVal, newVal) -> snapFX.setLocked(newVal));

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

    static List<String> getAppIconResources() {
        return APP_ICON_RESOURCES;
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

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");

        MenuItem openEditorItem = new MenuItem("Open Text File...");
        openEditorItem.setGraphic(IconUtil.loadIcon("folder-open-document-text.png"));
        openEditorItem.setOnAction(e -> openTextFileInEditor());

        MenuItem saveEditorItem = new MenuItem("Save Active Editor");
        saveEditorItem.setGraphic(IconUtil.loadIcon("disk.png"));
        saveEditorItem.setOnAction(e -> saveActiveEditor(false));

        MenuItem saveEditorAsItem = new MenuItem("Save Active Editor As...");
        saveEditorAsItem.setGraphic(IconUtil.loadIcon("disk--pencil.png"));
        saveEditorAsItem.setOnAction(e -> saveActiveEditor(true));

        SeparatorMenuItem separatorEditor = new SeparatorMenuItem();

        MenuItem saveLayoutItem = new MenuItem("Save Layout...");
        saveLayoutItem.setGraphic(IconUtil.loadIcon("disk-black.png"));
        saveLayoutItem.setOnAction(e -> saveLayout());

        MenuItem loadLayoutItem = new MenuItem("Load Layout...");
        loadLayoutItem.setGraphic(IconUtil.loadIcon("folder-open-document.png"));
        loadLayoutItem.setOnAction(e -> loadLayout());

        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem exitItem = new MenuItem("Exit");
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

        // Layout Menu
        Menu layoutMenu = new Menu("Layout");

        MenuItem resetItem = new MenuItem("Reset to Default");
        resetItem.setGraphic(IconUtil.loadIcon("arrow-circle.png"));
        resetItem.setOnAction(e -> resetLayoutToDefault());

        CheckMenuItem lockItem = new CheckMenuItem("Lock Layout");
        lockItem.setGraphic(IconUtil.loadIcon("lock.png"));
        lockItem.selectedProperty().bindBidirectional(lockLayoutProperty);
        lockItem.selectedProperty().addListener((obs, oldVal, newVal) ->
            lockItem.setGraphic(IconUtil.loadIcon(Boolean.TRUE.equals(newVal) ? "lock.png" : "lock-unlock.png"))
        );
        lockItem.selectedProperty().bindBidirectional(lockLayoutProperty);

        SeparatorMenuItem sep2 = new SeparatorMenuItem();

        hiddenWindowsMenu = new Menu("Hidden Windows");
        updateHiddenWindowsMenu();

        floatNodeMenu = new Menu("Float Node");
        floatingWindowsMenu = new Menu("Floating Windows");
        updateFloatingMenus();

        // Listen to hidden nodes changes
        snapFX.getHiddenNodes().addListener((ListChangeListener<DockNode>) c ->
            updateHiddenWindowsMenu()
        );

        snapFX.getDockGraph().revisionProperty().addListener((obs, oldVal, newVal) ->
            updateFloatingMenus()
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
            floatNodeMenu,
            floatingWindowsMenu
        );

        // Help Menu
        Menu helpMenu = new Menu("Help");

        MenuItem aboutItem = new MenuItem("About...");
        aboutItem.setGraphic(IconUtil.loadIcon("question.png"));
        aboutItem.setOnAction(e -> AboutDialog.show(primaryStage, getHostServices()::showDocument));

        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, layoutMenu, helpMenu);
        return menuBar;
    }


    private void updateHiddenWindowsMenu() {
        hiddenWindowsMenu.getItems().clear();

        if (snapFX.getHiddenNodes().isEmpty()) {
            MenuItem emptyItem = new MenuItem("(no hidden windows)");
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

    private void updateFloatingMenus() {
        updateFloatNodeMenu();
        updateFloatingWindowsMenu();
    }

    private void updateFloatNodeMenu() {
        floatNodeMenu.getItems().clear();
        List<DockNode> nodes = new ArrayList<>();
        collectDockNodes(snapFX.getDockGraph().getRoot(), nodes);

        if (nodes.isEmpty()) {
            MenuItem emptyItem = new MenuItem("(no docked nodes)");
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
            MenuItem emptyItem = new MenuItem("(no floating windows)");
            emptyItem.setDisable(true);
            floatingWindowsMenu.getItems().add(emptyItem);
            return;
        }

        MenuItem attachAllItem = new MenuItem("Attach All");
        attachAllItem.setOnAction(e -> attachAllFloatingWindows());
        floatingWindowsMenu.getItems().add(attachAllItem);
        floatingWindowsMenu.getItems().add(new SeparatorMenuItem());

        for (DockFloatingWindow window : snapFX.getFloatingWindows()) {
            List<DockNode> nodes = window.getDockNodes();
            String label;
            if (nodes.isEmpty()) {
                label = "Attach: Floating Window";
            } else if (nodes.size() == 1) {
                label = "Attach: " + nodes.getFirst().getTitle();
            } else {
                label = "Attach: " + nodes.getFirst().getTitle() + " +" + (nodes.size() - 1);
            }
            MenuItem attachItem = new MenuItem(label);
            if (!nodes.isEmpty()) {
                attachItem.setGraphic(createMenuItemIcon(nodes.getFirst()));
            }
            attachItem.setOnAction(e -> snapFX.attachFloatingWindow(window));
            floatingWindowsMenu.getItems().add(attachItem);
        }
    }

    private Node createMenuItemIcon(DockNode node) {
        if (node == null) {
            return null;
        }
        return copyMenuIcon(node.getIcon());
    }

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

    private void collectDockNodes(DockElement element, List<DockNode> nodes) {
        if (element == null) {
            return;
        }
        if (element instanceof DockNode node) {
            nodes.add(node);
            return;
        }
        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                collectDockNodes(child, nodes);
            }
        }
    }

    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar();

        // Lock toggle
        CheckBox lockCheckBox = new CheckBox("Lock");
        lockCheckBox.selectedProperty().bindBidirectional(lockLayoutProperty);

        Separator sep1 = new Separator();

        Label addLabel = new Label("Add:");
        addLabel.setStyle(FX_FONT_WEIGHT_BOLD);

        Button addEditorBtn = new Button("+ Editor");
        addEditorBtn.setGraphic(IconUtil.loadIcon("document--pencil.png"));
        addEditorBtn.setOnAction(e -> addNewEditorNode());

        Button addPropsBtn = new Button("+ Properties");
        addPropsBtn.setGraphic(IconUtil.loadIcon("property.png"));
        addPropsBtn.setOnAction(e -> addNewPropertiesNode());

        Button addConsoleBtn = new Button("+ Console");
        addConsoleBtn.setGraphic(IconUtil.loadIcon("terminal.png"));
        addConsoleBtn.setOnAction(e -> addNewConsoleNode());

        Button addGenericBtn = new Button("+ Panel");
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
        DockNode node = demoNodeFactory.createEditorNode("Untitled");
        addDockNode(node);
    }

    private void addNewPropertiesNode() {
        DockNode node = demoNodeFactory.createPropertiesPanelNode();
        addDockNode(node);
    }

    private void addNewConsoleNode() {
        DockNode node = demoNodeFactory.createConsolePanelNode();
        addDockNode(node);
    }

    private void addNewGenericPanelNode() {
        String name = "Panel_" + (snapFX.getDockNodeCount(DockNodeType.GENERIC_PANEL.getId()) + 1);
        DockNode node = demoNodeFactory.createGenericPanelNode(name);
        addDockNode(node);
    }

    private void addDockNode(DockNode node) {
        registerEditorNode(node);
        if (snapFX.getDockGraph().getRoot() == null) {
            snapFX.getDockGraph().setRoot(node);
        } else {
            snapFX.getDockGraph().dock(node, snapFX.getDockGraph().getRoot(), DockPosition.RIGHT);
        }
    }

    /**
     * Setup the node factory for proper save/load support across sessions.
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
        snapFX.closeFloatingWindows(false);
        snapFX.getHiddenNodes().clear();
        createDemoLayout();
        updateDockLayout();
        updateHiddenWindowsMenu();
        updateFloatingMenus();
    }

    private void installDebugPanel() {
        // Get the current dock layout from mainLayout
        Node dockLayout = mainLayout.getCenter();

        DockGraphDebugView debugView = new DockGraphDebugView(snapFX.getDockGraph(), snapFX.getDragService());
        debugView.setPrefWidth(420);

        // Enable auto-export by default
        debugView.setAutoExportOnDrop(true);

        TabPane debugTabs = new TabPane();
        debugTabs.setPrefWidth(420);
        debugTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab debugTab = new Tab("Debug", debugView);
        debugTab.setGraphic(IconUtil.loadIcon("bug.png"));
        Tab settingsTab = new Tab("Settings", createSettingsPanel());
        settingsTab.setGraphic(IconUtil.loadIcon("hammer-screwdriver.png"));
        debugTabs.getTabs().addAll(debugTab, settingsTab);

        // Create split pane with dock layout on left and debug view on right
        mainSplit = new SplitPane();
        mainSplit.getItems().addAll(dockLayout, debugTabs);
        mainSplit.setDividerPositions(0.72);

        // Add a small HUD overlay that shows current D&D state
        DockDebugOverlay hud = new DockDebugOverlay(snapFX.getDockGraph(), snapFX.getDragService());
        StackPane stack = new StackPane(mainSplit, hud);
        StackPane.setAlignment(hud, Pos.TOP_LEFT);
        StackPane.setMargin(hud, new Insets(10));

        // Replace center with the new stack containing split + HUD
        mainLayout.setCenter(stack);

        // Rebuild debug tree when layout is rebuilt
        debugView.rebuildTree();

        // Expand debug tree by default
        debugView.expandAll();
    }

    private Parent createSettingsPanel() {
        Label header = new Label("Layout Settings");
        header.setStyle(FX_FONT_WEIGHT_BOLD);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints labelColumn = new ColumnConstraints();
        ColumnConstraints controlColumn = new ColumnConstraints();
        controlColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, controlColumn);

        // Add future demo settings here as new options become available.
        ComboBox<DockTitleBarMode> titleBarMode = new ComboBox<>();
        titleBarMode.getItems().setAll(DockTitleBarMode.values());
        titleBarMode.setMaxWidth(Double.MAX_VALUE);
        titleBarMode.setValue(snapFX.getTitleBarMode());
        titleBarMode.valueProperty().addListener((obs, oldVal, newVal) -> onTitleBarModeChanged(newVal));
        grid.addRow(0, new Label("Title Bar Mode"), titleBarMode);

        ComboBox<DockCloseButtonMode> closeButtonMode = new ComboBox<>();
        closeButtonMode.getItems().setAll(DockCloseButtonMode.values());
        closeButtonMode.setMaxWidth(Double.MAX_VALUE);
        closeButtonMode.setValue(snapFX.getCloseButtonMode());
        closeButtonMode.valueProperty().addListener((obs, oldVal, newVal) -> onCloseButtonModeChanged(newVal));
        grid.addRow(1, new Label("Close Button Mode"), closeButtonMode);

        ComboBox<DockDropVisualizationMode> dropMode = new ComboBox<>();
        dropMode.getItems().setAll(DockDropVisualizationMode.values());
        dropMode.setMaxWidth(Double.MAX_VALUE);
        dropMode.setValue(snapFX.getDropVisualizationMode());
        dropMode.valueProperty().addListener((obs, oldVal, newVal) -> onDropVisualizationModeChanged(newVal));
        grid.addRow(2, new Label("Drop Visualization"), dropMode);

        CheckBox lockCheckBox = new CheckBox("Locked");
        lockCheckBox.selectedProperty().bindBidirectional(lockLayoutProperty);
        grid.addRow(3, new Label("Layout Lock"), lockCheckBox);

        ComboBox<DockFloatingPinButtonMode> pinButtonMode = new ComboBox<>();
        pinButtonMode.getItems().setAll(DockFloatingPinButtonMode.values());
        pinButtonMode.setMaxWidth(Double.MAX_VALUE);
        pinButtonMode.setValue(snapFX.getFloatingPinButtonMode());
        pinButtonMode.valueProperty().addListener((obs, oldVal, newVal) -> onFloatingPinButtonModeChanged(newVal));
        grid.addRow(4, new Label("Floating Pin Button"), pinButtonMode);

        CheckBox allowPinToggleCheckBox = new CheckBox("Allow pin toggle in title bar");
        allowPinToggleCheckBox.setSelected(snapFX.isAllowFloatingPinToggle());
        allowPinToggleCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            snapFX.setAllowFloatingPinToggle(Boolean.TRUE.equals(newVal))
        );
        grid.addRow(5, new Label("Floating Pin Toggle"), allowPinToggleCheckBox);

        CheckBox defaultPinnedCheckBox = new CheckBox("New floating windows start pinned");
        defaultPinnedCheckBox.setSelected(snapFX.isDefaultFloatingAlwaysOnTop());
        defaultPinnedCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            snapFX.setDefaultFloatingAlwaysOnTop(Boolean.TRUE.equals(newVal))
        );
        grid.addRow(6, new Label("Default Pinned"), defaultPinnedCheckBox);

        ComboBox<DockFloatingPinLockedBehavior> pinLockedBehavior = new ComboBox<>();
        pinLockedBehavior.getItems().setAll(DockFloatingPinLockedBehavior.values());
        pinLockedBehavior.setMaxWidth(Double.MAX_VALUE);
        pinLockedBehavior.setValue(snapFX.getFloatingPinLockedBehavior());
        pinLockedBehavior.valueProperty().addListener((obs, oldVal, newVal) -> onFloatingPinLockedBehaviorChanged(newVal));
        grid.addRow(7, new Label("Pin in Lock Mode"), pinLockedBehavior);

        CheckBox floatingSnappingCheckBox = new CheckBox("Enable snapping while dragging title bars");
        floatingSnappingCheckBox.setSelected(snapFX.isFloatingWindowSnappingEnabled());
        floatingSnappingCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
            onFloatingWindowSnappingEnabledChanged(newVal)
        );
        grid.addRow(8, new Label("Floating Snapping"), floatingSnappingCheckBox);

        Spinner<Double> snapDistanceSpinner = new Spinner<>();
        snapDistanceSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 64.0, snapFX.getFloatingWindowSnapDistance(), 1.0)
        );
        snapDistanceSpinner.setEditable(true);
        snapDistanceSpinner.setMaxWidth(Double.MAX_VALUE);
        snapDistanceSpinner.valueProperty().addListener((obs, oldVal, newVal) -> onFloatingWindowSnapDistanceChanged(newVal));
        grid.addRow(9, new Label("Snap Distance (px)"), snapDistanceSpinner);

        CheckBox screenSnapTargetCheckBox = new CheckBox("Screen");
        CheckBox mainWindowSnapTargetCheckBox = new CheckBox("Main Window");
        CheckBox floatingWindowsSnapTargetCheckBox = new CheckBox("Floating Windows");
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
        grid.addRow(10, new Label("Snap Targets"), snapTargetsBox);

        CheckBox promptEditorCloseCheckBox = new CheckBox("Prompt for unsaved editors");
        promptEditorCloseCheckBox.selectedProperty().bindBidirectional(promptOnEditorCloseProperty);
        grid.addRow(11, new Label("Close Hook"), promptEditorCloseCheckBox);

        Label hint = new Label("Changes apply immediately. Dirty editors are marked with '*'.");

        VBox panel = new VBox(12, header, grid, hint);
        panel.setPadding(new Insets(10));
        return panel;
    }

    private void updateDockLayout() {
        Parent dockLayout = snapFX.buildLayout();

        // If we have a split pane (with debug panel), update only the dock layout part
        if (mainSplit != null && !mainSplit.getItems().isEmpty()) {
            // Save divider position
            double[] dividerPositions = mainSplit.getDividerPositions();

            // Replace the dock layout (first item in split)
            mainSplit.getItems().set(0, dockLayout);

            // Restore divider position
            if (dividerPositions.length > 0) {
                Platform.runLater(() -> mainSplit.setDividerPositions(dividerPositions));
            }
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
                showError("Error while saving:\n" + e.getMessage());
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

                // Synchronize lock state from loaded layout
                lockLayoutProperty.set(snapFX.isLocked());

                // Success - no popup needed for better UX
            } catch (DockLayoutLoadException e) {
                LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Layout load failed: {0}",
                    e.toDisplayMessage()
                );
                showError(buildLayoutLoadErrorMessage(e));
            } catch (IOException e) {
                showError("Error while loading:\n" + e.getMessage());
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
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Save changes before closing \"" + node.getTitle().replace(DIRTY_TITLE_SUFFIX, "") + "\"?");
        alert.setContentText("Your changes will be lost if you choose \"Don't Save\".");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType discardButton = new ButtonType("Don't Save");
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
            showError("Error while opening file:\n" + e.getMessage());
        }
    }

    private void saveActiveEditor(boolean forceSaveAs) {
        DockNode activeEditorNode = findActiveEditorNode();
        if (activeEditorNode == null) {
            showError("No active editor available.");
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
            showError("Error while saving file:\n" + e.getMessage());
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
            SAVE_LAYOUT_CHOOSER_TITLE,
            List.of(createJsonFileExtensionFilter())
        );
        fileChooser.setInitialFileName(DEFAULT_LAYOUT_FILE_NAME);
        applyDocumentsInitialDirectory(fileChooser);
        return fileChooser;
    }

    private FileChooser createLayoutLoadFileChooser() {
        FileChooser fileChooser = createFileChooser(
            LOAD_LAYOUT_CHOOSER_TITLE,
            List.of(createJsonFileExtensionFilter())
        );
        applyDocumentsInitialDirectory(fileChooser);
        return fileChooser;
    }

    private FileChooser createEditorOpenFileChooser() {
        return createFileChooser(OPEN_TEXT_FILE_CHOOSER_TITLE, createEditorFileExtensionFilters());
    }

    private FileChooser createEditorSaveFileChooser(EditorDocumentState state) {
        FileChooser fileChooser = createFileChooser(SAVE_EDITOR_CHOOSER_TITLE, createEditorFileExtensionFilters());
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

    private String stripDirtySuffix(String title) {
        if (title == null || title.isBlank()) {
            return "Untitled";
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
        Alert alert = createErrorAlert(message, primaryStage);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

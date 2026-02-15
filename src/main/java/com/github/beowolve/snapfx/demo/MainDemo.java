package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.SnapFX;
import com.github.beowolve.snapfx.close.DockCloseBehavior;
import com.github.beowolve.snapfx.debug.DockDebugOverlay;
import com.github.beowolve.snapfx.debug.DockGraphDebugView;
import com.github.beowolve.snapfx.dnd.DockDropVisualizationMode;
import com.github.beowolve.snapfx.floating.DockFloatingWindow;
import com.github.beowolve.snapfx.model.DockContainer;
import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import com.github.beowolve.snapfx.view.DockCloseButtonMode;
import com.github.beowolve.snapfx.view.DockTitleBarMode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo application for the SnapFX framework.
 * Shows a typical IDE-like layout with sidebar, editor, and console.
 */
public class MainDemo extends Application {
    public static final String FX_FONT_WEIGHT_BOLD = "-fx-font-weight: bold;";
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
        applyApplicationIcons(stage);

        // Create demo node factory
        demoNodeFactory = new DemoNodeFactory();

        // Setup node factory for proper save/load across sessions
        setupNodeFactory();

        // Keep default close behavior explicit for demo clarity
        snapFX.setDefaultCloseBehavior(DockCloseBehavior.HIDE);

        // Set drop visualization mode
        snapFX.setDropVisualizationMode(DockDropVisualizationMode.DEFAULT);

        // Set close button mode to show only on active tab for cleaner look
        snapFX.setCloseButtonMode(DockCloseButtonMode.BOTH);

        // Set title bar mode to auto (show only when needed)
        snapFX.setTitleBarMode(DockTitleBarMode.AUTO);

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

    static List<String> getAppIconResources() {
        return APP_ICON_RESOURCES;
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

        MenuItem saveItem = new MenuItem("Save Layout...");
        saveItem.setGraphic(IconUtil.loadIcon("disk.png"));
        saveItem.setOnAction(e -> saveLayout());

        MenuItem loadItem = new MenuItem("Load Layout...");
        loadItem.setGraphic(IconUtil.loadIcon("folder-open-document-text.png"));
        loadItem.setOnAction(e -> loadLayout());

        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setGraphic(IconUtil.loadIcon("logout.png"));
        exitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(saveItem, loadItem, separator1, exitItem);

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
        attachAllItem.setOnAction(e -> {
            List<DockFloatingWindow> windows = new ArrayList<>(snapFX.getFloatingWindows());
            for (DockFloatingWindow window : windows) {
                snapFX.attachFloatingWindow(window);
            }
        });
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
            attachItem.setOnAction(e -> snapFX.attachFloatingWindow(window));
            floatingWindowsMenu.getItems().add(attachItem);
        }
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
        addEditorBtn.setOnAction(e -> {
            DockNode node = demoNodeFactory.createEditorNode("Untitled");
            addDockNode(node);
        });

        Button addPropsBtn = new Button("+ Properties");
        addPropsBtn.setGraphic(IconUtil.loadIcon("property.png"));
        addPropsBtn.setOnAction(e -> {
            DockNode node = demoNodeFactory.createPropertiesPanelNode();
            addDockNode(node);
        });

        Button addConsoleBtn = new Button("+ Console");
        addConsoleBtn.setGraphic(IconUtil.loadIcon("terminal.png"));
        addConsoleBtn.setOnAction(e -> {
            DockNode node = demoNodeFactory.createConsolePanelNode();
            addDockNode(node);
        });

        Button addGenericBtn = new Button("+ Panel");
        addGenericBtn.setGraphic(IconUtil.loadIcon("plus.png"));
        addGenericBtn.setOnAction(e -> {
            String name = "Panel_" + (snapFX.getDockNodeCount(DockNodeType.GENERIC_PANEL.getId()) + 1);
            DockNode node = demoNodeFactory.createGenericPanelNode(name);
            addDockNode(node);
        });

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
    private void addDockNode(DockNode node) {
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

    /**
     * Creates the demo layout with fixed node IDs for persistence.
     */
    private void createDemoLayout() {
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
        Tab settingsTab = new Tab("Settings", createSettingsPanel());
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
        titleBarMode.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                snapFX.setTitleBarMode(newVal);
            }
        });
        grid.addRow(0, new Label("Title Bar Mode"), titleBarMode);

        ComboBox<DockCloseButtonMode> closeButtonMode = new ComboBox<>();
        closeButtonMode.getItems().setAll(DockCloseButtonMode.values());
        closeButtonMode.setMaxWidth(Double.MAX_VALUE);
        closeButtonMode.setValue(snapFX.getCloseButtonMode());
        closeButtonMode.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                snapFX.setCloseButtonMode(newVal);
            }
        });
        grid.addRow(1, new Label("Close Button Mode"), closeButtonMode);

        ComboBox<DockDropVisualizationMode> dropMode = new ComboBox<>();
        dropMode.getItems().setAll(DockDropVisualizationMode.values());
        dropMode.setMaxWidth(Double.MAX_VALUE);
        dropMode.setValue(snapFX.getDropVisualizationMode());
        dropMode.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                snapFX.setDropVisualizationMode(newVal);
            }
        });
        grid.addRow(2, new Label("Drop Visualization"), dropMode);

        CheckBox lockCheckBox = new CheckBox("Locked");
        lockCheckBox.selectedProperty().bindBidirectional(lockLayoutProperty);
        grid.addRow(3, new Label("Layout Lock"), lockCheckBox);

        Label hint = new Label("Changes apply immediately.");

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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save layout");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON files", "*.json")
        );
        fileChooser.setInitialFileName("snapfx-layout.json");

        // Set initial directory to user's Documents folder
        String documentsPath = System.getProperty("user.home") + File.separator + "Documents";
        File documentsDir = new File(documentsPath);
        if (documentsDir.exists() && documentsDir.isDirectory()) {
            fileChooser.setInitialDirectory(documentsDir);
        }

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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load layout");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON files", "*.json")
        );

        // Set initial directory to user's Documents folder
        String documentsPath = System.getProperty("user.home") + File.separator + "Documents";
        File documentsDir = new File(documentsPath);
        if (documentsDir.exists() && documentsDir.isDirectory()) {
            fileChooser.setInitialDirectory(documentsDir);
        }

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                snapFX.loadLayout(json);
                updateDockLayout();

                // Synchronize lock state from loaded layout
                lockLayoutProperty.set(snapFX.isLocked());

                // Success - no popup needed for better UX
            } catch (IOException e) {
                showError("Error while loading:\n" + e.getMessage());
            }
        }
    }


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

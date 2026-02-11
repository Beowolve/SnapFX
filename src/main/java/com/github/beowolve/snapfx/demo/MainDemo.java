package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.SnapFX;
import com.github.beowolve.snapfx.debug.DockDebugOverlay;
import com.github.beowolve.snapfx.debug.DockGraphDebugView;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Demo application for the SnapFX framework.
 * Shows a typical IDE-like layout with sidebar, editor, and console.
 */
public class MainDemo extends Application {
    public static final String FX_FONT_WEIGHT_BOLD = "-fx-font-weight: bold;";
    private SnapFX snapFX;
    private Stage primaryStage;
    private BorderPane mainLayout;
    private SplitPane mainSplit;

    // Menu for hidden windows
    private Menu hiddenWindowsMenu;

    // Shared lock state property
    private final javafx.beans.property.BooleanProperty lockLayoutProperty =
        new javafx.beans.property.SimpleBooleanProperty(false);

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

        // Create demo node factory
        demoNodeFactory = new DemoNodeFactory();

        // Setup node factory for proper save/load across sessions
        setupNodeFactory();

        // Set close handler to hide instead of remove (BEFORE creating layout)
        snapFX.setOnNodeCloseRequest(node -> snapFX.hide(node));

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
        resetItem.setOnAction(e -> {
            createDemoLayout();
            updateDockLayout();
        });

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

        // Listen to hidden nodes changes
        snapFX.getHiddenNodes().addListener((javafx.collections.ListChangeListener<DockNode>) c ->
            updateHiddenWindowsMenu()
        );

        layoutMenu.getItems().addAll(resetItem, lockItem, sep2, hiddenWindowsMenu);

        // Help Menu
        Menu helpMenu = new Menu("Help");

        MenuItem aboutItem = new MenuItem("About...");
        aboutItem.setGraphic(IconUtil.loadIcon("question.png"));
        aboutItem.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, layoutMenu, helpMenu);
        return menuBar;
    }


    /**
     * Show the About dialog with license information.
     */
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About SnapFX");
        alert.setHeaderText("SnapFX Docking Framework");

        // Create content with hyperlinks
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label versionLabel = new Label("Version 1.0-SNAPSHOT");
        versionLabel.setStyle(FX_FONT_WEIGHT_BOLD);

        Label descriptionLabel = new Label(
            "A high-performance, lightweight JavaFX docking framework\n" +
            "designed for professional IDE-like applications."
        );
        descriptionLabel.setWrapText(true);

        Separator separator = new Separator();

        Label licenseTitle = new Label("Icon Credits:");
        licenseTitle.setStyle(FX_FONT_WEIGHT_BOLD);

        // Create clickable hyperlinks for license information
        javafx.scene.layout.HBox licenseBox = new javafx.scene.layout.HBox(5);
        licenseBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label licenseText1 = new Label("Some icons by ");

        Hyperlink authorLink = new Hyperlink("Yusuke Kamiyamane");
        authorLink.setOnAction(e -> getHostServices().showDocument("http://p.yusukekamiyamane.com/"));

        Label licenseText2 = new Label(". Licensed under a ");

        Hyperlink licenseLink = new Hyperlink("Creative Commons Attribution 3.0 License");
        licenseLink.setOnAction(e -> getHostServices().showDocument("http://creativecommons.org/licenses/by/3.0/deed.de"));

        Label licenseText3 = new Label(".");

        javafx.scene.layout.FlowPane flowPane = new javafx.scene.layout.FlowPane(5, 5);
        flowPane.getChildren().addAll(
            licenseText1, authorLink, licenseText2, licenseLink, licenseText3
        );

        content.getChildren().addAll(
            versionLabel,
            descriptionLabel,
            separator,
            licenseTitle,
            flowPane
        );

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(500);

        alert.showAndWait();
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
    }

    private void installDebugPanel() {
        // Get the current dock layout from mainLayout
        javafx.scene.Node dockLayout = mainLayout.getCenter();

        DockGraphDebugView debugView = new DockGraphDebugView(snapFX.getDockGraph(), snapFX.getDragService());
        debugView.setPrefWidth(420);

        // Enable auto-export by default
        debugView.setAutoExportOnDrop(true);

        // Create split pane with dock layout on left and debug view on right
        mainSplit = new SplitPane();
        mainSplit.getItems().addAll(dockLayout, debugView);
        mainSplit.setDividerPositions(0.72);

        // Add a small HUD overlay that shows current D&D state
        DockDebugOverlay hud = new DockDebugOverlay(snapFX.getDockGraph(), snapFX.getDragService());
        StackPane stack = new StackPane(mainSplit, hud);
        StackPane.setAlignment(hud, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(hud, new Insets(10));

        // Replace center with the new stack containing split + HUD
        mainLayout.setCenter(stack);

        // Rebuild debug tree when layout is rebuilt
        debugView.rebuildTree();

        // Expand debug tree by default
        debugView.expandAll();
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
                javafx.application.Platform.runLater(() -> mainSplit.setDividerPositions(dividerPositions));
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

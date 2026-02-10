package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.persistence.DockNodeFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Demo implementation of DockNodeFactory that creates nodes for the MainDemo application.
 * This class demonstrates how to properly implement the factory pattern for cross-session persistence.
 * All node definitions are managed centrally via the DockNodeType enum.
 */
public class DemoNodeFactory implements DockNodeFactory {

    /**
     * Creates a new DemoNodeFactory.
     */
    public DemoNodeFactory() {
    }

    /**
     * Creates a DockNode based on the given DockNode-ID (typbasiert).
     * Das Framework sorgt für die eindeutige Layout-ID und ruft die Factory mit der DockNode-ID auf.
     */
    @Override
    public DockNode createNode(String dockNodeId) {
        DockNodeType type = DockNodeType.fromId(dockNodeId);
        if (type == null) return null;
        return switch (type) {
            case PROJECT_EXPLORER -> createProjectExplorerNode();
            case MAIN_EDITOR -> createMainEditorNode();
            case PROPERTIES -> createPropertiesNode();
            case CONSOLE -> createConsoleNode();
            case TASKS -> createTasksNode();
            case EDITOR -> createEditorNode("Untitled");
            case PROPERTIES_PANEL -> createPropertiesPanelNode();
            case CONSOLE_PANEL -> createConsolePanelNode();
            case GENERIC_PANEL -> createGenericPanelNode("Panel");
        };
    }

    /**
     * Helper method to create a DockNode with the given type and content.
     * This centralizes the creation logic and ensures consistent properties.
     */
    private DockNode createDockNode(DockNodeType type, Node content) {
        DockNode node = new DockNode(type.getId(), content, type.getDefaultTitle());
        node.setIcon(IconUtil.loadIcon(type.getIconName()));
        return node;
    }

    /**
     * Creates the Project Explorer node with fixed ID from enum.
     */
    public DockNode createProjectExplorerNode() {
        TreeView<String> projectTree = new TreeView<>();
        TreeItem<String> root = new TreeItem<>("Project");
        root.setExpanded(true);

        TreeItem<String> src = new TreeItem<>("src");
        TreeItem<String> test = new TreeItem<>("test");
        TreeItem<String> resources = new TreeItem<>("resources");

        src.getChildren().addAll(
            java.util.List.of(
                new TreeItem<>("Main.java"),
                new TreeItem<>("Utils.java")
            )
        );

        root.getChildren().addAll(java.util.List.of(src, test, resources));
        projectTree.setRoot(root);

        return createDockNode(DockNodeType.PROJECT_EXPLORER, projectTree);
    }

    /**
     * Creates the Main Editor node with fixed ID "mainEditor".
     * Uses SerializableEditor to demonstrate content persistence.
     */
    public DockNode createMainEditorNode() {
        SerializableEditor editor = new SerializableEditor("""
            public class Main {
                public static void main(String[] args) {
                    System.out.println("SnapFX Demo");

                    // SnapFX.dock(myNode, "Title");
                    // Simple API for docking!
                }
            }
            """);

        return createDockNode(DockNodeType.MAIN_EDITOR, editor);
    }

    /**
     * Creates the Properties node with fixed ID "properties".
     */
    public DockNode createPropertiesNode() {
        VBox propertiesContent = new VBox(10);
        propertiesContent.setPadding(new Insets(10));

        Label propLabel = new Label("Properties");
        propLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane propsGrid = new GridPane();
        propsGrid.setHgap(10);
        propsGrid.setVgap(5);

        propsGrid.add(new Label("Name:"), 0, 0);
        propsGrid.add(new TextField("Main.java"), 1, 0);

        propsGrid.add(new Label("Type:"), 0, 1);
        propsGrid.add(new Label("Java file"), 1, 1);

        propsGrid.add(new Label("Size:"), 0, 2);
        propsGrid.add(new Label("1.2 KB"), 1, 2);

        propertiesContent.getChildren().addAll(propLabel, new Separator(), propsGrid);

        return createDockNode(DockNodeType.PROPERTIES, propertiesContent);
    }

    /**
     * Creates the Console node with fixed ID "console".
     */
    public DockNode createConsoleNode() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setText("""
            SnapFX Framework v1.0
            ====================================
            [INFO] Docking system initialized
            [INFO] Layout loaded
            [INFO] Ready for drag & drop

            Drag & drop is fully functional.
            Save/Load works across sessions using fixed node IDs.
            """);
        console.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        return createDockNode(DockNodeType.CONSOLE, console);
    }

    /**
     * Creates the Tasks node with fixed ID "tasks".
     */
    public DockNode createTasksNode() {
        ListView<String> tasksList = new ListView<>();
        tasksList.getItems().addAll(
            "TODO: Implement full drag & drop",
            "TODO: Floating windows",
            "TODO: Advanced zone detection",
            "DONE: Base architecture",
            "DONE: Layout engine",
            "DONE: Persistence with fixed IDs"
        );

        return createDockNode(DockNodeType.TASKS, tasksList);
    }

    // Methoden für dynamische Nodes (Toolbar)
    public DockNode createEditorNode(String title) {
        SerializableEditor editor = new SerializableEditor(title);
        DockNode node = new DockNode(DockNodeType.EDITOR.getId(), editor, title);
        node.setIcon(IconUtil.loadIcon(DockNodeType.EDITOR.getIconName()));
        return node;
    }

    public DockNode createPropertiesPanelNode() {
        VBox propertiesContent = new VBox(10);
        propertiesContent.setPadding(new Insets(10));
        Label propLabel = new Label("Properties");
        propLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane propsGrid = new GridPane();
        propsGrid.setHgap(10);
        propsGrid.setVgap(5);
        propsGrid.add(new Label("Name:"), 0, 0);
        propsGrid.add(new TextField("Main.java"), 1, 0);
        propsGrid.add(new Label("Type:"), 0, 1);
        propsGrid.add(new Label("Java file"), 1, 1);
        propsGrid.add(new Label("Size:"), 0, 2);
        propsGrid.add(new Label("1.2 KB"), 1, 2);
        propertiesContent.getChildren().addAll(propLabel, new Separator(), propsGrid);
        DockNode node = new DockNode(DockNodeType.PROPERTIES_PANEL.getId(), propertiesContent, "Properties");
        node.setIcon(IconUtil.loadIcon(DockNodeType.PROPERTIES_PANEL.getIconName()));
        return node;
    }

    public DockNode createConsolePanelNode() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setText("""
            SnapFX Framework v1.0
            ====================================
            [INFO] Docking system initialized
            [INFO] Layout loaded
            [INFO] Ready for drag & drop
            """);
        console.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        DockNode node = new DockNode(DockNodeType.CONSOLE_PANEL.getId(), console, "Console");
        node.setIcon(IconUtil.loadIcon(DockNodeType.CONSOLE_PANEL.getIconName()));
        return node;
    }

    public DockNode createGenericPanelNode(String title) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        Label label = new Label("This is: " + title);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        TextArea text = new TextArea("Content for " + title + "\n\nYou can add any JavaFX node here.");
        text.setPrefRowCount(10);
        content.getChildren().addAll(label, text);
        DockNode node = new DockNode(DockNodeType.GENERIC_PANEL.getId(), content, title);
        node.setIcon(IconUtil.loadIcon(DockNodeType.GENERIC_PANEL.getIconName()));
        return node;
    }
}

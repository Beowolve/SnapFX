package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.persistence.DockNodeFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Demo implementation of DockNodeFactory that creates nodes for the MainDemo application.
 * This class demonstrates how to properly implement the factory pattern for cross-session persistence.
 * All node definitions are managed centrally via the DockNodeType enum.
 */
public final class DemoNodeFactory implements DockNodeFactory {

    private static final String MAIN_JAVA = "Main.java";
    private static final String PROPERTIES = "Properties";

    /**
     * Creates a new DemoNodeFactory.
     */
    public DemoNodeFactory() {
        // No initialization required
    }

    /**
     * Creates a DockNode based on the given DockNode-ID (type-based).
     * The framework ensures unique layout IDs and calls the factory with the DockNode-ID.
     * @param dockNodeId the DockNode type ID
     * @return DockNode instance or null if not found
     */
    @Override
    public DockNode createNode(String dockNodeId) {
        Optional<DockNodeType> typeOpt = fromIdOptional(dockNodeId);
        if (typeOpt.isEmpty()) return null;
        DockNodeType type = typeOpt.get();
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
     * Returns an Optional of DockNodeType for the given id.
     * @param id DockNodeType id
     * @return Optional of DockNodeType
     */
    private Optional<DockNodeType> fromIdOptional(String id) {
        for (DockNodeType type : DockNodeType.values()) {
            if (type.getId().equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * Helper method to create a DockNode with the given type and content.
     * This centralizes the creation logic and ensures consistent properties.
     * @param type DockNodeType
     * @param content JavaFX Node content
     * @return DockNode instance
     */
    private DockNode createDockNode(DockNodeType type, Node content) {
        DockNode node = new DockNode(type.getId(), content, type.getDefaultTitle());
        node.setIcon(IconUtil.loadIcon(type.getIconName()));
        return node;
    }

    /**
     * Creates the Project Explorer node with fixed ID from enum.
     * @return DockNode instance
     */
    public DockNode createProjectExplorerNode() {
        TreeView<String> projectTree = new TreeView<>();
        TreeItem<String> root = new TreeItem<>("Project");
        root.setExpanded(true);

        TreeItem<String> src = new TreeItem<>("src");
        TreeItem<String> test = new TreeItem<>("test");
        TreeItem<String> resources = new TreeItem<>("resources");

        src.getChildren().addAll(
            List.of(
                new TreeItem<>(MAIN_JAVA),
                new TreeItem<>("Utils.java")
            )
        );

        root.getChildren().addAll(List.of(src, test, resources));
        projectTree.setRoot(root);

        return createDockNode(DockNodeType.PROJECT_EXPLORER, projectTree);
    }

    /**
     * Creates the Main Editor node with fixed ID "mainEditor".
     * Uses SerializableEditor to demonstrate content persistence.
     * @return DockNode instance
     */
    public DockNode createMainEditorNode() {
        SerializableEditor editor = new SerializableEditor(
            """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("SnapFX Demo");

                    // SnapFX.dock(myNode, "Title");
                    // Simple API for docking!
                }
            }
            """
        );

        return createDockNode(DockNodeType.MAIN_EDITOR, editor);
    }

    /**
     * Creates the Properties node with fixed ID "properties".
     * @return DockNode instance
     */
    public DockNode createPropertiesNode() {
        VBox propertiesContent = new VBox(10);
        propertiesContent.setPadding(new Insets(10));

        Label propLabel = new Label(PROPERTIES);
        propLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane propsGrid = new GridPane();
        propsGrid.setHgap(10);
        propsGrid.setVgap(5);

        propsGrid.add(new Label("Name:"), 0, 0);
        propsGrid.add(new TextField(MAIN_JAVA), 1, 0);

        propsGrid.add(new Label("Type:"), 0, 1);
        propsGrid.add(new Label("Java file"), 1, 1);

        propsGrid.add(new Label("Size:"), 0, 2);
        propsGrid.add(new Label("1.2 KB"), 1, 2);

        propertiesContent.getChildren().addAll(propLabel, new Separator(), propsGrid);

        return createDockNode(DockNodeType.PROPERTIES, propertiesContent);
    }

    /**
     * Creates the Console node with fixed ID "console".
     * @return DockNode instance
     */
    public DockNode createConsoleNode() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setText(
            """
            SnapFX Framework v1.0
            ====================================
            [INFO] Docking system initialized
            [INFO] Layout loaded
            [INFO] Ready for drag & drop

            Drag & drop is fully functional.
            Save/Load works across sessions using fixed node IDs.
            """
        );
        console.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        return createDockNode(DockNodeType.CONSOLE, console);
    }

    /**
     * Creates the Tasks node with fixed ID "tasks".
     * @return DockNode instance
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

    /**
     * Creates a new Editor node with a unique ID for dynamic addition.
     * This demonstrates how to create dynamic nodes with unique IDs.
     * @param title the title for the editor
     * @return DockNode instance
     */
    public DockNode createEditorNode(String title) {
        SerializableEditor editor = new SerializableEditor(title);
        DockNode node = new DockNode(DockNodeType.EDITOR.getId(), editor, title);
        node.setIcon(IconUtil.loadIcon(DockNodeType.EDITOR.getIconName()));
        return node;
    }

    /**
     * Creates a Properties panel node with the same content as the main Properties.
     * This demonstrates how to create dynamic nodes with unique IDs.
     * @return DockNode instance
     */
    public DockNode createPropertiesPanelNode() {
        VBox propertiesContent = new VBox(10);
        propertiesContent.setPadding(new Insets(10));
        Label propLabel = new Label(PROPERTIES);
        propLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane propsGrid = new GridPane();
        propsGrid.setHgap(10);
        propsGrid.setVgap(5);
        propsGrid.add(new Label("Name:"), 0, 0);
        propsGrid.add(new TextField(MAIN_JAVA), 1, 0);
        propsGrid.add(new Label("Type:"), 0, 1);
        propsGrid.add(new Label("Java file"), 1, 1);
        propsGrid.add(new Label("Size:"), 0, 2);
        propsGrid.add(new Label("1.2 KB"), 1, 2);
        propertiesContent.getChildren().addAll(propLabel, new Separator(), propsGrid);
        DockNode node = new DockNode(DockNodeType.PROPERTIES_PANEL.getId(), propertiesContent, PROPERTIES);
        node.setIcon(IconUtil.loadIcon(DockNodeType.PROPERTIES_PANEL.getIconName()));
        return node;
    }

    /**
     * Creates a Console panel node with the same content as the main Console.
     * This demonstrates how to create dynamic nodes with unique IDs.
     * @return DockNode instance
     */
    public DockNode createConsolePanelNode() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setText(
            """
            SnapFX Framework v1.0
            ====================================
            [INFO] Docking system initialized
            [INFO] Layout loaded
            [INFO] Ready for drag & drop
            """
        );
        console.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        DockNode node = new DockNode(DockNodeType.CONSOLE_PANEL.getId(), console, "Console");
        node.setIcon(IconUtil.loadIcon(DockNodeType.CONSOLE_PANEL.getIconName()));
        return node;
    }

    /**
     * Creates a generic panel node with the given title.
     * This demonstrates how to create dynamic nodes with unique IDs.
     * @param title the title for the panel
     * @return DockNode instance
     */
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

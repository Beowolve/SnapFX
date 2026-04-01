package org.snapfx.demo.factory;

import org.snapfx.demo.editor.SerializableEditor;
import org.snapfx.demo.i18n.DemoLocalizationService;
import org.snapfx.demo.util.IconUtil;
import org.snapfx.model.DockNode;
import org.snapfx.persistence.DockNodeFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Demo implementation of DockNodeFactory that creates nodes for the MainDemo application.
 * This class demonstrates how to properly implement the factory pattern for cross-session persistence.
 * All node definitions are managed centrally via the DockNodeType enum.
 */
public final class DemoNodeFactory implements DockNodeFactory {
    private static final String MAIN_JAVA = "Main.java";

    private final boolean useFrameworkUnknownNodePlaceholder;
    private final DemoLocalizationService localizationService;

    /**
     * Creates a new DemoNodeFactory.
     */
    public DemoNodeFactory() {
        this(true);
    }

    /**
     * Creates a new DemoNodeFactory with localized resources.
     *
     * @param localizationService localization service used for demo strings
     */
    public DemoNodeFactory(DemoLocalizationService localizationService) {
        this(true, localizationService);
    }

    /**
     * Creates a new DemoNodeFactory with configurable unknown-node fallback behavior.
     *
     * @param useFrameworkUnknownNodePlaceholder {@code true} to use SnapFX built-in placeholders,
     *                                           {@code false} to return demo-specific fallback nodes
     */
    public DemoNodeFactory(boolean useFrameworkUnknownNodePlaceholder) {
        this(useFrameworkUnknownNodePlaceholder, new DemoLocalizationService(DemoNodeFactory.class.getModule()));
    }

    /**
     * Creates a new DemoNodeFactory with configurable unknown-node fallback behavior and localization service.
     *
     * @param useFrameworkUnknownNodePlaceholder {@code true} to use SnapFX built-in placeholders,
     *                                           {@code false} to return demo-specific fallback nodes
     * @param localizationService localization service used for demo strings
     */
    public DemoNodeFactory(boolean useFrameworkUnknownNodePlaceholder, DemoLocalizationService localizationService) {
        this.useFrameworkUnknownNodePlaceholder = useFrameworkUnknownNodePlaceholder;
        this.localizationService = Objects.requireNonNull(localizationService, "localizationService");
    }

    /**
     * Creates a DockNode based on the given DockNode-ID (type-based).
     * The framework ensures unique layout IDs and calls the factory with the DockNode-ID.
     *
     * @param dockNodeId the DockNode type ID
     * @return DockNode instance or null if not found
     */
    @Override
    public DockNode createNode(String dockNodeId) {
        Optional<DockNodeType> typeOpt = fromIdOptional(dockNodeId);
        if (typeOpt.isEmpty()) {
            return null;
        }
        DockNodeType type = typeOpt.get();
        return switch (type) {
            case PROJECT_EXPLORER -> createProjectExplorerNode();
            case MAIN_EDITOR -> createMainEditorNode();
            case PROPERTIES -> createPropertiesNode();
            case CONSOLE -> createConsoleNode();
            case TASKS -> createTasksNode();
            case EDITOR -> createUntitledEditorNode();
            case PROPERTIES_PANEL -> createPropertiesPanelNode();
            case CONSOLE_PANEL -> createConsolePanelNode();
            case GENERIC_PANEL -> createGenericPanelNode(localizationService.text("demo.node.genericPanel.title"));
        };
    }

    @Override
    public DockNode createUnknownNode(UnknownElementContext context) {
        if (useFrameworkUnknownNodePlaceholder || context == null) {
            return null;
        }
        String resolvedDockNodeId = context.dockNodeId() == null || context.dockNodeId().isBlank()
            ? "unknown"
            : context.dockNodeId();
        String resolvedTitle = context.title() == null || context.title().isBlank()
            ? localizationService.text("demo.node.unavailable")
            : context.title();
        Label content = new Label(
            localizationService.text(
                "demo.node.unknown.content",
                context.elementType(),
                resolvedDockNodeId,
                context.jsonPath()
            )
        );
        return new DockNode(resolvedDockNodeId, content, resolvedTitle);
    }

    /**
     * Returns an Optional of DockNodeType for the given id.
     *
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
     *
     * @param type DockNodeType
     * @param content JavaFX Node content
     * @return DockNode instance
     */
    private DockNode createDockNode(DockNodeType type, Node content) {
        String titleKey = type.getDefaultTitleKey();
        DockNode node = new DockNode(
            type.getId(),
            content,
            titleKey == null ? type.getDefaultTitle() : localizationService.text(titleKey)
        );
        if (titleKey != null) {
            registerLocalizedNodeTitle(node, titleKey);
        }
        node.setIcon(IconUtil.loadImage(type.getIconName()));
        return node;
    }

    /**
     * Creates the Project Explorer node with fixed ID from enum.
     *
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
     *
     * @return DockNode instance
     */
    public DockNode createMainEditorNode() {
        SerializableEditor editor = new SerializableEditor(
            """
            public class Main {
                public static void main(String[] args) {
                    System.out.println(\"SnapFX Demo\");

                    // SnapFX.dock(myNode, \"Title\");
                    // Simple API for docking!
                }
            }
            """
        );

        return createDockNode(DockNodeType.MAIN_EDITOR, editor);
    }

    /**
     * Creates the Properties node with fixed ID "properties".
     *
     * @return DockNode instance
     */
    public DockNode createPropertiesNode() {
        return createDockNode(DockNodeType.PROPERTIES, createPropertiesContent());
    }

    /**
     * Creates the Console node with fixed ID "console".
     *
     * @return DockNode instance
     */
    public DockNode createConsoleNode() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.textProperty().bind(localizationService.createBinding("demo.node.console.text"));
        console.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        return createDockNode(DockNodeType.CONSOLE, console);
    }

    /**
     * Creates the Tasks node with fixed ID "tasks".
     *
     * @return DockNode instance
     */
    public DockNode createTasksNode() {
        ListView<String> tasksList = new ListView<>();
        updateTaskItems(tasksList);
        localizationService.localeProperty().addListener((obs, oldLocale, newLocale) -> updateTaskItems(tasksList));

        return createDockNode(DockNodeType.TASKS, tasksList);
    }

    /**
     * Creates a new untitled Editor node using the current locale.
     *
     * @return DockNode instance
     */
    public DockNode createUntitledEditorNode() {
        return createEditorNode(localizationService.text("demo.node.editor.untitled"));
    }

    /**
     * Creates a new Editor node with a unique ID for dynamic addition.
     * This demonstrates how to create dynamic nodes with unique IDs.
     *
     * @param title the title for the editor
     * @return DockNode instance
     */
    public DockNode createEditorNode(String title) {
        SerializableEditor editor = new SerializableEditor(title);
        DockNode node = new DockNode(DockNodeType.EDITOR.getId(), editor, title);
        node.setIcon(IconUtil.loadImage(DockNodeType.EDITOR.getIconName()));
        return node;
    }

    /**
     * Creates a Properties panel node with the same content as the main Properties.
     * This demonstrates how to create dynamic nodes with unique IDs.
     *
     * @return DockNode instance
     */
    public DockNode createPropertiesPanelNode() {
        return createDockNode(DockNodeType.PROPERTIES_PANEL, createPropertiesContent());
    }

    /**
     * Creates a Console panel node with the same content as the main Console.
     * This demonstrates how to create dynamic nodes with unique IDs.
     *
     * @return DockNode instance
     */
    public DockNode createConsolePanelNode() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.textProperty().bind(localizationService.createBinding("demo.node.consolePanel.text"));
        console.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        DockNode node = new DockNode(
            DockNodeType.CONSOLE_PANEL.getId(),
            console,
            localizationService.text(DockNodeType.CONSOLE_PANEL.getDefaultTitleKey())
        );
        registerLocalizedNodeTitle(node, DockNodeType.CONSOLE_PANEL.getDefaultTitleKey());
        node.setIcon(IconUtil.loadImage(DockNodeType.CONSOLE_PANEL.getIconName()));
        return node;
    }

    /**
     * Creates a generic panel node with the given title.
     * This demonstrates how to create dynamic nodes with unique IDs.
     *
     * @param title the title for the panel
     * @return DockNode instance
     */
    public DockNode createGenericPanelNode(String title) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        Label label = new Label(localizationService.text("demo.node.generic.header", title));
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        TextArea text = new TextArea(localizationService.text("demo.node.generic.content", title));
        text.setPrefRowCount(10);
        content.getChildren().addAll(label, text);
        DockNode node = new DockNode(DockNodeType.GENERIC_PANEL.getId(), content, title);
        if (localizationService.matchesInSupportedLocales("demo.node.genericPanel.title", title)) {
            registerLocalizedNodeTitle(node, DockNodeType.GENERIC_PANEL.getDefaultTitleKey());
        }
        node.setIcon(IconUtil.loadImage(DockNodeType.GENERIC_PANEL.getIconName()));
        return node;
    }

    private VBox createPropertiesContent() {
        VBox propertiesContent = new VBox(10);
        propertiesContent.setPadding(new Insets(10));

        Label headerLabel = new Label();
        localizationService.bind(headerLabel, "demo.node.properties.header");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane propsGrid = new GridPane();
        propsGrid.setHgap(10);
        propsGrid.setVgap(5);

        Label nameLabel = new Label();
        localizationService.bind(nameLabel, "demo.node.properties.name");
        Label typeLabel = new Label();
        localizationService.bind(typeLabel, "demo.node.properties.type");
        Label sizeLabel = new Label();
        localizationService.bind(sizeLabel, "demo.node.properties.size");
        Label javaFileLabel = new Label();
        localizationService.bind(javaFileLabel, "demo.node.properties.javaFile");
        Label sizeValueLabel = new Label();
        localizationService.bind(sizeValueLabel, "demo.node.properties.sizeValue");
        TextField fileNameField = new TextField(localizationService.text("demo.node.properties.mainJava"));

        propsGrid.add(nameLabel, 0, 0);
        propsGrid.add(fileNameField, 1, 0);
        propsGrid.add(typeLabel, 0, 1);
        propsGrid.add(javaFileLabel, 1, 1);
        propsGrid.add(sizeLabel, 0, 2);
        propsGrid.add(sizeValueLabel, 1, 2);

        propertiesContent.getChildren().addAll(headerLabel, new Separator(), propsGrid);
        return propertiesContent;
    }

    private void updateTaskItems(ListView<String> tasksList) {
        tasksList.getItems().setAll(
            localizationService.text("demo.node.tasks.todoDnd"),
            localizationService.text("demo.node.tasks.todoFloating"),
            localizationService.text("demo.node.tasks.todoZones"),
            localizationService.text("demo.node.tasks.doneArchitecture"),
            localizationService.text("demo.node.tasks.doneLayout"),
            localizationService.text("demo.node.tasks.donePersistence")
        );
    }

    private void registerLocalizedNodeTitle(DockNode node, String titleKey) {
        if (node == null || titleKey == null || titleKey.isBlank()) {
            return;
        }
        localizationService.localeProperty().addListener((obs, oldLocale, newLocale) ->
            node.setTitle(localizationService.text(titleKey))
        );
    }
}

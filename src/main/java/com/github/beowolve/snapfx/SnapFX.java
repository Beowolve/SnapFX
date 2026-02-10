package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.*;
import com.github.beowolve.snapfx.persistence.DockLayoutSerializer;
import com.github.beowolve.snapfx.persistence.DockNodeFactory;
import com.github.beowolve.snapfx.view.DockLayoutEngine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * Main API class for the SnapFX docking framework.
 * Provides a simple, fluent API for docking JavaFX nodes.
 */
public class SnapFX {
    private final DockGraph dockGraph;
    private final DockLayoutEngine layoutEngine;
    private final DockDragService dragService;
    private final DockLayoutSerializer serializer;
    private Stage primaryStage;

    // Hidden nodes (removed from layout but not destroyed)
    private final ObservableList<DockNode> hiddenNodes;

    private Pane rootContainer; // Container that holds the buildLayout() result

    public SnapFX() {
        this.dockGraph = new DockGraph();
        this.dragService = new DockDragService(dockGraph);
        this.layoutEngine = new DockLayoutEngine(dockGraph, dragService);
        this.serializer = new DockLayoutSerializer(dockGraph);
        this.hiddenNodes = FXCollections.observableArrayList();

        // Auto-rebuild view when revision changes (after D&D, dock/undock operations)
        this.dockGraph.revisionProperty().addListener((obs, o, n) -> {
            if (rootContainer != null) {
                // Rebuild on next frame to ensure all model changes are complete
                javafx.application.Platform.runLater(this::rebuildRootView);
            }
        });

        // Auto-rebuild view when root element changes
        this.dockGraph.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (oldRoot != newRoot && rootContainer != null) {
                // Root element changed, rebuild the view
                javafx.application.Platform.runLater(this::rebuildRootView);
            }
        });
    }

    /**
     * Initializes SnapFX with the primary stage.
     */
    public void initialize(Stage stage) {
        this.primaryStage = stage;
        dragService.initialize(stage);
        dragService.setLayoutEngine(layoutEngine);
    }

    /**
     * Simple API to dock a node with a title.
     * Creates a DockNode and adds it to the root.
     */
    public DockNode dock(Node content, String title) {
        DockNode dockNode = new DockNode(content, title);

        if (dockGraph.getRoot() == null) {
            dockGraph.setRoot(dockNode);
        } else {
            // Default: split to the right
            dockGraph.dock(dockNode, dockGraph.getRoot(), DockPosition.RIGHT);
        }

        return dockNode;
    }

    /**
     * Docks a node at a specific position relative to a target.
     */
    public DockNode dock(Node content, String title, DockElement target, DockPosition position) {
        DockNode dockNode = new DockNode(content, title);
        dockGraph.dock(dockNode, target, position);
        return dockNode;
    }

    /**
     * Docks an existing DockNode at a specific position.
     */
    public void dock(DockNode node, DockElement target, DockPosition position) {
        dockGraph.dock(node, target, position);
    }

    /**
     * Removes a DockNode from the graph.
     */
    @SuppressWarnings("unused")
    public void undock(DockNode node) {
        dockGraph.undock(node);
    }

    /**
     * Builds the visual representation of the current dock layout.
     * The returned Parent will automatically update when the model changes.
     */
    public Parent buildLayout() {
        Node layout = layoutEngine.buildSceneGraph();

        // Wrap in a container that we can update
        if (rootContainer == null) {
            rootContainer = new javafx.scene.layout.StackPane();
        }

        rootContainer.getChildren().clear();
        if (layout != null) {
            rootContainer.getChildren().add(layout);
        }

        return rootContainer;
    }

    private void rebuildRootView() {
        if (rootContainer == null) {
            return;
        }

        Node layout = layoutEngine.buildSceneGraph();
        rootContainer.getChildren().clear();
        if (layout != null) {
            rootContainer.getChildren().add(layout);
        }
    }

    /**
     * Locks or unlocks the layout (disables drag & drop when locked).
     */
    public void setLocked(boolean locked) {
        dockGraph.setLocked(locked);
    }

    @SuppressWarnings("unused")
    public boolean isLocked() {
        return dockGraph.isLocked();
    }

    /**
     * Sets the factory used to create DockNodes when loading layouts.
     * This is required for proper persistence across application sessions.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * snapFX.setNodeFactory(nodeId -> switch(nodeId) {
     *     case "projectExplorer" -> createProjectExplorer();
     *     case "mainEditor" -> createMainEditor();
     *     default -> null;
     * });
     * }</pre>
     *
     * @param factory Factory that creates nodes from their IDs
     */
    public void setNodeFactory(DockNodeFactory factory) {
        serializer.setNodeFactory(factory);
    }

    /**
     * Saves the current layout as JSON.
     */
    public String saveLayout() {
        return serializer.serialize();
    }

    /**
     * Loads a layout from JSON.
     */
    public void loadLayout(String json) {
        serializer.deserialize(json);
        // Rebuild view
        layoutEngine.clearCache();
    }

    /**
     * Hides a DockNode (removes from layout but keeps in memory for restore).
     */
    public void hide(DockNode node) {
        if (node == null || hiddenNodes.contains(node)) {
            return;
        }

        // Store last known position
        if (node.getParent() != null) {
            DockContainer parent = node.getParent();
            int index = parent.getChildren().indexOf(node);

            // Find a sibling or parent as target
            DockElement target = null;
            DockPosition position = DockPosition.RIGHT;

            if (parent.getChildren().size() > 1) {
                // Use sibling as target
                if (index > 0) {
                    target = parent.getChildren().get(index - 1);
                    position = DockPosition.RIGHT;
                } else if (index < parent.getChildren().size() - 1) {
                    target = parent.getChildren().get(index + 1);
                    position = DockPosition.LEFT;
                }
            } else {
                // Use parent as target
                target = parent;
                position = DockPosition.CENTER;
            }

            node.setLastKnownTarget(target);
            node.setLastKnownPosition(position);
        }

        // Remove from layout
        dockGraph.undock(node);

        // Add to hidden list
        hiddenNodes.add(node);
    }

    /**
     * Restores a hidden DockNode back to the layout.
     */
    public void restore(DockNode node) {
        if (node == null || !hiddenNodes.contains(node)) {
            return;
        }

        hiddenNodes.remove(node);

        // Try to restore at last known position
        DockElement target = node.getLastKnownTarget();
        DockPosition position = node.getLastKnownPosition();

        // Validate target still exists in graph
        if (target != null && isInGraph(target)) {
            dockGraph.dock(node, target, position);
        } else {
            // Fallback: dock to root
            if (dockGraph.getRoot() == null) {
                dockGraph.setRoot(node);
            } else {
                dockGraph.dock(node, dockGraph.getRoot(), DockPosition.RIGHT);
            }
        }
    }

    private boolean isInGraph(DockElement element) {
        if (element == dockGraph.getRoot()) {
            return true;
        }
        return findInGraph(dockGraph.getRoot(), element);
    }

    private boolean findInGraph(DockElement current, DockElement target) {
        if (current == null) {
            return false;
        }
        if (current == target) {
            return true;
        }
        if (current instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                if (findInGraph(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ObservableList<DockNode> getHiddenNodes() {
        return hiddenNodes;
    }

    // Getters
    public DockGraph getDockGraph() {
        return dockGraph;
    }

    @SuppressWarnings("unused")
    public DockLayoutEngine getLayoutEngine() {
        return layoutEngine;
    }

    public DockDragService getDragService() {
        return dragService;
    }

    @SuppressWarnings("unused")
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setOnNodeCloseRequest(Consumer<DockNode> handler) {
        layoutEngine.setOnNodeCloseRequest(handler);
    }

    public int getDockNodeCount(String id) {
        return dockGraph.getDockNodeCount(id);
    }
}

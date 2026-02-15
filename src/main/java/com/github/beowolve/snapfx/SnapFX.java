package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.dnd.DockDropVisualizationMode;
import com.github.beowolve.snapfx.model.*;
import com.github.beowolve.snapfx.persistence.DockLayoutSerializer;
import com.github.beowolve.snapfx.persistence.DockNodeFactory;
import com.github.beowolve.snapfx.view.DockCloseButtonMode;
import com.github.beowolve.snapfx.view.DockLayoutEngine;
import com.github.beowolve.snapfx.view.DockTitleBarMode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
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
    private final ObservableList<DockFloatingWindow> floatingWindows;

    private Pane rootContainer; // Container that holds the buildLayout() result

    public SnapFX() {
        this.dockGraph = new DockGraph();
        this.dragService = new DockDragService(dockGraph);
        this.layoutEngine = new DockLayoutEngine(dockGraph, dragService);
        this.serializer = new DockLayoutSerializer(dockGraph);
        this.hiddenNodes = FXCollections.observableArrayList();
        this.floatingWindows = FXCollections.observableArrayList();
        this.layoutEngine.setOnNodeFloatRequest(node -> floatNode(node));
        this.dragService.setOnDropRequest(this::handleResolvedDropRequest);
        this.dragService.setOnFloatDetachRequest(this::handleUnresolvedDropRequest);

        // Auto-rebuild view when revision changes (after D&D, dock/undock operations)
        this.dockGraph.revisionProperty().addListener((obs, o, n) -> {
            if (rootContainer != null) {
                // Rebuild on next frame to ensure all model changes are complete
                Platform.runLater(this::rebuildRootView);
            }
        });

        // Auto-rebuild view when root element changes
        this.dockGraph.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (oldRoot != newRoot && rootContainer != null) {
                // Root element changed, rebuild the view
                Platform.runLater(this::rebuildRootView);
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
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.show(primaryStage);
        }
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
            rootContainer = new StackPane();
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

    private void requestRebuild() {
        if (rootContainer != null) {
            Platform.runLater(this::rebuildRootView);
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
        closeAllFloatingWindows(false);
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
        DockFloatingWindow floatingWindow = findFloatingWindow(node);
        if (floatingWindow != null) {
            rememberFloatingBoundsForNodes(floatingWindow);
            floatingWindow.undockNode(node);
            if (floatingWindow.isEmpty()) {
                floatingWindows.remove(floatingWindow);
                floatingWindow.closeWithoutNotification();
            }
        } else if (isInGraph(node)) {
            rememberLastKnownPlacement(node);
            dockGraph.undock(node);
        }

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
        dockAtRememberedOrFallback(node);
    }

    /**
     * Moves an existing DockNode into an external floating window.
     */
    public DockFloatingWindow floatNode(DockNode node) {
        return floatNode(node, null, null);
    }

    /**
     * Moves an existing DockNode into an external floating window at a screen position.
     * The screen coordinates can be on any monitor.
     */
    public DockFloatingWindow floatNode(DockNode node, Double screenX, Double screenY) {
        if (node == null) {
            return null;
        }

        DockFloatingWindow existingWindow = findFloatingWindow(node);
        if (existingWindow != null) {
            existingWindow.setPreferredPosition(screenX, screenY);
            existingWindow.show(primaryStage);
            return existingWindow;
        }

        hiddenNodes.remove(node);

        DockFloatingWindow sourceFloatingWindow = null;
        if (isInGraph(node)) {
            rememberLastKnownPlacement(node);
            dockGraph.undock(node);
        } else {
            sourceFloatingWindow = findFloatingWindow(node);
            if (sourceFloatingWindow != null) {
                sourceFloatingWindow.undockNode(node);
                if (sourceFloatingWindow.isEmpty()) {
                    floatingWindows.remove(sourceFloatingWindow);
                    sourceFloatingWindow.closeWithoutNotification();
                }
            }
        }

        DockFloatingWindow floatingWindow = new DockFloatingWindow(node, dragService);
        applyRememberedFloatingBounds(node, floatingWindow);
        if (screenX != null || screenY != null) {
            floatingWindow.setPreferredPosition(screenX, screenY);
        }
        floatingWindow.setOnAttachRequested(() -> attachFloatingWindow(floatingWindow));
        floatingWindow.setOnWindowClosed(this::handleFloatingWindowClosed);
        floatingWindows.add(floatingWindow);
        if (primaryStage != null) {
            floatingWindow.show(primaryStage);
        }
        return floatingWindow;
    }

    /**
     * Attaches a floating window back into the main layout.
     */
    public void attachFloatingWindow(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null || !floatingWindows.remove(floatingWindow)) {
            return;
        }

        floatingWindow.closeWithoutNotification();
        rememberFloatingBoundsForNodes(floatingWindow);

        List<DockNode> nodesToAttach = new ArrayList<>(floatingWindow.getDockNodes());
        for (DockNode node : nodesToAttach) {
            floatingWindow.undockNode(node);
        }

        for (DockNode node : nodesToAttach) {
            dockAtRememberedOrFallback(node);
        }
    }

    /**
     * Returns all currently open floating windows.
     */
    public ObservableList<DockFloatingWindow> getFloatingWindows() {
        return FXCollections.unmodifiableObservableList(floatingWindows);
    }

    private void handleResolvedDropRequest(DockDragService.DropRequest request) {
        if (request == null
            || request.draggedNode() == null
            || request.target() == null
            || request.position() == null) {
            return;
        }

        DockNode draggedNode = request.draggedNode();
        DockFloatingWindow sourceWindow = findFloatingWindow(draggedNode);

        if (sourceWindow == null) {
            dockGraph.move(draggedNode, request.target(), request.position(), request.tabIndex());
            return;
        }

        sourceWindow.undockNode(draggedNode);
        if (sourceWindow.isEmpty()) {
            rememberFloatingBoundsForNodes(sourceWindow);
            floatingWindows.remove(sourceWindow);
            sourceWindow.closeWithoutNotification();
        }

        dockGraph.dock(draggedNode, request.target(), request.position(), request.tabIndex());
    }

    private void handleUnresolvedDropRequest(DockDragService.FloatDetachRequest request) {
        if (request == null || request.draggedNode() == null) {
            return;
        }
        if (tryDropIntoFloatingWindow(request.draggedNode(), request.screenX(), request.screenY())) {
            return;
        }
        floatNode(request.draggedNode(), request.screenX(), request.screenY());
    }

    private boolean tryDropIntoFloatingWindow(DockNode node, double screenX, double screenY) {
        DockFloatingWindow bestWindow = null;
        DockFloatingWindow.DropTarget bestTarget = null;

        for (DockFloatingWindow floatingWindow : floatingWindows) {
            DockFloatingWindow.DropTarget candidate = floatingWindow.resolveDropTarget(screenX, screenY, node);
            if (candidate == null) {
                continue;
            }
            if (bestTarget == null
                || candidate.depth() > bestTarget.depth()
                || (candidate.depth() == bestTarget.depth() && candidate.area() < bestTarget.area())) {
                bestWindow = floatingWindow;
                bestTarget = candidate;
            }
        }

        if (bestWindow == null || bestTarget == null) {
            return false;
        }

        DockFloatingWindow sourceWindow = findFloatingWindow(node);
        if (sourceWindow != null && sourceWindow == bestWindow) {
            bestWindow.moveNode(node, bestTarget.target(), bestTarget.position(), bestTarget.tabIndex());
            bestWindow.toFront();
            return true;
        }

        if (isInGraph(node)) {
            rememberLastKnownPlacement(node);
            dockGraph.undock(node);
        } else if (sourceWindow != null) {
            rememberFloatingBoundsForNodes(sourceWindow);
            sourceWindow.undockNode(node);
            if (sourceWindow.isEmpty()) {
                floatingWindows.remove(sourceWindow);
                sourceWindow.closeWithoutNotification();
            }
        }

        hiddenNodes.remove(node);
        bestWindow.dockNode(node, bestTarget.target(), bestTarget.position(), bestTarget.tabIndex());
        bestWindow.toFront();
        return true;
    }

    private void applyRememberedFloatingBounds(DockNode node, DockFloatingWindow floatingWindow) {
        if (node == null || floatingWindow == null) {
            return;
        }
        if (node.getLastFloatingWidth() != null && node.getLastFloatingHeight() != null) {
            floatingWindow.setPreferredSize(node.getLastFloatingWidth(), node.getLastFloatingHeight());
        }
        if (node.getLastFloatingX() != null || node.getLastFloatingY() != null) {
            floatingWindow.setPreferredPosition(node.getLastFloatingX(), node.getLastFloatingY());
        }
    }

    private void rememberFloatingBoundsForNodes(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        floatingWindow.captureCurrentBounds();
        for (DockNode node : floatingWindow.getDockNodes()) {
            node.setLastFloatingX(floatingWindow.getPreferredX());
            node.setLastFloatingY(floatingWindow.getPreferredY());
            node.setLastFloatingWidth(floatingWindow.getPreferredWidth());
            node.setLastFloatingHeight(floatingWindow.getPreferredHeight());
        }
    }

    private boolean isInGraph(DockNode node) {
        return node != null && findInGraph(dockGraph.getRoot(), node);
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

    public void setDropVisualizationMode(DockDropVisualizationMode mode) {
        dragService.setDropVisualizationMode(mode);
    }

    public DockDropVisualizationMode getDropVisualizationMode() {
        return dragService.getDropVisualizationMode();
    }

    public void setCloseButtonMode(DockCloseButtonMode mode) {
        layoutEngine.setCloseButtonMode(mode);
        requestRebuild();
    }

    public DockCloseButtonMode getCloseButtonMode() {
        return layoutEngine.getCloseButtonMode();
    }

    public void setTitleBarMode(DockTitleBarMode mode) {
        layoutEngine.setTitleBarMode(mode);
        requestRebuild();
    }

    public DockTitleBarMode getTitleBarMode() {
        return layoutEngine.getTitleBarMode();
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

    /**
     * Sets the pane ratios of the root split pane.
     * <p>
     * Ratios are normalized automatically, so both {@code (0.25, 0.5, 0.25)}
     * and {@code (25, 50, 25)} are valid inputs.
     * </p>
     *
     * @param paneRatios Ratios for each pane of the root split pane
     * @return {@code true} if ratios were applied; {@code false} if the root is not a split pane
     * or if input validation failed
     */
    public boolean setRootSplitRatios(double... paneRatios) {
        DockElement root = dockGraph.getRoot();
        if (!(root instanceof DockSplitPane rootSplit)) {
            return false;
        }
        return setSplitRatios(rootSplit, paneRatios);
    }

    /**
     * Sets the pane ratios of a specific split pane.
     * <p>
     * The number of ratios must match the number of split children.
     * </p>
     *
     * @param splitPane Split pane to configure
     * @param paneRatios Ratios for each pane in the split pane
     * @return {@code true} if ratios were applied; {@code false} if validation failed
     */
    public boolean setSplitRatios(DockSplitPane splitPane, double... paneRatios) {
        if (splitPane == null || paneRatios == null) {
            return false;
        }

        int paneCount = splitPane.getChildren().size();
        if (paneCount < 2 || paneRatios.length != paneCount) {
            return false;
        }

        double sum = 0.0;
        for (double ratio : paneRatios) {
            if (!Double.isFinite(ratio) || ratio <= 0.0) {
                return false;
            }
            sum += ratio;
        }

        if (sum <= 0.0) {
            return false;
        }

        double cumulative = 0.0;
        for (int i = 0; i < paneRatios.length - 1; i++) {
            cumulative += paneRatios[i] / sum;
            splitPane.setDividerPosition(i, cumulative);
        }

        requestRebuild();
        return true;
    }

    private DockFloatingWindow findFloatingWindow(DockNode node) {
        if (node == null) {
            return null;
        }
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            if (floatingWindow.containsNode(node)) {
                return floatingWindow;
            }
        }
        return null;
    }

    private void closeAllFloatingWindows(boolean attachBack) {
        if (floatingWindows.isEmpty()) {
            return;
        }
        for (DockFloatingWindow floatingWindow : new ArrayList<>(floatingWindows)) {
            if (attachBack) {
                attachFloatingWindow(floatingWindow);
            } else {
                floatingWindows.remove(floatingWindow);
                floatingWindow.closeWithoutNotification();
            }
        }
    }

    private void handleFloatingWindowClosed(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null || !floatingWindows.remove(floatingWindow)) {
            return;
        }
        rememberFloatingBoundsForNodes(floatingWindow);
        List<DockNode> nodes = new ArrayList<>(floatingWindow.getDockNodes());
        for (DockNode node : nodes) {
            floatingWindow.undockNode(node);
        }
        for (DockNode node : nodes) {
            dockAtRememberedOrFallback(node);
        }
    }

    private void rememberLastKnownPlacement(DockNode node) {
        if (node == null || node.getParent() == null) {
            return;
        }

        DockContainer parent = node.getParent();
        int index = parent.getChildren().indexOf(node);

        if (parent instanceof DockTabPane tabPane) {
            DockElement tabTarget = tabPane;
            if (tabPane.getChildren().size() > 1) {
                if (index > 0) {
                    tabTarget = tabPane.getChildren().get(index - 1);
                } else if (index < tabPane.getChildren().size() - 1) {
                    tabTarget = tabPane.getChildren().get(index + 1);
                }
            }
            node.setLastKnownTarget(tabTarget);
            node.setLastKnownPosition(DockPosition.CENTER);
            node.setLastKnownTabIndex(index >= 0 ? index : tabPane.getChildren().size());
            return;
        }

        DockElement target = null;
        DockPosition position = DockPosition.RIGHT;
        node.setLastKnownTabIndex(null);

        if (parent.getChildren().size() > 1) {
            if (index > 0) {
                target = parent.getChildren().get(index - 1);
                position = DockPosition.RIGHT;
            } else if (index < parent.getChildren().size() - 1) {
                target = parent.getChildren().get(index + 1);
                position = DockPosition.LEFT;
            }
        } else {
            target = parent;
            position = DockPosition.CENTER;
        }

        node.setLastKnownTarget(target);
        node.setLastKnownPosition(position);
    }

    private void dockAtRememberedOrFallback(DockNode node) {
        DockElement target = node.getLastKnownTarget();
        DockPosition position = node.getLastKnownPosition();
        Integer tabIndex = node.getLastKnownTabIndex();

        if (target != null && position != null && isInGraph(target)) {
            dockGraph.dock(node, target, position, position == DockPosition.CENTER ? tabIndex : null);
            return;
        }

        if (dockGraph.getRoot() == null) {
            dockGraph.setRoot(node);
        } else {
            dockGraph.dock(node, dockGraph.getRoot(), DockPosition.RIGHT);
        }
    }
}

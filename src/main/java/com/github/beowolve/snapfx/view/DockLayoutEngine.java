package com.github.beowolve.snapfx.view;

import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.*;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Layout engine that converts the logical DockGraph into a visual scene graph.
 * Keeps model and view in sync.
 */
public class DockLayoutEngine {
    private final DockGraph dockGraph;
    private final DockDragService dragService;
    private final Map<String, Node> viewCache;
    private final Map<DockNode, DockNodeView> dockNodeViews;  // Use DockNode instance as key, not ID

    private Consumer<DockNode> onNodeCloseRequest;

    public DockLayoutEngine(DockGraph dockGraph, DockDragService dragService) {
        this.dockGraph = dockGraph;
        this.dragService = dragService;
        this.viewCache = new HashMap<>();
        this.dockNodeViews = new HashMap<>();
    }

    /**
     * Builds the visual representation of the DockGraph.
     */
    public Node buildSceneGraph() {
        // Always clear caches to ensure fresh views
        // This is critical after D&D operations to ensure views are properly attached
        clearCache();

        DockElement root = dockGraph.getRoot();

        if (root == null) {
            return new StackPane(); // Empty container
        }

        return createView(root);
    }

    /**
     * Recursively creates the view for a DockElement.
     */
    private Node createView(DockElement element) {
        if (element == null) {
            return new StackPane();
        }

        // Check cache
        Node cached = viewCache.get(element.getId());
        if (cached != null) {
            return cached;
        }

        Node view = switch (element) {
            case DockNode dockNode -> createDockNodeView(dockNode);
            case DockSplitPane splitPane -> createSplitPaneView(splitPane);
            case DockTabPane tabPane -> createTabPaneView(tabPane);
            default -> new StackPane();
        };

        viewCache.put(element.getId(), view);
        return view;
    }

    private Node createDockNodeView(DockNode dockNode) {
        DockNodeView nodeView = new DockNodeView(dockNode, dockGraph, dragService);

        // Set close button action
        // If custom handler is set, use it; otherwise use default undock behavior
        if (onNodeCloseRequest != null) {
            nodeView.setOnCloseRequest(() -> onNodeCloseRequest.accept(dockNode));
        } else {
            nodeView.setOnCloseRequest(() -> dockGraph.undock(dockNode));
        }

        dockNodeViews.put(dockNode, nodeView);  // Use instance as key
        return nodeView;
    }

    private Node createSplitPaneView(DockSplitPane model) {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(model.getOrientation());
        splitPane.getStyleClass().add("dock-split-pane");

        // Add children
        for (DockElement child : model.getChildren()) {
            Node childView = createView(child);
            splitPane.getItems().add(childView);
        }

        // Bind divider positions
        bindDividerPositions(splitPane, model);

        // Listener for changes to children
        model.getChildren().addListener((javafx.collections.ListChangeListener<DockElement>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    rebuildSplitPane(splitPane, model);
                }
            }
        });

        return splitPane;
    }

    private void rebuildSplitPane(SplitPane splitPane, DockSplitPane model) {
        // Store divider positions
        double[] positions = splitPane.getDividerPositions();

        splitPane.getItems().clear();
        for (DockElement child : model.getChildren()) {
            Node childView = createView(child);
            splitPane.getItems().add(childView);
        }

        // Restore divider positions
        if (positions.length > 0 && !splitPane.getDividers().isEmpty()) {
            splitPane.setDividerPositions(positions);
        }

        bindDividerPositions(splitPane, model);
    }

    private void bindDividerPositions(SplitPane splitPane, DockSplitPane model) {
        for (int i = 0; i < model.getDividerPositions().size() && i < splitPane.getDividers().size(); i++) {
            final int index = i;
            SplitPane.Divider divider = splitPane.getDividers().get(i);

            // Bidirectional binding (divider -> model)
            divider.positionProperty().addListener((obs, old, newVal) -> {
                if (index < model.getDividerPositions().size()) {
                    model.setDividerPosition(index, newVal.doubleValue());
                }
            });

            // Set initial position
            if (index < model.getDividerPositions().size()) {
                divider.setPosition(model.getDividerPositions().get(index).get());
            }
        }
    }

    private Node createTabPaneView(DockTabPane model) {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("dock-tab-pane");

        // Create tabs
        for (DockElement child : model.getChildren()) {
            Tab tab = createTab(child);
            tabPane.getTabs().add(tab);
        }

        // Bind selection
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                model.setSelectedIndex(newVal.intValue());
            }
        });

        model.selectedIndexProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && newVal.intValue() >= 0 && newVal.intValue() < tabPane.getTabs().size()) {
                tabPane.getSelectionModel().select(newVal.intValue());
            }
        });

        // Set initial selection
        if (model.getSelectedIndex() >= 0 && model.getSelectedIndex() < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(model.getSelectedIndex());
        }

        // Listener for changes
        model.getChildren().addListener((javafx.collections.ListChangeListener<DockElement>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    rebuildTabPane(tabPane, model);
                }
            }
        });

        // Auto-hide in locked mode
        tabPane.visibleProperty().bind(
            dockGraph.lockedProperty().not()
            .or(javafx.beans.binding.Bindings.size(tabPane.getTabs()).greaterThan(1))
        );

        return tabPane;
    }

    private void rebuildTabPane(TabPane tabPane, DockTabPane model) {
        int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();

        // Clear cache entries for all children to ensure fresh views
        for (DockElement child : model.getChildren()) {
            clearCacheForElement(child);
        }

        tabPane.getTabs().clear();
        for (DockElement child : model.getChildren()) {
            Tab tab = createTab(child);
            tabPane.getTabs().add(tab);
        }

        // Restore selection
        if (selectedIndex >= 0 && selectedIndex < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(selectedIndex);
        }
    }

    /**
     * Recursively clears cache entries for an element and all its children.
     */
    private void clearCacheForElement(DockElement element) {
        if (element == null) {
            return;
        }

        viewCache.remove(element.getId());
        if (element instanceof DockNode dockNode) {
            dockNodeViews.remove(dockNode);  // Use instance as key
        }

        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                clearCacheForElement(child);
            }
        }
    }

    private Tab createTab(DockElement element) {
        Tab tab = new Tab();

        // Use the actual view for the element as the tab content
        Node contentView = createView(element);
        tab.setContent(contentView);

        // Configure tab based on element type
        if (element instanceof DockNode dockNode) {
            // Don't set tab.textProperty() here, as we use a graphic instead
            // (setting both would cause "Console Console" display)

            // Create HBox with icon and label for tab header
            javafx.scene.layout.HBox tabHeader = new javafx.scene.layout.HBox(5);
            tabHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Icon pane
            javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane();
            iconPane.setPrefSize(16, 16);
            iconPane.setMaxSize(16, 16);
            iconPane.setMinSize(16, 16);

            // Bind icon
            dockNode.iconProperty().addListener((obs, oldIcon, newIcon) -> {
                iconPane.getChildren().clear();
                if (newIcon != null) {
                    iconPane.getChildren().add(newIcon);
                }
            });

            // Set initial icon
            if (dockNode.getIcon() != null) {
                iconPane.getChildren().add(dockNode.getIcon());
            }

            // Icon visibility
            iconPane.visibleProperty().bind(dockNode.iconProperty().isNotNull());
            iconPane.managedProperty().bind(iconPane.visibleProperty());

            // Label
            javafx.scene.control.Label tabLabel = new javafx.scene.control.Label();
            tabLabel.textProperty().bind(dockNode.titleProperty());

            tabHeader.getChildren().addAll(iconPane, tabLabel);
            tab.setGraphic(tabHeader);

            // Attach drag handlers to the tab header so tabs can be dragged
            if (dragService != null) {
                tabHeader.setOnMousePressed(e -> dragService.startDrag(dockNode, e));
                tabHeader.setOnMouseDragged(e -> {
                    if (dragService.isDragging()) dragService.updateDrag(e);
                });
                tabHeader.setOnMouseReleased(e -> {
                    if (dragService.isDragging()) dragService.endDrag(e);
                });
            }

            // Closeable Binding
            tab.closableProperty().bind(
                dockNode.closeableProperty()
                .and(dockGraph.lockedProperty().not())
            );

            // OnClosed Handler
            tab.setOnClosed(event -> dockGraph.undock(dockNode));
        } else {
            // For containers (DockSplitPane, DockTabPane), use a generic label
            tab.setText(element.getClass().getSimpleName());

            // Make closeable only if not locked
            tab.closableProperty().bind(dockGraph.lockedProperty().not());

            // OnClosed Handler for containers: remove from parent
            tab.setOnClosed(event -> {
                if (element.getParent() != null) {
                    element.removeFromParent();
                }
            });
        }

        return tab;
    }

    /**
     * Returns the view for a DockNode (used for drag & drop).
     */
    public DockNodeView getDockNodeView(DockNode dockNode) {
        return dockNodeViews.get(dockNode);  // Use instance as key
    }

    /**
     * Clears cached views.
     */
    public void clearCache() {
        viewCache.clear();
        // Clean up dockNodeViews: remove views for nodes that are no longer in the graph
        // Keep views for nodes still in graph (needed for drag & drop snapshots)
        dockNodeViews.entrySet().removeIf(entry -> {
            DockNode node = entry.getKey();
            // Check if this node is still in the graph by searching for it
            return !isNodeInGraph(node);
        });
    }

    /**
     * Check if a DockNode is still in the graph.
     */
    private boolean isNodeInGraph(DockNode node) {
        return findNodeInGraph(dockGraph.getRoot(), node);
    }

    private boolean findNodeInGraph(DockElement current, DockNode target) {
        if (current == null) {
            return false;
        }
        if (current == target) {
            return true;
        }
        if (current instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                if (findNodeInGraph(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the model element (DockElement) at scene coordinates by checking the built views.
     * Uses intelligent target selection: prefers smaller/more specific targets when mouse is near center,
     * and larger/parent targets when mouse is near edges.
     */
    public DockElement findElementAt(double sceneX, double sceneY) {
        // Collect all potential targets (all elements containing the point)
        List<TargetCandidate> candidates = new ArrayList<>();

        for (var entry : viewCache.entrySet()) {
            Node n = entry.getValue();
            if (n != null && n.getScene() != null) {

                Bounds b = n.localToScene(n.getBoundsInLocal());
                if (b.contains(sceneX, sceneY)) {
                    DockElement element = findElementById(entry.getKey());
                    if (element != null) {
                        // Calculate distance from center (normalized 0..1)
                        double centerX = b.getMinX() + b.getWidth() / 2;
                        double centerY = b.getMinY() + b.getHeight() / 2;
                        double dx = Math.abs(sceneX - centerX) / (b.getWidth() / 2);
                        double dy = Math.abs(sceneY - centerY) / (b.getHeight() / 2);
                        double distanceFromCenter = Math.max(dx, dy); // 0 = center, 1 = edge

                        candidates.add(new TargetCandidate(element, n, b, distanceFromCenter));
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Sort candidates by selection priority
        candidates.sort((a, b) -> {
            // Priority 1: Check if mouse is over a TabPane header
            boolean aIsTabHeader = isOverTabHeader(a.node, sceneX, sceneY);
            boolean bIsTabHeader = isOverTabHeader(b.node, sceneX, sceneY);
            if (aIsTabHeader && !bIsTabHeader) return -1;
            if (!aIsTabHeader && bIsTabHeader) return 1;

            // Priority 2: If near center (< 0.6), prefer smaller elements (DockNode over containers)
            if (a.distanceFromCenter < 0.6 && b.distanceFromCenter < 0.6) {
                // Prefer DockNode over containers
                boolean aIsLeaf = a.element instanceof DockNode;
                boolean bIsLeaf = b.element instanceof DockNode;
                if (aIsLeaf && !bIsLeaf) return -1;
                if (!aIsLeaf && bIsLeaf) return 1;

                // Prefer smaller elements
                double aArea = a.bounds.getWidth() * a.bounds.getHeight();
                double bArea = b.bounds.getWidth() * b.bounds.getHeight();
                return Double.compare(aArea, bArea);
            }

            // Priority 3: If near edge (>= 0.6), prefer larger parent containers
            if (a.distanceFromCenter >= 0.6 || b.distanceFromCenter >= 0.6) {
                // Prefer containers over DockNode
                boolean aIsContainer = a.element instanceof DockContainer;
                boolean bIsContainer = b.element instanceof DockContainer;
                if (aIsContainer && !bIsContainer) return -1;
                if (!aIsContainer && bIsContainer) return 1;

                // Prefer larger elements
                double aArea = a.bounds.getWidth() * a.bounds.getHeight();
                double bArea = b.bounds.getWidth() * b.bounds.getHeight();
                return Double.compare(bArea, aArea); // Descending
            }

            return 0;
        });

        return candidates.getFirst().element;
    }

    /**
     * Checks if mouse is over a TabPane header area.
     */
    private boolean isOverTabHeader(Node node, double sceneX, double sceneY) {
        if (!(node instanceof javafx.scene.control.TabPane tp)) {
            return false;
        }

        // Check tab header area
        Node header = tp.lookup(".tab-header-area");
        if (header != null && header.getScene() != null) {
            javafx.geometry.Bounds hb = header.localToScene(header.getBoundsInLocal());
            return hb.contains(sceneX, sceneY);
        }

        // Fallback: check individual tabs
        var tabs = tp.lookupAll(".tab");
        for (Node tabNode : tabs) {
            if (tabNode != null && tabNode.getScene() != null) {
                javafx.geometry.Bounds hb = tabNode.localToScene(tabNode.getBoundsInLocal());
                if (hb.contains(sceneX, sceneY)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Helper class to store target candidates with metadata.
     */
    private static class TargetCandidate {
        final DockElement element;
        final Node node;
        final javafx.geometry.Bounds bounds;
        final double distanceFromCenter;

        TargetCandidate(DockElement element, Node node, javafx.geometry.Bounds bounds, double distanceFromCenter) {
            this.element = element;
            this.node = node;
            this.bounds = bounds;
            this.distanceFromCenter = distanceFromCenter;
        }
    }

    /**
     * Returns the view node for a model element, if available.
     */
    public Node getViewForElement(DockElement element) {
        if (element == null) return null;
        return viewCache.get(element.getId());
    }

    public void setOnNodeCloseRequest(Consumer<DockNode> handler) {
        this.onNodeCloseRequest = handler;
    }

    private DockElement findElementById(String id) {
        return findElementByIdRecursive(dockGraph.getRoot(), id);
    }

    private DockElement findElementByIdRecursive(DockElement el, String id) {
        if (el == null) return null;
        if (id.equals(el.getId())) return el;
        if (el instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                DockElement found = findElementByIdRecursive(child, id);
                if (found != null) return found;
            }
        }
        return null;
    }
}

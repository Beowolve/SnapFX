package com.github.beowolve.snapfx.view;

import com.github.beowolve.snapfx.close.DockCloseSource;
import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.*;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Layout engine that converts the logical DockGraph into a visual scene graph.
 * Keeps model and view in sync.
 */
public class DockLayoutEngine {
    private final DockGraph dockGraph;
    private final DockDragService dragService;
    private final Map<String, Node> viewCache;
    private final StackPane emptyLayoutView;
    private static final String CLEANUP_TASKS_KEY = "snapfx.cleanupTasks";
    private static final String TAB_CLEANUP_KEY = "snapfx.tabCleanup";
    public static final String TAB_DOCK_NODE_KEY = "snapfx.tabDockNode";
    private static final double DROP_ZONE_RATIO = 0.30;
    private static final double DROP_ZONE_MIN_PX = 40.0;
    private static final double DROP_ZONE_MAX_RATIO = 0.45;
    private static final double LEAF_DROP_ZONE_RATIO = 0.18;
    private static final double LEAF_DROP_ZONE_MIN_PX = 18.0;
    private static final double LEAF_DROP_ZONE_MAX_RATIO = 0.30;
    private static final double LEAF_DROP_ZONE_INSET_PX = 12.0;

    private DockCloseButtonMode closeButtonMode = DockCloseButtonMode.BOTH;
    private DockTitleBarMode titleBarMode = DockTitleBarMode.AUTO;
    private BiConsumer<DockNode, DockCloseSource> onNodeCloseRequest;
    private Consumer<DockNode> onNodeFloatRequest;

    public DockLayoutEngine(DockGraph dockGraph, DockDragService dragService) {
        this.dockGraph = dockGraph;
        this.dragService = dragService;
        this.viewCache = new HashMap<>();
        this.emptyLayoutView = new StackPane();
        this.emptyLayoutView.getStyleClass().add("dock-empty-layout");
    }

    /**
     * Builds the visual representation of the DockGraph.
     */
    public Node buildSceneGraph() {
        // Always clear caches to ensure fresh views
        // This is critical after D&D operations to ensure views are properly attached
        clearCache();

        DockElement root = dockGraph.getRoot();
        DockElement optimizedRoot = unwrapSingleContainerRoot(root);

        if (optimizedRoot == null) {
            return emptyLayoutView; // Empty layout
        }

        return createView(optimizedRoot);
    }

    private DockElement unwrapSingleContainerRoot(DockElement root) {
        DockElement current = root;
        while (current instanceof DockContainer container) {
            if (container.getChildren().isEmpty()) {
                return null;
            }
            if (container.getChildren().size() != 1) {
                return current;
            }
            DockElement child = container.getChildren().getFirst();
            if (child == current) {
                return current;
            }
            current = child;
        }
        return current;
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
        nodeView.setOnCloseRequest(() -> handleCloseRequest(dockNode, DockCloseSource.TITLE_BAR));
        nodeView.setOnFloatRequest(() -> handleFloatRequest(dockNode));
        applyTitleBarVisibility(nodeView, dockNode);
        applyTitleCloseVisibility(nodeView, dockNode);

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
        ListChangeListener<DockElement> childrenListener = change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    rebuildSplitPane(splitPane, model);
                }
            }
        };
        model.getChildren().addListener(childrenListener);
        registerCleanupTask(splitPane, () -> model.getChildren().removeListener(childrenListener));

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

        ChangeListener<Number> modelSelectionListener = (obs, old, newVal) -> {
            if (newVal != null && newVal.intValue() >= 0 && newVal.intValue() < tabPane.getTabs().size()) {
                tabPane.getSelectionModel().select(newVal.intValue());
            }
        };
        model.selectedIndexProperty().addListener(modelSelectionListener);

        // Set initial selection
        if (model.getSelectedIndex() >= 0 && model.getSelectedIndex() < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(model.getSelectedIndex());
        }

        // Listener for changes
        ListChangeListener<DockElement> childrenListener = change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    rebuildTabPane(tabPane, model);
                }
            }
        };
        model.getChildren().addListener(childrenListener);

        // Auto-hide in locked mode
        tabPane.visibleProperty().bind(
            dockGraph.lockedProperty().not()
            .or(Bindings.size(tabPane.getTabs()).greaterThan(1))
        );

        registerCleanupTask(tabPane, () -> model.selectedIndexProperty().removeListener(modelSelectionListener));
        registerCleanupTask(tabPane, () -> model.getChildren().removeListener(childrenListener));
        registerCleanupTask(tabPane, () -> {
            if (tabPane.visibleProperty().isBound()) {
                tabPane.visibleProperty().unbind();
            }
            disposeTabs(tabPane);
        });

        return tabPane;
    }

    private void rebuildTabPane(TabPane tabPane, DockTabPane model) {
        int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();

        // Clear cache entries for all children to ensure fresh views
        for (DockElement child : model.getChildren()) {
            clearCacheForElement(child);
        }

        disposeTabs(tabPane);
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

        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                clearCacheForElement(child);
            }
        }
    }

    /**
     * Creates a Tab for the given DockElement. For DockNode, the tab header contains icon and title.
     * For containers, a generic label is used. Handles drag & drop and close events.
     * @param element The DockElement to represent as a Tab
     * @return The created Tab
     */
    private Tab createTab(DockElement element) {
        Tab tab = new Tab();
        Node contentView = createView(element);
        tab.setContent(contentView);

        if (element instanceof DockNode dockNode) {
            TabHeader tabHeader = createTabHeader(dockNode);
            tab.setGraphic(tabHeader.node());
            tab.getProperties().put(TAB_CLEANUP_KEY, tabHeader.cleanup());
            tab.getProperties().put(TAB_DOCK_NODE_KEY, dockNode);
            tab.textProperty().bind(dockNode.titleProperty());
            tab.getStyleClass().add("dock-tab-graphic");
            setupTabDragHandlers(tab, dockNode);
            bindTabCloseable(tab, dockNode);
            tab.setOnCloseRequest(event -> {
                handleCloseRequest(dockNode, DockCloseSource.TAB);
                event.consume();
            });
        } else {
            tab.setText(element.getClass().getSimpleName());
            if (!shouldShowTabCloseButton()) {
                tab.setClosable(false);
            } else {
                tab.closableProperty().bind(dockGraph.lockedProperty().not());
            }
            tab.setOnClosed(event -> {
                if (element.getParent() != null) {
                    element.removeFromParent();
                }
            });
        }
        return tab;
    }

    /**
     * Creates the tab header (icon + title) for a DockNode.
     * @param dockNode The DockNode
     * @return HBox containing icon and title
     */
    private TabHeader createTabHeader(DockNode dockNode) {
        HBox tabHeader = new HBox(5);
        tabHeader.setAlignment(Pos.CENTER_LEFT);
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(16, 16);
        iconPane.setMaxSize(16, 16);
        iconPane.setMinSize(16, 16);

        ChangeListener<Node> iconListener = (obs, oldIcon, newIcon) -> {
            iconPane.getChildren().clear();
            if (newIcon != null) {
                iconPane.getChildren().add(newIcon);
            }
        };
        dockNode.iconProperty().addListener(iconListener);

        if (dockNode.getIcon() != null) {
            iconPane.getChildren().add(dockNode.getIcon());
        }
        iconPane.visibleProperty().bind(dockNode.iconProperty().isNotNull());
        iconPane.managedProperty().bind(iconPane.visibleProperty());

        Label tabLabel = new Label();
        tabLabel.textProperty().bind(dockNode.titleProperty());

        Button floatButton = new Button();
        floatButton.getStyleClass().addAll("dock-node-close-button", "dock-tab-float-button");
        floatButton.setGraphic(createControlIcon("dock-control-icon-float"));
        floatButton.setTooltip(new Tooltip("Float window"));
        floatButton.setFocusTraversable(false);
        floatButton.visibleProperty().bind(dockGraph.lockedProperty().not());
        floatButton.managedProperty().bind(floatButton.visibleProperty());
        floatButton.setOnAction(e -> {
            handleFloatRequest(dockNode);
            e.consume();
        });

        tabHeader.getChildren().addAll(iconPane, tabLabel, floatButton);

        Runnable cleanup = () -> {
            dockNode.iconProperty().removeListener(iconListener);
            tabLabel.textProperty().unbind();
            iconPane.visibleProperty().unbind();
            iconPane.managedProperty().unbind();
            iconPane.getChildren().clear();
            floatButton.visibleProperty().unbind();
            floatButton.managedProperty().unbind();
            floatButton.setOnAction(null);
        };
        return new TabHeader(tabHeader, cleanup);
    }

    private Region createControlIcon(String styleClass) {
        Region icon = new Region();
        icon.getStyleClass().addAll("dock-control-icon", styleClass);
        icon.setMouseTransparent(true);
        return icon;
    }

    /**
     * Sets up drag handlers for a tab header to enable D&D for DockNode tabs.
     * @param tab The Tab
     * @param dockNode The DockNode
     */
    private void setupTabDragHandlers(Tab tab, DockNode dockNode) {
        Node header = tab.getGraphic();
        if (dragService != null && header != null) {
            header.setOnMousePressed(e -> {
                if (isInteractiveControlTarget(e.getTarget())) {
                    return;
                }
                dragService.startDrag(dockNode, e);
            });
            header.setOnMouseDragged(e -> {
                if (isInteractiveControlTarget(e.getTarget())) {
                    return;
                }
                if (dragService.isDragging()) {
                    dragService.updateDrag(e);
                }
            });
            header.setOnMouseReleased(e -> {
                if (dragService.isDragging()) {
                    dragService.endDrag(e);
                }
            });
        }
    }

    private boolean isInteractiveControlTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current instanceof Button) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Binds the closeable property of a tab to the DockNode and DockGraph locked state.
     * @param tab The Tab
     * @param dockNode The DockNode
     */
    private void bindTabCloseable(Tab tab, DockNode dockNode) {
        if (!shouldShowTabCloseButton()) {
            tab.setClosable(false);
            return;
        }
        tab.closableProperty().bind(
            dockNode.closeableProperty().and(dockGraph.lockedProperty().not())
        );
    }

    private void applyTitleBarVisibility(DockNodeView nodeView, DockNode dockNode) {
        nodeView.setHeaderVisible(shouldShowTitleBar(dockNode));
    }

    private void applyTitleCloseVisibility(DockNodeView nodeView, DockNode dockNode) {
        if (shouldShowTitleCloseButton()) {
            nodeView.bindCloseButtonVisible(
                dockNode.closeableProperty().and(dockGraph.lockedProperty().not())
            );
        } else {
            nodeView.setCloseButtonVisible(false);
        }
    }

    private boolean shouldShowTabCloseButton() {
        return closeButtonMode.showTabClose();
    }

    private boolean shouldShowTitleCloseButton() {
        return closeButtonMode.showTitleClose();
    }

    private boolean shouldShowTitleBar(DockNode dockNode) {
        return switch (titleBarMode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AUTO -> !(dockNode.getParent() instanceof DockTabPane);
        };
    }

    private void handleCloseRequest(DockNode dockNode, DockCloseSource source) {
        if (onNodeCloseRequest != null) {
            onNodeCloseRequest.accept(dockNode, source);
        } else {
            dockGraph.undock(dockNode);
        }
    }

    private void handleFloatRequest(DockNode dockNode) {
        if (onNodeFloatRequest != null) {
            onNodeFloatRequest.accept(dockNode);
        }
    }

    /**
     * Finds the model element (DockElement) at the given scene coordinates.
     * Uses intelligent target selection: prefers smaller/more specific targets when mouse is near center,
     * and larger/parent targets when mouse is near edges.
     * @param sceneX X coordinate in scene
     * @param sceneY Y coordinate in scene
     * @return The best matching DockElement or null
     */
    public DockElement findElementAt(double sceneX, double sceneY) {
        List<TargetCandidate> candidates = collectTargetCandidates(sceneX, sceneY);
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(this::compareCandidates);
        return candidates.getFirst().element;
    }

    /**
     * Collects drop zones for all elements in the current graph.
     */
    public List<DockDropZone> collectDropZones() {
        List<DockDropZone> zones = new ArrayList<>();
        DockElement root = dockGraph.getRoot();
        if (root == null) {
            return zones;
        }
        collectDropZonesRecursive(root, 0, zones);
        return zones;
    }

    /**
     * Selects the best drop zone for the given scene coordinates.
     */
    public DockDropZone findBestDropZone(List<DockDropZone> zones, double sceneX, double sceneY) {
        DockDropZone best = null;
        int bestPriority = Integer.MIN_VALUE;
        double bestArea = Double.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (DockDropZone zone : zones) {
            if (!zone.contains(sceneX, sceneY)) {
                continue;
            }
            int priority = getZonePriority(zone);
            double area = zone.area();
            double distance = calculateDistanceFromCenter(zone.getBounds(), sceneX, sceneY);

            if (priority > bestPriority
                || (priority == bestPriority && area < bestArea)
                || (priority == bestPriority && area == bestArea && distance < bestDistance)) {
                best = zone;
                bestPriority = priority;
                bestArea = area;
                bestDistance = distance;
            }
        }

        if (best != null && best.getType() == DockDropZoneType.TAB_HEADER) {
            Integer tabIndex = resolveTabInsertIndex(best, sceneX);
            if (tabIndex != null) {
                Double insertLineX = resolveTabInsertLineX(best, tabIndex);
                best = new DockDropZone(best.getTarget(), best.getPosition(), best.getType(),
                    best.getBounds(), best.getDepth(), tabIndex, insertLineX);
            }
        }

        return best;
    }

    /**
     * Collects all DockElements whose view contains the given scene coordinates.
     * @param sceneX X coordinate
     * @param sceneY Y coordinate
     * @return List of TargetCandidate
     */
    private List<TargetCandidate> collectTargetCandidates(double sceneX, double sceneY) {
        List<TargetCandidate> candidates = new ArrayList<>();
        for (var entry : viewCache.entrySet()) {
            Node n = entry.getValue();
            if (n != null && n.getScene() != null) {
                Bounds b = n.localToScene(n.getBoundsInLocal());
                if (b.contains(sceneX, sceneY)) {
                    DockElement element = findElementById(entry.getKey());
                    addTargetCandidateIfValid(candidates, element, n, b, sceneX, sceneY);
                }
            }
        }
        return candidates;
    }

    /**
     * Adds a TargetCandidate to the list if the element is valid.
     */
    private void addTargetCandidateIfValid(List<TargetCandidate> candidates, DockElement element, Node n, Bounds b, double sceneX, double sceneY) {
        if (element == null) return;
        double distanceFromCenter = calculateDistanceFromCenter(b, sceneX, sceneY);
        boolean isTabHeader = isOverTabHeader(n, sceneX, sceneY);
        candidates.add(new TargetCandidate(element, n, b, distanceFromCenter, isTabHeader));
    }

    private void collectDropZonesRecursive(DockElement element, int depth, List<DockDropZone> zones) {
        Node view = viewCache.get(element.getId());
        if (view != null && view.getScene() != null) {
            Bounds bounds = view.localToScene(view.getBoundsInLocal());
            addElementZones(element, view, bounds, depth, zones);
        }

        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                collectDropZonesRecursive(child, depth + 1, zones);
            }
        }
    }

    private void addElementZones(DockElement element, Node view, Bounds bounds, int depth, List<DockDropZone> zones) {
        if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return;
        }

        boolean isLeaf = !(element instanceof DockContainer);
        Bounds zoneBounds = (isLeaf && element.getParent() != null)
            ? insetBounds(bounds, LEAF_DROP_ZONE_INSET_PX)
            : bounds;
        if (zoneBounds.getWidth() <= 0 || zoneBounds.getHeight() <= 0) {
            zoneBounds = bounds;
        }

        double zoneRatio = isLeaf ? LEAF_DROP_ZONE_RATIO : DROP_ZONE_RATIO;
        double zoneMin = isLeaf ? LEAF_DROP_ZONE_MIN_PX : DROP_ZONE_MIN_PX;
        double zoneMaxRatio = isLeaf ? LEAF_DROP_ZONE_MAX_RATIO : DROP_ZONE_MAX_RATIO;

        double edgeW = Math.clamp(zoneBounds.getWidth() * zoneRatio,
            zoneMin, zoneBounds.getWidth() * zoneMaxRatio);
        double edgeH = Math.clamp(zoneBounds.getHeight() * zoneRatio,
            zoneMin, zoneBounds.getHeight() * zoneMaxRatio);

        Bounds left = new BoundingBox(zoneBounds.getMinX(), zoneBounds.getMinY(), edgeW, zoneBounds.getHeight());
        Bounds right = new BoundingBox(zoneBounds.getMaxX() - edgeW, zoneBounds.getMinY(), edgeW, zoneBounds.getHeight());
        Bounds top = new BoundingBox(zoneBounds.getMinX(), zoneBounds.getMinY(), zoneBounds.getWidth(), edgeH);
        Bounds bottom = new BoundingBox(zoneBounds.getMinX(), zoneBounds.getMaxY() - edgeH, zoneBounds.getWidth(), edgeH);

        Bounds center = buildCenterBounds(zoneBounds, edgeW, edgeH);

        zones.add(new DockDropZone(element, DockPosition.LEFT, DockDropZoneType.EDGE, left, depth, null, null));
        zones.add(new DockDropZone(element, DockPosition.RIGHT, DockDropZoneType.EDGE, right, depth, null, null));
        zones.add(new DockDropZone(element, DockPosition.TOP, DockDropZoneType.EDGE, top, depth, null, null));
        zones.add(new DockDropZone(element, DockPosition.BOTTOM, DockDropZoneType.EDGE, bottom, depth, null, null));
        zones.add(new DockDropZone(element, DockPosition.CENTER, DockDropZoneType.CENTER, center, depth, null, null));

        if (view instanceof TabPane tabPane) {
            addTabHeaderZone(element, tabPane, depth, zones);
        }
    }

    private Bounds buildCenterBounds(Bounds bounds, double edgeW, double edgeH) {
        double innerW = bounds.getWidth() - (edgeW * 2);
        double innerH = bounds.getHeight() - (edgeH * 2);
        if (innerW <= 1 || innerH <= 1) {
            return bounds;
        }
        double innerX = bounds.getMinX() + edgeW;
        double innerY = bounds.getMinY() + edgeH;
        return new BoundingBox(innerX, innerY, innerW, innerH);
    }

    private void addTabHeaderZone(DockElement element, TabPane tabPane, int depth, List<DockDropZone> zones) {
        tabPane.applyCss();
        Node headerArea = tabPane.lookup(".tab-header-area");
        if (headerArea == null) {
            return;
        }
        Bounds headerBounds = headerArea.localToScene(headerArea.getBoundsInLocal());
        if (headerBounds.getWidth() <= 0 || headerBounds.getHeight() <= 0) {
            return;
        }
        zones.add(new DockDropZone(element, DockPosition.CENTER, DockDropZoneType.TAB_HEADER,
            headerBounds, depth, null, null));
    }

    private Integer resolveTabInsertIndex(DockDropZone zone, double sceneX) {
        Node view = viewCache.get(zone.getTarget().getId());
        if (!(view instanceof TabPane tabPane)) {
            return null;
        }
        List<Bounds> headerBounds = collectTabHeaderBounds(tabPane);
        if (headerBounds.isEmpty()) {
            return null;
        }
        for (int i = 0; i < headerBounds.size(); i++) {
            Bounds b = headerBounds.get(i);
            double centerX = b.getMinX() + (b.getWidth() / 2.0);
            if (sceneX < centerX) {
                return i;
            }
        }
        return headerBounds.size();
    }

    private Double resolveTabInsertLineX(DockDropZone zone, int tabIndex) {
        Node view = viewCache.get(zone.getTarget().getId());
        if (!(view instanceof TabPane tabPane)) {
            return null;
        }
        List<Bounds> headerBounds = collectTabHeaderBounds(tabPane);
        if (headerBounds.isEmpty()) {
            return null;
        }
        if (tabIndex <= 0) {
            return headerBounds.getFirst().getMinX();
        }
        if (tabIndex >= headerBounds.size()) {
            return headerBounds.getLast().getMaxX();
        }
        Bounds left = headerBounds.get(tabIndex - 1);
        Bounds right = headerBounds.get(tabIndex);
        return (left.getMaxX() + right.getMinX()) / 2.0;
    }

    private List<Bounds> collectTabHeaderBounds(TabPane tabPane) {
        tabPane.applyCss();
        List<Bounds> bounds = new ArrayList<>();
        for (Node header : tabPane.lookupAll(".tab")) {
            if (!header.isVisible()) {
                continue;
            }
            Bounds b = header.localToScene(header.getBoundsInLocal());
            if (b.getWidth() > 0 && b.getHeight() > 0) {
                bounds.add(b);
            }
        }
        bounds.sort((a, b) -> Double.compare(a.getMinX(), b.getMinX()));
        return bounds;
    }

    private int getZonePriority(DockDropZone zone) {
        int typePriority = switch (zone.getType()) {
            case TAB_INSERT -> 400;
            case TAB_HEADER -> 350;
            case EDGE -> 200;
            case CENTER -> 100;
        };
        return (zone.getDepth() * 1000) + typePriority;
    }

    private Bounds insetBounds(Bounds bounds, double inset) {
        double w = bounds.getWidth() - (inset * 2);
        double h = bounds.getHeight() - (inset * 2);
        if (w <= 0 || h <= 0) {
            return bounds;
        }
        return new BoundingBox(bounds.getMinX() + inset, bounds.getMinY() + inset, w, h);
    }

    /**
     * Calculates the normalized distance from the center of the bounds to the given point.
     */
    private double calculateDistanceFromCenter(Bounds b, double sceneX, double sceneY) {
        double[] center = getCenter(b);
        double dx = Math.abs(sceneX - center[0]) / (b.getWidth() / 2);
        double dy = Math.abs(sceneY - center[1]) / (b.getHeight() / 2);
        return Math.max(dx, dy);
    }

    private double[] getCenter(Bounds b) {
        return new double[]{b.getMinX() + b.getWidth() / 2, b.getMinY() + b.getHeight() / 2};
    }

    /**
     * Compares two TargetCandidates for selection priority.
     * @param a First candidate
     * @param b Second candidate
     * @return Comparison result
     */
    private int compareCandidates(TargetCandidate a, TargetCandidate b) {
        int tabHeaderResult = compareTabHeaderPriority(a, b);
        if (tabHeaderResult != 0) return tabHeaderResult;
        int leafResult = compareLeafPriority(a, b);
        if (leafResult != 0) return leafResult;
        int containerResult = compareContainerPriority(a, b);
        if (containerResult != 0) return containerResult;
        return compareAreaPriority(a, b);
    }

    private int compareTabHeaderPriority(TargetCandidate a, TargetCandidate b) {
        boolean aIsTabHeader = a.isTabHeader;
        boolean bIsTabHeader = b.isTabHeader;
        if (aIsTabHeader && !bIsTabHeader) return -1;
        if (!aIsTabHeader && bIsTabHeader) return 1;
        return 0;
    }


    private int compareLeafPriority(TargetCandidate a, TargetCandidate b) {
        boolean aIsLeaf = a.element instanceof DockNode;
        boolean bIsLeaf = b.element instanceof DockNode;
        if (aIsLeaf && !bIsLeaf) return -1;
        if (!aIsLeaf && bIsLeaf) return 1;
        return 0;
    }

    private int compareContainerPriority(TargetCandidate a, TargetCandidate b) {
        boolean aIsContainer = a.element instanceof DockContainer;
        boolean bIsContainer = b.element instanceof DockContainer;
        if (aIsContainer && !bIsContainer) return -1;
        if (!aIsContainer && bIsContainer) return 1;
        return 0;
    }

    private int compareAreaPriority(TargetCandidate a, TargetCandidate b) {
        double aArea = a.bounds.getWidth() * a.bounds.getHeight();
        double bArea = b.bounds.getWidth() * b.bounds.getHeight();
        if (isNearCenter(a, b)) {
            return Double.compare(aArea, bArea);
        } else if (isNearEdge(a, b)) {
            return Double.compare(bArea, aArea);
        }
        return 0;
    }

    private boolean isNearCenter(TargetCandidate a, TargetCandidate b) {
        return a.distanceFromCenter < 0.6 && b.distanceFromCenter < 0.6;
    }

    private boolean isNearEdge(TargetCandidate a, TargetCandidate b) {
        return a.distanceFromCenter >= 0.6 || b.distanceFromCenter >= 0.6;
    }

    /**
     * Finds the DockElement by its ID.
     * @param id The ID of the DockElement
     * @return The DockElement or null if not found
     */
    private DockElement findElementById(String id) {
        DockElement root = dockGraph.getRoot();
        if (root == null) return null;
        if (root.getId().equals(id)) return root;
        if (root instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                DockElement result = findElementByIdRecursive(child, id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Recursively finds the DockElement by its ID.
     * @param element The current DockElement
     * @param id The ID to find
     * @return The DockElement or null if not found
     */
    private DockElement findElementByIdRecursive(DockElement element, String id) {
        if (element.getId().equals(id)) {
            return element;
        }
        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                DockElement result = findElementByIdRecursive(child, id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the given coordinates are over a tab header.
     * @param node The node
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if over tab header, false otherwise
     */
    private boolean isOverTabHeader(Node node, double x, double y) {
        if (!(node instanceof TabPane tabPane) || tabPane.getScene() == null) {
            return false;
        }
        Node headerArea = tabPane.lookup(".tab-header-area");
        if (headerArea == null) {
            return false;
        }
        Bounds headerBounds = headerArea.localToScene(headerArea.getBoundsInLocal());
        return headerBounds.contains(x, y);
    }

    private void disposeTabs(TabPane tabPane) {
        for (Tab tab : tabPane.getTabs()) {
            Object cleanup = tab.getProperties().remove(TAB_CLEANUP_KEY);
            if (cleanup instanceof Runnable runnable) {
                runnable.run();
            }
            if (tab.textProperty().isBound()) {
                tab.textProperty().unbind();
            }
            if (tab.closableProperty().isBound()) {
                tab.closableProperty().unbind();
            }
            tab.setOnCloseRequest(null);
            tab.setOnClosed(null);
            tab.setGraphic(null);
            tab.setContent(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerCleanupTask(Node view, Runnable cleanupTask) {
        if (view == null || cleanupTask == null) {
            return;
        }
        Object existing = view.getProperties().get(CLEANUP_TASKS_KEY);
        List<Runnable> cleanupTasks;
        if (existing instanceof List<?> list) {
            cleanupTasks = (List<Runnable>) list;
        } else {
            cleanupTasks = new ArrayList<>();
            view.getProperties().put(CLEANUP_TASKS_KEY, cleanupTasks);
        }
        cleanupTasks.add(cleanupTask);
    }

    @SuppressWarnings("unchecked")
    private void runCleanupTasks(Node view) {
        if (view == null) {
            return;
        }
        Object existing = view.getProperties().remove(CLEANUP_TASKS_KEY);
        if (!(existing instanceof List<?> list)) {
            if (view instanceof DockNodeView dockNodeView) {
                dockNodeView.dispose();
            }
            return;
        }

        for (Object task : list) {
            if (task instanceof Runnable runnable) {
                runnable.run();
            }
        }

        if (view instanceof DockNodeView dockNodeView) {
            dockNodeView.dispose();
        }
    }

    /**
     * Clears the entire view cache.
     * This is called before a new scene graph is built to ensure no stale views are used.
     */
    public void clearCache() {
        for (Node view : new ArrayList<>(viewCache.values())) {
            runCleanupTasks(view);
        }
        viewCache.clear();
    }

    /**
     * Sets the action to be performed when a node close is requested.
     * @param onNodeCloseRequest The action to set
     */
    public void setOnNodeCloseRequest(BiConsumer<DockNode, DockCloseSource> onNodeCloseRequest) {
        this.onNodeCloseRequest = onNodeCloseRequest;
    }

    /**
     * Legacy close hook retained for compatibility.
     * Prefer {@link #setOnNodeCloseRequest(BiConsumer)} for source-aware handling.
     */
    @Deprecated(forRemoval = false)
    public void setOnNodeCloseRequest(Consumer<DockNode> onNodeCloseRequest) {
        if (onNodeCloseRequest == null) {
            this.onNodeCloseRequest = null;
            return;
        }
        this.onNodeCloseRequest = (dockNode, source) -> onNodeCloseRequest.accept(dockNode);
    }

    public void setOnNodeFloatRequest(Consumer<DockNode> onNodeFloatRequest) {
        this.onNodeFloatRequest = onNodeFloatRequest;
    }

    public DockCloseButtonMode getCloseButtonMode() {
        return closeButtonMode;
    }

    public void setCloseButtonMode(DockCloseButtonMode closeButtonMode) {
        if (closeButtonMode != null) {
            this.closeButtonMode = closeButtonMode;
        }
    }

    public DockTitleBarMode getTitleBarMode() {
        return titleBarMode;
    }

    public void setTitleBarMode(DockTitleBarMode titleBarMode) {
        if (titleBarMode != null) {
            this.titleBarMode = titleBarMode;
        }
    }

    /**
     * Returns the Node view for a given DockElement, or null if not found.
     */
    public Node getViewForElement(DockElement element) {
        if (element == null) return null;
        return viewCache.get(element.getId());
    }

    /**
     * Returns the DockNodeView for a given DockNode, or null if not found.
     */
    public DockNodeView getDockNodeView(DockNode node) {
        Node n = getViewForElement(node);
        if (n instanceof DockNodeView dnv) return dnv;
        return null;
    }

    /**
     * Helper class to represent a candidate for D&D target selection.
     */
    private static class TargetCandidate {
        final DockElement element;
        final Node node;
        final Bounds bounds;
        final double distanceFromCenter;
        final boolean isTabHeader;
        TargetCandidate(DockElement element, Node node, Bounds bounds, double distanceFromCenter, boolean isTabHeader) {
            this.element = element;
            this.node = node;
            this.bounds = bounds;
            this.distanceFromCenter = distanceFromCenter;
            this.isTabHeader = isTabHeader;
        }
    }

    private record TabHeader(HBox node, Runnable cleanup) {
    }
}

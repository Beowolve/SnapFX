package com.github.beowolve.snapfx.floating;

import com.github.beowolve.snapfx.close.DockCloseSource;
import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.dnd.DockDropVisualizationMode;
import com.github.beowolve.snapfx.model.DockContainer;
import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import com.github.beowolve.snapfx.model.DockTabPane;
import com.github.beowolve.snapfx.theme.DockThemeStyleClasses;
import com.github.beowolve.snapfx.view.DockDropZone;
import com.github.beowolve.snapfx.view.DockDropZoneType;
import com.github.beowolve.snapfx.view.DockLayoutEngine;
import com.github.beowolve.snapfx.view.DockNodeView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents an external floating window that can host a full dock layout subtree.
 */
public final class DockFloatingWindow {
    private static final String TITLE_PREFIX = "SnapFX";

    private static final double DEFAULT_WIDTH = 640;
    private static final double DEFAULT_HEIGHT = 420;
    private static final double DEFAULT_OFFSET_X = 40;
    private static final double DEFAULT_OFFSET_Y = 40;
    private static final double RESIZE_MARGIN = 6.0;
    private static final double MAXIMIZED_RESTORE_DRAG_THRESHOLD = 6.0;
    private static final double MIN_WINDOW_WIDTH = 280.0;
    private static final double MIN_WINDOW_HEIGHT = 180.0;

    private static final int RESIZE_LEFT = 1;
    private static final int RESIZE_RIGHT = 1 << 1;
    private static final int RESIZE_TOP = 1 << 2;
    private static final int RESIZE_BOTTOM = 1 << 3;

    private final String id;
    private final DockNode primaryDockNode;
    private final String titlePrefix;
    private final DockGraph floatingGraph;
    private final DockLayoutEngine floatingLayoutEngine;
    private final StackPane layoutContainer;
    private final FloatingDropIndicator dropIndicator;
    private final FloatingDropZonesOverlay dropZonesOverlay;
    private final List<Runnable> tabSelectionListenersCleanup;

    private Stage stage;
    private Double preferredX;
    private Double preferredY;
    private double preferredWidth = DEFAULT_WIDTH;
    private double preferredHeight = DEFAULT_HEIGHT;
    private boolean suppressCloseNotification;

    private Runnable onAttachRequested;
    private Consumer<DockFloatingWindow> onWindowClosed;
    private Runnable onWindowActivated;
    private BooleanSupplier onCloseRequested;
    private BiConsumer<DockNode, DockCloseSource> onNodeCloseRequest;
    private Consumer<DockNode> onNodeFloatRequest;
    private boolean suppressCloseRequestHandling;

    private StackPane iconPane;
    private Label titleLabel;
    private Button maximizeButton;
    private Tooltip maximizeTooltip;
    private Button pinButton;
    private Tooltip pinTooltip;
    private ContextMenu titleBarContextMenu;

    private double dragOffsetX;
    private double dragOffsetY;
    private boolean titleBarDragActive;
    private boolean awaitingMaximizedRestoreDrag;
    private double titleBarPressScreenX;
    private double titleBarPressScreenY;
    private double restoreX;
    private double restoreY;
    private double restoreWidth = DEFAULT_WIDTH;
    private double restoreHeight = DEFAULT_HEIGHT;
    private boolean hasRestoreBounds;

    private boolean resizing;
    private int activeResizeMask;
    private double resizeStartScreenX;
    private double resizeStartScreenY;
    private double resizeStartWindowX;
    private double resizeStartWindowY;
    private double resizeStartWindowWidth;
    private double resizeStartWindowHeight;
    private boolean alwaysOnTop = true;
    private DockFloatingPinButtonMode pinButtonMode = DockFloatingPinButtonMode.AUTO;
    private DockFloatingPinLockedBehavior pinLockedBehavior = DockFloatingPinLockedBehavior.ALLOW;
    private boolean pinToggleEnabled = true;
    private BiConsumer<Boolean, DockFloatingPinSource> onAlwaysOnTopChanged;
    private Node resizeCursorTargetNode;
    private Cursor resizeCursorTargetPrevious;
    private final DockFloatingSnapEngine snapEngine = new DockFloatingSnapEngine();
    private boolean snappingEnabled;
    private double snapDistance = 12.0;
    private EnumSet<DockFloatingSnapTarget> snapTargets = EnumSet.noneOf(DockFloatingSnapTarget.class);
    private Supplier<List<DockFloatingWindow>> snapPeerWindowsSupplier;

    public DockFloatingWindow(DockNode dockNode) {
        this((DockElement) dockNode, TITLE_PREFIX, null);
    }

    public DockFloatingWindow(DockNode dockNode, DockDragService dragService) {
        this((DockElement) dockNode, TITLE_PREFIX, dragService);
    }

    public DockFloatingWindow(DockNode dockNode, String titlePrefix) {
        this((DockElement) dockNode, titlePrefix, null);
    }

    public DockFloatingWindow(DockNode dockNode, String titlePrefix, DockDragService dragService) {
        this((DockElement) dockNode, titlePrefix, dragService);
    }

    public DockFloatingWindow(DockElement floatingRoot, DockDragService dragService) {
        this(floatingRoot, TITLE_PREFIX, dragService);
    }

    public DockFloatingWindow(DockElement floatingRoot, String titlePrefix, DockDragService dragService) {
        this.id = UUID.randomUUID().toString();
        DockElement rootElement = Objects.requireNonNull(floatingRoot, "floatingRoot");
        DockNode representative = findFirstDockNode(rootElement);
        this.primaryDockNode = Objects.requireNonNull(representative, "floatingRoot must contain at least one DockNode");
        this.titlePrefix = (titlePrefix == null || titlePrefix.isBlank()) ? TITLE_PREFIX : titlePrefix;
        this.floatingGraph = new DockGraph();
        this.floatingLayoutEngine = new DockLayoutEngine(floatingGraph, dragService);
        this.floatingLayoutEngine.setOnNodeCloseRequest(this::handleInnerNodeCloseRequest);
        this.floatingLayoutEngine.setOnNodeFloatRequest(this::handleInnerNodeFloatRequest);
        this.floatingLayoutEngine.setCanFloatNodePredicate(node -> getDockNodes().size() > 1);
        this.layoutContainer = new StackPane();
        this.layoutContainer.getStyleClass().add(DockThemeStyleClasses.DOCK_FLOATING_LAYOUT_CONTAINER);
        this.dropIndicator = new FloatingDropIndicator();
        this.dropZonesOverlay = new FloatingDropZonesOverlay();
        this.tabSelectionListenersCleanup = new ArrayList<>();
        this.dropIndicator.setMouseTransparent(true);
        this.dropIndicator.setManaged(false);
        this.dropZonesOverlay.setMouseTransparent(true);
        this.dropZonesOverlay.setManaged(false);

        floatingGraph.setRoot(rootElement);
        floatingGraph.revisionProperty().addListener((obs, oldValue, newValue) ->
            Platform.runLater(this::rebuildLayout)
        );
        floatingGraph.rootProperty().addListener((obs, oldValue, newValue) ->
            Platform.runLater(() -> {
                rebuildLayout();
                if (newValue == null && stage != null && stage.isShowing()) {
                    closeWithoutNotification();
                }
            })
        );
        floatingGraph.lockedProperty().addListener((obs, oldValue, newValue) -> updatePinButtonVisibility());
    }

    private static DockNode findFirstDockNode(DockElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof DockNode dockNode) {
            return dockNode;
        }
        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                DockNode found = findFirstDockNode(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public DockNode getDockNode() {
        return primaryDockNode;
    }

    public DockGraph getDockGraph() {
        return floatingGraph;
    }

    public List<DockNode> getDockNodes() {
        List<DockNode> nodes = new ArrayList<>();
        collectDockNodes(floatingGraph.getRoot(), nodes);
        return nodes;
    }

    public boolean containsNode(DockNode node) {
        return findNode(floatingGraph.getRoot(), node);
    }

    public boolean isEmpty() {
        return floatingGraph.getRoot() == null;
    }

    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }

    /**
     * Returns the current scene of this floating window, or {@code null} when not shown.
     */
    public Scene getScene() {
        if (stage == null) {
            return null;
        }
        return stage.getScene();
    }

    /**
     * Returns whether the given scene belongs to this floating window.
     */
    public boolean ownsScene(Scene scene) {
        return scene != null && stage != null && stage.getScene() == scene;
    }

    public boolean containsScreenPoint(double screenX, double screenY) {
        if (stage == null || !stage.isShowing() || stage.getScene() == null) {
            return false;
        }
        Node sceneRoot = stage.getScene().getRoot();
        if (sceneRoot == null) {
            return false;
        }
        Point2D scenePoint = sceneRoot.screenToLocal(screenX, screenY);
        return scenePoint != null && sceneRoot.getBoundsInLocal().contains(scenePoint);
    }

    public void toFront() {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            notifyWindowActivated();
        }
    }

    public void setPreferredPosition(Double screenX, Double screenY) {
        preferredX = screenX;
        preferredY = screenY;
        if (stage != null && !stage.isMaximized()) {
            if (screenX != null) {
                stage.setX(screenX);
            }
            if (screenY != null) {
                stage.setY(screenY);
            }
        }
    }

    public void setPreferredSize(double width, double height) {
        if (width > 0) {
            preferredWidth = width;
        }
        if (height > 0) {
            preferredHeight = height;
        }
        if (stage != null && !stage.isMaximized()) {
            stage.setWidth(preferredWidth);
            stage.setHeight(preferredHeight);
        }
    }

    public Double getPreferredX() {
        return preferredX;
    }

    public Double getPreferredY() {
        return preferredY;
    }

    public double getPreferredWidth() {
        return preferredWidth;
    }

    public double getPreferredHeight() {
        return preferredHeight;
    }

    public void captureCurrentBounds() {
        if (stage == null || !stage.isShowing()) {
            return;
        }
        if (stage.isMaximized() && hasRestoreBounds) {
            preferredX = restoreX;
            preferredY = restoreY;
            preferredWidth = restoreWidth;
            preferredHeight = restoreHeight;
            return;
        }
        preferredX = stage.getX();
        preferredY = stage.getY();
        preferredWidth = stage.getWidth();
        preferredHeight = stage.getHeight();
    }

    public void show(Stage ownerStage) {
        if (stage == null) {
            stage = createStage(ownerStage);
        }
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();
        notifyWindowActivated();
        rebuildLayout();
    }

    public void close() {
        if (floatingGraph.isLocked()) {
            return;
        }
        if (!canProcessCloseRequest()) {
            return;
        }
        closeInternal(true);
    }

    public void closeWithoutNotification() {
        closeInternal(false);
    }

    public void setOnAttachRequested(Runnable onAttachRequested) {
        this.onAttachRequested = onAttachRequested;
    }

    public void setOnWindowClosed(Consumer<DockFloatingWindow> onWindowClosed) {
        this.onWindowClosed = onWindowClosed;
    }

    public void setOnWindowActivated(Runnable onWindowActivated) {
        this.onWindowActivated = onWindowActivated;
    }

    public void setOnCloseRequested(BooleanSupplier onCloseRequested) {
        this.onCloseRequested = onCloseRequested;
    }

    public void setOnNodeCloseRequest(BiConsumer<DockNode, DockCloseSource> onNodeCloseRequest) {
        this.onNodeCloseRequest = onNodeCloseRequest;
    }

    public void setOnNodeFloatRequest(Consumer<DockNode> onNodeFloatRequest) {
        this.onNodeFloatRequest = onNodeFloatRequest;
    }

    /**
     * Enables or disables snapping while dragging the floating window title bar.
     */
    public void setSnappingEnabled(boolean enabled) {
        snappingEnabled = enabled;
    }

    /**
     * Returns whether drag snapping is enabled.
     */
    public boolean isSnappingEnabled() {
        return snappingEnabled;
    }

    /**
     * Sets the snap distance in pixels.
     */
    public void setSnapDistance(double pixels) {
        if (Double.isFinite(pixels) && pixels >= 0.0) {
            snapDistance = pixels;
        }
    }

    /**
     * Returns the snap distance in pixels.
     */
    public double getSnapDistance() {
        return snapDistance;
    }

    /**
     * Configures which snap targets are considered during drag.
     */
    public void setSnapTargets(Set<DockFloatingSnapTarget> targets) {
        EnumSet<DockFloatingSnapTarget> resolvedTargets = EnumSet.noneOf(DockFloatingSnapTarget.class);
        if (targets != null) {
            for (DockFloatingSnapTarget target : targets) {
                if (target != null) {
                    resolvedTargets.add(target);
                }
            }
        }
        snapTargets = resolvedTargets;
    }

    /**
     * Returns the configured snap targets.
     */
    public Set<DockFloatingSnapTarget> getSnapTargets() {
        if (snapTargets.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(snapTargets));
    }

    /**
     * Sets the supplier used to resolve other floating windows for snapping.
     */
    public void setSnapPeerWindowsSupplier(Supplier<List<DockFloatingWindow>> supplier) {
        snapPeerWindowsSupplier = supplier;
    }

    /**
     * Returns whether the window is currently configured as always-on-top.
     */
    public boolean isAlwaysOnTop() {
        if (stage != null) {
            return stage.isAlwaysOnTop();
        }
        return alwaysOnTop;
    }

    /**
     * Sets always-on-top and marks the change as API-driven.
     */
    public void setAlwaysOnTop(boolean value) {
        setAlwaysOnTop(value, DockFloatingPinSource.API);
    }

    /**
     * Sets always-on-top with an explicit change source.
     */
    public void setAlwaysOnTop(boolean value, DockFloatingPinSource source) {
        DockFloatingPinSource effectiveSource = source == null ? DockFloatingPinSource.API : source;
        applyAlwaysOnTop(value, effectiveSource);
    }

    /**
     * Returns the pin-button visibility mode.
     */
    public DockFloatingPinButtonMode getPinButtonMode() {
        return pinButtonMode;
    }

    /**
     * Sets the pin-button visibility mode.
     */
    public void setPinButtonMode(DockFloatingPinButtonMode mode) {
        pinButtonMode = mode == null ? DockFloatingPinButtonMode.AUTO : mode;
        updatePinButtonVisibility();
    }

    /**
     * Returns whether users may toggle always-on-top from the title bar.
     */
    public boolean isPinToggleEnabled() {
        return pinToggleEnabled;
    }

    /**
     * Enables or disables title-bar pin toggling.
     */
    public void setPinToggleEnabled(boolean enabled) {
        pinToggleEnabled = enabled;
        updatePinButtonVisibility();
    }

    /**
     * Returns the lock-mode behavior used for the pin button.
     */
    public DockFloatingPinLockedBehavior getPinLockedBehavior() {
        return pinLockedBehavior;
    }

    /**
     * Sets how the pin button behaves while the layout is locked.
     */
    public void setPinLockedBehavior(DockFloatingPinLockedBehavior behavior) {
        pinLockedBehavior = behavior == null ? DockFloatingPinLockedBehavior.ALLOW : behavior;
        updatePinButtonVisibility();
    }

    /**
     * Sets a callback that is invoked when always-on-top changes.
     */
    public void setOnAlwaysOnTopChanged(BiConsumer<Boolean, DockFloatingPinSource> onAlwaysOnTopChanged) {
        this.onAlwaysOnTopChanged = onAlwaysOnTopChanged;
    }

    public void undockNode(DockNode node) {
        floatingGraph.undock(node);
    }

    public void dockNode(DockNode node, DockElement target, DockPosition position, Integer tabIndex) {
        floatingGraph.dock(node, target, position, tabIndex);
    }

    public void moveNode(DockNode node, DockElement target, DockPosition position, Integer tabIndex) {
        floatingGraph.move(node, target, position, tabIndex);
    }

    public void requestFloatForNode(DockNode node) {
        handleInnerNodeFloatRequest(node);
    }

    public DockNodeView getDockNodeView(DockNode node) {
        return floatingLayoutEngine.getDockNodeView(node);
    }

    public DropTarget resolveDropTarget(double screenX, double screenY, DockNode draggedNode) {
        DropZoneResolution resolution = resolveDropZone(screenX, screenY, draggedNode, true);
        DockDropZone bestZone = resolution.activeZone();
        if (bestZone == null) {
            return null;
        }
        return new DropTarget(
            bestZone.getTarget(),
            bestZone.getPosition(),
            bestZone.getTabIndex(),
            bestZone.getDepth(),
            bestZone.area()
        );
    }

    public void updateDropPreview(
        DockNode draggedNode,
        double screenX,
        double screenY,
        DockDropVisualizationMode visualizationMode
    ) {
        DropZoneResolution resolution = resolveDropZone(screenX, screenY, draggedNode, true);
        DockDropZone activeZone = resolution.activeZone();
        List<DockDropZone> validZones = resolution.validZones();

        if (activeZone == null) {
            clearDropPreview();
            return;
        }

        List<DockDropZone> zonesToShow = getZonesForVisualization(validZones, activeZone, visualizationMode);
        if (zonesToShow.isEmpty()) {
            dropZonesOverlay.hide();
        } else {
            dropZonesOverlay.showZones(zonesToShow);
        }

        if (visualizationMode == DockDropVisualizationMode.OFF) {
            dropIndicator.hide();
            return;
        }
        dropIndicator.show(activeZone.getBounds(), activeZone.getInsertLineX());
    }

    public void clearDropPreview() {
        dropIndicator.hide();
        dropZonesOverlay.hide();
    }

    private DropZoneResolution resolveDropZone(
        double screenX,
        double screenY,
        DockNode draggedNode,
        boolean activateTabHover
    ) {
        if (stage == null || !stage.isShowing() || stage.getScene() == null || draggedNode == null) {
            return DropZoneResolution.empty();
        }

        Node sceneRoot = stage.getScene().getRoot();
        Point2D scenePoint = sceneRoot.screenToLocal(screenX, screenY);
        if (scenePoint == null) {
            return DropZoneResolution.empty();
        }
        Bounds localBounds = sceneRoot.getBoundsInLocal();
        if (!localBounds.contains(scenePoint)) {
            return DropZoneResolution.empty();
        }

        List<DockDropZone> zones = floatingLayoutEngine.collectDropZones();
        if (zones.isEmpty()) {
            return DropZoneResolution.empty();
        }
        List<DockDropZone> validZones = filterZonesForDrop(zones, draggedNode);
        DockDropZone activeZone = floatingLayoutEngine.findBestDropZone(
            validZones,
            scenePoint.getX(),
            scenePoint.getY()
        );
        if (activateTabHover && activateTabHoverIfNeeded(activeZone)) {
            zones = floatingLayoutEngine.collectDropZones();
            validZones = filterZonesForDrop(zones, draggedNode);
            activeZone = floatingLayoutEngine.findBestDropZone(
                validZones,
                scenePoint.getX(),
                scenePoint.getY()
            );
        }
        return new DropZoneResolution(validZones, activeZone);
    }

    private void closeInternal(boolean notify) {
        if (!notify) {
            suppressCloseNotification = true;
            suppressCloseRequestHandling = true;
        }
        if (stage == null) {
            if (notify && onWindowClosed != null) {
                onWindowClosed.accept(this);
            }
            suppressCloseNotification = false;
            suppressCloseRequestHandling = false;
            return;
        }
        stage.close();
    }

    private Stage createStage(Stage ownerStage) {
        Stage window = new Stage(StageStyle.UNDECORATED);
        if (ownerStage != null) {
            window.initOwner(ownerStage);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add(DockThemeStyleClasses.DOCK_FLOATING_WINDOW);

        HBox titleBar = createTitleBar(window);
        root.setTop(titleBar);
        StackPane contentStack = new StackPane(layoutContainer, dropZonesOverlay, dropIndicator);
        dropZonesOverlay.prefWidthProperty().bind(contentStack.widthProperty());
        dropZonesOverlay.prefHeightProperty().bind(contentStack.heightProperty());
        dropIndicator.prefWidthProperty().bind(contentStack.widthProperty());
        dropIndicator.prefHeightProperty().bind(contentStack.heightProperty());
        root.setCenter(contentStack);

        setupResizeHandling(root, window);

        Scene scene = new Scene(root, preferredWidth, preferredHeight);
        if (ownerStage != null && ownerStage.getScene() != null) {
            scene.getStylesheets().addAll(ownerStage.getScene().getStylesheets());
        }
        setupTitleBarDrag(titleBar, window, scene);
        window.setScene(scene);
        window.setMinWidth(resolveMinimumWindowWidth(window));
        window.setMinHeight(resolveMinimumWindowHeight(window));
        window.setAlwaysOnTop(alwaysOnTop);
        applyWindowPosition(window, ownerStage);

        window.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                notifyWindowActivated();
            }
        });
        window.maximizedProperty().addListener((obs, oldValue, newValue) -> updateMaximizeButtonState(window));
        updateMaximizeButtonState(window);
        window.setOnCloseRequest(event -> {
            if (!canProcessCloseRequest()) {
                event.consume();
            }
        });
        window.setOnHidden(e -> onWindowHidden(window));

        return window;
    }

    private HBox createTitleBar(Stage window) {
        HBox titleBar = new HBox(6);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().addAll(DockThemeStyleClasses.DOCK_NODE_HEADER, DockThemeStyleClasses.DOCK_FLOATING_TITLE_BAR);

        iconPane = new StackPane();
        iconPane.setPrefSize(16, 16);
        iconPane.setMaxSize(16, 16);
        iconPane.setMinSize(16, 16);

        titleLabel = new Label();
        titleLabel.getStyleClass().add(DockThemeStyleClasses.DOCK_NODE_TITLE_LABEL);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button attachButton = createControlButton(
                DockThemeStyleClasses.DOCK_CONTROL_ICON_ATTACH,
            "Attach to layout",
            this::requestAttach,
                DockThemeStyleClasses.DOCK_WINDOW_ATTACH_BUTTON
        );

        maximizeTooltip = new Tooltip("Maximize window");
        maximizeButton = createControlButton(
                DockThemeStyleClasses.DOCK_CONTROL_ICON_MAXIMIZE,
            maximizeTooltip.getText(),
            () -> toggleMaximize(window),
                DockThemeStyleClasses.DOCK_WINDOW_MAXIMIZE_BUTTON
        );
        maximizeButton.setTooltip(maximizeTooltip);

        pinButton = createPinButton(window);

        Button closeButton = createControlButton(
                DockThemeStyleClasses.DOCK_CONTROL_ICON_CLOSE,
            "Close floating window",
            this::close,
                DockThemeStyleClasses.DOCK_WINDOW_CLOSE_BUTTON
        );

        titleBar.getChildren().addAll(iconPane, titleLabel, spacer, attachButton, pinButton, maximizeButton, closeButton);
        installTitleBarContextMenu(titleBar);
        return titleBar;
    }

    private void installTitleBarContextMenu(HBox titleBar) {
        if (titleBar == null) {
            return;
        }
        if (titleBarContextMenu != null) {
            titleBarContextMenu.hide();
        }
        titleBarContextMenu = createTitleBarContextMenu();
        titleBar.setOnContextMenuRequested(event -> showTitleBarContextMenu(titleBar, event));
    }

    private ContextMenu createTitleBarContextMenu() {
        MenuItem attachItem = new MenuItem("Attach to Layout");
        attachItem.setGraphic(createControlIcon(DockThemeStyleClasses.DOCK_CONTROL_ICON_ATTACH));
        attachItem.setOnAction(this::onAttachContextMenuAction);

        CheckMenuItem alwaysOnTopItem = new CheckMenuItem("Always on Top");
        alwaysOnTopItem.setOnAction(event -> onAlwaysOnTopContextMenuAction(alwaysOnTopItem));
        alwaysOnTopItem.setGraphic(createControlIcon(DockThemeStyleClasses.DOCK_CONTROL_ICON_PIN_ON));

        ContextMenu contextMenu = new ContextMenu(attachItem, new SeparatorMenuItem(), alwaysOnTopItem);
        contextMenu.setOnShowing(event -> updateTitleBarContextMenuState(attachItem, alwaysOnTopItem));
        return contextMenu;
    }

    private void onAttachContextMenuAction(ActionEvent event) {
        if (floatingGraph.isLocked()) {
            event.consume();
            return;
        }
        requestAttach();
        event.consume();
    }

    private void requestAttach() {
        if (onAttachRequested != null) {
            onAttachRequested.run();
        }
    }

    private void onAlwaysOnTopContextMenuAction(CheckMenuItem alwaysOnTopItem) {
        setAlwaysOnTop(alwaysOnTopItem.isSelected(), DockFloatingPinSource.USER);
    }

    private void updateTitleBarContextMenuState(MenuItem attachItem, CheckMenuItem alwaysOnTopItem) {
        attachItem.setDisable(floatingGraph.isLocked() || onAttachRequested == null);
        alwaysOnTopItem.setSelected(isAlwaysOnTop());
        alwaysOnTopItem.setDisable(floatingGraph.isLocked() || !pinToggleEnabled);
        alwaysOnTopItem.setGraphic(
            createControlIcon(isAlwaysOnTop() ? DockThemeStyleClasses.DOCK_CONTROL_ICON_PIN_ON : DockThemeStyleClasses.DOCK_CONTROL_ICON_PIN_OFF)
        );
    }

    private void showTitleBarContextMenu(HBox titleBar, ContextMenuEvent event) {
        if (titleBarContextMenu == null) {
            return;
        }
        titleBarContextMenu.show(titleBar, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    private Button createControlButton(String iconStyleClass, String tooltipText, Runnable action, String styleClass) {
        Button button = new Button();
        button.getStyleClass().addAll(DockThemeStyleClasses.DOCK_NODE_CLOSE_BUTTON, DockThemeStyleClasses.DOCK_WINDOW_CONTROL_BUTTON, styleClass);
        button.setGraphic(createControlIcon(iconStyleClass));
        button.setFocusTraversable(false);
        button.visibleProperty().bind(floatingGraph.lockedProperty().not());
        button.managedProperty().bind(button.visibleProperty());
        button.disableProperty().bind(floatingGraph.lockedProperty());
        if (tooltipText != null && !tooltipText.isBlank()) {
            button.setTooltip(new Tooltip(tooltipText));
        }
        button.setOnAction(e -> {
            if (floatingGraph.isLocked()) {
                e.consume();
                return;
            }
            if (action != null) {
                action.run();
            }
            e.consume();
        });
        return button;
    }

    private Button createPinButton(Stage window) {
        pinTooltip = new Tooltip();
        Button button = new Button();
        pinButton = button;
        button.getStyleClass().addAll(DockThemeStyleClasses.DOCK_NODE_CLOSE_BUTTON, DockThemeStyleClasses.DOCK_WINDOW_CONTROL_BUTTON, DockThemeStyleClasses.DOCK_WINDOW_PIN_BUTTON);
        button.setFocusTraversable(false);
        button.setTooltip(pinTooltip);
        button.setOnAction(this::onPinButtonAction);
        updatePinButtonState();
        updatePinButtonVisibility();
        if (window != null) {
            window.setAlwaysOnTop(alwaysOnTop);
        }
        return button;
    }

    /**
     * Handles pin button action events, toggling always-on-top if allowed.
     * @param event the action event triggered by the pin button
     */
    private void onPinButtonAction(ActionEvent event) {
        if (!canTogglePinFromButton()) {
            event.consume();
            return;
        }
        applyAlwaysOnTop(!isAlwaysOnTop(), DockFloatingPinSource.USER);
        event.consume();
    }

    /**
     * Determines whether the pin button can toggle always-on-top in the current state.
     * @return {@code true} if the pin button can toggle always-on-top, {@code false} if the button should be disabled or non-interactive
     */
    private boolean canTogglePinFromButton() {
        // The button should not be clickable when toggling is disabled or when locked and pinning is disallowed while locked
        if (!pinToggleEnabled || pinButtonMode == DockFloatingPinButtonMode.NEVER) {
            return false;
        }
        if (floatingGraph.isLocked() && pinLockedBehavior == DockFloatingPinLockedBehavior.HIDE_BUTTON) {
            return false;
        }
        return true;
    }

    private boolean shouldShowPinButton() {
        if (!pinToggleEnabled || pinButtonMode == DockFloatingPinButtonMode.NEVER) {
            return false;
        }
        if (pinButtonMode == DockFloatingPinButtonMode.ALWAYS) {
            return true;
        }
        return !(floatingGraph.isLocked() && pinLockedBehavior == DockFloatingPinLockedBehavior.HIDE_BUTTON);
    }

    private void updatePinButtonVisibility() {
        if (pinButton == null) {
            return;
        }
        boolean visible = shouldShowPinButton();
        pinButton.setVisible(visible);
        pinButton.setManaged(visible);
    }

    private void updatePinButtonState() {
        if (pinButton == null) {
            return;
        }
        boolean pinned = isAlwaysOnTop();
        pinButton.setGraphic(createControlIcon(pinned ? DockThemeStyleClasses.DOCK_CONTROL_ICON_PIN_ON : DockThemeStyleClasses.DOCK_CONTROL_ICON_PIN_OFF));
        if (pinTooltip != null) {
            pinTooltip.setText(pinned ? "Disable always on top" : "Enable always on top");
        }
    }

    private void applyAlwaysOnTop(boolean value, DockFloatingPinSource source) {
        boolean previous = isAlwaysOnTop();
        alwaysOnTop = value;
        if (stage != null) {
            stage.setAlwaysOnTop(value);
        }
        updatePinButtonState();
        if (previous != value && onAlwaysOnTopChanged != null) {
            onAlwaysOnTopChanged.accept(value, source);
        }
    }

    private Region createControlIcon(String styleClass) {
        Region icon = new Region();
        icon.getStyleClass().addAll(DockThemeStyleClasses.DOCK_CONTROL_ICON, styleClass);
        icon.setMouseTransparent(true);
        return icon;
    }

    private void setupTitleBarDrag(HBox titleBar, Stage window, Scene scene) {
        if (titleBar == null || window == null || scene == null) {
            return;
        }
        titleBar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> onTitleBarMousePressed(event, window, titleBar));
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> onSceneMouseDragged(event, window, titleBar));
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onSceneMouseReleased);
        titleBar.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> onTitleBarMouseClicked(event, window));
    }

    private void onTitleBarMousePressed(MouseEvent event, Stage window, HBox titleBar) {
        hideTitleBarContextMenu();
        titleBarDragActive = false;
        awaitingMaximizedRestoreDrag = false;
        if (!isPrimaryTitleBarPress(event, window)) {
            return;
        }
        titleBarDragActive = true;
        if (window.isMaximized()) {
            ensureRestoreBounds(window);
            double width = window.getWidth();
            double pointerRatio = width > 0 ? event.getX() / width : 0.5;
            pointerRatio = Math.clamp(pointerRatio, 0.0, 1.0);
            dragOffsetX = restoreWidth * pointerRatio;
            double titleBarHeight = titleBar.getHeight() > 0 ? titleBar.getHeight() : 32.0;
            dragOffsetY = Math.clamp(event.getY(), 0.0, titleBarHeight);
            titleBarPressScreenX = event.getScreenX();
            titleBarPressScreenY = event.getScreenY();
            awaitingMaximizedRestoreDrag = true;
            return;
        }
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }

    private void onSceneMouseDragged(MouseEvent event, Stage window, HBox titleBar) {
        if (!titleBarDragActive || resizing) {
            return;
        }
        if (!event.isPrimaryButtonDown()) {
            titleBarDragActive = false;
            awaitingMaximizedRestoreDrag = false;
            return;
        }
        if (window.isMaximized()) {
            if (awaitingMaximizedRestoreDrag && !hasExceededMaximizedRestoreThreshold(event)) {
                return;
            }
            restoreWindowForDrag(window, event, titleBar);
            awaitingMaximizedRestoreDrag = false;
        }
        double requestedX = event.getScreenX() - dragOffsetX;
        double requestedY = event.getScreenY() - dragOffsetY;
        Point2D snappedPosition = resolveSnappedDragPosition(window, requestedX, requestedY, event.getScreenX(), event.getScreenY());
        window.setX(snappedPosition.getX());
        window.setY(snappedPosition.getY());
    }

    private Point2D resolveSnappedDragPosition(
        Stage window,
        double requestedX,
        double requestedY,
        double pointerScreenX,
        double pointerScreenY
    ) {
        if (!snappingEnabled || snapDistance <= 0.0 || window == null || snapTargets.isEmpty()) {
            return new Point2D(requestedX, requestedY);
        }
        List<Double> xCandidates = new ArrayList<>();
        List<Double> yCandidates = new ArrayList<>();
        collectSnapCandidates(window, requestedX, requestedY, pointerScreenX, pointerScreenY, xCandidates, yCandidates);
        if (xCandidates.isEmpty() && yCandidates.isEmpty()) {
            return new Point2D(requestedX, requestedY);
        }
        return snapEngine.snap(requestedX, requestedY, snapDistance, xCandidates, yCandidates);
    }

    private void collectSnapCandidates(
        Stage window,
        double requestedX,
        double requestedY,
        double pointerScreenX,
        double pointerScreenY,
        List<Double> xCandidates,
        List<Double> yCandidates
    ) {
        if (window == null || xCandidates == null || yCandidates == null) {
            return;
        }
        double windowWidth = window.getWidth();
        double windowHeight = window.getHeight();
        if (snapTargets.contains(DockFloatingSnapTarget.SCREEN)) {
            snapEngine.addAlignmentCandidates(
                resolveScreenSnapBounds(
                    requestedX,
                    requestedY,
                    pointerScreenX,
                    pointerScreenY,
                    windowWidth,
                    windowHeight
                ),
                windowWidth,
                windowHeight,
                xCandidates,
                yCandidates
            );
        }
        if (snapTargets.contains(DockFloatingSnapTarget.MAIN_WINDOW)) {
            Rectangle2D mainWindowBounds = resolveMainWindowSnapBounds(window);
            if (mainWindowBounds != null) {
                snapEngine.addOverlapAwareCandidates(
                    List.of(mainWindowBounds),
                    requestedX,
                    requestedY,
                    windowWidth,
                    windowHeight,
                    snapDistance,
                    xCandidates,
                    yCandidates
                );
            }
        }
        if (snapTargets.contains(DockFloatingSnapTarget.FLOATING_WINDOWS)) {
            snapEngine.addOverlapAwareCandidates(
                resolvePeerFloatingSnapBounds(window),
                requestedX,
                requestedY,
                windowWidth,
                windowHeight,
                snapDistance,
                xCandidates,
                yCandidates
            );
        }
    }

    private List<Rectangle2D> resolveScreenSnapBounds(
        double requestedX,
        double requestedY,
        double pointerScreenX,
        double pointerScreenY,
        double windowWidth,
        double windowHeight
    ) {
        List<Screen> screens = Screen.getScreensForRectangle(pointerScreenX, pointerScreenY, 1.0, 1.0);
        if (screens.isEmpty()) {
            screens = Screen.getScreensForRectangle(
                requestedX,
                requestedY,
                Math.max(1.0, windowWidth),
                Math.max(1.0, windowHeight)
            );
        }
        if (screens.isEmpty()) {
            Screen primaryScreen = Screen.getPrimary();
            if (primaryScreen != null) {
                screens = List.of(primaryScreen);
            }
        }
        List<Rectangle2D> bounds = new ArrayList<>();
        for (Screen screen : screens) {
            if (screen != null) {
                bounds.add(screen.getVisualBounds());
            }
        }
        return bounds;
    }

    private Rectangle2D resolveMainWindowSnapBounds(Stage window) {
        if (window == null || !(window.getOwner() instanceof Stage ownerStage)) {
            return null;
        }
        if (ownerStage.getWidth() <= 0.0 || ownerStage.getHeight() <= 0.0) {
            return null;
        }

        Rectangle2D stageBounds = new Rectangle2D(
            ownerStage.getX(),
            ownerStage.getY(),
            ownerStage.getWidth(),
            ownerStage.getHeight()
        );
        if (ownerStage.getScene() == null || ownerStage.getScene().getRoot() == null) {
            return stageBounds;
        }
        Bounds sceneBounds = ownerStage.getScene().getRoot().localToScreen(
            ownerStage.getScene().getRoot().getBoundsInLocal()
        );
        if (sceneBounds == null) {
            return stageBounds;
        }

        double leftInset = Math.max(0.0, sceneBounds.getMinX() - stageBounds.getMinX());
        double rightInset = Math.max(0.0, stageBounds.getMaxX() - sceneBounds.getMaxX());
        double bottomInset = Math.max(0.0, stageBounds.getMaxY() - sceneBounds.getMaxY());
        double inferredShadowInset = snapEngine.inferShadowInset(leftInset, rightInset, bottomInset);
        if (inferredShadowInset <= 0.0) {
            return stageBounds;
        }

        double adjustedWidth = stageBounds.getWidth() - (2.0 * inferredShadowInset);
        double adjustedHeight = stageBounds.getHeight() - inferredShadowInset;
        if (adjustedWidth <= 0.0 || adjustedHeight <= 0.0) {
            return stageBounds;
        }
        return new Rectangle2D(
            stageBounds.getMinX() + inferredShadowInset,
            stageBounds.getMinY(),
            adjustedWidth,
            adjustedHeight
        );
    }

    private List<Rectangle2D> resolvePeerFloatingSnapBounds(Stage window) {
        List<Rectangle2D> peerBounds = new ArrayList<>();
        if (snapPeerWindowsSupplier == null) {
            return peerBounds;
        }
        List<DockFloatingWindow> peerWindows = snapPeerWindowsSupplier.get();
        if (peerWindows == null || peerWindows.isEmpty()) {
            return peerBounds;
        }
        for (DockFloatingWindow peerWindow : peerWindows) {
            if (peerWindow == null || peerWindow == this) {
                continue;
            }
            Stage peerStage = peerWindow.stage;
            if (peerStage == null || peerStage == window || peerStage.getWidth() <= 0.0 || peerStage.getHeight() <= 0.0) {
                continue;
            }
            peerBounds.add(new Rectangle2D(
                peerStage.getX(),
                peerStage.getY(),
                peerStage.getWidth(),
                peerStage.getHeight()
            ));
        }
        return peerBounds;
    }

    private void onSceneMouseReleased(MouseEvent event) {
        titleBarDragActive = false;
        awaitingMaximizedRestoreDrag = false;
    }

    private void onTitleBarMouseClicked(MouseEvent event, Stage window) {
        if (!isTitleBarActionCandidate(event, window)
            || event.getButton() != MouseButton.PRIMARY
            || event.getClickCount() != 2) {
            return;
        }
        toggleMaximize(window);
    }

    private boolean isPrimaryTitleBarPress(MouseEvent event, Stage window) {
        return event != null
            && event.getButton() == MouseButton.PRIMARY
            && isTitleBarActionCandidate(event, window);
    }

    private boolean hasExceededMaximizedRestoreThreshold(MouseEvent event) {
        if (event == null) {
            return false;
        }
        double deltaX = event.getScreenX() - titleBarPressScreenX;
        double deltaY = event.getScreenY() - titleBarPressScreenY;
        double distance = Math.hypot(deltaX, deltaY);
        return distance >= MAXIMIZED_RESTORE_DRAG_THRESHOLD;
    }

    private void hideTitleBarContextMenu() {
        if (titleBarContextMenu == null) {
            return;
        }
        titleBarContextMenu.hide();
    }

    private boolean isTitleBarActionCandidate(MouseEvent event, Stage window) {
        return !resizing
            && !isInteractiveControlTarget(event.getTarget())
            && resolveResizeMask(event.getScreenX(), event.getScreenY(), window) == 0;
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

    private void toggleMaximize(Stage window) {
        if (window.isMaximized()) {
            window.setMaximized(false);
            restoreWindowBounds(window);
        } else {
            rememberRestoreBounds(window);
            window.setMaximized(true);
        }
        updateMaximizeButtonState(window);
    }

    private void ensureRestoreBounds(Stage window) {
        if (hasRestoreBounds || window == null) {
            return;
        }
        double fallbackWidth = preferredWidth > 0 ? preferredWidth : DEFAULT_WIDTH;
        double fallbackHeight = preferredHeight > 0 ? preferredHeight : DEFAULT_HEIGHT;
        restoreWidth = Math.max(resolveMinimumWindowWidth(window), fallbackWidth);
        restoreHeight = Math.max(resolveMinimumWindowHeight(window), fallbackHeight);
        restoreX = window.getX();
        restoreY = window.getY();
        hasRestoreBounds = true;
    }

    private void restoreWindowForDrag(Stage window, MouseEvent event, HBox titleBar) {
        ensureRestoreBounds(window);
        window.setMaximized(false);
        restoreWindowBounds(window);
        double titleBarHeight = titleBar.getHeight() > 0 ? titleBar.getHeight() : 32.0;
        dragOffsetY = Math.clamp(event.getY(), 0.0, titleBarHeight);
    }

    private void rememberRestoreBounds(Stage window) {
        restoreX = window.getX();
        restoreY = window.getY();
        restoreWidth = window.getWidth();
        restoreHeight = window.getHeight();
        hasRestoreBounds = true;
    }

    private void restoreWindowBounds(Stage window) {
        if (!hasRestoreBounds) {
            return;
        }
        window.setX(restoreX);
        window.setY(restoreY);
        window.setWidth(restoreWidth);
        window.setHeight(restoreHeight);
    }

    private void updateMaximizeButtonState(Stage window) {
        if (maximizeButton == null) {
            return;
        }
        if (window.isMaximized()) {
            maximizeButton.setGraphic(createControlIcon(DockThemeStyleClasses.DOCK_CONTROL_ICON_RESTORE));
            if (maximizeTooltip != null) {
                maximizeTooltip.setText("Restore window");
            }
        } else {
            maximizeButton.setGraphic(createControlIcon(DockThemeStyleClasses.DOCK_CONTROL_ICON_MAXIMIZE));
            if (maximizeTooltip != null) {
                maximizeTooltip.setText("Maximize window");
            }
        }
    }

    private void setupResizeHandling(BorderPane root, Stage window) {
        root.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, event -> updateResizeCursor(root, window, event));
        root.addEventFilter(MouseEvent.MOUSE_MOVED, event -> updateResizeCursor(root, window, event));
        root.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, event -> applyCursor(root, window.getScene(), Cursor.DEFAULT, event.getTarget()));

        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (window.isMaximized()
                || event.getButton() != MouseButton.PRIMARY
                || event.getTarget() instanceof Button) {
                return;
            }
            int mask = resolveResizeMask(event.getScreenX(), event.getScreenY(), window);
            if (mask == 0) {
                return;
            }
            beginResize(event, window, mask);
        });

        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!resizing || window.isMaximized()) {
                return;
            }
            performResize(event, window);
            applyCursor(root, window.getScene(), resolveCursor(activeResizeMask), event.getTarget());
        });

        root.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!resizing) {
                return;
            }
            resizing = false;
            activeResizeMask = 0;
            updateResizeCursor(root, window, event);
        });
    }

    private void updateResizeCursor(BorderPane root, Stage window, MouseEvent event) {
        if (window == null || event == null) {
            return;
        }
        if (window.isMaximized() || resizing) {
            applyCursor(root, window.getScene(), Cursor.DEFAULT, event.getTarget());
            return;
        }
        int mask = resolveResizeMask(event.getScreenX(), event.getScreenY(), window);
        applyCursor(root, window.getScene(), resolveCursor(mask), event.getTarget());
    }

    private void applyCursor(Node node, Scene scene, Cursor cursor, Object target) {
        Cursor effectiveCursor = cursor == null ? Cursor.DEFAULT : cursor;
        if (node != null) {
            node.setCursor(effectiveCursor);
        }
        if (scene != null) {
            scene.setCursor(effectiveCursor);
        }
        applyTargetCursor(target, effectiveCursor);
    }

    private void applyTargetCursor(Object target, Cursor cursor) {
        Node nodeTarget = target instanceof Node node ? node : null;
        Cursor effectiveCursor = cursor == null ? Cursor.DEFAULT : cursor;

        if (effectiveCursor == Cursor.DEFAULT || nodeTarget == null) {
            clearTargetCursorOverride();
            return;
        }

        if (resizeCursorTargetNode != nodeTarget) {
            clearTargetCursorOverride();
            resizeCursorTargetNode = nodeTarget;
            resizeCursorTargetPrevious = nodeTarget.getCursor();
        }
        nodeTarget.setCursor(effectiveCursor);
    }

    private void clearTargetCursorOverride() {
        if (resizeCursorTargetNode == null) {
            return;
        }
        resizeCursorTargetNode.setCursor(resizeCursorTargetPrevious);
        resizeCursorTargetNode = null;
        resizeCursorTargetPrevious = null;
    }

    private void beginResize(MouseEvent event, Stage window, int mask) {
        resizing = true;
        activeResizeMask = mask;
        resizeStartScreenX = event.getScreenX();
        resizeStartScreenY = event.getScreenY();
        resizeStartWindowX = window.getX();
        resizeStartWindowY = window.getY();
        resizeStartWindowWidth = window.getWidth();
        resizeStartWindowHeight = window.getHeight();
        rememberRestoreBounds(window);
        event.consume();
    }

    private void performResize(MouseEvent event, Stage window) {
        if (window == null) {
            return;
        }

        double deltaX = event.getScreenX() - resizeStartScreenX;
        double deltaY = event.getScreenY() - resizeStartScreenY;
        double minWidth = resolveMinimumWindowWidth(window);
        double minHeight = resolveMinimumWindowHeight(window);

        double x = resizeStartWindowX;
        double y = resizeStartWindowY;
        double width = resizeStartWindowWidth;
        double height = resizeStartWindowHeight;

        if ((activeResizeMask & RESIZE_LEFT) != 0) {
            double candidateWidth = resizeStartWindowWidth - deltaX;
            if (candidateWidth < minWidth) {
                candidateWidth = minWidth;
                x = resizeStartWindowX + (resizeStartWindowWidth - minWidth);
            } else {
                x = resizeStartWindowX + deltaX;
            }
            width = candidateWidth;
        } else if ((activeResizeMask & RESIZE_RIGHT) != 0) {
            width = Math.max(minWidth, resizeStartWindowWidth + deltaX);
        }

        if ((activeResizeMask & RESIZE_TOP) != 0) {
            double candidateHeight = resizeStartWindowHeight - deltaY;
            if (candidateHeight < minHeight) {
                candidateHeight = minHeight;
                y = resizeStartWindowY + (resizeStartWindowHeight - minHeight);
            } else {
                y = resizeStartWindowY + deltaY;
            }
            height = candidateHeight;
        } else if ((activeResizeMask & RESIZE_BOTTOM) != 0) {
            height = Math.max(minHeight, resizeStartWindowHeight + deltaY);
        }

        window.setX(x);
        window.setY(y);
        window.setWidth(width);
        window.setHeight(height);
    }

    private double resolveMinimumWindowWidth(Stage window) {
        double minimum = MIN_WINDOW_WIDTH;
        if (window != null) {
            minimum = Math.max(minimum, window.getMinWidth());
            Scene scene = window.getScene();
            if (scene != null && scene.getRoot() != null) {
                double contentMinimum = scene.getRoot().minWidth(-1);
                if (Double.isFinite(contentMinimum) && contentMinimum > 0.0) {
                    minimum = Math.max(minimum, contentMinimum);
                }
            }
        }
        return minimum;
    }

    private double resolveMinimumWindowHeight(Stage window) {
        double minimum = MIN_WINDOW_HEIGHT;
        if (window != null) {
            minimum = Math.max(minimum, window.getMinHeight());
            Scene scene = window.getScene();
            if (scene != null && scene.getRoot() != null) {
                double contentMinimum = scene.getRoot().minHeight(-1);
                if (Double.isFinite(contentMinimum) && contentMinimum > 0.0) {
                    minimum = Math.max(minimum, contentMinimum);
                }
            }
        }
        return minimum;
    }

    private int resolveResizeMask(double screenX, double screenY, Stage window) {
        if (window == null) {
            return 0;
        }
        double localX = screenX - window.getX();
        double localY = screenY - window.getY();
        double width = window.getWidth();
        double height = window.getHeight();

        boolean nearLeft = localX <= RESIZE_MARGIN;
        boolean nearRight = localX >= (width - RESIZE_MARGIN);
        boolean nearTop = localY <= RESIZE_MARGIN;
        boolean nearBottom = localY >= (height - RESIZE_MARGIN);

        int mask = 0;
        if (nearLeft) {
            mask |= RESIZE_LEFT;
        } else if (nearRight) {
            mask |= RESIZE_RIGHT;
        }
        if (nearTop) {
            mask |= RESIZE_TOP;
        } else if (nearBottom) {
            mask |= RESIZE_BOTTOM;
        }
        return mask;
    }

    private Cursor resolveCursor(int mask) {
        return switch (mask) {
            case RESIZE_LEFT -> Cursor.W_RESIZE;
            case RESIZE_RIGHT -> Cursor.E_RESIZE;
            case RESIZE_TOP -> Cursor.N_RESIZE;
            case RESIZE_BOTTOM -> Cursor.S_RESIZE;
            case RESIZE_LEFT | RESIZE_TOP -> Cursor.NW_RESIZE;
            case RESIZE_RIGHT | RESIZE_TOP -> Cursor.NE_RESIZE;
            case RESIZE_LEFT | RESIZE_BOTTOM -> Cursor.SW_RESIZE;
            case RESIZE_RIGHT | RESIZE_BOTTOM -> Cursor.SE_RESIZE;
            default -> Cursor.DEFAULT;
        };
    }

    private void rebuildLayout() {
        if (layoutContainer == null) {
            return;
        }
        Node layout = floatingLayoutEngine.buildSceneGraph();
        layoutContainer.getChildren().clear();
        if (layout != null) {
            layoutContainer.getChildren().add(layout);
        }
        refreshTabSelectionListeners();
        updateInnerNodeActionVisibility();
        updateWindowTitleAndIcon();
    }

    private void applyWindowPosition(Stage window, Stage ownerStage) {
        if (preferredX != null) {
            window.setX(preferredX);
        } else if (ownerStage != null) {
            window.setX(ownerStage.getX() + DEFAULT_OFFSET_X);
        }

        if (preferredY != null) {
            window.setY(preferredY);
        } else if (ownerStage != null) {
            window.setY(ownerStage.getY() + DEFAULT_OFFSET_Y);
        }
    }

    private void updateWindowTitleAndIcon() {
        DockNode representativeNode = resolveRepresentativeNode(floatingGraph.getRoot());
        String title = buildWindowTitle(representativeNode);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
        if (stage != null) {
            stage.setTitle(titlePrefix + " - " + title);
        }

        if (iconPane != null) {
            iconPane.getChildren().clear();
            if (representativeNode != null) {
                Node iconNode = createDockNodeIcon(representativeNode.getIcon());
                if (iconNode != null) {
                    iconPane.getChildren().add(iconNode);
                }
            }
        }
    }

    private ImageView createDockNodeIcon(Image image) {
        if (image == null) {
            return null;
        }
        ImageView icon = new ImageView(image);
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        icon.setCache(true);
        icon.setMouseTransparent(true);
        return icon;
    }

    private void refreshTabSelectionListeners() {
        clearTabSelectionListeners();
        registerTabSelectionListeners(floatingGraph.getRoot());
    }

    private void registerTabSelectionListeners(DockElement element) {
        if (element == null) {
            return;
        }
        if (element instanceof DockTabPane tabPane) {
            ChangeListener<Number> selectedIndexListener = (obs, oldValue, newValue) -> updateWindowTitleAndIcon();
            tabPane.selectedIndexProperty().addListener(selectedIndexListener);
            tabSelectionListenersCleanup.add(() -> tabPane.selectedIndexProperty().removeListener(selectedIndexListener));
        }
        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                registerTabSelectionListeners(child);
            }
        }
    }

    private void clearTabSelectionListeners() {
        for (Runnable cleanup : new ArrayList<>(tabSelectionListenersCleanup)) {
            cleanup.run();
        }
        tabSelectionListenersCleanup.clear();
    }

    private String buildWindowTitle(DockNode representativeNode) {
        int nodeCount = getDockNodes().size();
        if (representativeNode == null) {
            return "Floating Window";
        }
        String baseTitle = representativeNode.getTitle();
        if (baseTitle == null || baseTitle.isBlank()) {
            baseTitle = "Floating Window";
        }
        if (nodeCount <= 1) {
            return baseTitle;
        }
        return baseTitle + " +" + (nodeCount - 1);
    }

    private DockNode resolveRepresentativeNode(DockElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof DockNode dockNode) {
            return dockNode;
        }
        if (element instanceof DockTabPane tabPane && !tabPane.getChildren().isEmpty()) {
            int selectedIndex = Math.clamp(tabPane.getSelectedIndex(), 0, tabPane.getChildren().size() - 1);
            return resolveRepresentativeNode(tabPane.getChildren().get(selectedIndex));
        }
        if (element instanceof DockContainer container && !container.getChildren().isEmpty()) {
            return resolveRepresentativeNode(container.getChildren().getFirst());
        }
        return null;
    }

    private List<DockDropZone> filterZonesForDrop(List<DockDropZone> zones, DockNode draggedNode) {
        List<DockDropZone> validZones = new ArrayList<>();
        for (DockDropZone zone : zones) {
            if (zone == null || zone.getTarget() == null) {
                continue;
            }
            DockElement target = zone.getTarget();
            if (target == draggedNode) {
                continue;
            }
            if (!isElementVisibleForInteraction(target)) {
                continue;
            }
            if (isDescendantOf(target, draggedNode)) {
                continue;
            }
            if (zone.getType() == DockDropZoneType.TAB_INSERT || zone.getType() == DockDropZoneType.TAB_HEADER
                || zone.getType() == DockDropZoneType.EDGE || zone.getType() == DockDropZoneType.CENTER) {
                validZones.add(zone);
            }
        }
        return validZones;
    }

    private boolean activateTabHoverIfNeeded(DockDropZone activeZone) {
        if (activeZone == null || activeZone.getType() != DockDropZoneType.TAB_HEADER) {
            return false;
        }
        if (!(activeZone.getTarget() instanceof DockTabPane targetTabPane)) {
            return false;
        }
        if (targetTabPane.getChildren().isEmpty()) {
            return false;
        }
        Integer tabIndex = activeZone.getTabIndex();
        if (tabIndex == null) {
            return false;
        }
        int hoveredTabIndex = Math.clamp(tabIndex, 0, targetTabPane.getChildren().size() - 1);
        if (targetTabPane.getSelectedIndex() == hoveredTabIndex) {
            return false;
        }
        targetTabPane.setSelectedIndex(hoveredTabIndex);
        return true;
    }

    private List<DockDropZone> getZonesForVisualization(
        List<DockDropZone> validZones,
        DockDropZone activeZone,
        DockDropVisualizationMode visualizationMode
    ) {
        if (visualizationMode == null || visualizationMode == DockDropVisualizationMode.OFF) {
            return List.of();
        }
        if (visualizationMode == DockDropVisualizationMode.ACTIVE_ONLY) {
            return List.of();
        }
        if (visualizationMode == DockDropVisualizationMode.ALL_ZONES) {
            return validZones;
        }
        if (activeZone == null) {
            return List.of();
        }
        if (visualizationMode == DockDropVisualizationMode.SUBTREE) {
            return filterZonesBySubtree(validZones, activeZone.getTarget());
        }
        if (visualizationMode == DockDropVisualizationMode.DEFAULT) {
            return filterZonesByTarget(validZones, activeZone.getTarget());
        }
        return List.of();
    }

    private List<DockDropZone> filterZonesBySubtree(List<DockDropZone> zones, DockElement subtreeRoot) {
        List<DockDropZone> result = new ArrayList<>();
        for (DockDropZone zone : zones) {
            if (zone.getTarget() != null && isDescendantOf(zone.getTarget(), subtreeRoot)) {
                result.add(zone);
            }
        }
        return result;
    }

    private List<DockDropZone> filterZonesByTarget(List<DockDropZone> zones, DockElement target) {
        List<DockDropZone> result = new ArrayList<>();
        for (DockDropZone zone : zones) {
            if (zone.getTarget() == target) {
                result.add(zone);
            }
        }
        return result;
    }

    private boolean isElementVisibleForInteraction(DockElement element) {
        DockElement current = element;
        while (current != null) {
            if (current.getParent() instanceof DockTabPane parentTabPane) {
                int selectedIndex = parentTabPane.getSelectedIndex();
                int childIndex = parentTabPane.getChildren().indexOf(current);
                if (childIndex < 0 || childIndex != selectedIndex) {
                    return false;
                }
            }
            current = current.getParent();
        }
        return true;
    }

    private boolean isDescendantOf(DockElement element, DockElement ancestor) {
        DockElement current = element;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void collectDockNodes(DockElement element, List<DockNode> nodes) {
        if (element == null) {
            return;
        }
        if (element instanceof DockNode dockNode) {
            nodes.add(dockNode);
            return;
        }
        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                collectDockNodes(child, nodes);
            }
        }
    }

    private boolean findNode(DockElement element, DockNode target) {
        if (element == null || target == null) {
            return false;
        }
        if (element == target) {
            return true;
        }
        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                if (findNode(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class FloatingDropIndicator extends Pane {
        private final Rectangle indicator;
        private final Line insertLine;

        FloatingDropIndicator() {
            setMouseTransparent(true);
            setVisible(false);

            indicator = new Rectangle();
            indicator.setFill(Color.DODGERBLUE);
            indicator.setOpacity(0.3);
            indicator.setStroke(Color.BLUE);
            indicator.setStrokeWidth(2);

            insertLine = new Line();
            insertLine.setStroke(Color.web("#ff8a00"));
            insertLine.setStrokeWidth(3);
            insertLine.setVisible(false);

            getChildren().addAll(indicator, insertLine);
        }

        void show(Bounds bounds, Double insertLineX) {
            if (bounds == null) {
                hide();
                return;
            }

            Point2D topLeft = sceneToLocal(bounds.getMinX(), bounds.getMinY());
            Point2D bottomRight = sceneToLocal(bounds.getMaxX(), bounds.getMaxY());
            double x = topLeft.getX();
            double y = topLeft.getY();
            double width = Math.max(1, bottomRight.getX() - topLeft.getX());
            double height = Math.max(1, bottomRight.getY() - topLeft.getY());

            indicator.setX(x);
            indicator.setY(y);
            indicator.setWidth(width);
            indicator.setHeight(height);
            setVisible(true);
            super.toFront();

            if (insertLineX != null) {
                Point2D lineTop = sceneToLocal(insertLineX, bounds.getMinY());
                Point2D lineBottom = sceneToLocal(insertLineX, bounds.getMaxY());
                insertLine.setStartX(lineTop.getX());
                insertLine.setStartY(lineTop.getY());
                insertLine.setEndX(lineBottom.getX());
                insertLine.setEndY(lineBottom.getY());
                insertLine.setVisible(true);
            } else {
                insertLine.setVisible(false);
            }
        }

        void hide() {
            setVisible(false);
            insertLine.setVisible(false);
        }
    }

    private static class FloatingDropZonesOverlay extends Pane {
        private final List<Rectangle> rectangles = new ArrayList<>();

        FloatingDropZonesOverlay() {
            setMouseTransparent(true);
            setVisible(false);
        }

        void showZones(List<DockDropZone> zones) {
            getChildren().clear();
            rectangles.clear();
            if (zones == null || zones.isEmpty()) {
                hide();
                return;
            }

            for (DockDropZone zone : zones) {
                Bounds bounds = zone.getBounds();
                if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                    continue;
                }
                Point2D topLeft = sceneToLocal(bounds.getMinX(), bounds.getMinY());
                Point2D bottomRight = sceneToLocal(bounds.getMaxX(), bounds.getMaxY());
                double x = topLeft.getX();
                double y = topLeft.getY();
                double width = Math.max(1, bottomRight.getX() - topLeft.getX());
                double height = Math.max(1, bottomRight.getY() - topLeft.getY());

                Rectangle rect = new Rectangle(x, y, width, height);
                rect.setFill(Color.web("#3a7bd5", 0.10));
                rect.setStroke(Color.web("#3a7bd5", 0.25));
                rect.setStrokeWidth(1);
                rectangles.add(rect);
            }

            if (rectangles.isEmpty()) {
                hide();
                return;
            }
            getChildren().addAll(rectangles);
            setVisible(true);
            super.toFront();
        }

        void hide() {
            setVisible(false);
            getChildren().clear();
            rectangles.clear();
        }
    }

    private void notifyWindowActivated() {
        if (onWindowActivated != null) {
            onWindowActivated.run();
        }
    }

    private boolean canProcessCloseRequest() {
        if (suppressCloseRequestHandling) {
            return true;
        }
        if (floatingGraph.isLocked()) {
            return false;
        }
        if (onCloseRequested == null) {
            return true;
        }
        return onCloseRequested.getAsBoolean();
    }

    private void handleInnerNodeCloseRequest(DockNode node, DockCloseSource source) {
        if (node == null || floatingGraph.isLocked()) {
            return;
        }
        if (onNodeCloseRequest != null) {
            onNodeCloseRequest.accept(node, source);
            return;
        }
        floatingGraph.undock(node);
    }

    private void handleInnerNodeFloatRequest(DockNode node) {
        if (node == null || floatingGraph.isLocked() || getDockNodes().size() <= 1) {
            return;
        }
        if (onNodeFloatRequest != null) {
            onNodeFloatRequest.accept(node);
        }
    }

    private void updateInnerNodeActionVisibility() {
        boolean showNodeActions = getDockNodes().size() > 1;
        List<DockNode> nodes = getDockNodes();
        for (DockNode node : nodes) {
            DockNodeView nodeView = floatingLayoutEngine.getDockNodeView(node);
            if (nodeView == null) {
                continue;
            }
            if (showNodeActions) {
                nodeView.bindFloatButtonVisible(floatingGraph.lockedProperty().not());
                nodeView.bindCloseButtonVisible(node.closeableProperty().and(floatingGraph.lockedProperty().not()));
            } else {
                nodeView.setFloatButtonVisible(false);
                nodeView.setCloseButtonVisible(false);
            }
        }
    }

    private void onWindowHidden(Stage hiddenStage) {
        clearDropPreview();
        clearTargetCursorOverride();
        clearTabSelectionListeners();
        if (hiddenStage.isMaximized() && hasRestoreBounds) {
            preferredX = restoreX;
            preferredY = restoreY;
            preferredWidth = restoreWidth;
            preferredHeight = restoreHeight;
        } else {
            preferredX = hiddenStage.getX();
            preferredY = hiddenStage.getY();
            preferredWidth = hiddenStage.getWidth();
            preferredHeight = hiddenStage.getHeight();
        }

        if (iconPane != null) {
            iconPane.getChildren().clear();
        }
        if (titleBarContextMenu != null) {
            titleBarContextMenu.hide();
            titleBarContextMenu = null;
        }
        pinButton = null;
        pinTooltip = null;
        maximizeButton = null;
        maximizeTooltip = null;

        stage = null;
        resizing = false;
        activeResizeMask = 0;
        suppressCloseRequestHandling = false;
        if (!suppressCloseNotification && onWindowClosed != null) {
            onWindowClosed.accept(this);
        }
        suppressCloseNotification = false;
    }

    public record DropTarget(DockElement target, DockPosition position, Integer tabIndex, int depth, double area) {
    }

    private record DropZoneResolution(List<DockDropZone> validZones, DockDropZone activeZone) {
        private static DropZoneResolution empty() {
            return new DropZoneResolution(List.of(), null);
        }
    }
}

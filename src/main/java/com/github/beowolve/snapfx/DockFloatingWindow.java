package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.dnd.DockDropVisualizationMode;
import com.github.beowolve.snapfx.model.DockContainer;
import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import com.github.beowolve.snapfx.model.DockTabPane;
import com.github.beowolve.snapfx.view.DockDropZone;
import com.github.beowolve.snapfx.view.DockDropZoneType;
import com.github.beowolve.snapfx.view.DockLayoutEngine;
import com.github.beowolve.snapfx.view.DockNodeView;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents an external floating window that can host a full dock layout subtree.
 */
public final class DockFloatingWindow {
    private static final double DEFAULT_WIDTH = 640;
    private static final double DEFAULT_HEIGHT = 420;
    private static final double DEFAULT_OFFSET_X = 40;
    private static final double DEFAULT_OFFSET_Y = 40;
    private static final double RESIZE_MARGIN = 6.0;
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

    private Stage stage;
    private Double preferredX;
    private Double preferredY;
    private double preferredWidth = DEFAULT_WIDTH;
    private double preferredHeight = DEFAULT_HEIGHT;
    private boolean suppressCloseNotification;

    private Runnable onAttachRequested;
    private Consumer<DockFloatingWindow> onWindowClosed;
    private Runnable onWindowActivated;
    private Consumer<DockNode> onNodeFloatRequest;

    private StackPane iconPane;
    private Label titleLabel;
    private Button maximizeButton;
    private Tooltip maximizeTooltip;

    private double dragOffsetX;
    private double dragOffsetY;
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

    DockFloatingWindow(DockNode dockNode) {
        this((DockElement) dockNode, "SnapFX", null);
    }

    DockFloatingWindow(DockNode dockNode, DockDragService dragService) {
        this((DockElement) dockNode, "SnapFX", dragService);
    }

    DockFloatingWindow(DockNode dockNode, String titlePrefix) {
        this((DockElement) dockNode, titlePrefix, null);
    }

    DockFloatingWindow(DockNode dockNode, String titlePrefix, DockDragService dragService) {
        this((DockElement) dockNode, titlePrefix, dragService);
    }

    DockFloatingWindow(DockElement floatingRoot, DockDragService dragService) {
        this(floatingRoot, "SnapFX", dragService);
    }

    DockFloatingWindow(DockElement floatingRoot, String titlePrefix, DockDragService dragService) {
        this.id = UUID.randomUUID().toString();
        DockElement rootElement = Objects.requireNonNull(floatingRoot, "floatingRoot");
        DockNode representative = findFirstDockNode(rootElement);
        this.primaryDockNode = Objects.requireNonNull(representative, "floatingRoot must contain at least one DockNode");
        this.titlePrefix = (titlePrefix == null || titlePrefix.isBlank()) ? "SnapFX" : titlePrefix;
        this.floatingGraph = new DockGraph();
        this.floatingLayoutEngine = new DockLayoutEngine(floatingGraph, dragService);
        this.floatingLayoutEngine.setOnNodeFloatRequest(this::handleInnerNodeFloatRequest);
        this.layoutContainer = new StackPane();
        this.layoutContainer.getStyleClass().add("dock-floating-layout-container");
        this.dropIndicator = new FloatingDropIndicator();
        this.dropZonesOverlay = new FloatingDropZonesOverlay();
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
                    closeInternal(true);
                }
            })
        );
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

    boolean containsScreenPoint(double screenX, double screenY) {
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

    void captureCurrentBounds() {
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
        closeInternal(true);
    }

    void closeWithoutNotification() {
        closeInternal(false);
    }

    void setOnAttachRequested(Runnable onAttachRequested) {
        this.onAttachRequested = onAttachRequested;
    }

    void setOnWindowClosed(Consumer<DockFloatingWindow> onWindowClosed) {
        this.onWindowClosed = onWindowClosed;
    }

    void setOnWindowActivated(Runnable onWindowActivated) {
        this.onWindowActivated = onWindowActivated;
    }

    void setOnNodeFloatRequest(Consumer<DockNode> onNodeFloatRequest) {
        this.onNodeFloatRequest = onNodeFloatRequest;
    }

    void undockNode(DockNode node) {
        floatingGraph.undock(node);
    }

    void dockNode(DockNode node, DockElement target, DockPosition position, Integer tabIndex) {
        floatingGraph.dock(node, target, position, tabIndex);
    }

    void moveNode(DockNode node, DockElement target, DockPosition position, Integer tabIndex) {
        floatingGraph.move(node, target, position, tabIndex);
    }

    void requestFloatForNode(DockNode node) {
        handleInnerNodeFloatRequest(node);
    }

    DockNodeView getDockNodeView(DockNode node) {
        return floatingLayoutEngine.getDockNodeView(node);
    }

    DropTarget resolveDropTarget(double screenX, double screenY, DockNode draggedNode) {
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

    void updateDropPreview(
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

    void clearDropPreview() {
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
        }
        if (stage == null) {
            if (notify && onWindowClosed != null) {
                onWindowClosed.accept(this);
            }
            suppressCloseNotification = false;
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
        root.getStyleClass().add("dock-floating-window");

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
        window.setScene(scene);
        applyWindowPosition(window, ownerStage);

        window.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                notifyWindowActivated();
            }
        });
        window.maximizedProperty().addListener((obs, oldValue, newValue) -> updateMaximizeButtonState(window));
        updateMaximizeButtonState(window);
        window.setOnHidden(e -> onWindowHidden(window));

        return window;
    }

    private HBox createTitleBar(Stage window) {
        HBox titleBar = new HBox(6);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().addAll("dock-node-header", "dock-floating-title-bar");

        iconPane = new StackPane();
        iconPane.setPrefSize(16, 16);
        iconPane.setMaxSize(16, 16);
        iconPane.setMinSize(16, 16);

        titleLabel = new Label();
        titleLabel.getStyleClass().add("dock-node-title-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button attachButton = createControlButton(
            "dock-control-icon-attach",
            "Attach to layout",
            () -> {
                if (onAttachRequested != null) {
                    onAttachRequested.run();
                }
            },
            "dock-window-attach-button"
        );

        maximizeTooltip = new Tooltip("Maximize window");
        maximizeButton = createControlButton(
            "dock-control-icon-maximize",
            maximizeTooltip.getText(),
            () -> toggleMaximize(window),
            "dock-window-maximize-button"
        );
        maximizeButton.setTooltip(maximizeTooltip);

        Button closeButton = createControlButton(
            "dock-control-icon-close",
            "Close floating window",
            this::close,
            "dock-window-close-button"
        );

        titleBar.getChildren().addAll(iconPane, titleLabel, spacer, attachButton, maximizeButton, closeButton);
        setupTitleBarDrag(titleBar, window);
        return titleBar;
    }

    private Button createControlButton(String iconStyleClass, String tooltipText, Runnable action, String styleClass) {
        Button button = new Button();
        button.getStyleClass().addAll("dock-node-close-button", "dock-window-control-button", styleClass);
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

    private Region createControlIcon(String styleClass) {
        Region icon = new Region();
        icon.getStyleClass().addAll("dock-control-icon", styleClass);
        icon.setMouseTransparent(true);
        return icon;
    }

    private void setupTitleBarDrag(HBox titleBar, Stage window) {
        titleBar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!isTitleBarActionCandidate(event, window)) {
                return;
            }
            if (window.isMaximized()) {
                ensureRestoreBounds(window);
                double width = window.getWidth();
                double pointerRatio = width > 0 ? event.getX() / width : 0.5;
                pointerRatio = Math.clamp(pointerRatio, 0.0, 1.0);
                dragOffsetX = restoreWidth * pointerRatio;
                double titleBarHeight = titleBar.getHeight() > 0 ? titleBar.getHeight() : 32.0;
                dragOffsetY = Math.clamp(event.getY(), 0.0, titleBarHeight);
                return;
            }
            dragOffsetX = event.getScreenX() - window.getX();
            dragOffsetY = event.getScreenY() - window.getY();
        });

        titleBar.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!isTitleBarActionCandidate(event, window)) {
                return;
            }
            if (window.isMaximized()) {
                restoreWindowForDrag(window, event, titleBar);
            }
            window.setX(event.getScreenX() - dragOffsetX);
            window.setY(event.getScreenY() - dragOffsetY);
        });

        titleBar.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (!isTitleBarActionCandidate(event, window)
                || event.getButton() != MouseButton.PRIMARY
                || event.getClickCount() != 2) {
                return;
            }
            toggleMaximize(window);
        });
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
        if (hasRestoreBounds) {
            return;
        }
        double fallbackWidth = preferredWidth > 0 ? preferredWidth : DEFAULT_WIDTH;
        double fallbackHeight = preferredHeight > 0 ? preferredHeight : DEFAULT_HEIGHT;
        restoreWidth = Math.max(MIN_WINDOW_WIDTH, fallbackWidth);
        restoreHeight = Math.max(MIN_WINDOW_HEIGHT, fallbackHeight);
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
            maximizeButton.setGraphic(createControlIcon("dock-control-icon-restore"));
            if (maximizeTooltip != null) {
                maximizeTooltip.setText("Restore window");
            }
        } else {
            maximizeButton.setGraphic(createControlIcon("dock-control-icon-maximize"));
            if (maximizeTooltip != null) {
                maximizeTooltip.setText("Maximize window");
            }
        }
    }

    private void setupResizeHandling(BorderPane root, Stage window) {
        root.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, event -> updateResizeCursor(root, window, event));
        root.addEventFilter(MouseEvent.MOUSE_MOVED, event -> updateResizeCursor(root, window, event));
        root.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, event -> applyCursor(root, window.getScene(), Cursor.DEFAULT));

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
            applyCursor(root, window.getScene(), resolveCursor(activeResizeMask));
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
            applyCursor(root, window.getScene(), Cursor.DEFAULT);
            return;
        }
        int mask = resolveResizeMask(event.getScreenX(), event.getScreenY(), window);
        applyCursor(root, window.getScene(), resolveCursor(mask));
    }

    private void applyCursor(Node node, Scene scene, Cursor cursor) {
        Cursor effectiveCursor = cursor == null ? Cursor.DEFAULT : cursor;
        if (node != null) {
            node.setCursor(effectiveCursor);
        }
        if (scene != null) {
            scene.setCursor(effectiveCursor);
        }
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
        double deltaX = event.getScreenX() - resizeStartScreenX;
        double deltaY = event.getScreenY() - resizeStartScreenY;

        double x = resizeStartWindowX;
        double y = resizeStartWindowY;
        double width = resizeStartWindowWidth;
        double height = resizeStartWindowHeight;

        if ((activeResizeMask & RESIZE_LEFT) != 0) {
            double candidateWidth = resizeStartWindowWidth - deltaX;
            if (candidateWidth < MIN_WINDOW_WIDTH) {
                candidateWidth = MIN_WINDOW_WIDTH;
                x = resizeStartWindowX + (resizeStartWindowWidth - MIN_WINDOW_WIDTH);
            } else {
                x = resizeStartWindowX + deltaX;
            }
            width = candidateWidth;
        } else if ((activeResizeMask & RESIZE_RIGHT) != 0) {
            width = Math.max(MIN_WINDOW_WIDTH, resizeStartWindowWidth + deltaX);
        }

        if ((activeResizeMask & RESIZE_TOP) != 0) {
            double candidateHeight = resizeStartWindowHeight - deltaY;
            if (candidateHeight < MIN_WINDOW_HEIGHT) {
                candidateHeight = MIN_WINDOW_HEIGHT;
                y = resizeStartWindowY + (resizeStartWindowHeight - MIN_WINDOW_HEIGHT);
            } else {
                y = resizeStartWindowY + deltaY;
            }
            height = candidateHeight;
        } else if ((activeResizeMask & RESIZE_BOTTOM) != 0) {
            height = Math.max(MIN_WINDOW_HEIGHT, resizeStartWindowHeight + deltaY);
        }

        window.setX(x);
        window.setY(y);
        window.setWidth(width);
        window.setHeight(height);
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
            if (representativeNode != null && representativeNode.getIcon() != null) {
                iconPane.getChildren().add(representativeNode.getIcon());
            }
        }
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
            toFront();

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
            toFront();
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

    private void handleInnerNodeFloatRequest(DockNode node) {
        if (node == null || floatingGraph.isLocked()) {
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

        stage = null;
        resizing = false;
        activeResizeMask = 0;
        if (!suppressCloseNotification && onWindowClosed != null) {
            onWindowClosed.accept(this);
        }
        suppressCloseNotification = false;
    }

    record DropTarget(DockElement target, DockPosition position, Integer tabIndex, int depth, double area) {
    }

    private record DropZoneResolution(List<DockDropZone> validZones, DockDropZone activeZone) {
        private static DropZoneResolution empty() {
            return new DropZoneResolution(List.of(), null);
        }
    }
}

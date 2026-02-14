package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.DockContainer;
import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import com.github.beowolve.snapfx.model.DockTabPane;
import com.github.beowolve.snapfx.view.DockControlIcons;
import com.github.beowolve.snapfx.view.DockDropZone;
import com.github.beowolve.snapfx.view.DockDropZoneType;
import com.github.beowolve.snapfx.view.DockLayoutEngine;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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

    private Stage stage;
    private Double preferredX;
    private Double preferredY;
    private double preferredWidth = DEFAULT_WIDTH;
    private double preferredHeight = DEFAULT_HEIGHT;
    private boolean suppressCloseNotification;

    private Runnable onAttachRequested;
    private Consumer<DockFloatingWindow> onWindowClosed;

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
        this(dockNode, "SnapFX", null);
    }

    DockFloatingWindow(DockNode dockNode, DockDragService dragService) {
        this(dockNode, "SnapFX", dragService);
    }

    DockFloatingWindow(DockNode dockNode, String titlePrefix) {
        this(dockNode, titlePrefix, null);
    }

    DockFloatingWindow(DockNode dockNode, String titlePrefix, DockDragService dragService) {
        this.id = UUID.randomUUID().toString();
        this.primaryDockNode = Objects.requireNonNull(dockNode, "dockNode");
        this.titlePrefix = (titlePrefix == null || titlePrefix.isBlank()) ? "SnapFX" : titlePrefix;
        this.floatingGraph = new DockGraph();
        this.floatingLayoutEngine = new DockLayoutEngine(floatingGraph, dragService);
        this.layoutContainer = new StackPane();
        this.layoutContainer.getStyleClass().add("dock-floating-layout-container");

        floatingGraph.setRoot(dockNode);
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

    public void toFront() {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
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
        rebuildLayout();
    }

    public void close() {
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

    void undockNode(DockNode node) {
        floatingGraph.undock(node);
    }

    void dockNode(DockNode node, DockElement target, DockPosition position, Integer tabIndex) {
        floatingGraph.dock(node, target, position, tabIndex);
    }

    void moveNode(DockNode node, DockElement target, DockPosition position, Integer tabIndex) {
        floatingGraph.move(node, target, position, tabIndex);
    }

    DropTarget resolveDropTarget(double screenX, double screenY, DockNode draggedNode) {
        if (stage == null || !stage.isShowing() || stage.getScene() == null || draggedNode == null) {
            return null;
        }

        Node sceneRoot = stage.getScene().getRoot();
        Point2D scenePoint = sceneRoot.screenToLocal(screenX, screenY);
        if (scenePoint == null) {
            return null;
        }
        Bounds localBounds = sceneRoot.getBoundsInLocal();
        if (!localBounds.contains(scenePoint)) {
            return null;
        }

        List<DockDropZone> zones = floatingLayoutEngine.collectDropZones();
        if (zones.isEmpty()) {
            return null;
        }
        List<DockDropZone> validZones = filterZonesForDrop(zones, draggedNode);
        DockDropZone bestZone = floatingLayoutEngine.findBestDropZone(
            validZones,
            scenePoint.getX(),
            scenePoint.getY()
        );
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
        root.setCenter(layoutContainer);

        setupResizeHandling(root, window);

        Scene scene = new Scene(root, preferredWidth, preferredHeight);
        if (ownerStage != null && ownerStage.getScene() != null) {
            scene.getStylesheets().addAll(ownerStage.getScene().getStylesheets());
        }
        window.setScene(scene);
        applyWindowPosition(window, ownerStage);

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
            DockControlIcons.createAttachIcon(),
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
            DockControlIcons.createMaximizeIcon(),
            maximizeTooltip.getText(),
            () -> toggleMaximize(window),
            "dock-window-maximize-button"
        );
        maximizeButton.setTooltip(maximizeTooltip);

        Button closeButton = createControlButton(
            DockControlIcons.createCloseIcon(),
            "Close floating window",
            this::close,
            "dock-window-close-button"
        );

        titleBar.getChildren().addAll(iconPane, titleLabel, spacer, attachButton, maximizeButton, closeButton);
        setupTitleBarDrag(titleBar, window);
        return titleBar;
    }

    private Button createControlButton(Node icon, String tooltipText, Runnable action, String styleClass) {
        Button button = new Button();
        button.getStyleClass().addAll("dock-node-close-button", "dock-window-control-button", styleClass);
        button.setGraphic(icon);
        button.setFocusTraversable(false);
        if (tooltipText != null && !tooltipText.isBlank()) {
            button.setTooltip(new Tooltip(tooltipText));
        }
        button.setOnAction(e -> {
            if (action != null) {
                action.run();
            }
            e.consume();
        });
        button.addEventFilter(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);
        return button;
    }

    private void setupTitleBarDrag(HBox titleBar, Stage window) {
        titleBar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!isTitleDragCandidate(event, window)) {
                return;
            }
            dragOffsetX = event.getScreenX() - window.getX();
            dragOffsetY = event.getScreenY() - window.getY();
        });

        titleBar.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!isTitleDragCandidate(event, window) || window.isMaximized()) {
                return;
            }
            window.setX(event.getScreenX() - dragOffsetX);
            window.setY(event.getScreenY() - dragOffsetY);
        });

        titleBar.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (!isTitleDragCandidate(event, window)
                || event.getButton() != MouseButton.PRIMARY
                || event.getClickCount() != 2) {
                return;
            }
            toggleMaximize(window);
        });
    }

    private boolean isTitleDragCandidate(MouseEvent event, Stage window) {
        return !resizing
            && !window.isMaximized()
            && !(event.getTarget() instanceof Button)
            && resolveResizeMask(event.getScreenX(), event.getScreenY(), window) == 0;
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
            maximizeButton.setGraphic(DockControlIcons.createRestoreIcon());
            if (maximizeTooltip != null) {
                maximizeTooltip.setText("Restore window");
            }
        } else {
            maximizeButton.setGraphic(DockControlIcons.createMaximizeIcon());
            if (maximizeTooltip != null) {
                maximizeTooltip.setText("Maximize window");
            }
        }
    }

    private void setupResizeHandling(BorderPane root, Stage window) {
        root.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (window.isMaximized() || resizing) {
                root.setCursor(Cursor.DEFAULT);
                return;
            }
            int mask = resolveResizeMask(event.getScreenX(), event.getScreenY(), window);
            root.setCursor(resolveCursor(mask));
        });

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
        });

        root.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!resizing) {
                return;
            }
            resizing = false;
            activeResizeMask = 0;
            root.setCursor(Cursor.DEFAULT);
        });
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

    private void onWindowHidden(Stage hiddenStage) {
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
}

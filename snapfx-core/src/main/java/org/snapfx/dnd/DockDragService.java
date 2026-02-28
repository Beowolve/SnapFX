package org.snapfx.dnd;

import org.snapfx.model.DockElement;
import org.snapfx.model.DockGraph;
import org.snapfx.model.DockNode;
import org.snapfx.model.DockTabPane;
import org.snapfx.view.DockLayoutEngine;
import org.snapfx.view.DockNodeView;
import org.snapfx.view.DockDropZone;
import org.snapfx.view.DockDropZoneType;
import org.snapfx.model.DockPosition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Central service for drag &amp; drop operations.
 * Manages the drag state and coordinates visual feedback.
 */
@SuppressWarnings("unused")
public class DockDragService {
    private final DockGraph dockGraph;
    private DockDragData currentDrag;
    private DockDropVisualizationMode dropVisualizationMode = DockDropVisualizationMode.DEFAULT;

    // Drag threshold: minimum distance before drag starts
    private static final double DRAG_THRESHOLD = 5.0;
    private static final double GHOST_OFFSET_X = 12.0;
    private static final double GHOST_OFFSET_Y = 12.0;
    private double dragStartX;
    private double dragStartY;
    private boolean dragThresholdExceeded;

    private final ObjectProperty<DockDragData> currentDragProperty = new SimpleObjectProperty<>();

    private DockGhostOverlay ghostOverlay;
    private Stage ghostStage;
    private DockDropIndicator dropIndicator;
    private DockDropZonesOverlay dropZonesOverlay;
    private Stage mainStage;
    private DockLayoutEngine layoutEngine;
    private Consumer<FloatDetachRequest> onFloatDetachRequest;
    private Consumer<DropRequest> onDropRequest;
    private Consumer<DragHoverEvent> onDragHover;
    private Runnable onDragFinished;
    private BiPredicate<Double, Double> suppressMainDropAtScreenPoint;
    private final EventHandler<KeyEvent> dragCancelKeyHandler = this::handleDragCancelKeyPressed;
    private final List<Scene> dragCancelScenes = new ArrayList<>();

    /**
     * Creates a drag service bound to one dock graph.
     *
     * @param dockGraph dock graph updated by drag/drop operations
     */
    public DockDragService(DockGraph dockGraph) {
        this.dockGraph = dockGraph;
    }

    /**
     * Initializes the service with the primary stage.
     *
     * @param stage primary stage hosting the main dock scene
     */
    public void initialize(Stage stage) {
        this.mainStage = stage;
        Scene scene = stage.getScene();
        if (scene != null) {
            Pane targetPane = ensureSceneRootIsPane(scene);
            createAndSetupOverlays(scene, targetPane);
            attachRootChangeListener(scene);
        }
    }

    /**
     * Ensures the scene root is a Pane, wrapping it in a StackPane if necessary.
     */
    private Pane ensureSceneRootIsPane(Scene scene) {
        Node originalRoot = scene.getRoot();
        if (originalRoot instanceof Pane pane) {
            return pane;
        }
        StackPane stack = new StackPane();
        stack.getChildren().add(originalRoot);
        scene.setRoot(stack);
        return stack;
    }

    /**
     * Creates overlay components and adds them to the target pane.
     */
    private void createAndSetupOverlays(Scene scene, Pane targetPane) {
        if (ghostOverlay == null) {
            ghostOverlay = new DockGhostOverlay();
        }
        dropZonesOverlay = new DockDropZonesOverlay();
        dropIndicator = new DockDropIndicator();
        configureOverlayProperties();
        targetPane.getChildren().addAll(dropZonesOverlay, dropIndicator);
        bindOverlaysToSceneSize(scene);
        initializeGhostStage();
    }

    /**
     * Configures overlay properties (mouse transparency, layout management).
     */
    private void configureOverlayProperties() {
        dropZonesOverlay.setMouseTransparent(true);
        dropZonesOverlay.setManaged(false);
        dropIndicator.setMouseTransparent(true);
        dropIndicator.setManaged(false);
    }

    /**
     * Binds overlay dimensions to scene dimensions.
     */
    private void bindOverlaysToSceneSize(Scene scene) {
        dropZonesOverlay.prefWidthProperty().bind(scene.widthProperty());
        dropZonesOverlay.prefHeightProperty().bind(scene.heightProperty());
        dropIndicator.prefWidthProperty().bind(scene.widthProperty());
        dropIndicator.prefHeightProperty().bind(scene.heightProperty());
    }

    /**
     * Attaches a listener to re-attach overlays when the scene root changes.
     */
    private void attachRootChangeListener(Scene scene) {
        scene.rootProperty().addListener((obs, oldRoot, newRoot) ->
            Platform.runLater(() -> reattachOverlaysToNewRoot(scene, newRoot))
        );
    }

    /**
     * Re-attaches overlays to a new scene root.
     */
    private void reattachOverlaysToNewRoot(Scene scene, Node newRoot) {
        if (newRoot == null) return;
        Pane target = ensureNodeIsPane(scene, newRoot);
        addOverlaysToPane(target);
    }

    /**
     * Ensures a node is a Pane, wrapping it if necessary.
     */
    private Pane ensureNodeIsPane(Scene scene, Node node) {
        if (node instanceof Pane p) {
            return p;
        }
        StackPane sp = new StackPane();
        sp.getChildren().add(node);
        scene.setRoot(sp);
        return sp;
    }

    /**
     * Adds overlays to the target pane if they are not already present.
     */
    private void addOverlaysToPane(Pane target) {
        if (dropZonesOverlay != null && !target.getChildren().contains(dropZonesOverlay)) {
            target.getChildren().add(dropZonesOverlay);
        }
        if (dropIndicator != null && !target.getChildren().contains(dropIndicator)) {
            target.getChildren().add(dropIndicator);
        }
    }

    private void initializeGhostStage() {
        if (ghostStage != null || ghostOverlay == null) {
            return;
        }
        Scene ghostScene = new Scene(ghostOverlay);
        ghostScene.setFill(Color.TRANSPARENT);

        ghostStage = new Stage(StageStyle.TRANSPARENT);
        if (mainStage != null) {
            ghostStage.initOwner(mainStage);
        }
        ghostStage.setScene(ghostScene);
        ghostStage.setAlwaysOnTop(true);
        ghostStage.setResizable(false);
    }

    private Point2D toMainScenePoint(double screenX, double screenY) {
        if (mainStage == null || mainStage.getScene() == null) {
            return new Point2D(screenX, screenY);
        }
        Node root = mainStage.getScene().getRoot();
        if (root == null) return new Point2D(screenX, screenY);
        return root.screenToLocal(screenX, screenY);
    }

    /**
     * Prepares for a potential drag operation (called on mouse press).
     * Actual dragging starts only after threshold is exceeded.
     *
     * @param node node that should be dragged
     * @param event source mouse-press event
     */
    public void startDrag(DockNode node, MouseEvent event) {
        // Only allow dragging with left mouse button
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        if (dockGraph.isLocked()) {
            return; // No drag in locked mode
        }

        // Store initial position
        dragStartX = event.getScreenX();
        dragStartY = event.getScreenY();
        dragThresholdExceeded = false;

        // Prepare drag data but don't activate yet
        currentDrag = new DockDragData(node);
        currentDrag.setMouseX(dragStartX);
        currentDrag.setMouseY(dragStartY);
        Scene sourceScene = null;
        if (event.getSource() instanceof Node sourceNode) {
            sourceScene = sourceNode.getScene();
        }
        registerDragCancelKeyHandlers(sourceScene);
    }

    /**
     * Activates the drag operation visually (creates ghost overlay, etc.).
     * Called once threshold is exceeded.
     */
    private void activateDrag() {
        if (currentDrag == null) {
            return;
        }

        currentDragProperty.set(currentDrag);

        // Create snapshot of the dragged element
        if (ghostOverlay != null) {
            WritableImage img = null;
            try {
                if (layoutEngine != null) {
                    DockNodeView nodeView = layoutEngine.getDockNodeView(currentDrag.getDraggedNode());
                    if (nodeView != null) {
                        img = nodeView.snapshot(null, null);
                    }
                }
                if (img == null && currentDrag.getDraggedNode().getContent() != null) {
                    img = currentDrag.getDraggedNode().getContent().snapshot(null, null);
                }
            } catch (Exception ex) {
                // ignore and fall back
            }

            // Fallback: small placeholder image with title if snapshot isn't possible
            if (img == null) {
                img = createPlaceholderImage(currentDrag.getDraggedNode().getTitle());
            }

            ghostOverlay.setImage(img);
            showGhostOverlay();
            updateGhostOverlay(dragStartX, dragStartY);
        }
    }

    /**
     * Updates the drag position (called on mouse drag).
     * Activates drag only after threshold is exceeded.
     *
     * @param event source drag event
     */
    public void updateDrag(MouseEvent event) {
        if (currentDrag == null) return;
        if (!checkDragThreshold(event)) return;

        updateDragPosition(event);
        updateGhostOverlay(event.getScreenX(), event.getScreenY());
        Point2D scenePoint = toMainScenePoint(event.getScreenX(), event.getScreenY());

        if (layoutEngine != null) {
            updateDropTarget(scenePoint.getX(), scenePoint.getY(), event.getScreenX(), event.getScreenY());
        } else {
            clearDropTarget();
        }
    }

    /**
     * Checks if drag threshold is exceeded and activates drag if needed.
     */
    private boolean checkDragThreshold(MouseEvent event) {
        if (dragThresholdExceeded) return true;

        double deltaX = event.getScreenX() - dragStartX;
        double deltaY = event.getScreenY() - dragStartY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance < DRAG_THRESHOLD) return false;

        dragThresholdExceeded = true;
        activateDrag();
        return true;
    }

    /**
     * Updates the current drag position.
     */
    private void updateDragPosition(MouseEvent event) {
        currentDrag.setMouseX(event.getScreenX());
        currentDrag.setMouseY(event.getScreenY());
        if (onDragHover != null) {
            onDragHover.accept(new DragHoverEvent(currentDrag.getDraggedNode(), event.getScreenX(), event.getScreenY()));
        }
    }

    /**
     * Updates the ghost overlay position.
     */
    private void updateGhostOverlay(double screenX, double screenY) {
        if (ghostOverlay != null) {
            ghostOverlay.updatePosition();
        }
        if (ghostStage != null) {
            ghostStage.setX(screenX + GHOST_OFFSET_X);
            ghostStage.setY(screenY + GHOST_OFFSET_Y);
            ghostStage.toFront();
        }
    }

    private void showGhostOverlay() {
        if (ghostOverlay == null || ghostStage == null) {
            return;
        }
        if (!ghostStage.isShowing()) {
            ghostStage.show();
        }
        ghostStage.toFront();
    }

    private void hideGhostOverlay() {
        if (ghostOverlay != null) {
            ghostOverlay.hide();
        }
        if (ghostStage != null && ghostStage.isShowing()) {
            ghostStage.hide();
        }
    }

    /**
     * Updates the drop target and indicator based on mouse position.
     */
    private void updateDropTarget(double sceneX, double sceneY, double screenX, double screenY) {
        if (shouldSuppressMainDropAt(screenX, screenY)) {
            clearDropTarget();
            return;
        }

        List<DockDropZone> zones = layoutEngine.collectDropZones();
        List<DockDropZone> validZones = filterZonesForDrag(zones, currentDrag.getDraggedNode());
        DockDropZone activeZone = layoutEngine.findBestDropZone(validZones, sceneX, sceneY);

        if (activateTabHoverIfNeeded(activeZone)) {
            zones = layoutEngine.collectDropZones();
            validZones = filterZonesForDrag(zones, currentDrag.getDraggedNode());
            activeZone = layoutEngine.findBestDropZone(validZones, sceneX, sceneY);
        }

        if (dropZonesOverlay != null) {
            List<DockDropZone> zonesToShow = getZonesForVisualization(validZones, activeZone);
            if (zonesToShow.isEmpty()) {
                dropZonesOverlay.hide();
            } else {
                dropZonesOverlay.showZones(zonesToShow);
            }
        }

        if (activeZone == null) {
            clearDropTarget(false);
            return;
        }

        setDropTarget(activeZone.getTarget(), activeZone.getPosition(), activeZone.getTabIndex());
        if (dropVisualizationMode == DockDropVisualizationMode.OFF) {
            if (dropIndicator != null) {
                dropIndicator.hide();
            }
            return;
        }
        showDropIndicator(activeZone);
    }

    /**
     * Sets the drop target and position in the current drag data.
     */
    private void setDropTarget(DockElement target, DockPosition position) {
        setDropTarget(target, position, null);
    }

    private void setDropTarget(DockElement target, DockPosition position, Integer tabIndex) {
        currentDrag.setDropTarget(target);
        currentDrag.setDropPosition(position);
        currentDrag.setDropTabIndex(tabIndex);
        currentDragProperty.set(currentDrag);
    }

    /**
     * Shows the drop indicator at the specified position.
     */
    private void showDropIndicator(DockDropZone zone) {
        if (dropIndicator == null) return;

        Bounds bounds = zone.getBounds();
        dropIndicator.show(bounds, zone.getInsertLineX());

        if (ghostStage != null && ghostStage.isShowing()) {
            ghostStage.toFront();
        }
    }

    /**
     * Clears the current drop target.
     */
    private void clearDropTarget() {
        clearDropTarget(true);
    }

    private void clearDropTarget(boolean hideZones) {
        if (dropIndicator != null) {
            dropIndicator.hide();
        }
        if (hideZones && dropZonesOverlay != null) {
            dropZonesOverlay.hide();
        }
        currentDrag.setDropTarget(null);
        currentDrag.setDropPosition(null);
        currentDrag.setDropTabIndex(null);
        currentDragProperty.set(currentDrag);
    }

    /**
     * Ends the drag operation and performs the dock operation.
     *
     * @param event source mouse-release event, may be {@code null} for programmatic endings
     */
    public void endDrag(MouseEvent event) {
        if (currentDrag == null) {
            return;
        }

        if (event != null) {
            event.consume();
        }

        // If threshold was never exceeded, this was just a click - cancel drag
        if (!dragThresholdExceeded) {
            unregisterDragCancelKeyHandlers();
            currentDrag = null;
            if (onDragFinished != null) {
                onDragFinished.run();
            }
            return;
        }

        // Hide overlays
        hideGhostOverlay();
        if (dropIndicator != null) {
            dropIndicator.hide();
        }
        if (dropZonesOverlay != null) {
            dropZonesOverlay.hide();
        }

        DockNode dragged = currentDrag.getDraggedNode();
        DockElement target = currentDrag.getDropTarget();
        DockPosition pos = currentDrag.getDropPosition();
        Integer tabIndex = currentDrag.getDropTabIndex();

        // Perform dock operation only when we have a valid target.
        // This avoids destroying the layout when the hover target becomes null during release.
        if (dragged != null && target != null && pos != null) {
            if (onDropRequest != null) {
                onDropRequest.accept(new DropRequest(dragged, target, pos, tabIndex));
            } else {
                dockGraph.move(dragged, target, pos, tabIndex);
            }
        } else if (dragged != null && event != null) {
            requestFloatDetach(dragged, event.getScreenX(), event.getScreenY());
        }

        unregisterDragCancelKeyHandlers();
        currentDrag = null;
        dragThresholdExceeded = false;
        currentDragProperty.set(null);
        if (onDragFinished != null) {
            onDragFinished.run();
        }
    }

    /**
     * Cancels the drag operation.
     */
    @SuppressWarnings("unused")
    public void cancelDrag() {
        if (currentDrag == null) {
            return;
        }

        // Hide overlays
        hideGhostOverlay();
        if (dropIndicator != null) {
            dropIndicator.hide();
        }
        if (dropZonesOverlay != null) {
            dropZonesOverlay.hide();
        }

        unregisterDragCancelKeyHandlers();
        currentDrag = null;
        dragThresholdExceeded = false;
        currentDragProperty.set(null);
        if (onDragFinished != null) {
            onDragFinished.run();
        }
    }

    /**
     * Returns whether a drag operation is currently active.
     *
     * @return {@code true} when drag data is currently tracked
     */
    public boolean isDragging() {
        return currentDrag != null;
    }

    /**
     * Returns the current drag data snapshot.
     *
     * @return current drag data, or {@code null} when not dragging
     */
    @SuppressWarnings("unused")
    public DockDragData getCurrentDrag() {
        return currentDrag;
    }

    /**
     * Sets the layout engine used for hit-testing and drop-zone collection.
     *
     * @param layoutEngine layout engine bound to the active dock scene
     */
    public void setLayoutEngine(DockLayoutEngine layoutEngine) {
        this.layoutEngine = layoutEngine;
    }

    /**
     * Sets the callback for unresolved drop requests that should detach to floating windows.
     *
     * @param onFloatDetachRequest detach callback, or {@code null}
     */
    public void setOnFloatDetachRequest(Consumer<FloatDetachRequest> onFloatDetachRequest) {
        this.onFloatDetachRequest = onFloatDetachRequest;
    }

    /**
     * Sets the callback for resolved drop requests.
     *
     * @param onDropRequest drop callback, or {@code null}
     */
    public void setOnDropRequest(Consumer<DropRequest> onDropRequest) {
        this.onDropRequest = onDropRequest;
    }

    /**
     * Sets the callback invoked while pointer hover updates during drag.
     *
     * @param onDragHover hover callback, or {@code null}
     */
    public void setOnDragHover(Consumer<DragHoverEvent> onDragHover) {
        this.onDragHover = onDragHover;
    }

    /**
     * Sets a callback invoked after drag completion or cancellation.
     *
     * @param onDragFinished completion callback, or {@code null}
     */
    public void setOnDragFinished(Runnable onDragFinished) {
        this.onDragFinished = onDragFinished;
    }

    /**
     * Sets an optional predicate that can suppress main-scene drop handling for given screen coordinates.
     *
     * @param suppressMainDropAtScreenPoint suppression predicate, or {@code null}
     */
    public void setSuppressMainDropAtScreenPoint(BiPredicate<Double, Double> suppressMainDropAtScreenPoint) {
        this.suppressMainDropAtScreenPoint = suppressMainDropAtScreenPoint;
    }

    /**
     * Returns the observable drag-data property.
     *
     * @return property that mirrors current drag lifecycle
     */
    public ObjectProperty<DockDragData> currentDragProperty() {
        return currentDragProperty;
    }

    // Fallback placeholder image generator
    private WritableImage createPlaceholderImage(String title) {
        int w = 220;
        int h = 36;
        Canvas canvas = new Canvas(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#f5f5f5"));
        gc.fillRoundRect(0, 0, w, h, 6, 6);
        gc.setStroke(Color.web("#bdbdbd"));
        gc.strokeRoundRect(0.5, 0.5, w - 1.0, h - 1.0, 6, 6);
        gc.setFill(Color.web("#222"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        double textX = 12;
        double textY = h / 2.0 + 4;
        gc.fillText(title != null ? title : "Untitled", textX, textY);
        WritableImage img = new WritableImage(w, h);
        canvas.snapshot(null, img);
        return img;
    }

    /**
     * Ghost overlay that shows a semi-transparent image of the dragged element.
     */
    private static class DockGhostOverlay extends StackPane {
        private final ImageView ghostView;

        /**
         * Creates a lightweight image host for the dragged node snapshot.
         */
        public DockGhostOverlay() {
            setMouseTransparent(true);
            setPickOnBounds(false);
            setVisible(false);

            ghostView = new ImageView();
            ghostView.setOpacity(0.85);
            ghostView.setSmooth(true);
            ghostView.setPreserveRatio(true);

            getChildren().add(ghostView);
        }

        /**
         * Updates the rendered ghost image.
         *
         * @param img drag snapshot image, or {@code null} to hide
         */
        public void setImage(Image img) {
            if (img != null) {
                ghostView.setImage(img);
                ghostView.setFitWidth(Math.min(300, img.getWidth()));
                ghostView.setFitHeight(Math.min(200, img.getHeight()));
                updatePosition();
                setVisible(true);
            } else {
                hide();
            }
        }

        /**
         * Recomputes preferred bounds from the active image.
         */
        public void updatePosition() {
            double w = ghostView.getBoundsInLocal().getWidth();
            double h = ghostView.getBoundsInLocal().getHeight();
            if (w <= 0 || h <= 0) {
                Image img = ghostView.getImage();
                if (img != null) {
                    double fw = ghostView.getFitWidth() > 0 ? ghostView.getFitWidth() : img.getWidth();
                    double fh = ghostView.getFitHeight() > 0 ? ghostView.getFitHeight() : img.getHeight();
                    w = fw;
                    h = fh;
                } else {
                    w = 100;
                    h = 30;
                }
            }
            setMinSize(w, h);
            setPrefSize(w, h);
            setMaxSize(w, h);
            requestLayout();
        }

        /**
         * Hides the ghost overlay and clears its image.
         */
        public void hide() {
            setVisible(false);
            ghostView.setImage(null);
        }
    }

    /**
     * Drop indicator that visualizes the drop zones.
     */
    private static class DockDropIndicator extends Pane {
        private final Rectangle indicator;
        private final Line insertLine;

        /**
         * Creates an overlay indicator for the currently active drop zone.
         */
        public DockDropIndicator() {
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

        /**
         * Shows the indicator for one drop-zone bounds and optional tab insert line.
         *
         * @param bounds target bounds in scene coordinates
         * @param insertLineX optional tab insert x-position in scene coordinates
         */
        public void show(Bounds bounds, Double insertLineX) {
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

        /**
         * Hides all drop indicator visuals.
         */
        public void hide() {
            setVisible(false);
            insertLine.setVisible(false);
        }
    }

    /**
     * Overlay that renders all available drop zones.
     */
    private static class DockDropZonesOverlay extends Pane {
        private final List<Rectangle> rectangles = new java.util.ArrayList<>();

        /**
         * Creates an overlay layer that paints all candidate drop zones.
         */
        public DockDropZonesOverlay() {
            setMouseTransparent(true);
            setVisible(false);
        }

        /**
         * Renders the provided drop-zone set.
         *
         * @param zones drop zones to render
         */
        public void showZones(List<DockDropZone> zones) {
            getChildren().clear();
            rectangles.clear();

            for (DockDropZone zone : zones) {
                Bounds b = zone.getBounds();
                if (b == null || b.getWidth() <= 0 || b.getHeight() <= 0) {
                    continue;
                }
                Point2D topLeft = sceneToLocal(b.getMinX(), b.getMinY());
                Point2D bottomRight = sceneToLocal(b.getMaxX(), b.getMaxY());

                double x = topLeft.getX();
                double y = topLeft.getY();
                double w = Math.max(1, bottomRight.getX() - topLeft.getX());
                double h = Math.max(1, bottomRight.getY() - topLeft.getY());

                Rectangle rect = new Rectangle(x, y, w, h);
                rect.setFill(Color.web("#3a7bd5", 0.10));
                rect.setStroke(Color.web("#3a7bd5", 0.25));
                rect.setStrokeWidth(1);
                rectangles.add(rect);
            }

            getChildren().addAll(rectangles);
            setVisible(true);
        }

        /**
         * Clears and hides all rendered drop zones.
         */
        public void hide() {
            setVisible(false);
            getChildren().clear();
            rectangles.clear();
        }
    }

    private List<DockDropZone> filterZonesForDrag(List<DockDropZone> zones, DockNode draggedNode) {
        List<DockDropZone> result = new ArrayList<>();
        for (DockDropZone zone : zones) {
            if (isZoneValidForDrag(zone, draggedNode)) {
                result.add(zone);
            }
        }
        return result;
    }

    private List<DockDropZone> getZonesForVisualization(List<DockDropZone> validZones, DockDropZone activeZone) {
        if (dropVisualizationMode == DockDropVisualizationMode.OFF) {
            return java.util.List.of();
        }
        if (dropVisualizationMode == DockDropVisualizationMode.ACTIVE_ONLY) {
            return java.util.List.of();
        }
        if (dropVisualizationMode == DockDropVisualizationMode.ALL_ZONES) {
            return validZones;
        }
        if (activeZone == null) {
            return java.util.List.of();
        }
        if (dropVisualizationMode == DockDropVisualizationMode.SUBTREE) {
            return filterZonesBySubtree(validZones, activeZone.getTarget());
        }
        if (dropVisualizationMode == DockDropVisualizationMode.DEFAULT) {
            return filterZonesByTarget(validZones, activeZone.getTarget());
        }
        return java.util.List.of();
    }

    private List<DockDropZone> filterZonesBySubtree(List<DockDropZone> zones, DockElement subtreeRoot) {
        List<DockDropZone> result = new java.util.ArrayList<>();
        for (DockDropZone zone : zones) {
            if (zone.getTarget() != null && isDescendantOf(zone.getTarget(), subtreeRoot)) {
                result.add(zone);
            }
        }
        return result;
    }

    private List<DockDropZone> filterZonesByTarget(List<DockDropZone> zones, DockElement target) {
        List<DockDropZone> result = new java.util.ArrayList<>();
        for (DockDropZone zone : zones) {
            if (zone.getTarget() == target) {
                result.add(zone);
            }
        }
        return result;
    }

    boolean isZoneValidForDrag(DockDropZone zone, DockNode draggedNode) {
        if (zone == null || draggedNode == null) {
            return false;
        }
        DockElement target = zone.getTarget();
        if (target == null) {
            return false;
        }
        if (!isElementVisibleForInteraction(target)) {
            return false;
        }
        if (target == draggedNode) {
            return false;
        }
        return !isDescendantOf(target, draggedNode);
    }

    boolean activateTabHoverIfNeeded(DockDropZone activeZone) {
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

    boolean isElementVisibleForInteraction(DockElement element) {
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

    boolean shouldSuppressMainDropAt(double screenX, double screenY) {
        return suppressMainDropAtScreenPoint != null
            && suppressMainDropAtScreenPoint.test(screenX, screenY);
    }

    /**
     * Returns the current drop-zone visualization mode.
     *
     * @return active visualization mode
     */
    public DockDropVisualizationMode getDropVisualizationMode() {
        return dropVisualizationMode;
    }

    /**
     * Sets the drop-zone visualization mode.
     *
     * @param mode visualization mode, ignored when {@code null}
     */
    public void setDropVisualizationMode(DockDropVisualizationMode mode) {
        if (mode != null) {
            this.dropVisualizationMode = mode;
        }
    }

    boolean requestFloatDetach(DockNode draggedNode, double screenX, double screenY) {
        if (draggedNode == null || onFloatDetachRequest == null) {
            return false;
        }
        onFloatDetachRequest.accept(new FloatDetachRequest(draggedNode, screenX, screenY));
        return true;
    }

    /**
     * Immutable callback payload used for float-detach requests.
     *
     * @param draggedNode dragged node to detach
     * @param screenX pointer x-coordinate in screen space
     * @param screenY pointer y-coordinate in screen space
     */
    public record FloatDetachRequest(DockNode draggedNode, double screenX, double screenY) {
    }

    /**
     * Immutable callback payload used for accepted dock-drop requests.
     *
     * @param draggedNode dragged node to move
     * @param target drop target element
     * @param position drop position on target
     * @param tabIndex target tab index for tab insert operations, or {@code null}
     */
    public record DropRequest(DockNode draggedNode, DockElement target, DockPosition position, Integer tabIndex) {
    }

    /**
     * Immutable callback payload emitted while drag hover coordinates update.
     *
     * @param draggedNode currently dragged node
     * @param screenX pointer x-coordinate in screen space
     * @param screenY pointer y-coordinate in screen space
     */
    public record DragHoverEvent(DockNode draggedNode, double screenX, double screenY) {
    }

    private void registerDragCancelKeyHandlers(Scene sourceScene) {
        unregisterDragCancelKeyHandlers();
        addDragCancelKeyHandler(sourceScene);
        if (mainStage != null && mainStage.getScene() != null) {
            addDragCancelKeyHandler(mainStage.getScene());
        }
        if (ghostStage != null && ghostStage.getScene() != null) {
            addDragCancelKeyHandler(ghostStage.getScene());
        }
    }

    private void addDragCancelKeyHandler(Scene scene) {
        if (scene == null || dragCancelScenes.contains(scene)) {
            return;
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, dragCancelKeyHandler);
        dragCancelScenes.add(scene);
    }

    private void unregisterDragCancelKeyHandlers() {
        for (Scene scene : new ArrayList<>(dragCancelScenes)) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, dragCancelKeyHandler);
        }
        dragCancelScenes.clear();
    }

    private void handleDragCancelKeyPressed(KeyEvent event) {
        if (event == null || event.getCode() != KeyCode.ESCAPE || currentDrag == null) {
            return;
        }
        cancelDrag();
        event.consume();
    }
}

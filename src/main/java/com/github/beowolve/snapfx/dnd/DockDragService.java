package com.github.beowolve.snapfx.dnd;

import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.view.DockLayoutEngine;
import com.github.beowolve.snapfx.view.DockNodeView;
import com.github.beowolve.snapfx.model.DockPosition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Central service for drag & drop operations.
 * Manages the drag state and coordinates visual feedback.
 */
@SuppressWarnings("unused")
public class DockDragService {
    private final DockGraph dockGraph;
    private DockDragData currentDrag;

    // Drag threshold: minimum distance before drag starts
    private static final double DRAG_THRESHOLD = 5.0;
    private double dragStartX;
    private double dragStartY;
    private boolean dragThresholdExceeded;

    private final ObjectProperty<DockDragData> currentDragProperty = new SimpleObjectProperty<>();

    private DockGhostOverlay ghostOverlay;
    private DockDropIndicator dropIndicator;
    private Stage mainStage;
    private DockLayoutEngine layoutEngine;

    public DockDragService(DockGraph dockGraph) {
        this.dockGraph = dockGraph;
    }

    /**
     * Initializes the service with the primary stage.
     */
    public void initialize(Stage stage) {
        this.mainStage = stage;
        Scene scene = stage.getScene();

        if (scene != null) {
            // Create overlay layers; if the root is not a Pane, wrap it in a StackPane
            Node originalRoot = scene.getRoot();
            javafx.scene.layout.Pane targetPane;
            if (originalRoot instanceof javafx.scene.layout.Pane pane) {
                targetPane = pane;
            } else {
                // Wrap existing root in a StackPane so we can place overlays on top
                javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane();
                // Removing references from the original parent isn't necessary here
                stack.getChildren().add(originalRoot);
                scene.setRoot(stack);
                targetPane = stack;
            }

            ghostOverlay = new DockGhostOverlay();
            dropIndicator = new DockDropIndicator();

            // Overlays shouldn't affect layout
            ghostOverlay.setMouseTransparent(true);
            ghostOverlay.setManaged(false);
            dropIndicator.setMouseTransparent(true);
            dropIndicator.setManaged(false);

            // Add overlays to the root (StackPane). Added last => top-most layer.
            targetPane.getChildren().addAll(ghostOverlay, dropIndicator);

            // Bind overlays to the scene size so they cover the entire window
            ghostOverlay.prefWidthProperty().bind(scene.widthProperty());
            ghostOverlay.prefHeightProperty().bind(scene.heightProperty());
            dropIndicator.prefWidthProperty().bind(scene.widthProperty());
            dropIndicator.prefHeightProperty().bind(scene.heightProperty());

            // If the scene root gets replaced later, re-attach the overlays
            final Scene finalScene = scene;
            finalScene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
                // Reattach overlays on the new root
                javafx.application.Platform.runLater(() -> {
                    if (newRoot == null) {
                        return;
                    }
                    javafx.scene.layout.Pane target;
                    if (newRoot instanceof javafx.scene.layout.Pane p) {
                        target = p;
                    } else {
                        javafx.scene.layout.StackPane sp = new javafx.scene.layout.StackPane();
                        sp.getChildren().add(newRoot);
                        finalScene.setRoot(sp);
                        target = sp;
                    }
                    if (ghostOverlay != null && dropIndicator != null) {
                        if (!target.getChildren().contains(ghostOverlay)) {
                            target.getChildren().add(ghostOverlay);
                        }
                        if (!target.getChildren().contains(dropIndicator)) {
                            target.getChildren().add(dropIndicator);
                        }
                    }
                });
            });
        }
    }

    private javafx.geometry.Point2D toMainScenePoint(double screenX, double screenY) {
        if (mainStage == null || mainStage.getScene() == null) {
            return new javafx.geometry.Point2D(screenX, screenY);
        }
        Node root = mainStage.getScene().getRoot();
        if (root == null) return new javafx.geometry.Point2D(screenX, screenY);
        return root.screenToLocal(screenX, screenY);
    }

    /**
     * Prepares for a potential drag operation (called on mouse press).
     * Actual dragging starts only after threshold is exceeded.
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
            javafx.scene.image.WritableImage img = null;
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
            // Convert to main scene coordinates
            javafx.geometry.Point2D sceneP = toMainScenePoint(dragStartX, dragStartY);
            ghostOverlay.show(sceneP.getX(), sceneP.getY());
        }
    }

    /**
     * Updates the drag position (called on mouse drag).
     * Activates drag only after threshold is exceeded.
     */
    public void updateDrag(MouseEvent event) {
        if (currentDrag == null) {
            return;
        }

        // Check if threshold is exceeded
        if (!dragThresholdExceeded) {
            double deltaX = event.getScreenX() - dragStartX;
            double deltaY = event.getScreenY() - dragStartY;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (distance < DRAG_THRESHOLD) {
                return; // Threshold not yet exceeded, don't start dragging
            }

            // Threshold exceeded - activate drag now
            dragThresholdExceeded = true;
            activateDrag();
        }

        currentDrag.setMouseX(event.getScreenX());
        currentDrag.setMouseY(event.getScreenY());

        // convert to main scene coords
        javafx.geometry.Point2D sceneP = toMainScenePoint(event.getScreenX(), event.getScreenY());
        double sceneX = sceneP.getX();
        double sceneY = sceneP.getY();

        // Update ghost position
        if (ghostOverlay != null) {
            ghostOverlay.updatePosition(sceneX, sceneY);
        }

        // Hit-testing: find the DockElement under the mouse via the layout engine
        if (layoutEngine != null) {
            DockElement targetElement = layoutEngine.findElementAt(sceneX, sceneY);
            if (targetElement != null) {
                Node targetViewNode = layoutEngine.getViewForElement(targetElement);
                if (targetViewNode != null && targetViewNode.getScene() != null) {
                    javafx.geometry.Bounds b = targetViewNode.localToScene(targetViewNode.getBoundsInLocal());

                    // Calculate zone thresholds
                    // Use 30% for edge zones (larger than before for easier targeting)
                    // But also consider minimum pixel sizes for small elements
                    double zoneRatio = 0.30;
                    double minZonePixels = 40; // Minimum zone size in pixels

                    double horizontalZone = Math.max(minZonePixels, b.getWidth() * zoneRatio);
                    double verticalZone = Math.max(minZonePixels, b.getHeight() * zoneRatio);

                    // Limit zone size so center zone doesn't disappear on small elements
                    horizontalZone = Math.min(horizontalZone, b.getWidth() * 0.4);
                    verticalZone = Math.min(verticalZone, b.getHeight() * 0.4);

                    double leftZone = b.getMinX() + horizontalZone;
                    double rightZone = b.getMaxX() - horizontalZone;
                    double topZone = b.getMinY() + verticalZone;
                    double bottomZone = b.getMaxY() - verticalZone;

                    // Determine position based on mouse location
                    // Prioritize horizontal/vertical based on which dimension the mouse is more extreme in
                    double horizontalDistance = Math.min(sceneX - b.getMinX(), b.getMaxX() - sceneX);
                    double verticalDistance = Math.min(sceneY - b.getMinY(), b.getMaxY() - sceneY);

                    // Normalize distances by element size
                    double normalizedHDist = horizontalDistance / b.getWidth();
                    double normalizedVDist = verticalDistance / b.getHeight();

                    DockPosition pos;

                    // If mouse is in outer zones, determine which edge
                    if (normalizedHDist < zoneRatio || normalizedVDist < zoneRatio) {
                        // Mouse is near an edge - determine which edge is closest
                        if (normalizedHDist < normalizedVDist) {
                            // Horizontal edge is closer
                            pos = (sceneX < (b.getMinX() + b.getMaxX()) / 2) ? DockPosition.LEFT : DockPosition.RIGHT;
                        } else {
                            // Vertical edge is closer
                            pos = (sceneY < (b.getMinY() + b.getMaxY()) / 2) ? DockPosition.TOP : DockPosition.BOTTOM;
                        }
                    } else {
                        // Mouse is in center zone
                        pos = DockPosition.CENTER;
                    }

                    currentDrag.setDropTarget(targetElement);
                    currentDrag.setDropPosition(pos);
                    currentDragProperty.set(currentDrag);

                    // Show indicator
                    if (dropIndicator != null) {
                        javafx.geometry.Point2D topLeft = dropIndicator.sceneToLocal(b.getMinX(), b.getMinY());
                        javafx.geometry.Point2D bottomRight = dropIndicator.sceneToLocal(b.getMaxX(), b.getMaxY());
                        double lx = topLeft.getX();
                        double ly = topLeft.getY();
                        double lw = Math.max(1, bottomRight.getX() - topLeft.getX());
                        double lh = Math.max(1, bottomRight.getY() - topLeft.getY());

                        // Calculate zone sizes for visualization (reuse horizontalZone/verticalZone from above)
                        double horizontalZoneSize = Math.max(minZonePixels, lw * zoneRatio);
                        double verticalZoneSize = Math.max(minZonePixels, lh * zoneRatio);
                        horizontalZoneSize = Math.min(horizontalZoneSize, lw * 0.4);
                        verticalZoneSize = Math.min(verticalZoneSize, lh * 0.4);

                        switch (pos) {
                            case LEFT -> dropIndicator.show(lx, ly, horizontalZoneSize, lh);
                            case RIGHT -> dropIndicator.show(lx + lw - horizontalZoneSize, ly, horizontalZoneSize, lh);
                            case TOP -> dropIndicator.show(lx, ly, lw, verticalZoneSize);
                            case BOTTOM -> dropIndicator.show(lx, ly + lh - verticalZoneSize, lw, verticalZoneSize);
                            case CENTER -> dropIndicator.show(lx, ly, lw, lh);
                        }
                        if (ghostOverlay != null) {
                            ghostOverlay.toFront();
                        }

                    }
                    return;
                }
            }
        }

        // No target
        if (dropIndicator != null) dropIndicator.hide();
        currentDrag.setDropTarget(null);
        currentDrag.setDropPosition(null);
        currentDragProperty.set(currentDrag);
    }

    /**
     * Ends the drag operation and performs the dock operation.
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
            currentDrag = null;
            return;
        }

        // Hide overlays
        if (ghostOverlay != null) {
            ghostOverlay.hide();
        }
        if (dropIndicator != null) {
            dropIndicator.hide();
        }

        DockNode dragged = currentDrag.getDraggedNode();
        DockElement target = currentDrag.getDropTarget();
        DockPosition pos = currentDrag.getDropPosition();

        // Perform dock operation only when we have a valid target.
        // This avoids destroying the layout when the hover target becomes null during release.
        if (dragged != null && target != null && pos != null) {
            dockGraph.move(dragged, target, pos);
        }

        currentDrag = null;
        dragThresholdExceeded = false;
        currentDragProperty.set(null);
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
        if (ghostOverlay != null) {
            ghostOverlay.hide();
        }
        if (dropIndicator != null) {
            dropIndicator.hide();
        }

        currentDrag = null;
        dragThresholdExceeded = false;
        currentDragProperty.set(null);
    }

    public boolean isDragging() {
        return currentDrag != null;
    }

    @SuppressWarnings("unused")
    public DockDragData getCurrentDrag() {
        return currentDrag;
    }

    public void setLayoutEngine(DockLayoutEngine layoutEngine) {
        this.layoutEngine = layoutEngine;
    }

    public ObjectProperty<DockDragData> currentDragProperty() {
        return currentDragProperty;
    }

    /**
     * Ghost overlay that shows a semi-transparent image of the dragged element.
     */
    private static class DockGhostOverlay extends Pane {
        private final javafx.scene.image.ImageView ghostView;

        public DockGhostOverlay() {
            setMouseTransparent(true);
            setVisible(false);

            ghostView = new javafx.scene.image.ImageView();
            ghostView.setOpacity(0.85);
            ghostView.setSmooth(true);
            ghostView.setPreserveRatio(true);

            getChildren().add(ghostView);
        }

        public void setImage(javafx.scene.image.Image img) {
            if (img != null) {
                ghostView.setImage(img);
                ghostView.setFitWidth(Math.min(300, img.getWidth()));
                ghostView.setFitHeight(Math.min(200, img.getHeight()));
                // Force CSS/layout so bounds are available immediately
                ghostView.applyCss();
                DockGhostOverlay.this.applyCss();
                DockGhostOverlay.this.requestLayout();
            } else {
                ghostView.setImage(null);
            }
        }

        public void show(double x, double y) {
            setVisible(true);
            toFront();
            // ensure it's front-most
            javafx.application.Platform.runLater(() -> {
                toFront();
                updatePosition(x, y);
            });
        }

        public void updatePosition(double x, double y) {
            // convert scene coords to overlay-local coords
            javafx.geometry.Point2D local = sceneToLocal(x, y);
            double w = ghostView.getBoundsInLocal().getWidth();
            double h = ghostView.getBoundsInLocal().getHeight();
            if (w <= 0 || h <= 0) {
                // fallback to image dimensions / fit sizes
                javafx.scene.image.Image img = ghostView.getImage();
                if (img != null) {
                    double fw = ghostView.getFitWidth() > 0 ? ghostView.getFitWidth() : img.getWidth();
                    double fh = ghostView.getFitHeight() > 0 ? ghostView.getFitHeight() : img.getHeight();
                    w = fw;
                    h = fh;
                } else {
                    w = 100; h = 30;
                }
            }
            ghostView.setLayoutX(Math.max(0, local.getX() - w / 2));
            ghostView.setLayoutY(Math.max(0, local.getY() - h / 2));
            toFront();
        }

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

        public DockDropIndicator() {
            setMouseTransparent(true);
            setVisible(false);

            indicator = new Rectangle();
            indicator.setFill(Color.DODGERBLUE);
            indicator.setOpacity(0.3);
            indicator.setStroke(Color.BLUE);
            indicator.setStrokeWidth(2);

            getChildren().add(indicator);
        }

        public void show(double x, double y, double width, double height) {
            indicator.setX(x);
            indicator.setY(y);
            indicator.setWidth(width);
            indicator.setHeight(height);
            setVisible(true);
            toFront();
        }

        public void hide() {
            setVisible(false);
        }
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
        gc.strokeRoundRect(0.5, 0.5, w - 1, h - 1, 6, 6);

        gc.setFill(Color.web("#222"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        double textX = 12;
        double textY = h / 2.0 + 4;
        gc.fillText(title != null ? title : "Untitled", textX, textY);

        WritableImage img = new WritableImage(w, h);
        canvas.snapshot(null, img);
        return img;
    }
}

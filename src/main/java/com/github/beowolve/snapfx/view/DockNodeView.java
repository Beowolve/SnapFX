package com.github.beowolve.snapfx.view;

import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockNode;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Visual representation of a DockNode.
 * Renders a header with title and control buttons, plus the content.
 */
public class DockNodeView extends VBox {
    private final DockNode dockNode;
    private final DockGraph dockGraph;  // NOSONAR - needed for button actions, but not exposed publicly
    private final DockDragService dragService;
    private final HBox header;
    private final StackPane iconPane;
    private final Button floatButton;
    private final Button closeButton;
    private final StackPane contentPane;
    private final Label titleLabel;
    private final ChangeListener<Image> iconListener;
    private final ChangeListener<Node> contentListener;
    private ContextMenu headerContextMenu;

    public DockNodeView(DockNode dockNode, DockGraph dockGraph, DockDragService dragService) {
        this.dockNode = dockNode;
        this.dockGraph = dockGraph;
        this.dragService = dragService;

        getStyleClass().add("dock-node-view");

        header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dock-node-header");

        iconPane = new StackPane();
        iconPane.setPrefSize(16, 16);
        iconPane.setMaxSize(16, 16);
        iconPane.setMinSize(16, 16);

        iconListener = (obs, oldIcon, newIcon) -> {
            iconPane.getChildren().clear();
            if (newIcon != null) {
                iconPane.getChildren().add(createDockNodeIcon(newIcon));
            }
        };
        dockNode.iconProperty().addListener(iconListener);

        if (dockNode.getIcon() != null) {
            iconPane.getChildren().add(createDockNodeIcon(dockNode.getIcon()));
        }

        iconPane.visibleProperty().bind(dockNode.iconProperty().isNotNull());
        iconPane.managedProperty().bind(iconPane.visibleProperty());

        titleLabel = new Label();
        titleLabel.getStyleClass().add("dock-node-title-label");
        titleLabel.textProperty().bind(dockNode.titleProperty());

        floatButton = new Button();
        floatButton.getStyleClass().addAll("dock-node-close-button", "dock-node-float-button");
        floatButton.setGraphic(createControlIcon("dock-control-icon-float"));
        floatButton.setTooltip(new Tooltip("Float window"));
        floatButton.setFocusTraversable(false);
        floatButton.setOnAction(e -> { });
        floatButton.visibleProperty().bind(dockGraph.lockedProperty().not());
        floatButton.managedProperty().bind(floatButton.visibleProperty());

        closeButton = new Button();
        closeButton.getStyleClass().add("dock-node-close-button");
        closeButton.setGraphic(createControlIcon("dock-control-icon-close"));
        closeButton.setTooltip(new Tooltip("Close panel"));
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(e -> dockGraph.undock(dockNode));
        closeButton.visibleProperty().bind(
            dockNode.closeableProperty()
                .and(dockGraph.lockedProperty().not())
        );
        closeButton.managedProperty().bind(closeButton.visibleProperty());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(iconPane, titleLabel, spacer, floatButton, closeButton);

        contentPane = new StackPane();
        contentPane.getStyleClass().add("dock-node-content");
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        if (dockNode.getContent() != null) {
            contentPane.getChildren().add(dockNode.getContent());
        }

        contentListener = (obs, oldContent, newContent) -> {
            contentPane.getChildren().clear();
            if (newContent != null) {
                contentPane.getChildren().add(newContent);
            }
        };
        dockNode.contentProperty().addListener(contentListener);

        header.setOnMousePressed(this::onHeaderPressed);
        header.setOnMouseDragged(this::onHeaderDragged);
        header.setOnMouseReleased(this::onHeaderReleased);

        getChildren().addAll(header, contentPane);
    }

    private void onHeaderPressed(MouseEvent event) {
        hideHeaderContextMenu();
        if (isInteractiveControlTarget(event.getTarget())) {
            return;
        }
        if (dragService != null) {
            dragService.startDrag(dockNode, event);
        }
    }

    private void onHeaderDragged(MouseEvent event) {
        if (isInteractiveControlTarget(event.getTarget())) {
            return;
        }
        if (dragService != null && dragService.isDragging()) {
            dragService.updateDrag(event);
        }
    }

    private void onHeaderReleased(MouseEvent event) {
        if (isInteractiveControlTarget(event.getTarget())) {
            return;
        }
        if (dragService != null && dragService.isDragging()) {
            dragService.endDrag(event);
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

    private Region createControlIcon(String styleClass) {
        Region icon = new Region();
        icon.getStyleClass().addAll("dock-control-icon", styleClass);
        icon.setMouseTransparent(true);
        return icon;
    }

    private ImageView createDockNodeIcon(Image image) {
        ImageView icon = new ImageView(image);
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        icon.setCache(true);
        icon.setMouseTransparent(true);
        return icon;
    }

    public void setOnCloseRequest(Runnable handler) {
        closeButton.setOnAction(e -> {
            if (handler != null) {
                handler.run();
            }
        });
    }

    public void setOnFloatRequest(Runnable handler) {
        floatButton.setOnAction(e -> {
            if (handler != null) {
                handler.run();
            }
        });
    }

    public void bindCloseButtonVisible(BooleanExpression expression) {
        if (expression == null) {
            return;
        }
        closeButton.visibleProperty().unbind();
        closeButton.visibleProperty().bind(expression);
        if (closeButton.managedProperty().isBound()) {
            closeButton.managedProperty().unbind();
        }
        closeButton.managedProperty().bind(closeButton.visibleProperty());
    }

    public void setCloseButtonVisible(boolean visible) {
        closeButton.visibleProperty().unbind();
        closeButton.setVisible(visible);
        if (closeButton.managedProperty().isBound()) {
            closeButton.managedProperty().unbind();
        }
        closeButton.setManaged(visible);
    }

    public void bindFloatButtonVisible(BooleanExpression expression) {
        if (expression == null) {
            return;
        }
        floatButton.visibleProperty().unbind();
        floatButton.visibleProperty().bind(expression);
        if (floatButton.managedProperty().isBound()) {
            floatButton.managedProperty().unbind();
        }
        floatButton.managedProperty().bind(floatButton.visibleProperty());
    }

    public void setFloatButtonVisible(boolean visible) {
        floatButton.visibleProperty().unbind();
        floatButton.setVisible(visible);
        if (floatButton.managedProperty().isBound()) {
            floatButton.managedProperty().unbind();
        }
        floatButton.setManaged(visible);
    }

    public boolean isFloatButtonVisible() {
        return floatButton.isVisible();
    }

    public boolean isCloseButtonVisible() {
        return closeButton.isVisible();
    }

    public DockNode getDockNode() {
        return dockNode;
    }

    public HBox getHeader() {
        return header;
    }

    /**
     * Installs a context menu for the node header.
     *
     * @param contextMenu the context menu to show on header right-click
     */
    public void setHeaderContextMenu(ContextMenu contextMenu) {
        headerContextMenu = contextMenu;
        header.setOnContextMenuRequested(this::onHeaderContextMenuRequested);
    }

    ContextMenu getHeaderContextMenu() {
        return headerContextMenu;
    }

    private void onHeaderContextMenuRequested(ContextMenuEvent event) {
        if (headerContextMenu == null) {
            return;
        }
        headerContextMenu.show(header, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    private void hideHeaderContextMenu() {
        if (headerContextMenu == null) {
            return;
        }
        headerContextMenu.hide();
    }

    public void setHeaderVisible(boolean visible) {
        header.setVisible(visible);
        header.setManaged(visible);
    }

    /**
     * Releases listeners and bindings to avoid retaining old views after rebuild cycles.
     */
    public void dispose() {
        dockNode.iconProperty().removeListener(iconListener);
        dockNode.contentProperty().removeListener(contentListener);

        titleLabel.textProperty().unbind();

        iconPane.visibleProperty().unbind();
        iconPane.managedProperty().unbind();
        iconPane.getChildren().clear();

        closeButton.visibleProperty().unbind();
        if (closeButton.managedProperty().isBound()) {
            closeButton.managedProperty().unbind();
        }
        closeButton.setOnAction(null);

        floatButton.visibleProperty().unbind();
        if (floatButton.managedProperty().isBound()) {
            floatButton.managedProperty().unbind();
        }
        floatButton.setOnAction(null);

        header.setOnMousePressed(null);
        header.setOnMouseDragged(null);
        header.setOnMouseReleased(null);
        header.setOnContextMenuRequested(null);
        if (headerContextMenu != null) {
            headerContextMenu.hide();
        }
        headerContextMenu = null;

        contentPane.getChildren().clear();
    }
}

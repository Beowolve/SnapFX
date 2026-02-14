package com.github.beowolve.snapfx.view;

import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockNode;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
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
    private final ChangeListener<Node> iconListener;
    private final ChangeListener<Node> contentListener;

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
                iconPane.getChildren().add(newIcon);
            }
        };
        dockNode.iconProperty().addListener(iconListener);

        if (dockNode.getIcon() != null) {
            iconPane.getChildren().add(dockNode.getIcon());
        }

        iconPane.visibleProperty().bind(dockNode.iconProperty().isNotNull());
        iconPane.managedProperty().bind(iconPane.visibleProperty());

        titleLabel = new Label();
        titleLabel.getStyleClass().add("dock-node-title-label");
        titleLabel.textProperty().bind(dockNode.titleProperty());

        floatButton = new Button();
        floatButton.getStyleClass().addAll("dock-node-close-button", "dock-node-float-button");
        floatButton.setGraphic(DockControlIcons.createFloatIcon());
        floatButton.setTooltip(new Tooltip("Float window"));
        floatButton.setFocusTraversable(false);
        floatButton.setOnAction(e -> { });
        floatButton.visibleProperty().bind(dockGraph.lockedProperty().not());
        floatButton.setManaged(true);

        closeButton = new Button();
        closeButton.getStyleClass().add("dock-node-close-button");
        closeButton.setGraphic(DockControlIcons.createCloseIcon());
        closeButton.setTooltip(new Tooltip("Close panel"));
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(e -> dockGraph.undock(dockNode));
        closeButton.visibleProperty().bind(
            dockNode.closeableProperty()
                .and(dockGraph.lockedProperty().not())
        );
        closeButton.setManaged(true);

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
        if (dragService != null) {
            dragService.startDrag(dockNode, event);
        }
    }

    private void onHeaderDragged(MouseEvent event) {
        if (dragService != null && dragService.isDragging()) {
            dragService.updateDrag(event);
        }
    }

    private void onHeaderReleased(MouseEvent event) {
        if (dragService != null && dragService.isDragging()) {
            dragService.endDrag(event);
        }
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
        closeButton.setManaged(true);
    }

    public void setCloseButtonVisible(boolean visible) {
        closeButton.visibleProperty().unbind();
        closeButton.setVisible(visible);
        closeButton.setManaged(true);
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
        closeButton.setOnAction(null);

        floatButton.visibleProperty().unbind();
        floatButton.setOnAction(null);

        header.setOnMousePressed(null);
        header.setOnMouseDragged(null);
        header.setOnMouseReleased(null);

        contentPane.getChildren().clear();
    }
}

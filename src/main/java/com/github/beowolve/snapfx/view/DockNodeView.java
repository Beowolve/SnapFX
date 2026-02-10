package com.github.beowolve.snapfx.view;

import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockNode;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

/**
 * Visual representation of a DockNode.
 * Renders a header with title and close button, plus the content.
 */
public class DockNodeView extends VBox {
    private final DockNode dockNode;
    private final DockGraph dockGraph;
    private final DockDragService dragService;
    private final HBox header;
    private final StackPane iconPane;
    private final Label titleLabel;
    private final Button closeButton;
    private final StackPane contentPane;

    public DockNodeView(DockNode dockNode, DockGraph dockGraph, DockDragService dragService) {
        this.dockNode = dockNode;
        this.dockGraph = dockGraph;
        this.dragService = dragService;

        getStyleClass().add("dock-node-view");

        // Build header
        header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dock-node-header");

        // Icon pane
        iconPane = new StackPane();
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

        titleLabel = new Label();
        titleLabel.getStyleClass().add("dock-node-title-label");
        titleLabel.textProperty().bind(dockNode.titleProperty());

        closeButton = new Button("×");
        closeButton.getStyleClass().add("dock-node-close-button");
        closeButton.setOnAction(e -> dockGraph.undock(dockNode));

        // Close button visibility
        // Keep button always managed (takes up space) to maintain consistent header height
        // But make it invisible when locked or not closeable
        closeButton.visibleProperty().bind(
            dockNode.closeableProperty()
                .and(dockGraph.lockedProperty().not())
        );
        // Always managed - button takes space even when invisible (for consistent header height)
        closeButton.setManaged(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(iconPane, titleLabel, spacer, closeButton);

        // Content pane
        contentPane = new StackPane();
        contentPane.getStyleClass().add("dock-node-content");
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        if (dockNode.getContent() != null) {
            contentPane.getChildren().add(dockNode.getContent());
        }

        // Content listener
        dockNode.contentProperty().addListener((obs, oldContent, newContent) -> {
            contentPane.getChildren().clear();
            if (newContent != null) {
                contentPane.getChildren().add(newContent);
            }
        });

        // Drag handlers on header
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

    public DockNode getDockNode() {
        return dockNode;
    }

    public HBox getHeader() {
        return header;
    }
}

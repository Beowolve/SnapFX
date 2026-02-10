package com.github.beowolve.snapfx.debug;

import com.github.beowolve.snapfx.dnd.DockDragData;
import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockPosition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.Objects;

/**
 * Lightweight debug overlay that renders the current D&D state as a small HUD.
 */
public class DockDebugOverlay extends StackPane {
    private final ObjectProperty<DockDragData> currentDrag;

    private final Text hudText;

    public DockDebugOverlay(DockGraph dockGraph, DockDragService dragService) {
        Objects.requireNonNull(dockGraph, "dockGraph");
        Objects.requireNonNull(dragService, "dragService");

        this.currentDrag = new SimpleObjectProperty<>(dragService.currentDragProperty().get());

        setMouseTransparent(true);
        setManaged(false);
        setPickOnBounds(false);

        setPadding(new Insets(8));

        hudText = new Text();
        hudText.setFill(Color.WHITE);

        var bg = new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.55), new CornerRadii(6), Insets.EMPTY));
        setBackground(bg);

        getChildren().add(hudText);
        StackPane.setMargin(hudText, new Insets(8));

        dragService.currentDragProperty().addListener((obs, old, cur) -> {
            currentDrag.set(cur);
            updateText(cur);
        });

        updateText(currentDrag.get());
    }

    private void updateText(DockDragData data) {
        if (data == null) {
            setVisible(false);
            return;
        }

        setVisible(true);

        DockElement target = data.getDropTarget();
        DockPosition pos = data.getDropPosition();

        String targetText = target == null ? "none" : target.getClass().getSimpleName();
        String posText = pos == null ? "none" : pos.name();

        hudText.setText("Drag: " + safeTitle(data) + "\nTarget: " + targetText + "\nZone: " + posText);
    }

    private String safeTitle(DockDragData data) {
        if (data == null || data.getDraggedNode() == null) {
            return "<null>";
        }
        String title = data.getDraggedNode().getTitle();
        return title == null || title.isEmpty() ? "<untitled>" : title;
    }
}

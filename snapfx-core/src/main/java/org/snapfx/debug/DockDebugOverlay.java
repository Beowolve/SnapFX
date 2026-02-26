package org.snapfx.debug;

import org.snapfx.model.DockNode;
import org.snapfx.theme.DockThemeStyleClasses;
import org.snapfx.dnd.DockDragData;
import org.snapfx.dnd.DockDragService;
import org.snapfx.model.DockElement;
import org.snapfx.model.DockGraph;
import org.snapfx.model.DockPosition;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.Objects;

/**
 * Lightweight debug overlay that renders the current D&amp;D state as a small HUD.
 */
public class DockDebugOverlay extends StackPane {
    private final DockDragService dragService;
    private final AnimationTimer refreshTimer;

    private final Text hudText;
    private String lastRenderedText;
    private boolean lastVisible;

    public DockDebugOverlay(DockGraph dockGraph, DockDragService dragService) {
        Objects.requireNonNull(dockGraph, "dockGraph");
        this.dragService = Objects.requireNonNull(dragService, "dragService");

        setMouseTransparent(true);
        setPickOnBounds(false);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPadding(new Insets(8));
        setVisible(false);
        getStyleClass().add(DockThemeStyleClasses.DOCK_DEBUG_PANEL);

        hudText = new Text();
        hudText.setFill(Color.RED);

        var bg = new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.55), new CornerRadii(6), Insets.EMPTY));
        setBackground(bg);

        getChildren().add(hudText);
        StackPane.setMargin(hudText, new Insets(8));

        dragService.currentDragProperty().addListener((obs, old, cur) -> {
            refreshFromDragService();
        });

        refreshTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                refreshFromDragService();
            }
        };
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                refreshTimer.stop();
            } else {
                refreshTimer.start();
                refreshFromDragService();
            }
        });

        refreshFromDragService();
    }

    void refreshFromDragService() {
        updateText(dragService.getCurrentDrag());
    }

    String getHudTextForTest() {
        return hudText.getText();
    }

    private void updateText(DockDragData data) {
        if (data == null) {
            applyHudState(false, null);
            return;
        }

        DockElement target = data.getDropTarget();
        DockPosition pos = data.getDropPosition();

        String targetText = target == null ? "none" : target.getClass().getSimpleName();
        String posText = pos == null ? "none" : pos.name();

        if (target instanceof DockNode dockNode) {
            targetText = dockNode.getTitle();
        }

        applyHudState(
            true,
            "Drag: " + safeTitle(data) + "\nTarget: " + targetText + "\nZone: " + posText
        );
    }

    private void applyHudState(boolean visible, String text) {
        if (lastVisible == visible && Objects.equals(lastRenderedText, text)) {
            return;
        }
        lastVisible = visible;
        lastRenderedText = text;
        setVisible(visible);
        hudText.setText(text);
    }

    private String safeTitle(DockDragData data) {
        if (data == null || data.getDraggedNode() == null) {
            return "<null>";
        }
        String title = data.getDraggedNode().getTitle();
        return title == null || title.isEmpty() ? "<untitled>" : title;
    }
}

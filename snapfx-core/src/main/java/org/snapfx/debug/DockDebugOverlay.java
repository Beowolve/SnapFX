package org.snapfx.debug;

import org.snapfx.localization.DockLocalizationProvider;
import org.snapfx.localization.internal.DockLocalizationService;
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

import java.util.Locale;
import java.util.Objects;

/**
 * Lightweight debug overlay that renders the current D&amp;D state as a small HUD.
 */
public class DockDebugOverlay extends StackPane {
    private final DockDragService dragService;
    private final AnimationTimer refreshTimer;
    private final DockLocalizationService localizationService;

    private final Text hudText;
    private String lastRenderedText;
    private boolean lastVisible;

    /**
     * Creates the debug HUD overlay for drag diagnostics.
     *
     * @param dockGraph active dock graph (used for lifecycle alignment)
     * @param dragService drag service that provides current drag state
     */
    public DockDebugOverlay(DockGraph dockGraph, DockDragService dragService) {
        Objects.requireNonNull(dockGraph, "dockGraph");
        this.dragService = Objects.requireNonNull(dragService, "dragService");
        this.localizationService = new DockLocalizationService();

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

    /**
     * Sets the locale used by the debug overlay.
     *
     * @param locale locale to apply, or {@code null} for default
     */
    public void setLocale(Locale locale) {
        localizationService.setLocale(locale);
        refreshFromDragService();
    }

    /**
     * Sets an optional user localization provider used by the debug overlay.
     *
     * @param provider provider to apply, or {@code null}
     */
    public void setLocalizationProvider(DockLocalizationProvider provider) {
        localizationService.setUserProvider(provider);
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

        String targetText = target == null ? text("dock.debug.none") : target.getClass().getSimpleName();
        String posText = pos == null ? text("dock.debug.none") : pos.name();

        if (target instanceof DockNode dockNode) {
            targetText = dockNode.getTitle();
        }

        applyHudState(
            true,
            text("dock.debug.overlay.dragLine", safeTitle(data))
                + "\n"
                + text("dock.debug.overlay.targetLine", targetText)
                + "\n"
                + text("dock.debug.overlay.zoneLine", posText)
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
            return text("dock.common.null");
        }
        String title = data.getDraggedNode().getTitle();
        return title == null || title.isEmpty() ? text("dock.common.untitled") : title;
    }

    private String text(String key, Object... args) {
        return localizationService.text(Objects.requireNonNull(key, "key"), args);
    }
}

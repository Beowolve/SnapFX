package org.snapfx.floating;

import org.snapfx.model.DockNode;
import javafx.scene.Scene;

import java.util.List;

/**
 * Manages floating-window selection helpers and active-floating-window state for {@code SnapFX}.
 *
 * <p>This controller keeps the currently active floating window and provides reusable lookup logic
 * (for example, by scene ownership or contained dock node), so those concerns are isolated from
 * facade orchestration code.</p>
 */
public final class DockFloatingController {
    private DockFloatingWindow activeFloatingWindow;

    /**
     * Creates a floating controller with an empty active-window state.
     */
    public DockFloatingController() {
        // create default state
    }

    /**
     * Marks a floating window as active.
     *
     * @param floatingWindow active floating window, or {@code null}
     */
    public void setActiveFloatingWindow(DockFloatingWindow floatingWindow) {
        activeFloatingWindow = floatingWindow;
    }

    /**
     * Clears the active floating window when it matches the provided window.
     *
     * @param floatingWindow window to clear if currently active
     */
    public void clearActiveFloatingWindowIfMatches(DockFloatingWindow floatingWindow) {
        if (activeFloatingWindow == floatingWindow) {
            activeFloatingWindow = null;
        }
    }

    /**
     * Resolves the active floating window using event/focus scenes, current active-window state,
     * and fallback-to-last-window behavior.
     *
     * @param floatingWindows current floating-window list
     * @param eventScene scene derived from the triggering event target, or {@code null}
     * @param focusedScene currently focused scene, or {@code null}
     * @return resolved active floating window, or {@code null}
     */
    public DockFloatingWindow resolveActiveFloatingWindow(
        List<DockFloatingWindow> floatingWindows,
        Scene eventScene,
        Scene focusedScene
    ) {
        DockFloatingWindow fromEventScene = findFloatingWindowByScene(floatingWindows, eventScene);
        if (fromEventScene != null) {
            return fromEventScene;
        }

        DockFloatingWindow fromFocusedScene = findFloatingWindowByScene(floatingWindows, focusedScene);
        if (fromFocusedScene != null) {
            return fromFocusedScene;
        }

        if (activeFloatingWindow != null
            && floatingWindows != null
            && floatingWindows.contains(activeFloatingWindow)) {
            return activeFloatingWindow;
        }

        if (floatingWindows == null || floatingWindows.isEmpty()) {
            return null;
        }
        return floatingWindows.getLast();
    }

    /**
     * Finds the floating window that owns a scene.
     *
     * @param floatingWindows current floating-window list
     * @param scene scene to match
     * @return owning floating window, or {@code null}
     */
    public DockFloatingWindow findFloatingWindowByScene(List<DockFloatingWindow> floatingWindows, Scene scene) {
        if (scene == null || floatingWindows == null || floatingWindows.isEmpty()) {
            return null;
        }
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            if (floatingWindow != null && floatingWindow.ownsScene(scene)) {
                return floatingWindow;
            }
        }
        return null;
    }

    /**
     * Finds the floating window that currently contains a dock node.
     *
     * @param floatingWindows current floating-window list
     * @param node dock node to locate
     * @return containing floating window, or {@code null}
     */
    public DockFloatingWindow findFloatingWindowContainingNode(List<DockFloatingWindow> floatingWindows, DockNode node) {
        if (node == null || floatingWindows == null || floatingWindows.isEmpty()) {
            return null;
        }
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            if (floatingWindow != null && floatingWindow.containsNode(node)) {
                return floatingWindow;
            }
        }
        return null;
    }
}

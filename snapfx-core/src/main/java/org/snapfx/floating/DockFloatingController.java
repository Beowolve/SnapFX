package org.snapfx.floating;

import org.snapfx.model.DockNode;
import javafx.scene.Scene;

import java.util.List;

/**
 * Manages floating-window interaction helpers and active-floating-window state for {@code SnapFX}.
 *
 * <p>This controller keeps the currently active floating window and provides reusable lookup/state
 * helpers (for example, by scene ownership, contained dock node, hit testing, list-order promotion,
 * and remembered floating bounds propagation), so those concerns are isolated from facade
 * orchestration code.</p>
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

    /**
     * Returns whether a main-layout drop should be suppressed because the pointer is above a
     * floating window.
     *
     * @param floatingWindows current floating-window list
     * @param screenX screen x-coordinate
     * @param screenY screen y-coordinate
     * @return {@code true} when a floating window is hit at the screen point
     */
    public boolean isMainDropSuppressedByFloatingWindow(
        List<DockFloatingWindow> floatingWindows,
        Double screenX,
        Double screenY
    ) {
        if (screenX == null || screenY == null) {
            return false;
        }
        return findTopFloatingWindowAt(floatingWindows, screenX, screenY) != null;
    }

    /**
     * Finds the top-most floating window containing the given screen point.
     *
     * @param floatingWindows current floating-window list
     * @param screenX screen x-coordinate
     * @param screenY screen y-coordinate
     * @return top-most hit floating window, or {@code null}
     */
    public DockFloatingWindow findTopFloatingWindowAt(
        List<DockFloatingWindow> floatingWindows,
        double screenX,
        double screenY
    ) {
        if (floatingWindows == null || floatingWindows.isEmpty()) {
            return null;
        }
        for (int i = floatingWindows.size() - 1; i >= 0; i--) {
            DockFloatingWindow floatingWindow = floatingWindows.get(i);
            if (floatingWindow != null && floatingWindow.containsScreenPoint(screenX, screenY)) {
                return floatingWindow;
            }
        }
        return null;
    }

    /**
     * Moves a floating window to the end of the ordered list to mark it as top-most in ordering.
     *
     * @param floatingWindows current floating-window list
     * @param floatingWindow window to promote
     */
    public void promoteFloatingWindowToFront(
        List<DockFloatingWindow> floatingWindows,
        DockFloatingWindow floatingWindow
    ) {
        if (floatingWindows == null || floatingWindow == null || floatingWindows.isEmpty()) {
            return;
        }
        int index = floatingWindows.indexOf(floatingWindow);
        if (index < 0 || index == floatingWindows.size() - 1) {
            return;
        }
        floatingWindows.remove(index);
        floatingWindows.add(floatingWindow);
    }

    /**
     * Applies remembered floating bounds and always-on-top state from a node to a floating window.
     *
     * @param node node that stores remembered floating state
     * @param floatingWindow target floating window
     */
    public void applyRememberedFloatingBounds(DockNode node, DockFloatingWindow floatingWindow) {
        if (node == null || floatingWindow == null) {
            return;
        }
        if (node.getLastFloatingWidth() != null && node.getLastFloatingHeight() != null) {
            floatingWindow.setPreferredSize(node.getLastFloatingWidth(), node.getLastFloatingHeight());
        }
        if (node.getLastFloatingX() != null || node.getLastFloatingY() != null) {
            floatingWindow.setPreferredPosition(node.getLastFloatingX(), node.getLastFloatingY());
        }
        if (node.getLastFloatingAlwaysOnTop() != null) {
            floatingWindow.setAlwaysOnTop(node.getLastFloatingAlwaysOnTop(), DockFloatingPinSource.API);
        }
    }

    /**
     * Captures preferred window bounds and propagates them plus always-on-top state to all hosted
     * nodes for restore workflows.
     *
     * @param floatingWindow source floating window
     */
    public void rememberFloatingBoundsForNodes(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        floatingWindow.captureCurrentBounds();
        rememberFloatingAlwaysOnTopForNodes(floatingWindow);
        for (DockNode node : floatingWindow.getDockNodes()) {
            node.setLastFloatingX(floatingWindow.getPreferredX());
            node.setLastFloatingY(floatingWindow.getPreferredY());
            node.setLastFloatingWidth(floatingWindow.getPreferredWidth());
            node.setLastFloatingHeight(floatingWindow.getPreferredHeight());
        }
    }

    /**
     * Propagates the floating window always-on-top state to all hosted nodes.
     *
     * @param floatingWindow source floating window
     */
    public void rememberFloatingAlwaysOnTopForNodes(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        for (DockNode node : floatingWindow.getDockNodes()) {
            node.setLastFloatingAlwaysOnTop(floatingWindow.isAlwaysOnTop());
        }
    }
}

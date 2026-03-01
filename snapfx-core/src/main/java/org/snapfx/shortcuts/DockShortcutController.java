package org.snapfx.shortcuts;

import org.snapfx.floating.DockFloatingWindow;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manages framework shortcut bindings and resolves matching shortcut actions for key events.
 *
 * <p>This controller is framework-internal implementation detail used by {@code SnapFX}
 * to keep shortcut state and matching logic isolated from facade orchestration code.</p>
 */
public final class DockShortcutController {
    private static final KeyCombination DEFAULT_SHORTCUT_CLOSE_ACTIVE_NODE = new KeyCodeCombination(
        KeyCode.W,
        KeyCombination.SHORTCUT_DOWN
    );
    private static final KeyCombination DEFAULT_SHORTCUT_NEXT_TAB = new KeyCodeCombination(
        KeyCode.TAB,
        KeyCombination.SHORTCUT_DOWN
    );
    private static final KeyCombination DEFAULT_SHORTCUT_PREVIOUS_TAB = new KeyCodeCombination(
        KeyCode.TAB,
        KeyCombination.SHORTCUT_DOWN,
        KeyCombination.SHIFT_DOWN
    );
    private static final KeyCombination DEFAULT_SHORTCUT_CANCEL_DRAG = new KeyCodeCombination(KeyCode.ESCAPE);
    private static final KeyCombination DEFAULT_SHORTCUT_TOGGLE_ACTIVE_FLOATING_PIN = new KeyCodeCombination(
        KeyCode.P,
        KeyCombination.SHORTCUT_DOWN,
        KeyCombination.SHIFT_DOWN
    );

    private final EnumMap<DockShortcutAction, KeyCombination> shortcuts = new EnumMap<>(DockShortcutAction.class);
    private final Map<DockFloatingWindow, Scene> floatingShortcutScenes = new HashMap<>();

    /**
     * Creates a controller with the default shortcut mapping.
     */
    public DockShortcutController() {
        resetToDefaults();
    }

    /**
     * Restores the default framework shortcut mapping.
     */
    public void resetToDefaults() {
        shortcuts.clear();
        shortcuts.put(DockShortcutAction.CLOSE_ACTIVE_NODE, DEFAULT_SHORTCUT_CLOSE_ACTIVE_NODE);
        shortcuts.put(DockShortcutAction.NEXT_TAB, DEFAULT_SHORTCUT_NEXT_TAB);
        shortcuts.put(DockShortcutAction.PREVIOUS_TAB, DEFAULT_SHORTCUT_PREVIOUS_TAB);
        shortcuts.put(DockShortcutAction.CANCEL_DRAG, DEFAULT_SHORTCUT_CANCEL_DRAG);
        shortcuts.put(DockShortcutAction.TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP, DEFAULT_SHORTCUT_TOGGLE_ACTIVE_FLOATING_PIN);
    }

    /**
     * Assigns or removes a key binding for a built-in shortcut action.
     *
     * @param action shortcut action to configure
     * @param keyCombination key combination to assign, or {@code null} to remove the binding
     */
    public void setShortcut(DockShortcutAction action, KeyCombination keyCombination) {
        if (action == null) {
            return;
        }
        if (keyCombination == null) {
            shortcuts.remove(action);
            return;
        }

        shortcuts.entrySet().removeIf(entry ->
            entry.getKey() != action && entry.getValue().equals(keyCombination)
        );
        shortcuts.put(action, keyCombination);
    }

    /**
     * Removes the key binding for a shortcut action.
     *
     * @param action shortcut action to clear
     */
    public void clearShortcut(DockShortcutAction action) {
        setShortcut(action, null);
    }

    /**
     * Returns the configured key binding for a shortcut action.
     *
     * @param action shortcut action to query
     * @return configured key combination, or {@code null}
     */
    public KeyCombination getShortcut(DockShortcutAction action) {
        if (action == null) {
            return null;
        }
        return shortcuts.get(action);
    }

    /**
     * Returns an immutable snapshot of all current shortcut bindings.
     *
     * @return immutable snapshot of shortcut bindings
     */
    public Map<DockShortcutAction, KeyCombination> getShortcutsSnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(shortcuts));
    }

    /**
     * Resolves the shortcut action that matches the given key event.
     *
     * @param event key event to evaluate
     * @return matching shortcut action, or {@code null} when no mapping matches
     */
    public DockShortcutAction resolveShortcutAction(KeyEvent event) {
        if (event == null) {
            return null;
        }
        for (Map.Entry<DockShortcutAction, KeyCombination> entry : shortcuts.entrySet()) {
            KeyCombination combination = entry.getValue();
            if (combination != null && combination.match(event)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Binds the framework shortcut key filter to the current scene of a floating window.
     *
     * <p>When the floating window scene changes, this method removes the filter from the previous
     * scene and installs it on the new one.</p>
     *
     * @param floatingWindow floating window whose scene should receive shortcut handling
     * @param keyEventFilter key filter used for framework shortcuts
     */
    public void bindFloatingShortcutScene(
        DockFloatingWindow floatingWindow,
        EventHandler<KeyEvent> keyEventFilter
    ) {
        if (floatingWindow == null || keyEventFilter == null) {
            return;
        }
        Scene scene = floatingWindow.getScene();
        Scene previousScene = floatingShortcutScenes.get(floatingWindow);
        if (previousScene == scene) {
            return;
        }
        if (previousScene != null) {
            previousScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventFilter);
        }
        if (scene == null) {
            floatingShortcutScenes.remove(floatingWindow);
            return;
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEventFilter);
        floatingShortcutScenes.put(floatingWindow, scene);
    }

    /**
     * Removes the framework shortcut key filter binding for a floating window scene.
     *
     * @param floatingWindow floating window whose bound scene should be unbound
     * @param keyEventFilter key filter previously installed for framework shortcuts
     */
    public void unbindFloatingShortcutScene(
        DockFloatingWindow floatingWindow,
        EventHandler<KeyEvent> keyEventFilter
    ) {
        if (floatingWindow == null || keyEventFilter == null) {
            return;
        }
        Scene scene = floatingShortcutScenes.remove(floatingWindow);
        if (scene == null) {
            return;
        }
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventFilter);
    }

    /**
     * Resolves a node from an event target object.
     *
     * @param eventTarget event target object ({@link Node}, {@link Scene}, or other)
     * @return resolved node, or {@code null}
     */
    public Node resolveNodeFromEventTarget(Object eventTarget) {
        if (eventTarget instanceof Node node) {
            return node;
        }
        if (eventTarget instanceof Scene scene) {
            return scene.getFocusOwner();
        }
        return null;
    }

    /**
     * Resolves the focused node associated with an event target.
     *
     * @param eventTarget event target object ({@link Node}, {@link Scene}, or other)
     * @param fallbackScene fallback scene used when target does not provide a focus owner
     * @return focused node, or {@code null}
     */
    public Node resolveFocusedNode(Object eventTarget, Scene fallbackScene) {
        if (eventTarget instanceof Node node && node.getScene() != null) {
            return node.getScene().getFocusOwner();
        }
        if (eventTarget instanceof Scene scene) {
            return scene.getFocusOwner();
        }
        if (fallbackScene != null) {
            return fallbackScene.getFocusOwner();
        }
        return null;
    }

    /**
     * Resolves a scene from an event target object.
     *
     * @param eventTarget event target object ({@link Scene}, {@link Node}, or other)
     * @return resolved scene, or {@code null}
     */
    public Scene resolveSceneFromEventTarget(Object eventTarget) {
        if (eventTarget instanceof Scene scene) {
            return scene;
        }
        if (eventTarget instanceof Node node) {
            return node.getScene();
        }
        return null;
    }

    /**
     * Resolves a scene from a node.
     *
     * @param node source node
     * @return node scene, or {@code null}
     */
    public Scene resolveSceneFromNode(Node node) {
        if (node == null) {
            return null;
        }
        return node.getScene();
    }

    /**
     * Resolves the currently active tab pane from event/focus context and an optional root fallback.
     *
     * @param eventTarget event target object ({@link Node}, {@link Scene}, or other)
     * @param fallbackScene fallback scene used for focus lookup
     * @param fallbackRoot fallback root node searched when no target/focus tab pane is found
     * @return resolved tab pane, or {@code null}
     */
    public TabPane resolveActiveTabPane(Object eventTarget, Scene fallbackScene, Node fallbackRoot) {
        Node targetNode = resolveNodeFromEventTarget(eventTarget);
        TabPane tabPaneFromTarget = findTabPaneInHierarchy(targetNode);
        if (tabPaneFromTarget != null) {
            return tabPaneFromTarget;
        }

        Node focusedNode = resolveFocusedNode(eventTarget, fallbackScene);
        TabPane tabPaneFromFocus = findTabPaneInHierarchy(focusedNode);
        if (tabPaneFromFocus != null) {
            return tabPaneFromFocus;
        }

        return findFirstTabPane(fallbackRoot);
    }

    /**
     * Finds the nearest {@link TabPane} while traversing parent hierarchy upward.
     *
     * @param node start node
     * @return nearest ancestor tab pane, or {@code null}
     */
    public TabPane findTabPaneInHierarchy(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof TabPane tabPane) {
                return tabPane;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Finds the first {@link TabPane} in a depth-first traversal of a node subtree.
     *
     * @param root traversal root node
     * @return first tab pane found, or {@code null}
     */
    public TabPane findFirstTabPane(Node root) {
        if (root == null) {
            return null;
        }
        if (root instanceof TabPane tabPane) {
            return tabPane;
        }
        if (!(root instanceof Parent parent)) {
            return null;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            TabPane childTabPane = findFirstTabPane(child);
            if (childTabPane != null) {
                return childTabPane;
            }
        }
        return null;
    }
}

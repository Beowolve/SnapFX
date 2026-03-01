package org.snapfx.shortcuts;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

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
}

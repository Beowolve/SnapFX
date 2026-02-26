package com.github.beowolve.snapfx;

/**
 * Built-in shortcut actions supported by {@link SnapFX}.
 *
 * <p>Applications can remap these actions to custom key combinations via
 * {@link SnapFX#setShortcut(DockShortcutAction, javafx.scene.input.KeyCombination)}.</p>
 */
public enum DockShortcutAction {
    /**
     * Close the currently active dock node.
     */
    CLOSE_ACTIVE_NODE,
    /**
     * Select the next tab in the active tab pane.
     */
    NEXT_TAB,
    /**
     * Select the previous tab in the active tab pane.
     */
    PREVIOUS_TAB,
    /**
     * Cancel an active drag operation.
     */
    CANCEL_DRAG,
    /**
     * Toggle always-on-top for the active floating window.
     */
    TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP
}

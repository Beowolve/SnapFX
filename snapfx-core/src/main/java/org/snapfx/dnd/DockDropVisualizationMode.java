package org.snapfx.dnd;

/**
 * Controls how drop zones are visualized during drag.
 */
public enum DockDropVisualizationMode {
    /** Hide all drop-zone visuals while dragging. */
    OFF,
    /** Show only zones related to the currently active target. */
    DEFAULT,
    /** Show all valid drop zones in the current layout tree. */
    ALL_ZONES,
    /** Show valid drop zones within the active target subtree. */
    SUBTREE,
    /** Show only the active indicator, without background zones. */
    ACTIVE_ONLY
}

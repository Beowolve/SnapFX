package org.snapfx.floating;

/**
 * Defines which surfaces are considered while snapping floating windows during title-bar drag operations.
 */
public enum DockFloatingSnapTarget {
    /**
     * Snap against current screen work-area edges.
     */
    SCREEN,

    /**
     * Snap against the primary/main application window edges.
     */
    MAIN_WINDOW,

    /**
     * Snap against other floating window edges.
     */
    FLOATING_WINDOWS
}


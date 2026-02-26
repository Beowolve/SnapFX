package org.snapfx.sidebar;

/**
 * Controls sidebar visibility behavior in {@code SnapFX}.
 *
 * <p>This mode only affects framework sidebar UI availability and rendering behavior. Sidebar model state
 * (for example pinned nodes and per-side panel widths) remains stored in {@code DockGraph} so hosts can
 * switch modes without losing sidebar state.</p>
 */
public enum DockSideBarMode {
    /**
     * Always render left/right sidebar strips, even when they are empty.
     *
     * <p>This allows direct drag-and-drop into empty sidebars.</p>
     */
    ALWAYS,

    /**
     * Render sidebars only when a side currently contains pinned sidebar nodes.
     */
    AUTO,

    /**
     * Disable framework sidebar UI and sidebar move context-menu actions.
     *
     * <p>Sidebar model state is preserved but hidden while this mode is active.</p>
     */
    NEVER
}

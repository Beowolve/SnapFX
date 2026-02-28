package org.snapfx.close;

/**
 * Describes where a close request originated.
 */
public enum DockCloseSource {
    /** Close request from a tab header close action. */
    TAB,
    /** Close request from a dock-node title-bar close action. */
    TITLE_BAR,
    /** Close request from a floating-window close action. */
    FLOATING_WINDOW
}

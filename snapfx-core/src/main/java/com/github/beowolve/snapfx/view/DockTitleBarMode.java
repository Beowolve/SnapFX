package com.github.beowolve.snapfx.view;

/**
 * Controls when DockNode title bars are shown.
 */
public enum DockTitleBarMode {
    /**
     * Always show the title bar, even when docked inside a tab pane.
     */
    ALWAYS,
    /**
     * Never show the title bar, even when docked outside a tab pane.
     */
    NEVER,
    /**
     * Show the title bar only when the node is not inside a tab pane.
     */
    AUTO
}

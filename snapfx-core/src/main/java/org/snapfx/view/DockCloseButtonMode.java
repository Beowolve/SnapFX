package org.snapfx.view;

/**
 * Controls which close buttons are visible for docked nodes.
 */
public enum DockCloseButtonMode {
    /** Show close controls only on tab headers. */
    TAB_ONLY,
    /** Show close controls only on dock-node title bars. */
    TITLE_ONLY,
    /** Show close controls on both tab headers and title bars. */
    BOTH;

    /**
     * Returns whether tab close controls should be visible.
     *
     * @return {@code true} when tab close controls are enabled
     */
    public boolean showTabClose() {
        return this == TAB_ONLY || this == BOTH;
    }

    /**
     * Returns whether title-bar close controls should be visible.
     *
     * @return {@code true} when title-bar close controls are enabled
     */
    public boolean showTitleClose() {
        return this == TITLE_ONLY || this == BOTH;
    }
}

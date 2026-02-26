package org.snapfx.view;

/**
 * Controls which close buttons are visible for docked nodes.
 */
public enum DockCloseButtonMode {
    TAB_ONLY,
    TITLE_ONLY,
    BOTH;

    public boolean showTabClose() {
        return this == TAB_ONLY || this == BOTH;
    }

    public boolean showTitleClose() {
        return this == TITLE_ONLY || this == BOTH;
    }
}

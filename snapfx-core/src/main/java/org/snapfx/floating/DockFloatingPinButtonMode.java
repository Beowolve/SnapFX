package org.snapfx.floating;

/**
 * Controls when the pin button is shown in floating window title bars.
 */
public enum DockFloatingPinButtonMode {
    /**
     * Always show the pin button when pin toggling is enabled.
     */
    ALWAYS,
    /**
     * Never show the pin button.
     */
    NEVER,
    /**
     * Show the pin button automatically based on floating window interaction state.
     */
    AUTO
}

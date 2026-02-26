package org.snapfx.floating;

/**
 * Indicates what triggered a floating window pin-state change.
 */
public enum DockFloatingPinSource {
    USER,
    API,
    LAYOUT_LOAD,
    WINDOW_CREATE_DEFAULT
}

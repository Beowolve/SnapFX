package org.snapfx.floating;

/**
 * Indicates what triggered a floating window pin-state change.
 */
public enum DockFloatingPinSource {
    /** User interaction from floating-window controls or menus. */
    USER,
    /** Programmatic API call from application code. */
    API,
    /** Layout deserialization restore flow. */
    LAYOUT_LOAD,
    /** Initial default when creating a new floating window. */
    WINDOW_CREATE_DEFAULT
}

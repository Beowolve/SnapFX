package org.snapfx.floating;

/**
 * Event emitted when a floating window pin state changes.
 *
 * @param window Floating window whose state changed
 * @param alwaysOnTop New always-on-top state
 * @param source Trigger source of the change
 */
public record DockFloatingPinChangeEvent(
    DockFloatingWindow window,
    boolean alwaysOnTop,
    DockFloatingPinSource source
) {
}

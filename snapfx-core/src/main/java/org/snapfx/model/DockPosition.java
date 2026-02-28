package org.snapfx.model;

/**
 * Represents the different docking zones used during drag &amp; drop.
 */
public enum DockPosition {
    /** Split above the target. */
    TOP,
    /** Split below the target. */
    BOTTOM,
    /** Split to the left of the target. */
    LEFT,
    /** Split to the right of the target. */
    RIGHT,
    /** Insert as tab (center docking). */
    CENTER
}

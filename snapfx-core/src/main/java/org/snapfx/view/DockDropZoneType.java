package org.snapfx.view;

/**
 * Type of a drop zone for drag-and-drop.
 */
public enum DockDropZoneType {
    /** Edge drop zone for split docking around a target. */
    EDGE,
    /** Center drop zone for tab-into/container docking. */
    CENTER,
    /** Tab header hover zone for selecting or targeting existing tabs. */
    TAB_HEADER,
    /** Tab insert zone that represents an insertion index between tabs. */
    TAB_INSERT
}

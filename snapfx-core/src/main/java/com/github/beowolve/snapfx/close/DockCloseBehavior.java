package com.github.beowolve.snapfx.close;

/**
 * Defines the default behavior applied when a close request is accepted.
 */
public enum DockCloseBehavior {
    /**
     * Removes the node from its layout and adds it to the hidden node list.
     */
    HIDE,
    /**
     * Removes the node from its layout without adding it to the hidden node list.
     */
    REMOVE
}

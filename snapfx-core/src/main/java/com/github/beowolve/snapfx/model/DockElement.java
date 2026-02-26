package com.github.beowolve.snapfx.model;

/**
 * Base interface for all elements in the DockGraph.
 * Enables uniform handling of containers and leaf nodes.
 */
public interface DockElement {
    /**
     * Returns the element's ID.
     */
    String getId();

    /**
     * Returns the parent container, or {@code null} if this is the root.
     */
    DockContainer getParent();

    /**
     * Sets the parent container.
     */
    void setParent(DockContainer parent);

    /**
     * Removes this element from its parent container.
     */
    default void removeFromParent() {
        if (getParent() != null) {
            getParent().removeChild(this);
        }
    }
}

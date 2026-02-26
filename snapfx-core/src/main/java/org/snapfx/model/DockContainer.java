package org.snapfx.model;

import javafx.collections.ObservableList;

/**
 * Interface for container elements in the DockGraph.
 * Containers can hold multiple DockElement instances.
 */
public interface DockContainer extends DockElement {
    /**
     * Returns the list of child elements.
     */
    ObservableList<DockElement> getChildren();

    /**
     * Adds a child element.
     */
    void addChild(DockElement element);

    /**
     * Removes a child element.
     */
    void removeChild(DockElement element);

    /**
     * Returns whether the container is empty.
     */
    default boolean isEmpty() {
        return getChildren().isEmpty();
    }

    /**
     * Auto-cleanup: removes this container from its parent when it becomes empty.
     */
    default void cleanupIfEmpty() {
        if (isEmpty() && getParent() != null) {
            removeFromParent();
        }
    }
}

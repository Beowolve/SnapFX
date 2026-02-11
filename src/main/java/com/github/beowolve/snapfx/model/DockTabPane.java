package com.github.beowolve.snapfx.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a tab container in the DockGraph.
 * Can contain multiple DockNodes as tabs.
 */
public class DockTabPane implements DockContainer {
    private final String id;
    private final ObservableList<DockElement> children;
    private final IntegerProperty selectedIndex;
    private DockContainer parent;
    private DockElement flattenedChild; // Temporary: child after flattening

    public DockTabPane() {
        this.id = UUID.randomUUID().toString();
        this.children = FXCollections.observableArrayList();
        this.selectedIndex = new SimpleIntegerProperty(0);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public DockContainer getParent() {
        return parent;
    }

    @Override
    public void setParent(DockContainer parent) {
        this.parent = parent;
    }

    @Override
    public ObservableList<DockElement> getChildren() {
        return children;
    }

    @Override
    public void addChild(DockElement element) {
        // Smart flattening: if the element is also a TabPane, merge its children into this TabPane
        if (element instanceof DockTabPane otherTabPane) {
            // Add all children of the other TabPane directly to this TabPane
            List<DockElement> childElements = new ArrayList<>(otherTabPane.getChildren());
            for (DockElement child : childElements) {
                otherTabPane.removeChild(child);
                children.add(child);
                child.setParent(this);
            }
            return;
        }

        children.add(element);
        element.setParent(this);

        // Auto-select the first tab
        if (children.size() == 1) {
            selectedIndex.set(0);
        }
    }

    @Override
    public void removeChild(DockElement element) {
        children.remove(element);
        element.setParent(null);

        // Adjust selected index
        if (selectedIndex.get() >= children.size()) {
            selectedIndex.set(Math.max(0, children.size() - 1));
        }

        // If only one tab remains, flatten FIRST before cleanup
        if (children.size() == 1) {
            DockElement onlyChild = children.getFirst();

            if (parent != null) {
                int indexInParent = parent.getChildren().indexOf(this);

                // Remove the remaining child from this container before re-inserting
                children.clear();

                if (indexInParent >= 0 && indexInParent < parent.getChildren().size()) {
                    parent.getChildren().set(indexInParent, onlyChild);
                } else {
                    parent.addChild(onlyChild);
                }

                onlyChild.setParent(parent);
                this.setParent(null);
            } else {
                // TabPane is root - the child becomes the new root
                flattenedChild = onlyChild; // Store for DockGraph
                children.clear();
                onlyChild.setParent(null);
            }
        }
        // Auto-cleanup: only after attempting to flatten
        // This prevents empty containers from being left in the tree
        else if (children.isEmpty()) {
            cleanupIfEmpty();
        }
    }

    /**
     * Returns the flattened child (only after flattening a root container).
     */
    public DockElement getFlattenedChild() {
        DockElement child = flattenedChild;
        flattenedChild = null; // Return only once
        return child;
    }

    public int getSelectedIndex() {
        return selectedIndex.get();
    }

    public IntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < children.size()) {
            selectedIndex.set(index);
        }
    }

    @Override
    public String toString() {
        return "DockTabPane{id='" + id + "', children=" + children.size() + "}";
    }
}

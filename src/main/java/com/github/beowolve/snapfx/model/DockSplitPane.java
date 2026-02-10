package com.github.beowolve.snapfx.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a split container in the DockGraph.
 * Can be oriented horizontally or vertically.
 */
public class DockSplitPane implements DockContainer {
    private final String id;
    private final Orientation orientation;
    private final ObservableList<DockElement> children;
    private final List<DoubleProperty> dividerPositions;
    private DockContainer parent;
    private DockElement flattenedChild; // Temporary: child after flattening

    public DockSplitPane(Orientation orientation) {
        this.id = UUID.randomUUID().toString();
        this.orientation = orientation;
        this.children = FXCollections.observableArrayList();
        this.dividerPositions = new ArrayList<>();
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
        // Smart splitting: flatten if the child has the same orientation
        if (element instanceof DockSplitPane splitPane) {
            if (splitPane.getOrientation() == this.orientation) {
                // Add all children of the child split pane directly
                List<DockElement> childElements = new ArrayList<>(splitPane.getChildren());
                for (DockElement child : childElements) {
                    splitPane.removeChild(child);
                    children.add(child);
                    child.setParent(this);
                }
                return;
            }
        }

        children.add(element);
        element.setParent(this);

        // Update divider positions
        updateDividerPositions();
    }

    @Override
    public void removeChild(DockElement element) {
        children.remove(element);
        element.setParent(null);
        updateDividerPositions();

        // If only one child remains, flatten FIRST before cleanup
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
                // SplitPane is root - the child becomes the new root
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

    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * Returns the flattened child (only after flattening a root container).
     */
    public DockElement getFlattenedChild() {
        DockElement child = flattenedChild;
        flattenedChild = null; // Return only once
        return child;
    }

    public List<DoubleProperty> getDividerPositions() {
        return dividerPositions;
    }

    public void setDividerPosition(int index, double position) {
        if (index >= 0 && index < dividerPositions.size()) {
            dividerPositions.get(index).set(position);
        }
    }

    private void updateDividerPositions() {
        int requiredDividers = Math.max(0, children.size() - 1);
        int currentDividers = dividerPositions.size();

        if (currentDividers == requiredDividers) {
            return; // No change needed
        }

        if (currentDividers > requiredDividers) {
            // Remove extra dividers from the end
            while (dividerPositions.size() > requiredDividers) {
                dividerPositions.removeLast();
            }
        } else {
            // Add missing dividers
            // Strategy: Distribute new dividers in the remaining space
            // If we have existing dividers, insert new ones proportionally
            if (currentDividers == 0 && requiredDividers > 0) {
                // No existing dividers: distribute evenly
                for (int i = 0; i < requiredDividers; i++) {
                    double position = (i + 1.0) / (requiredDividers + 1.0);
                    dividerPositions.add(new SimpleDoubleProperty(position));
                }
            } else {
                // We have existing dividers: add new ones without changing existing positions
                // Calculate the average gap and insert new dividers in the largest gaps
                while (dividerPositions.size() < requiredDividers) {
                    // Find the largest gap between dividers (or edges)
                    double largestGap = 0.0;
                    int insertIndex = 0;

                    // Check gap before first divider
                    if (!dividerPositions.isEmpty()) {
                        double gapBefore = dividerPositions.get(0).get();
                        if (gapBefore > largestGap) {
                            largestGap = gapBefore;
                            insertIndex = 0;
                        }
                    }

                    // Check gaps between dividers
                    for (int i = 0; i < dividerPositions.size() - 1; i++) {
                        double gap = dividerPositions.get(i + 1).get() - dividerPositions.get(i).get();
                        if (gap > largestGap) {
                            largestGap = gap;
                            insertIndex = i + 1;
                        }
                    }

                    // Check gap after last divider
                    if (!dividerPositions.isEmpty()) {
                        double gapAfter = 1.0 - dividerPositions.get(dividerPositions.size() - 1).get();
                        if (gapAfter > largestGap) {
                            largestGap = gapAfter;
                            insertIndex = dividerPositions.size();
                        }
                    }

                    // Insert new divider in the middle of the largest gap
                    double newPosition;
                    if (insertIndex == 0) {
                        if (dividerPositions.isEmpty()) {
                            newPosition = 0.5;
                        } else {
                            newPosition = dividerPositions.get(0).get() / 2.0;
                        }
                    } else if (insertIndex == dividerPositions.size()) {
                        double lastPos = dividerPositions.get(dividerPositions.size() - 1).get();
                        newPosition = lastPos + (1.0 - lastPos) / 2.0;
                    } else {
                        double prevPos = dividerPositions.get(insertIndex - 1).get();
                        double nextPos = dividerPositions.get(insertIndex).get();
                        newPosition = prevPos + (nextPos - prevPos) / 2.0;
                    }

                    dividerPositions.add(insertIndex, new SimpleDoubleProperty(newPosition));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "DockSplitPane{id='" + id + "', orientation=" + orientation + ", children=" + children.size() + "}";
    }
}

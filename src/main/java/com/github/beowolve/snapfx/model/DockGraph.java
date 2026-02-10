package com.github.beowolve.snapfx.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Central data structure of the docking system.
 * Holds the root of the logical dock tree.
 * Manages automatic generation of unique layout IDs for all nodes.
 */
public class DockGraph {
    private final BooleanProperty locked;
    private final LongProperty revision;
    private final ObjectProperty<DockElement> root;
    private long layoutIdCounter = 0; // Counter for generating unique layout IDs

    public DockGraph() {
        this.locked = new SimpleBooleanProperty(false);
        this.revision = new SimpleLongProperty(0);
        this.root = new SimpleObjectProperty<>(null);
    }

    public LongProperty revisionProperty() {
        return revision;
    }

    public long getRevision() {
        return revision.get();
    }

    private void bumpRevision() {
        revision.set(revision.get() + 1);
    }

    public DockElement getRoot() {
        return root.get();
    }

    public ObjectProperty<DockElement> rootProperty() {
        return root;
    }

    public void setRoot(DockElement newRoot) {
        if (newRoot != null) {
            newRoot.setParent(null);
            // Assign layout IDs to all nodes in the tree
            assignLayoutIds(newRoot);
        }
        root.set(newRoot);
        bumpRevision();
    }

    /**
     * Generates a new unique layout ID.
     */
    private String generateLayoutId() {
        return "dock-" + (++layoutIdCounter);
    }

    /**
     * Assigns unique layout IDs to all DockNodes in the tree that don't have one yet.
     */
    private void assignLayoutIds(DockElement element) {
        if (element instanceof DockNode node) {
            if (node.getId() == null || node.getId().equals(node.getDockNodeId())) {
                node.setLayoutId(generateLayoutId());
            }
        } else if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                assignLayoutIds(child);
            }
        }
    }

    /**
     * Resets the layout ID counter to a specific value.
     * Used during deserialization to continue from the highest ID in the loaded layout.
     */
    public void setLayoutIdCounter(long counter) {
        this.layoutIdCounter = counter;
    }

    /**
     * Returns the current layout ID counter value.
     */
    public long getLayoutIdCounter() {
        return layoutIdCounter;
    }

    public boolean isLocked() {
        return locked.get();
    }

    public BooleanProperty lockedProperty() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked.set(locked);
    }

    /**
     * Docks a node at a specific position relative to a target element.
     */
    public void dock(DockNode node, DockElement target, DockPosition position) {
        if (node == null) {
            return;
        }

        // Assign layout ID if not already set
        if (node.getId() == null || node.getId().equals(node.getDockNodeId())) {
            node.setLayoutId(generateLayoutId());
        }

        if (target == node) {
            return; // No-op: docking onto itself
        }

        if (target != null && isAncestor(node, target)) {
            return; // No-op: cannot dock into own subtree
        }

        if (target == null) {
            // First element becomes the root
            setRoot(node);
            return;
        }

        if (position == DockPosition.CENTER) {
            dockAsTab(node, target);
        } else {
            dockAsSplit(node, target, position);
        }

        bumpRevision();
    }

    private void dockAsTab(DockNode node, DockElement target) {
        DockContainer parent = target.getParent();

        // Optimization: If target is already in a TabPane, add node directly to that TabPane
        if (parent instanceof DockTabPane existingTabPane) {
            // Find the index of the target
            int targetIndex = existingTabPane.getChildren().indexOf(target);

            // Add the new node right after the target
            if (targetIndex >= 0 && targetIndex < existingTabPane.getChildren().size() - 1) {
                existingTabPane.getChildren().add(targetIndex + 1, node);
                node.setParent(existingTabPane);
            } else {
                // Add at the end
                existingTabPane.addChild(node);
            }

            // Select the newly added tab
            existingTabPane.setSelectedIndex(existingTabPane.getChildren().indexOf(node));

            bumpRevision();
            return;
        }

        if (target instanceof DockNode) {
            // Create a TabPane that contains both nodes
            DockTabPane tabPane = new DockTabPane();

            if (parent != null) {
                // Compute index before removal
                int index = parent.getChildren().indexOf(target);
                // Remove target directly from the children list without triggering cleanup
                if (index >= 0) {
                    parent.getChildren().remove(index);
                    target.setParent(null);
                }
                // Insert TabPane at the correct position
                if (index >= 0 && index <= parent.getChildren().size()) {
                    parent.getChildren().add(index, tabPane);
                } else {
                    parent.addChild(tabPane);
                }
                tabPane.setParent(parent);
            } else {
                // Target is root
                setRoot(tabPane);
            }

            tabPane.addChild(target);
            tabPane.addChild(node);

            // Select the newly added tab (index 1, since we added target first)
            tabPane.setSelectedIndex(1);

        } else if (target instanceof DockTabPane tabPane) {
            // Add directly to the TabPane
            tabPane.addChild(node);

            // Select the newly added tab (last index)
            tabPane.setSelectedIndex(tabPane.getChildren().size() - 1);
        } else if (target instanceof DockSplitPane) {
            // If target is a SplitPane, we can't add as tab directly
            // This should not happen in normal usage, but handle gracefully
            // by wrapping the target and new node in a TabPane
            DockTabPane tabPane = new DockTabPane();
            DockContainer targetParent = target.getParent();

            if (targetParent != null) {
                int index = targetParent.getChildren().indexOf(target);
                if (index >= 0) {
                    targetParent.getChildren().remove(index);
                    target.setParent(null);
                }
                if (index >= 0 && index <= targetParent.getChildren().size()) {
                    targetParent.getChildren().add(index, tabPane);
                } else {
                    targetParent.addChild(tabPane);
                }
                tabPane.setParent(targetParent);
            } else {
                setRoot(tabPane);
            }

            tabPane.addChild(target);
            tabPane.addChild(node);
            tabPane.setSelectedIndex(1);
        }
    }

    private void dockAsSplit(DockNode node, DockElement target, DockPosition position) {
        javafx.geometry.Orientation orientation =
            (position == DockPosition.LEFT || position == DockPosition.RIGHT)
                ? javafx.geometry.Orientation.HORIZONTAL
                : javafx.geometry.Orientation.VERTICAL;

        DockContainer parent = target.getParent();

        // Optimization: If parent is already a SplitPane with the same orientation,
        // add the node directly to the parent instead of creating a nested SplitPane
        if (parent instanceof DockSplitPane parentSplit && parentSplit.getOrientation() == orientation) {
            int targetIndex = parent.getChildren().indexOf(target);

            // Store existing divider positions before modification
            List<Double> savedPositions = new ArrayList<>();
            for (var divProp : parentSplit.getDividerPositions()) {
                savedPositions.add(divProp.get());
            }

            // Insert new node at the correct position relative to target
            int insertIndex;
            if (position == DockPosition.LEFT || position == DockPosition.TOP) {
                insertIndex = targetIndex;
            } else {
                insertIndex = targetIndex + 1;
            }

            parent.getChildren().add(insertIndex, node);
            node.setParent(parent);

            // Calculate new divider positions intelligently
            try {
                java.lang.reflect.Method updateMethod = DockSplitPane.class.getDeclaredMethod("updateDividerPositions");
                updateMethod.setAccessible(true);
                updateMethod.invoke(parentSplit);

                // Now adjust the new divider based on where we inserted
                // The updateDividerPositions() method adds a divider in the largest gap
                // But we want to insert it at a specific position to minimize layout changes

                int newDividerCount = parentSplit.getDividerPositions().size();
                if (newDividerCount == savedPositions.size() + 1) {
                    // A new divider was added - adjust positions to minimize change
                    List<Double> newPositions = new ArrayList<>();

                    if (insertIndex == 0) {
                        // Inserted at the beginning
                        // New element takes a portion from the first element
                        double firstDividerPos = savedPositions.isEmpty() ? 1.0 : savedPositions.getFirst();
                        double newDividerPos = firstDividerPos * 0.5; // Split the first section
                        newPositions.add(newDividerPos);
                        // Existing dividers keep their positions
                        newPositions.addAll(savedPositions);
                    } else if (insertIndex == parent.getChildren().size() - 1) {
                        // Inserted at the end
                        // New element takes a portion from the last element
                        double lastDividerPos = savedPositions.isEmpty() ? 0.0 : savedPositions.getLast();
                        // Existing dividers keep their positions
                        newPositions.addAll(savedPositions);
                        double newDividerPos = lastDividerPos + (1.0 - lastDividerPos) * 0.5;
                        newPositions.add(newDividerPos);
                    } else {
                        // Inserted in the middle
                        // New element splits the space between two dividers
                        double prevDividerPos = (insertIndex - 1) < savedPositions.size() ? savedPositions.get(insertIndex - 1) : 0.0;
                        double nextDividerPos = insertIndex < savedPositions.size() ? savedPositions.get(insertIndex) : 1.0;

                        // Keep all dividers before insertion point
                        for (int i = 0; i < insertIndex - 1; i++) {
                            newPositions.add(savedPositions.get(i));
                        }

                        // Add two new dividers that split the space
                        double midPoint = (prevDividerPos + nextDividerPos) / 2.0;
                        newPositions.add(midPoint);

                        // Keep all dividers after insertion point
                        for (int i = insertIndex - 1; i < savedPositions.size(); i++) {
                            newPositions.add(savedPositions.get(i));
                        }
                    }

                    // Apply new positions
                    for (int i = 0; i < newPositions.size() && i < parentSplit.getDividerPositions().size(); i++) {
                        parentSplit.setDividerPosition(i, newPositions.get(i));
                    }
                }
            } catch (Exception e) {
                // Fallback: dividers will be calculated by default algorithm
            }

            bumpRevision();
            return;
        }

        // Standard case: create new SplitPane
        DockSplitPane splitPane = new DockSplitPane(orientation);

        if (parent != null) {
            // Compute index before removal
            int index = parent.getChildren().indexOf(target);
            // Remove target directly from the children list without triggering cleanup
            if (index >= 0) {
                parent.getChildren().remove(index);
                target.setParent(null);
            }
            // Insert SplitPane at the correct position
            if (index >= 0 && index <= parent.getChildren().size()) {
                parent.getChildren().add(index, splitPane);
            } else {
                parent.addChild(splitPane);
            }
            splitPane.setParent(parent);
        } else {
            // Target is root
            setRoot(splitPane);
        }

        // Add children in the correct order
        if (position == DockPosition.LEFT || position == DockPosition.TOP) {
            splitPane.addChild(node);
            splitPane.addChild(target);
        } else {
            splitPane.addChild(target);
            splitPane.addChild(node);
        }
    }

    /**
     * Removes a DockNode from the graph.
     */
    public void undock(DockNode node) {
        if (node == getRoot()) {
            setRoot(null);
            return;
        }

        if (node.getParent() == null) {
            return; // Node is not in the graph
        }

        boolean rootWasParent = (node.getParent() == getRoot());
        node.removeFromParent();

        // Always normalize root after removal because containers may flatten themselves.
        DockElement newRoot = getRoot();
        if (getRoot() instanceof DockContainer container) {
            if (container.getChildren().isEmpty()) {
                // Check if TabPane/SplitPane stored a flattened child
                if (container instanceof DockTabPane tabPane) {
                    newRoot = tabPane.getFlattenedChild();
                } else if (container instanceof DockSplitPane splitPane) {
                    newRoot = splitPane.getFlattenedChild();
                } else {
                    newRoot = null;
                }
            } else if (container.getChildren().size() == 1) {
                newRoot = container.getChildren().getFirst();
            }
        }

        if (rootWasParent || newRoot != getRoot()) {
            setRoot(newRoot);
        } else {
            bumpRevision();
        }
    }

    /**
     * After each tree manipulation: check the root and update it if needed.
     *
     * @deprecated Prefer relying on explicit operations (dock/undock/move) which already keep the root consistent.
     */
    @Deprecated
    public void checkAndUpdateRoot() {
        if (root instanceof DockContainer container) {
            if (container.getChildren().isEmpty()) {
                setRoot(null);
            } else if (container.getChildren().size() == 1) {
                setRoot(container.getChildren().getFirst());
            }
        }
    }

    /**
     * Moves a DockNode from one position to another.
     */
    public void move(DockNode node, DockElement target, DockPosition position) {
        if (node == null) {
            return;
        }
        if (target == null || position == null) {
            return;
        }

        if (target == node) {
            return; // No-op: drop to the same element
        }
        if (isAncestor(node, target)) {
            return; // No-op: would move node into its own subtree
        }

        DockContainer sourceParent = node.getParent();
        DockContainer targetParent = target.getParent();

        // Special case: Moving within the same SplitPane with the same orientation
        // Check if we're trying to dock in a way that would keep it in the same parent
        if (position != DockPosition.CENTER && sourceParent instanceof DockSplitPane sourceSplit) {
            javafx.geometry.Orientation requiredOrientation =
                (position == DockPosition.LEFT || position == DockPosition.RIGHT)
                    ? javafx.geometry.Orientation.HORIZONTAL
                    : javafx.geometry.Orientation.VERTICAL;

            // Check if target's parent is the same SplitPane with matching orientation
            if (targetParent == sourceParent && sourceSplit.getOrientation() == requiredOrientation) {
                // Move within the same SplitPane - preserve all divider positions
                int sourceIndex = sourceParent.getChildren().indexOf(node);
                int targetIndex = sourceParent.getChildren().indexOf(target);

                if (sourceIndex == targetIndex) {
                    // Dropping on itself - no-op
                    return;
                }

                // Calculate new index based on position
                int newIndex;
                if (position == DockPosition.LEFT || position == DockPosition.TOP) {
                    newIndex = targetIndex;
                } else {
                    newIndex = targetIndex + 1;
                }

                // Adjust if source is before target
                if (sourceIndex < newIndex) {
                    newIndex--;
                }

                if (sourceIndex == newIndex) {
                    // Already at target position
                    return;
                }

                // Store divider positions before move
                List<Double> savedPositions = new ArrayList<>();
                for (var divProp : sourceSplit.getDividerPositions()) {
                    savedPositions.add(divProp.get());
                }

                // Move element in list
                sourceParent.getChildren().remove(sourceIndex);
                sourceParent.getChildren().add(newIndex, node);

                // Restore divider positions
                for (int i = 0; i < savedPositions.size() && i < sourceSplit.getDividerPositions().size(); i++) {
                    sourceSplit.setDividerPosition(i, savedPositions.get(i));
                }

                bumpRevision();
                return;
            }
        }

        // Check if we're dropping back to the exact same position (e.g., Project Explorer on its current position)
        if (sourceParent == targetParent && position != DockPosition.CENTER) {
            int nodeIndex = sourceParent.getChildren().indexOf(node);
            int targetIndex = sourceParent.getChildren().indexOf(target);

            javafx.geometry.Orientation requiredOrientation =
                (position == DockPosition.LEFT || position == DockPosition.RIGHT)
                    ? javafx.geometry.Orientation.HORIZONTAL
                    : javafx.geometry.Orientation.VERTICAL;

            if (sourceParent instanceof DockSplitPane split && split.getOrientation() == requiredOrientation) {
                int expectedIndex = (position == DockPosition.LEFT || position == DockPosition.TOP) ? targetIndex : targetIndex + 1;
                if (nodeIndex < expectedIndex) expectedIndex--;

                if (nodeIndex == expectedIndex) {
                    // Already at the exact target position - no-op
                    return;
                }
            }
        }

        // Standard move: undock and re-dock
        // IMPORTANT: After undock, the target might become invalid due to flattening.
        // We need to find the target again in the (potentially changed) tree.

        // Store target's ID to find it again after undock
        String targetId = target.getId();

        undock(node);

        // If the graph was emptied by undocking the last node, restore as root
        if (getRoot() == null) {
            setRoot(node);
            return;
        }

        // Find target again in case it moved due to flattening
        DockElement currentTarget = findElementById(getRoot(), targetId);

        // If target is no longer in the tree (very unlikely but possible),
        // dock to root instead
        if (currentTarget == null) {
            currentTarget = getRoot();
            // Adjust position if needed - if root is not a container, use a safe default
            if (!(currentTarget instanceof DockContainer) && position != DockPosition.CENTER) {
                position = DockPosition.RIGHT; // Safe default
            }
        }

        dock(node, currentTarget, position);
    }

    /**
     * Finds an element by ID in the tree.
     */
    private DockElement findElementById(DockElement root, String id) {
        if (root == null || id == null) {
            return null;
        }

        if (id.equals(root.getId())) {
            return root;
        }

        if (root instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                DockElement found = findElementById(child, id);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean isAncestor(DockElement ancestorCandidate, DockElement element) {
        if (ancestorCandidate == null || element == null) {
            return false;
        }
        DockElement current = element;
        while (current.getParent() != null) {
            DockContainer parent = current.getParent();
            if (parent == ancestorCandidate) {
                return true;
            }
            current = parent;
        }
        return false;
    }

    public int getDockNodeCount(String dockNodeId) {
        if (getRoot() == null || dockNodeId == null) {
            return 0;
        }
        return countDockNodeId(getRoot(), dockNodeId);
    }

    private int countDockNodeId(DockElement root, String dockNodeId) {
        int count = 0;
        if (root instanceof DockNode node) {
            if (dockNodeId.equals(node.getDockNodeId())) {
                count++;
            }
        } else if (root instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                count += countDockNodeId(child, dockNodeId);
            }
        }
        return count;
    }
}

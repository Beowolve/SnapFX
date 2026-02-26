package com.github.beowolve.snapfx.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Side;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Central data structure of the docking system.
 * Holds the root of the logical dock tree.
 * Manages automatic generation of unique layout IDs for all nodes.
 * Provides methods for docking, undocking, and moving nodes within the tree.
 */
public class DockGraph {
    /**
     * Default preferred width (in pixels) for sidebar panels.
     */
    public static final double DEFAULT_SIDE_BAR_PANEL_WIDTH = 300.0;

    private final BooleanProperty locked;
    private final LongProperty revision;
    private final ObjectProperty<DockElement> root;
    private final EnumMap<Side, ObservableList<DockNode>> sideBarNodes;
    private final EnumMap<Side, ObservableList<DockNode>> readOnlySideBarNodes;
    private final EnumMap<Side, BooleanProperty> sideBarPinnedOpen;
    private final EnumMap<Side, DoubleProperty> sideBarPanelWidths;
    private long layoutIdCounter = 0; // Counter for generating unique layout IDs

    public DockGraph() {
        this.locked = new SimpleBooleanProperty(false);
        this.revision = new SimpleLongProperty(0);
        this.root = new SimpleObjectProperty<>(null);
        this.sideBarNodes = new EnumMap<>(Side.class);
        this.readOnlySideBarNodes = new EnumMap<>(Side.class);
        this.sideBarPinnedOpen = new EnumMap<>(Side.class);
        this.sideBarPanelWidths = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            ObservableList<DockNode> nodes = FXCollections.observableArrayList();
            sideBarNodes.put(side, nodes);
            readOnlySideBarNodes.put(side, FXCollections.unmodifiableObservableList(nodes));
            sideBarPinnedOpen.put(side, new SimpleBooleanProperty(false));
            sideBarPanelWidths.put(side, new SimpleDoubleProperty(DEFAULT_SIDE_BAR_PANEL_WIDTH));
        }
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

    /**
     * Returns the root element of the dock graph, or null if the graph is empty.
     */
    public DockElement getRoot() {
        return root.get();
    }

    /**
     * Returns the root property of the dock graph.
     */
    public ObjectProperty<DockElement> rootProperty() {
        return root;
    }

    /**
     * Sets the root of the dock graph. This will replace the entire layout tree.
     * All nodes in the new tree will be assigned unique layout IDs if they don't have one already.
     * @param newRoot The new root element, or null to clear the graph
     */
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

    /**
     * Returns true if the graph is currently locked. When locked, all mutating operations (dock, undock, move) are no-ops.
     * This can be used to temporarily disable updates while performing batch modifications or during layout loading.
     */
    public boolean isLocked() {
        return locked.get();
    }

    /**
     * Returns the locked property. When locked, all mutating operations (dock, undock, move) are no-ops.
     * This can be used to temporarily disable updates while performing batch modifications or during layout loading.
     */
    public BooleanProperty lockedProperty() {
        return locked;
    }

    /**
     * Locks or unlocks the graph. When locked, all mutating operations (dock, undock, move) are no-ops.
     * This can be used to temporarily disable updates while performing batch modifications or during layout loading.
     */
    public void setLocked(boolean locked) {
        this.locked.set(locked);
    }

    /**
     * Returns the read-only list of pinned nodes for a sidebar side.
     */
    public ObservableList<DockNode> getSideBarNodes(Side side) {
        if (side == null) {
            return FXCollections.unmodifiableObservableList(FXCollections.observableArrayList());
        }
        return readOnlySideBarNodes.getOrDefault(side, FXCollections.unmodifiableObservableList(FXCollections.observableArrayList()));
    }

    /**
     * Returns whether the sidebar for the given side is pinned-open (layout-consuming).
     */
    public boolean isSideBarPinnedOpen(Side side) {
        if (side == null) {
            return false;
        }
        BooleanProperty pinnedOpenProperty = sideBarPinnedOpen.get(side);
        return pinnedOpenProperty != null && pinnedOpenProperty.get();
    }

    /**
     * Returns the pinned-open state property for a sidebar side.
     */
    public BooleanProperty sideBarPinnedOpenProperty(Side side) {
        if (side == null) {
            return new SimpleBooleanProperty(false);
        }
        return sideBarPinnedOpen.computeIfAbsent(side, ignored -> new SimpleBooleanProperty(false));
    }

    /**
     * Sets whether a sidebar is pinned-open (layout-consuming) or collapsed to the icon strip.
     */
    public void setSideBarPinnedOpen(Side side, boolean pinnedOpen) {
        if (side == null || isLocked()) {
            return;
        }
        BooleanProperty property = sideBarPinnedOpen.get(side);
        if (property == null || property.get() == pinnedOpen) {
            return;
        }
        property.set(pinnedOpen);
        bumpRevision();
    }

    /**
     * Convenience method to pin-open a sidebar.
     */
    public void pinOpenSideBar(Side side) {
        setSideBarPinnedOpen(side, true);
    }

    /**
     * Convenience method to collapse a pinned sidebar back to strip mode.
     */
    public void collapsePinnedSideBar(Side side) {
        setSideBarPinnedOpen(side, false);
    }

    /**
     * Returns the preferred sidebar panel width for the given side.
     *
     * <p>This is a persisted preference value. View hosts (for example {@code SnapFX}) may apply additional runtime
     * clamping depending on scene size and resize policies.</p>
     */
    public double getSideBarPanelWidth(Side side) {
        if (side == null) {
            return DEFAULT_SIDE_BAR_PANEL_WIDTH;
        }
        DoubleProperty property = sideBarPanelWidths.get(side);
        if (property == null) {
            return DEFAULT_SIDE_BAR_PANEL_WIDTH;
        }
        double width = property.get();
        return Double.isFinite(width) && width > 0.0 ? width : DEFAULT_SIDE_BAR_PANEL_WIDTH;
    }

    /**
     * Returns the preferred sidebar panel width property for the given side.
     *
     * <p>The property stores the persisted preferred width. Consumers should clamp it at render time as needed.</p>
     */
    public DoubleProperty sideBarPanelWidthProperty(Side side) {
        if (side == null) {
            return new SimpleDoubleProperty(DEFAULT_SIDE_BAR_PANEL_WIDTH);
        }
        return sideBarPanelWidths.computeIfAbsent(side, ignored -> new SimpleDoubleProperty(DEFAULT_SIDE_BAR_PANEL_WIDTH));
    }

    /**
     * Sets the preferred sidebar panel width for the given side.
     *
     * <p>This updates a persisted view preference and is intentionally allowed while the graph is locked because it
     * does not mutate the docking structure.</p>
     */
    public void setSideBarPanelWidth(Side side, double width) {
        if (side == null || !Double.isFinite(width) || width <= 0.0) {
            return;
        }
        DoubleProperty property = sideBarPanelWidthProperty(side);
        if (Double.compare(property.get(), width) == 0) {
            return;
        }
        property.set(width);
    }

    /**
     * Pins a node to a sidebar, removing it from the main layout if necessary.
     * The last main-layout placement is captured for deterministic restore behavior.
     *
     * <p>Pinning preserves the sidebar's current pinned-open/collapsed state. Newly pinned nodes therefore stay
     * collapsed by default unless callers explicitly open the sidebar via {@link #pinOpenSideBar(Side)}.</p>
     */
    public void pinToSideBar(DockNode node, Side side) {
        pinToSideBarInternal(node, side, null);
    }

    /**
     * Pins a node to a sidebar and inserts it at the requested sidebar index.
     *
     * <p>If the node is already pinned on the same side, this reorders the sidebar entry. The index is clamped
     * into the valid insertion range {@code [0..size]}.</p>
     */
    public void pinToSideBar(DockNode node, Side side, int index) {
        pinToSideBarInternal(node, side, index);
    }

    private void pinToSideBarInternal(DockNode node, Side side, Integer desiredIndex) {
        if (node == null || side == null || isLocked()) {
            return;
        }

        if (node.getId() == null || node.getId().equals(node.getDockNodeId())) {
            node.setLayoutId(generateLayoutId());
        }

        Side currentSide = findPinnedSide(node);
        if (currentSide == side) {
            if (desiredIndex == null) {
                return;
            }
            ObservableList<DockNode> entries = sideBarNodes.get(side);
            if (entries == null || !moveSideBarEntry(entries, node, desiredIndex)) {
                return;
            }
            bumpRevision();
            return;
        }

        if (currentSide != null) {
            ObservableList<DockNode> currentEntries = sideBarNodes.get(currentSide);
            ObservableList<DockNode> targetEntries = sideBarNodes.get(side);
            if (currentEntries == null || targetEntries == null) {
                return;
            }
            if (!currentEntries.remove(node)) {
                return;
            }
            addSideBarEntry(targetEntries, node, desiredIndex);
            bumpRevision();
            return;
        }

        if (isInMainTree(node)) {
            rememberLastKnownPlacementForSideBarRestore(node);
            undock(node);
        }

        ObservableList<DockNode> targetEntries = sideBarNodes.get(side);
        if (targetEntries == null || targetEntries.contains(node)) {
            return;
        }
        addSideBarEntry(targetEntries, node, desiredIndex);
        bumpRevision();
    }

    /**
     * Restores a pinned sidebar node back to the main layout using remembered placement or a root fallback.
     */
    public void restoreFromSideBar(DockNode node) {
        if (node == null || isLocked()) {
            return;
        }
        if (!unpinFromSideBar(node)) {
            return;
        }

        if (!tryRestorePinnedNodeToRememberedPlacement(node)) {
            if (getRoot() == null) {
                setRoot(node);
            } else {
                dock(node, getRoot(), DockPosition.RIGHT);
            }
        }
    }

    /**
     * Returns whether a node is currently pinned in any sidebar.
     */
    public boolean isPinnedToSideBar(DockNode node) {
        return findPinnedSide(node) != null;
    }

    /**
     * Returns the sidebar side the node is pinned to, or {@code null} if it is not pinned.
     */
    public Side getPinnedSide(DockNode node) {
        return findPinnedSide(node);
    }

    /**
     * Removes a node from its sidebar without restoring it to the main layout.
     *
     * <p>This is primarily used by higher-level hosts (for example {@code SnapFX}) that apply a custom
     * restore strategy after removing the node from the sidebar.</p>
     *
     * @return {@code true} if the node was removed from a sidebar; otherwise {@code false}
     */
    public boolean unpinFromSideBar(DockNode node) {
        if (node == null || isLocked()) {
            return false;
        }
        Side side = findPinnedSide(node);
        if (side == null) {
            return false;
        }

        ObservableList<DockNode> entries = sideBarNodes.get(side);
        if (entries == null || !entries.remove(node)) {
            return false;
        }
        bumpRevision();
        return true;
    }

    /**
     * Removes all pinned sidebar nodes and resets pinned-open sidebar state.
     * Nodes are not restored to the main layout.
     */
    public void clearSideBars() {
        if (isLocked()) {
            return;
        }
        clearSideBarsInternal(true);
    }

    /**
     * Docks a node at a specific position relative to a target element.
     * @param node The node to dock
     * @param target The target element to dock relative to (can be null if docking the first node)
     * @param position The position relative to the target to dock at
     */
    public void dock(DockNode node, DockElement target, DockPosition position) {
        dock(node, target, position, null);
    }

    /**
     * Docks a node at a specific position relative to a target element with optional tab index.
     * The tab index is only applicable when docking as a tab (position = CENTER) and will be ignored otherwise.
     * If position is CENTER and the target is not already a TabPane, a new TabPane will be created to hold both the target and the new node.
     * If position is CENTER and the target is already a TabPane, the new node will be added to that TabPane at the specified index (or right after the target if index is null).
     * If position is not CENTER, the node will be docked to the edge of the target as specified.
     * @param node The node to dock
     * @param target The target element to dock relative to (can be null if docking the first node)
     * @param position The position relative to the target to dock at
     * @param tabIndex Optional index to insert the new tab at (only applies if position is CENTER and target is a TabPane or already in a TabPane). If null, the new tab will be added right after the target tab (or at the end if target is not a tab itself).
     */
    public void dock(DockNode node, DockElement target, DockPosition position, Integer tabIndex) {
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
            dockAsTab(node, target, tabIndex);
        } else {
            dockAsSplit(node, target, position);
        }

        bumpRevision();
    }

    /**
     * Docks a node as a new tab relative to the target element.
     * If the target is not already in a TabPane, a new TabPane will be created to hold both the target and the new node.
     * If the target is already in a TabPane, the new node will be added to that TabPane.
     * @param node The node to dock
     * @param target The target element to dock relative to
     * @param tabIndex Optional index to insert the new tab at (only applies if target is a TabPane or already in a TabPane). If null, the new tab will be added right after the target tab (or at the end if target is not a tab itself).
     */
    private void dockAsTab(DockNode node, DockElement target, Integer tabIndex) {
        DockContainer parent = target.getParent();

        // Optimization: If target is already in a TabPane, add node directly to that TabPane
        if (parent instanceof DockTabPane existingTabPane) {
            dockToParentTabPane(node, target, tabIndex, existingTabPane);
            return;
        }

        switch (target) {
            case DockNode ignored -> replaceTargetWithTabPane(node, target, tabIndex, parent);
            case DockTabPane tabPane -> dockToTargetTabPane(node, tabIndex, tabPane);
            case DockSplitPane ignored -> handleDockAsTabToSplitPane(node, target, tabIndex);
            default -> {
                // Unsupported target type - should not happen
            }
        }
    }

    /**
     * Handles docking as tab when the target is a SplitPane (which cannot contain tabs directly).
     * @param node The node to dock
     * @param target The target SplitPane to dock relative to
     * @param tabIndex Optional index to insert the new tab at (only applies if target is a TabPane or already in a TabPane). If null, the new tab will be added right after the target tab (or at the end if target is not a tab itself).
     */
    private void handleDockAsTabToSplitPane(DockNode node, DockElement target, Integer tabIndex) {
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

        if (tabIndex != null && tabIndex <= 0) {
            tabPane.addChild(node);
            tabPane.addChild(target);
        } else {
            tabPane.addChild(target);
            tabPane.addChild(node);
        }
        tabPane.setSelectedIndex(tabPane.getChildren().indexOf(node));
    }

    /**
     * Docks a node directly to an existing TabPane.
     * @param node The node to dock
     * @param tabIndex Optional index to insert the new tab at. If null, the new tab will be added right after the target tab (or at the end if target is not a tab itself).
     * @param tabPane The TabPane to dock into
     */
    private static void dockToTargetTabPane(DockNode node, Integer tabIndex, DockTabPane tabPane) {
        // Add directly to the TabPane
        if (tabIndex != null) {
            tabPane.addChild(node, tabIndex);
        } else {
            tabPane.addChild(node);
        }

        // Select the newly added tab (last index)
        tabPane.setSelectedIndex(tabPane.getChildren().indexOf(node));
    }

    /**
     * Replaces the target element with a new TabPane that contains both the target and the new node.
     * @param node The node to dock
     * @param target The target element to dock relative to
     * @param tabIndex Optional index to insert the new tab at. If null, the new tab will be added right after the target tab (or at the end if target is not a tab itself).
     * @param parent The parent container of the target element, or null if the target is root
     */
    private void replaceTargetWithTabPane(DockNode node, DockElement target, Integer tabIndex, DockContainer parent) {
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

        if (tabIndex != null && tabIndex <= 0) {
            tabPane.addChild(node);
            tabPane.addChild(target);
        } else {
            tabPane.addChild(target);
            tabPane.addChild(node);
        }

        // Select the newly added tab (index 1, since we added target first)
        tabPane.setSelectedIndex(tabPane.getChildren().indexOf(node));
    }

    /**
     * Docks a node to an existing TabPane that already contains the target element.
     * @param node The node to dock
     * @param target The target element to dock relative to (must be a child of the existing TabPane)
     * @param tabIndex Optional index to insert the new tab at. If null, the new tab will be added right after the target tab (or at the end if target is not a tab itself).
     * @param existingTabPane The existing TabPane that already contains the target element
     */
    private void dockToParentTabPane(DockNode node, DockElement target, Integer tabIndex, DockTabPane existingTabPane) {
        if (tabIndex != null) {
            existingTabPane.addChild(node, tabIndex);
        } else {
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
        }

        // Select the newly added tab
        existingTabPane.setSelectedIndex(existingTabPane.getChildren().indexOf(node));
    }

    private void dockAsSplit(DockNode node, DockElement target, DockPosition position) {
        Orientation orientation = getOrientationFromDockPosition(position);

        DockElement effectiveTarget = resolveSplitDockTarget(target, position, orientation);
        DockContainer parent = effectiveTarget.getParent();

        // Optimization: If parent is already a SplitPane with the same orientation,
        // add the node directly to the parent instead of creating a nested SplitPane
        if (parent instanceof DockSplitPane parentSplit && parentSplit.getOrientation() == orientation) {
            int targetIndex = parent.getChildren().indexOf(effectiveTarget);

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

            parentSplit.updateDividerPositions();

            int newDividerCount = parentSplit.getDividerPositions().size();
            if (newDividerCount == savedPositions.size() + 1) {
                // Insert a new divider inside the target segment to keep existing positions intact.
                int newDividerIndex = (position == DockPosition.LEFT || position == DockPosition.TOP)
                    ? insertIndex
                    : insertIndex - 1;

                double prevBoundary = newDividerIndex > 0 ? savedPositions.get(newDividerIndex - 1) : 0.0;
                double nextBoundary = newDividerIndex < savedPositions.size() ? savedPositions.get(newDividerIndex) : 1.0;
                double newDividerPos = prevBoundary + (nextBoundary - prevBoundary) / 2.0;

                List<Double> newPositions = new ArrayList<>(savedPositions.size() + 1);
                for (int i = 0; i < savedPositions.size(); i++) {
                    if (i == newDividerIndex) {
                        newPositions.add(newDividerPos);
                    }
                    newPositions.add(savedPositions.get(i));
                }
                if (newDividerIndex == savedPositions.size()) {
                    newPositions.add(newDividerPos);
                }

                for (int i = 0; i < newPositions.size() && i < parentSplit.getDividerPositions().size(); i++) {
                    parentSplit.setDividerPosition(i, newPositions.get(i));
                }
            }

            bumpRevision();
            return;
        }

        // Standard case: create new SplitPane
        DockSplitPane splitPane = new DockSplitPane(orientation);

        if (parent != null) {
            // Compute index before removal
            int index = parent.getChildren().indexOf(effectiveTarget);
            // Remove target directly from the children list without triggering cleanup
            if (index >= 0) {
                parent.getChildren().remove(index);
                effectiveTarget.setParent(null);
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
            splitPane.addChild(effectiveTarget);
        } else {
            splitPane.addChild(effectiveTarget);
            splitPane.addChild(node);
        }
    }

    /**
     * Determines the required SplitPane orientation based on the dock position.
     */
    private static Orientation getOrientationFromDockPosition(DockPosition position) {
        return (position == DockPosition.LEFT || position == DockPosition.RIGHT)
                ? Orientation.HORIZONTAL
                : Orientation.VERTICAL;
    }

    private DockElement resolveSplitDockTarget(DockElement target, DockPosition position, Orientation orientation) {
        if (!(target instanceof DockSplitPane splitPane)) {
            return target;
        }
        if (splitPane.getOrientation() != orientation || splitPane.getChildren().isEmpty()) {
            return target;
        }
        if (position == DockPosition.LEFT || position == DockPosition.TOP) {
            return splitPane.getChildren().getFirst();
        }
        return splitPane.getChildren().getLast();
    }

    /**
     * Removes a DockNode from the graph.
     */
    public void undock(DockNode node) {
        if (node == null) {
            return;
        }
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
        DockElement newRoot = getNormalizedRoot();

        if (rootWasParent || newRoot != getRoot()) {
            setRoot(newRoot);
        } else {
            bumpRevision();
        }
    }

    /**
     * After undocking, the parent container may have flattened itself if it had only one remaining child.
     * This method checks for that case and returns the new root if it changed, or the existing root if not.
     */
    private DockElement getNormalizedRoot() {
        DockElement newRoot = getRoot();
        if (getRoot() instanceof DockContainer container) {
            if (container.getChildren().isEmpty()) {
                // Check if TabPane/SplitPane stored a flattened child
                newRoot = switch (container) {
                    case DockTabPane tabPane -> tabPane.getFlattenedChild();
                    case DockSplitPane splitPane -> splitPane.getFlattenedChild();
                    default -> null;
                };
            } else if (container.getChildren().size() == 1) {
                newRoot = container.getChildren().getFirst();
            }
        }
        return newRoot;
    }

    /**
     * Moves a DockNode from one position to another.
     */
    public void move(DockNode node, DockElement target, DockPosition position) {
        move(node, target, position, null);
    }

    /**
     * Moves a DockNode from one position to another with optional tab index.
     */
    public void move(DockNode node, DockElement target, DockPosition position, Integer tabIndex) {
        if (node == null) {
            return;
        }
        if (target == null || position == null) {
            return;
        }
        if (position != DockPosition.CENTER) {
            tabIndex = null;
        }

        if (target == node) {
            return; // No-op: drop to the same element
        }
        if (isAncestor(node, target)) {
            return; // No-op: would move node into its own subtree
        }

        DockContainer sourceParent = node.getParent();
        DockContainer targetParent = target.getParent();

        if (position == DockPosition.CENTER) {
            DockTabPane sourceTabPane = (sourceParent instanceof DockTabPane tabPane) ? tabPane : null;
            DockTabPane targetTabPane = resolveTargetTabPane(target);
            if (sourceTabPane != null && targetTabPane == sourceTabPane) {
                if (moveWithinTabPane(sourceTabPane, node, target, tabIndex)) {
                    bumpRevision();
                }
                return;
            }
        }

        if (isNoOpDropToParentSplitEdge(node, target, position)) {
            return;
        }

        // Special case: Moving within the same SplitPane with the same orientation
        // Check if we're trying to dock in a way that would keep it in the same parent
        if (position != DockPosition.CENTER && sourceParent instanceof DockSplitPane sourceSplit) {
            Orientation requiredOrientation = getOrientationFromDockPosition(position);

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

            Orientation requiredOrientation = getOrientationFromDockPosition(position);

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

        dock(node, currentTarget, position, tabIndex);
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

    /**
     * Checks if the ancestorCandidate is an ancestor of the element in the tree.
     * @param ancestorCandidate The potential ancestor element
     * @param element The element to check against
     * @return true if ancestorCandidate is an ancestor of element, false otherwise
     */
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

    /**
     * Checks if docking the node to the target at the given position would be a no-op because it's already in that position.
     * This specifically checks for the case of docking to the edge of a SplitPane where the node is already located.
     * @param node The node being docked
     * @param target The target element being docked to
     * @param position The position relative to the target
     * @return true if this would be a no-op, false otherwise
     */
    private boolean isNoOpDropToParentSplitEdge(DockNode node, DockElement target, DockPosition position) {
        if (node == null || target == null || position == null) {
            return false;
        }
        if (position == DockPosition.CENTER) {
            return false;
        }
        if (!(target instanceof DockSplitPane splitPane)) {
            return false;
        }
        if (node.getParent() != splitPane) {
            return false;
        }
        Orientation requiredOrientation = getOrientationFromDockPosition(position);
        if (splitPane.getOrientation() != requiredOrientation) {
            return false;
        }

        int nodeIndex = splitPane.getChildren().indexOf(node);
        if (nodeIndex < 0) {
            return false;
        }

        if (position == DockPosition.LEFT || position == DockPosition.TOP) {
            return nodeIndex == 0;
        }

        int lastIndex = splitPane.getChildren().size() - 1;
        return nodeIndex == lastIndex;
    }

    /**
     * Resolves the target TabPane for docking as tab.
     * If the target is a TabPane, returns it directly.
     * If the target is a child of a TabPane, returns that TabPane.
     * Otherwise, returns null.
     */
    private DockTabPane resolveTargetTabPane(DockElement target) {
        if (target instanceof DockTabPane tabPane) {
            return tabPane;
        }
        DockContainer parent = target.getParent();
        if (parent instanceof DockTabPane tabPane) {
            return tabPane;
        }
        return null;
    }

    /**
     * Moves a node within the same TabPane to a new position relative to the target tab.
     * @param tabPane The TabPane containing both the node and the target
     * @param node The node being moved
     * @param target The target element to move relative to
     * @param tabIndex Optional index to insert the new tab at. If null, the new tab will be added right after the target tab (or at the end if target is not a tab itself).
     * @return true if the node was moved, false if it was already in the desired position
     */
    private boolean moveWithinTabPane(DockTabPane tabPane, DockNode node, DockElement target, Integer tabIndex) {
        int sourceIndex = tabPane.getChildren().indexOf(node);
        if (sourceIndex < 0) {
            return false;
        }

        int desiredIndex;
        if (tabIndex != null) {
            desiredIndex = tabIndex;
        } else if (target instanceof DockNode) {
            int targetIndex = tabPane.getChildren().indexOf(target);
            desiredIndex = targetIndex >= 0 ? targetIndex + 1 : tabPane.getChildren().size();
        } else {
            desiredIndex = tabPane.getChildren().size();
        }

        int size = tabPane.getChildren().size();
        int insertIndex = Math.clamp(desiredIndex, 0, size);
        if (sourceIndex < insertIndex) {
            insertIndex--;
        }
        if (sourceIndex == insertIndex) {
            return false;
        }

        tabPane.getChildren().remove(sourceIndex);
        tabPane.getChildren().add(insertIndex, node);
        tabPane.setSelectedIndex(insertIndex);
        return true;
    }

    /**
     * Finds a dock element by layout ID across the main layout and pinned sidebars.
     */
    public DockElement findElementByLayoutId(String layoutId) {
        if (layoutId == null) {
            return null;
        }

        DockElement mainMatch = findElementById(getRoot(), layoutId);
        if (mainMatch != null) {
            return mainMatch;
        }

        for (Side side : Side.values()) {
            ObservableList<DockNode> entries = sideBarNodes.get(side);
            if (entries == null) {
                continue;
            }
            for (DockNode node : entries) {
                if (layoutId.equals(node.getId())) {
                    return node;
                }
            }
        }
        return null;
    }

    public int getDockNodeCount(String dockNodeId) {
        if (dockNodeId == null) {
            return 0;
        }
        int count = countDockNodeId(getRoot(), dockNodeId);
        for (Side side : Side.values()) {
            ObservableList<DockNode> entries = sideBarNodes.get(side);
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            for (DockNode node : entries) {
                if (dockNodeId.equals(node.getDockNodeId())) {
                    count++;
                }
            }
        }
        return count;
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

    private void clearSideBarsInternal(boolean resetVisibility) {
        boolean changed = false;
        for (Side side : Side.values()) {
            ObservableList<DockNode> entries = sideBarNodes.get(side);
            if (entries != null && !entries.isEmpty()) {
                entries.clear();
                changed = true;
            }
            if (resetVisibility) {
                BooleanProperty property = sideBarPinnedOpen.get(side);
                if (property != null && property.get()) {
                    property.set(false);
                    changed = true;
                }
                DoubleProperty widthProperty = sideBarPanelWidths.get(side);
                if (widthProperty != null && Double.compare(widthProperty.get(), DEFAULT_SIDE_BAR_PANEL_WIDTH) != 0) {
                    widthProperty.set(DEFAULT_SIDE_BAR_PANEL_WIDTH);
                }
            }
        }
        if (changed) {
            bumpRevision();
        }
    }

    private void addSideBarEntry(ObservableList<DockNode> entries, DockNode node, Integer desiredIndex) {
        if (entries == null || node == null) {
            return;
        }
        if (desiredIndex == null) {
            entries.add(node);
            return;
        }
        int insertIndex = Math.clamp(desiredIndex, 0, entries.size());
        entries.add(insertIndex, node);
    }

    private boolean moveSideBarEntry(ObservableList<DockNode> entries, DockNode node, int desiredIndex) {
        if (entries == null || node == null) {
            return false;
        }
        int sourceIndex = entries.indexOf(node);
        if (sourceIndex < 0) {
            return false;
        }

        int insertIndex = Math.clamp(desiredIndex, 0, entries.size());
        if (sourceIndex < insertIndex) {
            insertIndex--;
        }
        if (sourceIndex == insertIndex) {
            return false;
        }

        entries.remove(sourceIndex);
        entries.add(insertIndex, node);
        return true;
    }

    private Side findPinnedSide(DockNode node) {
        if (node == null) {
            return null;
        }
        for (Side side : Side.values()) {
            ObservableList<DockNode> entries = sideBarNodes.get(side);
            if (entries != null && entries.contains(node)) {
                return side;
            }
        }
        return null;
    }

    private boolean isInMainTree(DockElement target) {
        if (target == null) {
            return false;
        }
        return target == getRoot() || findInTree(getRoot(), target);
    }

    private boolean findInTree(DockElement current, DockElement target) {
        if (current == null || target == null) {
            return false;
        }
        if (current == target) {
            return true;
        }
        if (current instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                if (findInTree(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Captures main-layout restore anchors before moving a node into a sidebar.
     * The algorithm prefers a neighbor-relative placement, and falls back to the parent container if needed.
     */
    private void rememberLastKnownPlacementForSideBarRestore(DockNode node) {
        if (node == null || node.getParent() == null) {
            return;
        }

        DockContainer parent = node.getParent();
        int index = parent.getChildren().indexOf(node);
        if (index < 0) {
            return;
        }

        DockElement previousNeighbor = index > 0 ? parent.getChildren().get(index - 1) : null;
        DockElement nextNeighbor = index < parent.getChildren().size() - 1
            ? parent.getChildren().get(index + 1)
            : null;

        DockElement target;
        DockPosition position;
        Integer tabIndex = null;

        if (parent instanceof DockTabPane tabPane) {
            target = tabPane;
            position = DockPosition.CENTER;
            tabIndex = index;
        } else if (parent instanceof DockSplitPane splitPane) {
            Orientation orientation = splitPane.getOrientation();
            if (previousNeighbor != null) {
                target = previousNeighbor;
                position = orientation == Orientation.HORIZONTAL ? DockPosition.RIGHT : DockPosition.BOTTOM;
            } else if (nextNeighbor != null) {
                target = nextNeighbor;
                position = orientation == Orientation.HORIZONTAL ? DockPosition.LEFT : DockPosition.TOP;
            } else {
                target = parent;
                position = DockPosition.CENTER;
                tabIndex = 0;
            }
        } else {
            if (previousNeighbor != null) {
                target = previousNeighbor;
                position = DockPosition.RIGHT;
            } else if (nextNeighbor != null) {
                target = nextNeighbor;
                position = DockPosition.LEFT;
            } else {
                target = parent;
                position = DockPosition.CENTER;
                tabIndex = 0;
            }
        }

        node.setLastKnownTarget(target);
        node.setLastKnownPosition(position);
        node.setLastKnownTabIndex(position == DockPosition.CENTER ? tabIndex : null);
    }

    private boolean tryRestorePinnedNodeToRememberedPlacement(DockNode node) {
        if (node == null) {
            return false;
        }

        DockElement target = node.getLastKnownTarget();
        DockPosition position = node.getLastKnownPosition();
        Integer tabIndex = node.getLastKnownTabIndex();
        if (target == null || position == null || !isInMainTree(target)) {
            return false;
        }

        dock(node, target, position, position == DockPosition.CENTER ? tabIndex : null);
        return true;
    }
}

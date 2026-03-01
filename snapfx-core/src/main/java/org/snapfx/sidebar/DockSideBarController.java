package org.snapfx.sidebar;

import org.snapfx.model.DockNode;
import javafx.geometry.Side;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Manages transient sidebar view state and sidebar interaction policy for {@code SnapFX}.
 *
 * <p>This controller stores UI-only sidebar state such as selected sidebar nodes, temporary overlay visibility,
 * and temporary pinned-panel collapse flags. Persistent sidebar model state (pinned entries, pinned-open flags,
 * persisted panel width) remains owned by {@code DockGraph}.</p>
 */
public final class DockSideBarController {
    private static final List<Side> SIDES = List.of(Side.LEFT, Side.RIGHT);

    private final EnumMap<Side, DockNode> selectedSideBarNodes = new EnumMap<>(Side.class);
    private final EnumSet<Side> openOverlaySideBars = EnumSet.noneOf(Side.class);
    private final EnumSet<Side> collapsedPinnedSideBars = EnumSet.noneOf(Side.class);

    private DockSideBarMode sideBarMode = DockSideBarMode.AUTO;
    private boolean collapsePinnedSideBarOnActiveIconClick = true;

    /**
     * Creates a controller with a default sidebar interaction state.
     */
    public DockSideBarController() {
        // create default state
    }

    /**
     * Returns the current framework sidebar rendering mode.
     *
     * @return current sidebar mode
     */
    public DockSideBarMode getSideBarMode() {
        return sideBarMode;
    }

    /**
     * Sets the framework sidebar rendering mode.
     *
     * @param mode target mode, falls back to {@link DockSideBarMode#AUTO} when {@code null}
     * @return {@code true} when the mode changed; otherwise {@code false}
     */
    public boolean setSideBarMode(DockSideBarMode mode) {
        DockSideBarMode nextMode = mode == null ? DockSideBarMode.AUTO : mode;
        if (sideBarMode == nextMode) {
            return false;
        }
        sideBarMode = nextMode;
        return true;
    }

    /**
     * Returns whether active-icon click should collapse currently pinned-open side panels.
     *
     * @return {@code true} when active-icon click collapses pinned-open side panels
     */
    public boolean isCollapsePinnedSideBarOnActiveIconClick() {
        return collapsePinnedSideBarOnActiveIconClick;
    }

    /**
     * Controls whether active-icon click should collapse currently pinned-open side panels.
     *
     * @param collapsePinnedSideBarOnActiveIconClick collapse policy flag
     */
    public void setCollapsePinnedSideBarOnActiveIconClick(boolean collapsePinnedSideBarOnActiveIconClick) {
        this.collapsePinnedSideBarOnActiveIconClick = collapsePinnedSideBarOnActiveIconClick;
    }

    /**
     * Returns whether the transient overlay panel is currently open for a side.
     *
     * @param side sidebar side
     * @return {@code true} when the overlay panel is open
     */
    public boolean isOverlayOpen(Side side) {
        return side != null && openOverlaySideBars.contains(side);
    }

    /**
     * Returns whether the pinned-open side panel is temporarily collapsed for a side.
     *
     * @param side sidebar side
     * @return {@code true} when a pinned-open panel is temporarily collapsed
     */
    public boolean isPinnedPanelCollapsed(Side side) {
        return side != null && collapsedPinnedSideBars.contains(side);
    }

    /**
     * Returns whether any transient overlay sidebar panel is currently open.
     *
     * @return {@code true} when at least one transient overlay is open
     */
    public boolean hasOpenOverlays() {
        return !openOverlaySideBars.isEmpty();
    }

    /**
     * Updates transient state after a sidebar icon click.
     *
     * <p>Behavior mirrors the previous {@code SnapFX} implementation:
     * pinned-open sidebars toggle temporary collapse/expand behavior, while overlay sidebars toggle
     * open/close behavior for the selected node.</p>
     *
     * @param side sidebar side
     * @param dockNode selected node
     * @param sidePinnedOpen whether the side is currently pinned-open in the persistent model
     */
    public void onIconClicked(Side side, DockNode dockNode, boolean sidePinnedOpen) {
        if (side == null || dockNode == null) {
            return;
        }

        DockNode previousSelection = selectedSideBarNodes.get(side);
        boolean overlayWasOpen = openOverlaySideBars.contains(side);
        selectedSideBarNodes.put(side, dockNode);

        if (sidePinnedOpen) {
            openOverlaySideBars.remove(side);
            if (collapsedPinnedSideBars.contains(side)) {
                collapsedPinnedSideBars.remove(side);
            } else if (previousSelection == dockNode && collapsePinnedSideBarOnActiveIconClick) {
                collapsedPinnedSideBars.add(side);
            } else {
                collapsedPinnedSideBars.remove(side);
            }
            return;
        }

        if (overlayWasOpen && previousSelection == dockNode) {
            openOverlaySideBars.remove(side);
        } else {
            openOverlaySideBars.add(side);
        }
    }

    /**
     * Updates transient state after pinning a panel open.
     *
     * @param side sidebar side
     * @param dockNode selected node
     * @param sidePinnedOpen whether the pin-open operation is currently reflected in the model
     */
    public void onPanelPinnedOpen(Side side, DockNode dockNode, boolean sidePinnedOpen) {
        if (side == null || dockNode == null) {
            return;
        }
        selectedSideBarNodes.put(side, dockNode);
        collapsedPinnedSideBars.remove(side);
        if (sidePinnedOpen) {
            openOverlaySideBars.remove(side);
        }
    }

    /**
     * Updates transient state after collapsing a pinned-open panel.
     *
     * @param side sidebar side
     * @param dockNode selected node
     * @param sidePinnedOpen whether the side is still pinned-open after collapse attempt
     */
    public void onPanelCollapsed(Side side, DockNode dockNode, boolean sidePinnedOpen) {
        if (side == null || dockNode == null) {
            return;
        }
        selectedSideBarNodes.put(side, dockNode);
        collapsedPinnedSideBars.remove(side);
        if (!sidePinnedOpen) {
            openOverlaySideBars.add(side);
        }
    }

    /**
     * Updates transient state before restoring a node from sidebar to the main layout.
     *
     * @param side sidebar side
     * @param dockNode node to restore
     */
    public void onPanelRestoreRequested(Side side, DockNode dockNode) {
        if (dockNode == null) {
            return;
        }
        openOverlaySideBars.remove(side);
        if (selectedSideBarNodes.get(side) == dockNode) {
            selectedSideBarNodes.remove(side);
        }
    }

    /**
     * Updates transient state after a node is pinned into a sidebar.
     *
     * @param side sidebar side
     * @param dockNode pinned node
     */
    public void onNodePinned(Side side, DockNode dockNode) {
        if (side == null || dockNode == null) {
            return;
        }
        selectedSideBarNodes.put(side, dockNode);
        openOverlaySideBars.remove(side);
    }

    /**
     * Applies transient-state cleanup after attempting to pin-open a sidebar side.
     *
     * @param side sidebar side
     * @param sidePinnedOpen whether pin-open is currently active in the model
     */
    public void onPinOpenApplied(Side side, boolean sidePinnedOpen) {
        if (side == null || !sidePinnedOpen) {
            return;
        }
        openOverlaySideBars.remove(side);
        collapsedPinnedSideBars.remove(side);
    }

    /**
     * Applies transient-state cleanup after attempting to collapse a pinned-open sidebar side.
     *
     * @param side sidebar side
     * @param sidePinnedOpen whether the side remains pinned-open after the collapse attempt
     */
    public void onCollapseApplied(Side side, boolean sidePinnedOpen) {
        if (side == null || sidePinnedOpen) {
            return;
        }
        collapsedPinnedSideBars.remove(side);
    }

    /**
     * Closes transient overlays for sides that are not currently pinned-open.
     *
     * @param sidePinnedOpenResolver callback used to query persistent pinned-open state by side
     * @return {@code true} when at least one overlay was closed
     */
    public boolean closeTransientOverlays(Predicate<Side> sidePinnedOpenResolver) {
        if (sidePinnedOpenResolver == null) {
            return false;
        }
        boolean changed = false;
        for (Side side : SIDES) {
            if (sidePinnedOpenResolver.test(side)) {
                continue;
            }
            if (openOverlaySideBars.remove(side)) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Reconciles transient state for a side against currently pinned nodes and pinned-open model state.
     *
     * @param side sidebar side
     * @param pinnedNodes currently pinned nodes on the side
     * @param sidePinnedOpen whether the side is currently pinned-open in the model
     */
    public void pruneInvalidViewState(Side side, List<DockNode> pinnedNodes, boolean sidePinnedOpen) {
        if (side == null) {
            return;
        }
        if (pinnedNodes == null || pinnedNodes.isEmpty()) {
            clearSide(side);
            return;
        }

        DockNode selected = selectedSideBarNodes.get(side);
        if (selected == null || !pinnedNodes.contains(selected)) {
            selectedSideBarNodes.put(side, pinnedNodes.getFirst());
        }
        if (sidePinnedOpen) {
            openOverlaySideBars.remove(side);
        } else {
            collapsedPinnedSideBars.remove(side);
        }
    }

    /**
     * Resolves the selected node for a sidebar side using current pinned-node state.
     *
     * @param side sidebar side
     * @param pinnedNodes currently pinned nodes on the side
     * @return selected node for the side, or {@code null} when no pinned nodes are available
     */
    public DockNode resolveSelectedNode(Side side, List<DockNode> pinnedNodes) {
        if (side == null || pinnedNodes == null || pinnedNodes.isEmpty()) {
            if (side != null) {
                selectedSideBarNodes.remove(side);
            }
            return null;
        }

        DockNode selectedNode = selectedSideBarNodes.get(side);
        if (selectedNode == null || !pinnedNodes.contains(selectedNode)) {
            selectedNode = pinnedNodes.getFirst();
            selectedSideBarNodes.put(side, selectedNode);
        }
        return selectedNode;
    }

    /**
     * Clears all transient sidebar view state.
     */
    public void resetTransientViewState() {
        selectedSideBarNodes.clear();
        openOverlaySideBars.clear();
        collapsedPinnedSideBars.clear();
    }

    /**
     * Removes transient state entries that reference a given node.
     *
     * @param node node whose transient state should be removed
     */
    public void forgetTransientStateForNode(DockNode node) {
        if (node == null) {
            return;
        }
        selectedSideBarNodes.entrySet().removeIf(entry -> entry.getValue() == node);
        openOverlaySideBars.removeIf(side -> {
            DockNode selectedNode = selectedSideBarNodes.get(side);
            return selectedNode == null || selectedNode == node;
        });
        collapsedPinnedSideBars.removeIf(side -> {
            DockNode selectedNode = selectedSideBarNodes.get(side);
            return selectedNode == null || selectedNode == node;
        });
    }

    private void clearSide(Side side) {
        selectedSideBarNodes.remove(side);
        openOverlaySideBars.remove(side);
        collapsedPinnedSideBars.remove(side);
    }
}

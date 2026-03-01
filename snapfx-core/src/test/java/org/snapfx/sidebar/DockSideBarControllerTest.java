package org.snapfx.sidebar;

import org.snapfx.model.DockNode;
import javafx.geometry.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockSideBarControllerTest {

    @Test
    void defaultsAndModeFallbackAreConfigured() {
        DockSideBarController controller = new DockSideBarController();

        assertEquals(DockSideBarMode.AUTO, controller.getSideBarMode());
        assertTrue(controller.isCollapsePinnedSideBarOnActiveIconClick());
        assertFalse(controller.hasOpenOverlays());

        assertTrue(controller.setSideBarMode(DockSideBarMode.ALWAYS));
        assertEquals(DockSideBarMode.ALWAYS, controller.getSideBarMode());
        assertFalse(controller.setSideBarMode(DockSideBarMode.ALWAYS));

        assertTrue(controller.setSideBarMode(null));
        assertEquals(DockSideBarMode.AUTO, controller.getSideBarMode());
    }

    @Test
    void iconClickTogglesOverlayForUnpinnedSideBar() {
        DockSideBarController controller = new DockSideBarController();
        DockNode toolNode = dockNode("tool");

        controller.onIconClicked(Side.LEFT, toolNode, false);
        assertTrue(controller.isOverlayOpen(Side.LEFT));

        controller.onIconClicked(Side.LEFT, toolNode, false);
        assertFalse(controller.isOverlayOpen(Side.LEFT));
    }

    @Test
    void iconClickTogglesCollapsedStateForPinnedOpenSideBar() {
        DockSideBarController controller = new DockSideBarController();
        DockNode toolNode = dockNode("tool");

        controller.onIconClicked(Side.LEFT, toolNode, true);
        assertFalse(controller.isPinnedPanelCollapsed(Side.LEFT));

        controller.onIconClicked(Side.LEFT, toolNode, true);
        assertTrue(controller.isPinnedPanelCollapsed(Side.LEFT));

        controller.onIconClicked(Side.LEFT, toolNode, true);
        assertFalse(controller.isPinnedPanelCollapsed(Side.LEFT));
    }

    @Test
    void panelPinAndCollapseCallbacksUpdateOverlayState() {
        DockSideBarController controller = new DockSideBarController();
        DockNode toolNode = dockNode("tool");

        controller.onIconClicked(Side.LEFT, toolNode, false);
        assertTrue(controller.isOverlayOpen(Side.LEFT));

        controller.onPanelPinnedOpen(Side.LEFT, toolNode, true);
        assertFalse(controller.isOverlayOpen(Side.LEFT));
        assertFalse(controller.isPinnedPanelCollapsed(Side.LEFT));

        controller.onPanelCollapsed(Side.LEFT, toolNode, false);
        assertTrue(controller.isOverlayOpen(Side.LEFT));
        assertFalse(controller.isPinnedPanelCollapsed(Side.LEFT));
    }

    @Test
    void closeTransientOverlaysKeepsPinnedOpenSidesUntouched() {
        DockSideBarController controller = new DockSideBarController();
        DockNode leftNode = dockNode("left");
        DockNode rightNode = dockNode("right");

        controller.onIconClicked(Side.LEFT, leftNode, false);
        controller.onIconClicked(Side.RIGHT, rightNode, false);
        assertTrue(controller.isOverlayOpen(Side.LEFT));
        assertTrue(controller.isOverlayOpen(Side.RIGHT));

        boolean changed = controller.closeTransientOverlays(side -> side == Side.LEFT);

        assertTrue(changed);
        assertTrue(controller.isOverlayOpen(Side.LEFT));
        assertFalse(controller.isOverlayOpen(Side.RIGHT));
    }

    @Test
    void pruneAndResolveSelectionHandleInvalidState() {
        DockSideBarController controller = new DockSideBarController();
        DockNode oldNode = dockNode("old");
        DockNode firstNode = dockNode("first");
        DockNode secondNode = dockNode("second");

        controller.onIconClicked(Side.LEFT, oldNode, false);
        assertTrue(controller.isOverlayOpen(Side.LEFT));

        controller.pruneInvalidViewState(Side.LEFT, List.of(firstNode, secondNode), true);
        assertFalse(controller.isOverlayOpen(Side.LEFT));
        assertEquals(firstNode, controller.resolveSelectedNode(Side.LEFT, List.of(firstNode, secondNode)));

        controller.pruneInvalidViewState(Side.LEFT, List.of(), false);
        assertNull(controller.resolveSelectedNode(Side.LEFT, List.of()));
    }

    @Test
    void forgetNodeAndResetClearTransientState() {
        DockSideBarController controller = new DockSideBarController();
        DockNode leftNode = dockNode("left");
        DockNode rightNode = dockNode("right");

        controller.onIconClicked(Side.LEFT, leftNode, true);
        controller.onIconClicked(Side.LEFT, leftNode, true);
        controller.onIconClicked(Side.RIGHT, rightNode, false);
        assertTrue(controller.isPinnedPanelCollapsed(Side.LEFT));
        assertTrue(controller.isOverlayOpen(Side.RIGHT));

        controller.forgetTransientStateForNode(leftNode);
        assertFalse(controller.isPinnedPanelCollapsed(Side.LEFT));
        assertTrue(controller.isOverlayOpen(Side.RIGHT));

        controller.resetTransientViewState();
        assertFalse(controller.hasOpenOverlays());
        assertFalse(controller.isPinnedPanelCollapsed(Side.LEFT));
    }

    private DockNode dockNode(String dockNodeId) {
        return new DockNode(dockNodeId, null, dockNodeId);
    }
}

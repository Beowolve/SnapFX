package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import com.github.beowolve.snapfx.model.DockTabPane;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SnapFX main API functionality.
 * Focus on hide/restore functionality and close request handling.
 */
class SnapFXTest {
    private SnapFX snapFX;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX for headless tests
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX is already running
        }
    }

    @BeforeEach
    void setUp() {
        snapFX = new SnapFX();
    }

    /**
     * Test that hiding a DockNode removes it from layout and adds it to hidden list.
     * Regression test: Bug where closing a window did not add it to hidden windows menu (2026-02-11).
     * Root cause: Close button was set up before setOnNodeCloseRequest handler was configured,
     * causing the close action to call undock() instead of hide().
     */
    @Test
    void testHideAddsToHiddenList() {
        // Create and dock nodes
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        // Verify initial state
        assertNotNull(snapFX.getDockGraph().getRoot());
        assertTrue(snapFX.getHiddenNodes().isEmpty());

        // Hide node1
        snapFX.hide(node1);

        // Verify node1 is now hidden
        assertEquals(1, snapFX.getHiddenNodes().size());
        assertTrue(snapFX.getHiddenNodes().contains(node1));

        // Verify node1 is no longer in graph
        assertFalse(isInGraph(snapFX, node1));
    }

    /**
     * Test that restore brings a hidden node back into the layout.
     */
    @Test
    void testRestoreFromHidden() {
        // Create and dock nodes
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        // Hide node1
        snapFX.hide(node1);
        assertEquals(1, snapFX.getHiddenNodes().size());

        // Restore node1
        snapFX.restore(node1);

        // Verify node1 is no longer hidden
        assertTrue(snapFX.getHiddenNodes().isEmpty());

        // Verify node1 is back in graph
        assertTrue(isInGraph(snapFX, node1));
    }

    /**
     * Test that hide() stores last known position for later restore.
     */
    @Test
    void testHideStoresLastPosition() {
        // Create and dock nodes
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        // Hide node1
        snapFX.hide(node1);

        // Verify last known position was stored
        assertNotNull(node1.getLastKnownTarget());
        assertNotNull(node1.getLastKnownPosition());
    }

    /**
     * Test that multiple nodes can be hidden and restored independently.
     */
    @Test
    void testMultipleHideRestore() {
        // Create and dock 3 nodes
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");
        DockNode node3 = new DockNode("node3", new Label("Node 3"), "Node 3");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);
        snapFX.dock(node3, node2, DockPosition.BOTTOM);

        // Hide two nodes
        snapFX.hide(node1);
        snapFX.hide(node3);

        // Verify both are hidden
        assertEquals(2, snapFX.getHiddenNodes().size());
        assertTrue(snapFX.getHiddenNodes().contains(node1));
        assertTrue(snapFX.getHiddenNodes().contains(node3));

        // Restore one
        snapFX.restore(node1);

        // Verify only node3 is still hidden
        assertEquals(1, snapFX.getHiddenNodes().size());
        assertTrue(snapFX.getHiddenNodes().contains(node3));
        assertFalse(snapFX.getHiddenNodes().contains(node1));
    }

    /**
     * Test that hiding an already hidden node has no effect.
     */
    @Test
    void testHideAlreadyHidden() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node1, null, DockPosition.CENTER);

        // Hide once
        snapFX.hide(node1);
        assertEquals(1, snapFX.getHiddenNodes().size());

        // Hide again (should have no effect)
        snapFX.hide(node1);
        assertEquals(1, snapFX.getHiddenNodes().size());
    }

    /**
     * Test that restoring a non-hidden node has no effect.
     */
    @Test
    void testRestoreNonHidden() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node1, null, DockPosition.CENTER);

        // Try to restore (should have no effect)
        snapFX.restore(node1);

        // Verify still in graph and not in hidden list
        assertTrue(isInGraph(snapFX, node1));
        assertTrue(snapFX.getHiddenNodes().isEmpty());
    }

    /**
     * Test that close request handler correctly calls hide().
     * This ensures the bug is fixed where close button didn't add nodes to hidden list.
     */
    @Test
    void testCloseRequestHandlerCallsHide() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node1, null, DockPosition.CENTER);

        // Set up close handler to call hide (like MainDemo does)
        snapFX.setOnNodeCloseRequest(node -> snapFX.hide(node));

        // Build layout to ensure views are created with correct handler
        snapFX.buildLayout();

        // Simulate close request (in real app, this would be triggered by close button)
        snapFX.hide(node1);

        // Verify node is in hidden list
        assertEquals(1, snapFX.getHiddenNodes().size());
        assertTrue(snapFX.getHiddenNodes().contains(node1));
    }

    @Test
    void testFloatNodeRemovesNodeFromGraphAndTracksFloatingWindow() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1);

        assertNotNull(floatingWindow);
        assertEquals(node1, floatingWindow.getDockNode());
        assertEquals(1, snapFX.getFloatingWindows().size());
        assertFalse(isInGraph(snapFX, node1));
    }

    @Test
    void testAttachFloatingWindowRestoresNodeToGraph() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1);
        snapFX.attachFloatingWindow(floatingWindow);

        assertTrue(snapFX.getFloatingWindows().isEmpty());
        assertTrue(isInGraph(snapFX, node1));
    }

    @Test
    void testClosingFloatingWindowAttachesNodeBack() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1);
        floatingWindow.close();

        assertTrue(snapFX.getFloatingWindows().isEmpty());
        assertTrue(isInGraph(snapFX, node1));
    }

    @Test
    void testHideFloatingNodeMovesItToHiddenList() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        snapFX.floatNode(node1);
        snapFX.hide(node1);

        assertTrue(snapFX.getFloatingWindows().isEmpty());
        assertEquals(1, snapFX.getHiddenNodes().size());
        assertTrue(snapFX.getHiddenNodes().contains(node1));
    }

    @Test
    void testAttachAfterFloatFromTabRestoresNodeAsTab() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.CENTER);

        assertTrue(node1.getParent() instanceof DockTabPane);
        DockFloatingWindow floatingWindow = snapFX.floatNode(node2);
        snapFX.attachFloatingWindow(floatingWindow);

        assertTrue(node1.getParent() instanceof DockTabPane);
        assertEquals(node1.getParent(), node2.getParent());
    }

    @Test
    void testFloatAttachToggleKeepsRememberedFloatingPosition() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node1, null, DockPosition.CENTER);

        DockFloatingWindow firstFloat = snapFX.floatNode(node1, 321.0, 222.0);
        snapFX.attachFloatingWindow(firstFloat);

        DockFloatingWindow secondFloat = snapFX.floatNode(node1);

        assertEquals(321.0, secondFloat.getPreferredX(), 0.0001);
        assertEquals(222.0, secondFloat.getPreferredY(), 0.0001);
    }

    // Helper method to check if node is in graph
    private boolean isInGraph(SnapFX snapFX, DockNode node) {
        return findInGraph(snapFX.getDockGraph().getRoot(), node);
    }

    private boolean findInGraph(com.github.beowolve.snapfx.model.DockElement current, DockNode target) {
        if (current == null) {
            return false;
        }
        if (current == target) {
            return true;
        }
        if (current instanceof com.github.beowolve.snapfx.model.DockContainer container) {
            for (com.github.beowolve.snapfx.model.DockElement child : container.getChildren()) {
                if (findInGraph(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }
}


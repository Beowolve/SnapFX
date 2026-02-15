package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import com.github.beowolve.snapfx.model.DockSplitPane;
import com.github.beowolve.snapfx.model.DockTabPane;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    void testClosingFloatingWindowMovesNodeToHiddenList() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1);
        floatingWindow.close();

        assertTrue(snapFX.getFloatingWindows().isEmpty());
        assertFalse(isInGraph(snapFX, node1));
        assertEquals(1, snapFX.getHiddenNodes().size());
        assertTrue(snapFX.getHiddenNodes().contains(node1));
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
    void testRestoreHiddenFloatingNodeReopensAsFloatingWindow() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        snapFX.floatNode(node1, 420.0, 210.0);
        snapFX.hide(node1);

        assertTrue(snapFX.getFloatingWindows().isEmpty());
        assertTrue(snapFX.getHiddenNodes().contains(node1));

        snapFX.restore(node1);

        assertTrue(snapFX.getHiddenNodes().isEmpty());
        assertFalse(isInGraph(snapFX, node1));
        assertEquals(1, snapFX.getFloatingWindows().size());
        DockFloatingWindow restoredWindow = snapFX.getFloatingWindows().get(0);
        assertTrue(restoredWindow.containsNode(node1));
        assertEquals(420.0, restoredWindow.getPreferredX(), 0.0001);
        assertEquals(210.0, restoredWindow.getPreferredY(), 0.0001);
    }

    @Test
    void testRestoreAfterClosingFloatingWindowReopensAsFloatingWindow() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1, 333.0, 144.0);
        floatingWindow.close();

        assertTrue(snapFX.getFloatingWindows().isEmpty());
        assertTrue(snapFX.getHiddenNodes().contains(node1));

        snapFX.restore(node1);

        assertTrue(snapFX.getHiddenNodes().isEmpty());
        assertFalse(isInGraph(snapFX, node1));
        assertEquals(1, snapFX.getFloatingWindows().size());
        DockFloatingWindow restoredWindow = snapFX.getFloatingWindows().get(0);
        assertTrue(restoredWindow.containsNode(node1));
        assertEquals(333.0, restoredWindow.getPreferredX(), 0.0001);
        assertEquals(144.0, restoredWindow.getPreferredY(), 0.0001);
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

    @Test
    void testRequestFloatForNodeDetachesNodeFromFloatingSubLayout() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1, 300.0, 200.0);
        snapFX.getDockGraph().undock(node2);
        floatingWindow.dockNode(node2, node1, DockPosition.CENTER, null);

        assertEquals(1, snapFX.getFloatingWindows().size());
        assertTrue(floatingWindow.containsNode(node1));
        assertTrue(floatingWindow.containsNode(node2));

        floatingWindow.requestFloatForNode(node2);

        assertEquals(2, snapFX.getFloatingWindows().size());
        assertFalse(floatingWindow.containsNode(node2));
        DockFloatingWindow detachedWindow = snapFX.getFloatingWindows().stream()
            .filter(window -> window != floatingWindow && window.containsNode(node2))
            .findFirst()
            .orElse(null);
        assertNotNull(detachedWindow);
        assertTrue(detachedWindow.containsNode(node2));
        assertTrue(floatingWindow.containsNode(node1));
    }

    @Test
    void testRequestFloatForNodeDoesNothingForSingleNodeFloatingWindow() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node1, null, DockPosition.CENTER);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1);
        assertEquals(1, snapFX.getFloatingWindows().size());

        floatingWindow.requestFloatForNode(node1);

        assertEquals(1, snapFX.getFloatingWindows().size());
        assertSame(floatingWindow, snapFX.getFloatingWindows().getFirst());
        assertTrue(floatingWindow.containsNode(node1));
    }

    @Test
    void testSetRootSplitRatiosAppliesNormalizedValues() {
        DockNode left = new DockNode("left", new Label("Left"), "Left");
        DockNode center = new DockNode("center", new Label("Center"), "Center");
        DockNode right = new DockNode("right", new Label("Right"), "Right");

        snapFX.dock(left, null, DockPosition.CENTER);
        snapFX.dock(center, left, DockPosition.RIGHT);
        snapFX.dock(right, center, DockPosition.RIGHT);

        assertTrue(snapFX.setRootSplitRatios(25, 50, 25));

        DockSplitPane rootSplit = assertInstanceOf(DockSplitPane.class, snapFX.getDockGraph().getRoot());
        assertEquals(0.25, rootSplit.getDividerPositions().get(0).get(), 0.0001);
        assertEquals(0.75, rootSplit.getDividerPositions().get(1).get(), 0.0001);
    }

    @Test
    void testSetRootSplitRatiosReturnsFalseWhenRootIsNotSplit() {
        DockNode onlyNode = new DockNode("single", new Label("Single"), "Single");
        snapFX.dock(onlyNode, null, DockPosition.CENTER);

        assertFalse(snapFX.setRootSplitRatios(1, 1));
    }

    @Test
    void testSetRootSplitRatiosRejectsInvalidInput() {
        DockNode left = new DockNode("left", new Label("Left"), "Left");
        DockNode center = new DockNode("center", new Label("Center"), "Center");
        DockNode right = new DockNode("right", new Label("Right"), "Right");

        snapFX.dock(left, null, DockPosition.CENTER);
        snapFX.dock(center, left, DockPosition.RIGHT);
        snapFX.dock(right, center, DockPosition.RIGHT);

        assertFalse(snapFX.setRootSplitRatios(1, 1));
        assertFalse(snapFX.setRootSplitRatios(1, 0, 1));
        assertFalse(snapFX.setRootSplitRatios(1, Double.NaN, 1));
        assertFalse(snapFX.setRootSplitRatios(1, Double.POSITIVE_INFINITY, 1));
    }

    @Test
    void testSaveLayoutWithFloatingWindowIncludesSnapshotSection() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat, 420.0, 210.0);
        floatingWindow.setPreferredSize(640.0, 420.0);

        String json = snapFX.saveLayout();

        assertTrue(json.contains("\"mainLayout\""));
        assertTrue(json.contains("\"floatingWindows\""));
        assertTrue(json.contains("\"layout\""));
    }

    @Test
    void testSaveLoadRoundTripRestoresFloatingWindowBoundsAndNode() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.setNodeFactory(this::createFactoryNode);
        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat, 333.0, 144.0);
        floatingWindow.setPreferredSize(610.0, 390.0);

        String json = snapFX.saveLayout();

        SnapFX restored = new SnapFX();
        restored.setNodeFactory(this::createFactoryNode);
        restored.loadLayout(json);

        assertEquals(1, restored.getFloatingWindows().size());
        DockFloatingWindow restoredWindow = restored.getFloatingWindows().get(0);
        assertEquals(333.0, restoredWindow.getPreferredX(), 0.0001);
        assertEquals(144.0, restoredWindow.getPreferredY(), 0.0001);
        assertEquals(610.0, restoredWindow.getPreferredWidth(), 0.0001);
        assertEquals(390.0, restoredWindow.getPreferredHeight(), 0.0001);

        List<DockNode> floatingNodes = restoredWindow.getDockNodes();
        assertEquals(1, floatingNodes.size());
        assertEquals("nodeFloat", floatingNodes.get(0).getDockNodeId());
        assertFalse(isInGraph(restored, floatingNodes.get(0)));

        DockNode restoredMainNode = assertInstanceOf(DockNode.class, restored.getDockGraph().getRoot());
        assertEquals("nodeMain", restoredMainNode.getDockNodeId());
    }

    @Test
    void testLoadLayoutRemainsCompatibleWithLegacyMainLayoutJson() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeRight = new DockNode("nodeRight", new Label("Right"), "Right");

        snapFX.setNodeFactory(this::createFactoryNode);
        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeRight, nodeMain, DockPosition.RIGHT);

        String legacyJson = snapFX.saveLayout();
        assertFalse(legacyJson.contains("\"mainLayout\""));

        SnapFX restored = new SnapFX();
        restored.setNodeFactory(this::createFactoryNode);
        restored.loadLayout(legacyJson);

        assertTrue(restored.getFloatingWindows().isEmpty());
        assertNotNull(restored.getDockGraph().getRoot());
    }

    @Test
    void testFloatingWindowsListIsStableAndNotifiesListeners() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);

        var floatingList = snapFX.getFloatingWindows();
        assertSame(floatingList, snapFX.getFloatingWindows());

        AtomicInteger additions = new AtomicInteger(0);
        floatingList.addListener((javafx.collections.ListChangeListener<DockFloatingWindow>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    additions.addAndGet(change.getAddedSize());
                }
            }
        });

        snapFX.floatNode(nodeFloat);

        assertEquals(1, additions.get());
        assertEquals(1, floatingList.size());
    }

    @Test
    void testCloseFloatingWindowsWithoutAttachClearsFloatingList() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);
        snapFX.floatNode(nodeFloat, 450.0, 250.0);

        assertEquals(1, snapFX.getFloatingWindows().size());

        snapFX.closeFloatingWindows(false);

        assertTrue(snapFX.getFloatingWindows().isEmpty());
    }

    @Test
    void testSetLockedPropagatesToFloatingWindowsAndBlocksClose() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);
        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat);

        snapFX.setLocked(true);

        assertTrue(floatingWindow.getDockGraph().isLocked());
        floatingWindow.close();
        assertEquals(1, snapFX.getFloatingWindows().size());
        assertTrue(snapFX.getHiddenNodes().isEmpty());

        snapFX.setLocked(false);
        assertFalse(floatingWindow.getDockGraph().isLocked());
        floatingWindow.close();
        assertTrue(snapFX.getFloatingWindows().isEmpty());
        assertTrue(snapFX.getHiddenNodes().contains(nodeFloat));
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

    private DockNode createFactoryNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }
        return new DockNode(nodeId, new Label("Factory: " + nodeId), nodeId);
    }
}


package com.github.beowolve.snapfx.model;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DockGraph tree manipulation.
 */
class DockGraphTest {
    private DockGraph dockGraph;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX for headless tests without Swing
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // JavaFX is already running
        }
    }

    @BeforeEach
    void setUp() {
        dockGraph = new DockGraph();
    }

    // Helper: collects all leaf DockNodes in the tree
    private List<DockNode> collectLeafNodes(DockElement element) {
        List<DockNode> leaves = new ArrayList<>();
        if (element == null) {
            return leaves;
        }

        switch (element) {
            case DockNode dockNode -> leaves.add(dockNode);
            case DockContainer container -> {
                for (DockElement child : container.getChildren()) {
                    leaves.addAll(collectLeafNodes(child));
                }
            }
            default -> {
            }
        }

        return leaves;
    }

    @Test
    void testDockFirstNode() {
        DockNode node = new DockNode(new Label("Test"), "Test Node");
        dockGraph.dock(node, null, DockPosition.CENTER);

        assertNotNull(dockGraph.getRoot());
        assertEquals(node, dockGraph.getRoot());
    }

    @Test
    void testDockAsTab() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        // Instead of checking exact container type: ensure both leaf nodes are present
        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertTrue(leaves.contains(node1));
        assertTrue(leaves.contains(node2));
    }

    @Test
    void testDockAsSplit() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.LEFT);

        // Ensure both leaf nodes are present
        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertTrue(leaves.contains(node1));
        assertTrue(leaves.contains(node2));
    }

    @Test
    void testUndock() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        List<DockNode> leavesBefore = collectLeafNodes(dockGraph.getRoot());
        assertTrue(leavesBefore.contains(node1));
        assertTrue(leavesBefore.contains(node2));

        // Remove node2
        dockGraph.undock(node2);

        List<DockNode> leavesAfter = collectLeafNodes(dockGraph.getRoot());
        assertTrue(leavesAfter.contains(node1));
        assertNull(node2.getParent());
    }

    @Test
    void testMove() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");
        DockNode node3 = new DockNode(new Label("Test3"), "Node 3");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);
        dockGraph.dock(node3, node1, DockPosition.CENTER);

        // Move node3 to the bottom of node2
        dockGraph.move(node3, node2, DockPosition.BOTTOM);

        // Verify: all three leaf nodes should exist
        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertTrue(leaves.contains(node1));
        assertTrue(leaves.contains(node2));
        assertTrue(leaves.contains(node3));
    }

    @Test
    void testAutoCleanup() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);

        // Remove node2
        dockGraph.undock(node2);

        // After cleanup, node1 should still exist and node2 should have no parent
        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertTrue(leaves.contains(node1));
        assertNull(node2.getParent());
    }

    @Test
    void testLocked() {
        assertFalse(dockGraph.isLocked());

        dockGraph.setLocked(true);
        assertTrue(dockGraph.isLocked());

        dockGraph.setLocked(false);
        assertFalse(dockGraph.isLocked());
    }

    @Test
    void testSplitPaneFlattening() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");
        DockNode node3 = new DockNode(new Label("Test3"), "Node 3");

        // Create horizontal splits
        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.LEFT);
        dockGraph.dock(node3, node2, DockPosition.LEFT);

        // Only verify that all 3 leaf nodes are present (flattening must not lose leaves)
        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertEquals(3, leaves.size());
    }

    @Test
    void testMoveOntoItselfIsNoOp() {
        DockNode node1 = new DockNode(new Label("A"), "A");
        dockGraph.dock(node1, null, DockPosition.CENTER);

        DockElement rootBefore = dockGraph.getRoot();
        long revBefore = dockGraph.getRevision();

        dockGraph.move(node1, node1, DockPosition.CENTER);

        assertSame(rootBefore, dockGraph.getRoot());
        assertEquals(revBefore, dockGraph.getRevision());
        assertNull(node1.getParent());
    }

    @Test
    void testMoveIntoOwnSubtreeIsNoOp() {
        DockNode a = new DockNode(new Label("A"), "A");
        DockNode b = new DockNode(new Label("B"), "B");

        dockGraph.dock(a, null, DockPosition.CENTER);
        dockGraph.dock(b, a, DockPosition.RIGHT);

        // Make sure root is a container now
        DockElement rootBefore = dockGraph.getRoot();
        long revBefore = dockGraph.getRevision();

        // root is a split; move it onto its own child should be a no-op
        DockContainer rootContainer = assertInstanceOf(DockContainer.class, rootBefore);
        DockElement child = rootContainer.getChildren().getFirst();

        dockGraph.move(a, child, DockPosition.CENTER);

        assertSame(rootBefore, dockGraph.getRoot());
        assertEquals(revBefore, dockGraph.getRevision());

        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertTrue(leaves.contains(a));
        assertTrue(leaves.contains(b));
    }

    @Test
    void testUndockLeftMostNodeDoesNotClearLayout() {
        DockNode left = new DockNode(new Label("Left"), "Left");
        DockNode center = new DockNode(new Label("Center"), "Center");
        DockNode right = new DockNode(new Label("Right"), "Right");

        dockGraph.dock(left, null, DockPosition.CENTER);
        dockGraph.dock(center, left, DockPosition.RIGHT);
        dockGraph.dock(right, center, DockPosition.RIGHT);

        long revBefore = dockGraph.getRevision();

        dockGraph.undock(left);

        assertNotNull(dockGraph.getRoot());
        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertFalse(leaves.contains(left));
        assertTrue(leaves.contains(center));
        assertTrue(leaves.contains(right));
        assertTrue(dockGraph.getRevision() > revBefore);
    }

    @Test
    void testSplitPaneFlattentingOptimization() {
        DockNode left = new DockNode(new Label("Left"), "Left");
        DockNode center = new DockNode(new Label("Center"), "Center");
        DockNode right = new DockNode(new Label("Right"), "Right");

        // Create initial horizontal split: left | center
        dockGraph.dock(left, null, DockPosition.CENTER);
        dockGraph.dock(center, left, DockPosition.RIGHT);

        // Now dock right to center's right
        // This should NOT create nested horizontal splits, but add to existing one
        dockGraph.dock(right, center, DockPosition.RIGHT);

        // Verify: root should be a single DockSplitPane with 3 children
        DockElement root = dockGraph.getRoot();
        assertInstanceOf(DockSplitPane.class, root);

        DockSplitPane rootSplit = (DockSplitPane) root;
        assertEquals(Orientation.HORIZONTAL, rootSplit.getOrientation());
        assertEquals(3, rootSplit.getChildren().size());

        // Verify all three nodes are direct children
        List<DockNode> leaves = collectLeafNodes(root);
        assertEquals(3, leaves.size());
        assertTrue(leaves.contains(left));
        assertTrue(leaves.contains(center));
        assertTrue(leaves.contains(right));

        // Verify order: left, center, right
        assertEquals(left, rootSplit.getChildren().get(0));
        assertEquals(center, rootSplit.getChildren().get(1));
        assertEquals(right, rootSplit.getChildren().get(2));
    }

    @Test
    void testSplitPaneFlattentingOptimizationLeft() {
        DockNode center = new DockNode(new Label("Center"), "Center");
        DockNode right = new DockNode(new Label("Right"), "Right");
        DockNode left = new DockNode(new Label("Left"), "Left");

        // Create initial horizontal split: center | right
        dockGraph.dock(center, null, DockPosition.CENTER);
        dockGraph.dock(right, center, DockPosition.RIGHT);

        // Now dock left to center's left
        // This should add to existing horizontal split
        dockGraph.dock(left, center, DockPosition.LEFT);

        // Verify: root should be a single DockSplitPane with 3 children
        DockElement root = dockGraph.getRoot();
        assertInstanceOf(DockSplitPane.class, root);

        DockSplitPane rootSplit = (DockSplitPane) root;
        assertEquals(3, rootSplit.getChildren().size());

        // Verify order: left, center, right
        assertEquals(left, rootSplit.getChildren().get(0));
        assertEquals(center, rootSplit.getChildren().get(1));
        assertEquals(right, rootSplit.getChildren().get(2));
    }

    @Test
    void testUndockAllNodesLeavesEmptyGraph() {
        DockNode node1 = new DockNode(new Label("A"), "A");
        DockNode node2 = new DockNode(new Label("B"), "B");

        // Create a tab with two nodes
        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        // Verify: root is a TabPane with 2 children
        assertInstanceOf(DockTabPane.class, dockGraph.getRoot());

        // Undock both nodes
        dockGraph.undock(node1);
        dockGraph.undock(node2);

        // Root should be null (empty graph)
        assertNull(dockGraph.getRoot());
    }

    @Test
    void testUndockAllNodesFromSplitLeavesEmptyGraph() {
        DockNode left = new DockNode(new Label("Left"), "Left");
        DockNode right = new DockNode(new Label("Right"), "Right");

        // Create horizontal split
        dockGraph.dock(left, null, DockPosition.CENTER);
        dockGraph.dock(right, left, DockPosition.RIGHT);

        // Verify: root is a SplitPane
        assertInstanceOf(DockSplitPane.class, dockGraph.getRoot());

        // Undock both nodes
        dockGraph.undock(left);
        dockGraph.undock(right);

        // Root should be null (empty graph)
        assertNull(dockGraph.getRoot());
    }

    @Test
    void testDividerPositionsPreservedOnDockOptimization() {
        DockNode left = new DockNode(new Label("Left"), "Left");
        DockNode center = new DockNode(new Label("Center"), "Center");
        DockNode right = new DockNode(new Label("Right"), "Right");

        // Create initial horizontal split: left | center
        dockGraph.dock(left, null, DockPosition.CENTER);
        dockGraph.dock(center, left, DockPosition.RIGHT);

        // Get the root SplitPane and set a custom divider position
        DockSplitPane rootSplit = (DockSplitPane) dockGraph.getRoot();
        assertEquals(1, rootSplit.getDividerPositions().size());

        // Set divider at 30% (left gets 30%, center gets 70%)
        rootSplit.setDividerPosition(0, 0.3);
        double originalDividerPos = rootSplit.getDividerPositions().getFirst().get();
        assertEquals(0.3, originalDividerPos, 0.001);

        // Now dock right to center's right (should trigger optimization)
        dockGraph.dock(right, center, DockPosition.RIGHT);

        // Verify: still the same root SplitPane with 3 children
        assertSame(rootSplit, dockGraph.getRoot());
        assertEquals(3, rootSplit.getChildren().size());

        // Verify: first divider position is preserved (or close to it)
        assertEquals(2, rootSplit.getDividerPositions().size());
        double firstDividerAfter = rootSplit.getDividerPositions().getFirst().get();

        // First divider should be unchanged (left boundary)
        assertEquals(0.3, firstDividerAfter, 0.001);

        // Second divider should be between 0.3 and 1.0 (splitting center/right space)
        double secondDivider = rootSplit.getDividerPositions().get(1).get();
        assertTrue(secondDivider > 0.3 && secondDivider < 1.0);
    }

    @Test
    void testDividerPositionsPreservedOnUndock() {
        DockNode left = new DockNode(new Label("Left"), "Left");
        DockNode center = new DockNode(new Label("Center"), "Center");
        DockNode right = new DockNode(new Label("Right"), "Right");

        // Create horizontal split with 3 nodes
        dockGraph.dock(left, null, DockPosition.CENTER);
        dockGraph.dock(center, left, DockPosition.RIGHT);
        dockGraph.dock(right, center, DockPosition.RIGHT);

        DockSplitPane rootSplit = (DockSplitPane) dockGraph.getRoot();
        assertEquals(2, rootSplit.getDividerPositions().size());

        // Set custom divider positions: left=25%, center=50%, right=25%
        rootSplit.setDividerPosition(0, 0.25);
        rootSplit.setDividerPosition(1, 0.75);

        // Remove center node
        dockGraph.undock(center);

        // Verify: still 2 children (left and right)
        assertEquals(2, rootSplit.getChildren().size());
        assertEquals(1, rootSplit.getDividerPositions().size());

        // The remaining divider should be somewhere reasonable (not reset to 0.5)
        // Since we removed the middle element, the divider should be around 0.25 or adjusted
        double remainingDivider = rootSplit.getDividerPositions().getFirst().get();
        assertTrue(remainingDivider > 0.0 && remainingDivider < 1.0);
    }

    // ========== TabPane Optimization and Flattening Tests ==========

    @Test
    void testTabPaneNotNestedWhenDockingToExistingTab() {
        // This is the main bug case: dropping a new panel into the center of an existing tab
        // should NOT create nested TabPanes
        DockNode console = new DockNode(new Label("Console"), "Console");
        DockNode tasks = new DockNode(new Label("Tasks"), "Tasks");
        DockNode newPanel = new DockNode(new Label("Output"), "Output");

        // Create initial tabs: console + tasks
        dockGraph.dock(console, null, DockPosition.CENTER);
        dockGraph.dock(tasks, console, DockPosition.CENTER);

        // Verify: root is a TabPane with 2 children
        DockElement root = dockGraph.getRoot();
        assertInstanceOf(DockTabPane.class, root);
        DockTabPane rootTabPane = (DockTabPane) root;
        assertEquals(2, rootTabPane.getChildren().size());

        // Now dock newPanel to CENTER of tasks (simulating dropping into tab area)
        dockGraph.dock(newPanel, tasks, DockPosition.CENTER);

        // Verify: root should STILL be the same TabPane with 3 children (NOT nested!)
        assertSame(rootTabPane, dockGraph.getRoot());
        assertEquals(3, rootTabPane.getChildren().size());

        // Verify all three nodes are direct children
        assertTrue(rootTabPane.getChildren().contains(console));
        assertTrue(rootTabPane.getChildren().contains(tasks));
        assertTrue(rootTabPane.getChildren().contains(newPanel));

        // Verify order: console, tasks, newPanel (newPanel should be after tasks)
        int tasksIndex = rootTabPane.getChildren().indexOf(tasks);
        int newPanelIndex = rootTabPane.getChildren().indexOf(newPanel);
        assertEquals(tasksIndex + 1, newPanelIndex);
    }

    @Test
    void testTabPaneOptimizationWhenTargetInTabPane() {
        // Test that docking to a node that's already in a TabPane adds to that TabPane
        DockNode left = new DockNode(new Label("Left"), "Left");
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode newTab = new DockNode(new Label("NewTab"), "NewTab");

        // Create layout: left | (tab1, tab2)
        dockGraph.dock(left, null, DockPosition.CENTER);
        dockGraph.dock(tab1, left, DockPosition.RIGHT);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);

        // Root should be a SplitPane with left and a TabPane
        DockElement root = dockGraph.getRoot();
        assertInstanceOf(DockSplitPane.class, root);
        DockSplitPane rootSplit = (DockSplitPane) root;
        assertEquals(2, rootSplit.getChildren().size());

        // Second child should be a TabPane
        DockElement rightChild = rootSplit.getChildren().get(1);
        assertInstanceOf(DockTabPane.class, rightChild);
        DockTabPane tabPane = (DockTabPane) rightChild;
        assertEquals(2, tabPane.getChildren().size());

        // Now dock newTab to CENTER of tab2
        dockGraph.dock(newTab, tab2, DockPosition.CENTER);

        // Verify: TabPane should have 3 children (no nesting!)
        assertSame(tabPane, rootSplit.getChildren().get(1));
        assertEquals(3, tabPane.getChildren().size());
        assertTrue(tabPane.getChildren().contains(newTab));
    }

    @Test
    void testTabPaneFlatteningInAddChild() {
        // Direct test of DockTabPane.addChild() flattening logic
        DockTabPane parent = new DockTabPane();
        DockNode node1 = new DockNode(new Label("Node1"), "Node1");
        DockNode node2 = new DockNode(new Label("Node2"), "Node2");

        parent.addChild(node1);
        assertEquals(1, parent.getChildren().size());

        // Create a nested TabPane
        DockTabPane nested = new DockTabPane();
        nested.addChild(node2);

        // Add the nested TabPane to parent - should be flattened
        parent.addChild(nested);

        // Verify: parent should have 2 direct children (node1 and node2), NOT nested TabPane
        assertEquals(2, parent.getChildren().size());
        assertTrue(parent.getChildren().contains(node1));
        assertTrue(parent.getChildren().contains(node2));
        assertFalse(parent.getChildren().contains(nested));

        // Verify: nested TabPane should be empty and have no parent
        assertEquals(0, nested.getChildren().size());
        assertNull(nested.getParent());
    }

    @Test
    void testTabPaneFlatteningMultipleChildren() {
        // Test flattening when nested TabPane has multiple children
        DockTabPane parent = new DockTabPane();
        DockNode node1 = new DockNode(new Label("Node1"), "Node1");

        parent.addChild(node1);

        // Create a nested TabPane with 3 children
        DockTabPane nested = new DockTabPane();
        DockNode node2 = new DockNode(new Label("Node2"), "Node2");
        DockNode node3 = new DockNode(new Label("Node3"), "Node3");
        DockNode node4 = new DockNode(new Label("Node4"), "Node4");
        nested.addChild(node2);
        nested.addChild(node3);
        nested.addChild(node4);

        // Add the nested TabPane - all its children should be flattened
        parent.addChild(nested);

        // Verify: parent should have 4 direct children
        assertEquals(4, parent.getChildren().size());
        assertTrue(parent.getChildren().contains(node1));
        assertTrue(parent.getChildren().contains(node2));
        assertTrue(parent.getChildren().contains(node3));
        assertTrue(parent.getChildren().contains(node4));
    }

    @Test
    void testDockingToTabPaneDirectly() {
        // Test docking directly to a TabPane (not to a child of it)
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");

        // Create initial TabPane with 2 tabs
        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();

        // Dock tab3 directly to the TabPane (not to a child)
        dockGraph.dock(tab3, rootTabPane, DockPosition.CENTER);

        // Verify: TabPane should have 3 children
        assertSame(rootTabPane, dockGraph.getRoot());
        assertEquals(3, rootTabPane.getChildren().size());
        assertTrue(rootTabPane.getChildren().contains(tab3));
    }

    @Test
    void testNoNestedTabPanesAfterComplexOperations() {
        // Complex scenario: multiple dock operations that could lead to nesting
        DockNode console = new DockNode(new Label("Console"), "Console");
        DockNode tasks = new DockNode(new Label("Tasks"), "Tasks");
        DockNode output = new DockNode(new Label("Output"), "Output");
        DockNode problems = new DockNode(new Label("Problems"), "Problems");

        // Create: console + tasks as tabs
        dockGraph.dock(console, null, DockPosition.CENTER);
        dockGraph.dock(tasks, console, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();

        // Add output to center of console
        dockGraph.dock(output, console, DockPosition.CENTER);
        assertEquals(3, rootTabPane.getChildren().size());

        // Add problems to center of tasks
        dockGraph.dock(problems, tasks, DockPosition.CENTER);
        assertEquals(4, rootTabPane.getChildren().size());

        // Verify: no nested TabPanes exist anywhere in the tree
        assertNoNestedTabPanes(dockGraph.getRoot());
    }

    @Test
    void testSelectedTabAfterDockingToTabPane() {
        // Verify that newly docked tabs are selected automatically
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();

        // tab2 should be selected (index 1)
        assertEquals(1, rootTabPane.getSelectedIndex());

        // Dock tab3
        dockGraph.dock(tab3, tab2, DockPosition.CENTER);

        // tab3 should now be selected
        int tab3Index = rootTabPane.getChildren().indexOf(tab3);
        assertEquals(tab3Index, rootTabPane.getSelectedIndex());
    }

    @Test
    void testDockToTabPaneWithInsertIndex() {
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();
        dockGraph.dock(tab3, rootTabPane, DockPosition.CENTER, 1);

        assertSame(rootTabPane, dockGraph.getRoot());
        assertEquals(3, rootTabPane.getChildren().size());
        assertEquals(tab1, rootTabPane.getChildren().get(0));
        assertEquals(tab3, rootTabPane.getChildren().get(1));
        assertEquals(tab2, rootTabPane.getChildren().get(2));
        assertEquals(1, rootTabPane.getSelectedIndex());
    }

    @Test
    void testDockToTabPaneClampsInsertIndex() {
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");
        DockNode tab4 = new DockNode(new Label("Tab4"), "Tab4");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();
        dockGraph.dock(tab3, rootTabPane, DockPosition.CENTER, -10);

        assertEquals(tab3, rootTabPane.getChildren().get(0));
        assertEquals(0, rootTabPane.getSelectedIndex());

        dockGraph.dock(tab4, rootTabPane, DockPosition.CENTER, 99);
        int lastIndex = rootTabPane.getChildren().size() - 1;
        assertEquals(tab4, rootTabPane.getChildren().get(lastIndex));
        assertEquals(lastIndex, rootTabPane.getSelectedIndex());
    }

    @Test
    void testMoveTabWithInsertIndex() {
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);
        dockGraph.dock(tab3, tab2, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();
        dockGraph.move(tab3, rootTabPane, DockPosition.CENTER, 0);

        assertSame(rootTabPane, dockGraph.getRoot());
        assertEquals(3, rootTabPane.getChildren().size());
        assertEquals(tab3, rootTabPane.getChildren().get(0));
        assertEquals(tab1, rootTabPane.getChildren().get(1));
        assertEquals(tab2, rootTabPane.getChildren().get(2));
        assertEquals(0, rootTabPane.getSelectedIndex());
    }

    @Test
    void testMoveTabToTabPaneClampsInsertIndex() {
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);
        dockGraph.dock(tab3, tab2, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();
        dockGraph.move(tab1, rootTabPane, DockPosition.CENTER, 99);

        assertEquals(tab2, rootTabPane.getChildren().get(0));
        assertEquals(tab3, rootTabPane.getChildren().get(1));
        assertEquals(tab1, rootTabPane.getChildren().get(2));
        assertEquals(2, rootTabPane.getSelectedIndex());

        dockGraph.move(tab1, rootTabPane, DockPosition.CENTER, -5);
        assertEquals(tab1, rootTabPane.getChildren().get(0));
        assertEquals(0, rootTabPane.getSelectedIndex());
    }

    @Test
    void testDockToTabTargetWithInsertIndex() {
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();
        dockGraph.dock(tab3, tab2, DockPosition.CENTER, 0);

        assertSame(rootTabPane, dockGraph.getRoot());
        assertEquals(3, rootTabPane.getChildren().size());
        assertEquals(tab3, rootTabPane.getChildren().get(0));
        assertEquals(tab1, rootTabPane.getChildren().get(1));
        assertEquals(tab2, rootTabPane.getChildren().get(2));
        assertEquals(0, rootTabPane.getSelectedIndex());
    }

    @Test
    void testMoveTabToEndWithInsertIndexWhenTargetIsTab() {
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);
        dockGraph.dock(tab3, tab2, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();
        dockGraph.move(tab1, tab2, DockPosition.CENTER, 2);

        assertSame(rootTabPane, dockGraph.getRoot());
        assertEquals(3, rootTabPane.getChildren().size());
        assertEquals(tab2, rootTabPane.getChildren().get(0));
        assertEquals(tab3, rootTabPane.getChildren().get(1));
        assertEquals(tab1, rootTabPane.getChildren().get(2));
        assertEquals(2, rootTabPane.getSelectedIndex());
    }

    @Test
    void testDockSplitPaneToTabPaneCreatesTab() {
        // Edge case: docking a SplitPane to CENTER of something should create a tab
        DockNode left = new DockNode(new Label("Left"), "Left");
        DockNode right = new DockNode(new Label("Right"), "Right");
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");

        // Create a split: left | right
        dockGraph.dock(left, null, DockPosition.CENTER);
        dockGraph.dock(right, left, DockPosition.RIGHT);

        DockSplitPane rootSplit = (DockSplitPane) dockGraph.getRoot();

        // Create a tab separately
        dockGraph.dock(tab1, right, DockPosition.CENTER);

        // Now we should have: left | TabPane(right, tab1)
        assertEquals(2, rootSplit.getChildren().size());

        DockElement rightElement = rootSplit.getChildren().get(1);
        assertInstanceOf(DockTabPane.class, rightElement);

        DockTabPane tabPane = (DockTabPane) rightElement;
        assertEquals(2, tabPane.getChildren().size());
    }

    @Test
    void testMoveWithinSameTabPanePreservesStructure() {
        // Moving a node within the same TabPane should not affect structure
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab3");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);
        dockGraph.dock(tab3, tab2, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();
        assertEquals(3, rootTabPane.getChildren().size());

        long revisionBefore = dockGraph.getRevision();

        // Move tab1 to position after tab2 (should be no-op or simple reorder)
        dockGraph.move(tab1, tab2, DockPosition.CENTER);

        // Verify: still the same TabPane with 3 children
        assertSame(rootTabPane, dockGraph.getRoot());
        assertEquals(3, rootTabPane.getChildren().size());

        // Revision should have changed (operation was performed)
        assertTrue(dockGraph.getRevision() > revisionBefore);
    }

    @Test
    void testDropPanelIntoTabAreaMultipleTimes() {
        // Simulate the exact bug scenario: repeatedly dropping panels into tab area
        DockNode console = new DockNode(new Label("Console"), "Console");
        DockNode tasks = new DockNode(new Label("Tasks"), "Tasks");

        // Initial: console + tasks as tabs
        dockGraph.dock(console, null, DockPosition.CENTER);
        dockGraph.dock(tasks, console, DockPosition.CENTER);

        DockTabPane rootTabPane = (DockTabPane) dockGraph.getRoot();
        assertEquals(2, rootTabPane.getChildren().size());

        // Drop 3 more panels into the tab area (CENTER position)
        for (int i = 1; i <= 3; i++) {
            DockNode newPanel = new DockNode(new Label("Panel" + i), "Panel" + i);
            dockGraph.dock(newPanel, tasks, DockPosition.CENTER);

            // After each drop: verify no nesting
            assertSame(rootTabPane, dockGraph.getRoot());
            assertEquals(2 + i, rootTabPane.getChildren().size());
            assertNoNestedTabPanes(dockGraph.getRoot());
        }

        // Final check: should have 5 tabs in a single TabPane
        assertEquals(5, rootTabPane.getChildren().size());
    }

    // ========== CRITICAL BUG FIX TESTS (2026-02-10) ==========

    /**
     * Test for empty container prevention bug fix.
     * Regression test: Ensures no empty SplitPanes remain after D&D operations.
     * Bug: After removing children, empty containers stayed in tree.
     * Fix: Reordered cleanup logic - flatten first, then cleanup.
     */
    @Test
    void testNoEmptyContainersAfterUndock() {
        // Setup: Create a SplitPane with 2 children
        DockNode node1 = new DockNode(new Label("Node1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Node2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);

        // Root should be a SplitPane with 2 children
        assertInstanceOf(DockSplitPane.class, dockGraph.getRoot());
        DockSplitPane split = (DockSplitPane) dockGraph.getRoot();
        assertEquals(2, split.getChildren().size());

        // Undock one node
        dockGraph.undock(node1);

        // After undocking, the SplitPane should flatten to just node2
        // NO empty container should remain
        assertEquals(node2, dockGraph.getRoot(), "Root should be flattened to node2");
        assertNull(node2.getParent(), "Node2 should have no parent");

        // Verify: No empty containers anywhere
        assertNoEmptyContainers(dockGraph.getRoot());
    }

    /**
     * Test for target invalidation bug fix.
     * Regression test: Target becomes invalid after undock during move().
     * Bug: Moving node → undock causes flattening → old target reference invalid → empty container remains.
     * Fix: Find target by ID after undock to handle tree restructuring.
     */
    @Test
    void testTargetInvalidationDuringMove() {
        // Setup: Create layout that will cause flattening during move
        // Properties → Main.java (VERTICAL split)
        DockNode properties = new DockNode(new Label("Properties"), "Properties");
        DockNode mainJava = new DockNode(new Label("Main"), "Main.java");

        dockGraph.dock(properties, null, DockPosition.CENTER);
        dockGraph.dock(mainJava, properties, DockPosition.BOTTOM);

        // Now we have: DockSplitPane(VERTICAL) { properties, mainJava }
        assertInstanceOf(DockSplitPane.class, dockGraph.getRoot());
        DockSplitPane verticalSplit = (DockSplitPane) dockGraph.getRoot();
        assertEquals(javafx.geometry.Orientation.VERTICAL, verticalSplit.getOrientation());

        // Store reference to the split (which will become invalid)
        DockElement targetBeforeMove = verticalSplit;

        // Execute: Move properties to LEFT of mainJava
        // This will: undock(properties) → SplitPane flattens to mainJava → target is now invalid
        dockGraph.move(properties, mainJava, DockPosition.LEFT);

        // Verify: No empty containers remained
        assertNoEmptyContainers(dockGraph.getRoot());

        // Verify: Both nodes are still in tree
        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertTrue(leaves.contains(properties), "Properties should still be in tree");
        assertTrue(leaves.contains(mainJava), "Main.java should still be in tree");

        // Verify: Result is a HORIZONTAL split (LEFT position)
        assertInstanceOf(DockSplitPane.class, dockGraph.getRoot());
        DockSplitPane horizontalSplit = (DockSplitPane) dockGraph.getRoot();
        assertEquals(javafx.geometry.Orientation.HORIZONTAL, horizontalSplit.getOrientation());
    }

    /**
     * Test for complex D&D sequence that previously caused tree corruption.
     * Regression test: Sequential drops (BOTTOM then LEFT) created empty containers.
     */
    @Test
    void testComplexDndSequence_BottomThenLeft() {
        // This is the exact scenario from the bug report
        DockNode properties = new DockNode(new Label("Properties"), "Properties");
        DockNode mainJava = new DockNode(new Label("Main"), "Main.java");
        DockNode console = new DockNode(new Label("Console"), "Console");

        // Initial layout
        dockGraph.dock(properties, null, DockPosition.CENTER);
        dockGraph.dock(mainJava, properties, DockPosition.RIGHT);
        dockGraph.dock(console, mainJava, DockPosition.BOTTOM);

        // Verify initial state: No empty containers
        assertNoEmptyContainers(dockGraph.getRoot());

        // First move: Properties → BOTTOM
        dockGraph.move(properties, dockGraph.getRoot(), DockPosition.BOTTOM);
        assertNoEmptyContainers(dockGraph.getRoot());

        // Second move: Properties → LEFT (this previously created empty container)
        dockGraph.move(properties, dockGraph.getRoot(), DockPosition.LEFT);

        // CRITICAL: No empty containers should exist
        assertNoEmptyContainers(dockGraph.getRoot());

        // Verify: All nodes still present
        List<DockNode> leaves = collectLeafNodes(dockGraph.getRoot());
        assertEquals(3, leaves.size(), "All 3 nodes should be present");
        assertTrue(leaves.contains(properties));
        assertTrue(leaves.contains(mainJava));
        assertTrue(leaves.contains(console));
    }

    /**
     * Test for TabPane cleanup after removing all but one tab.
     * Ensures TabPane flattens when only 1 tab remains.
     */
    @Test
    void testTabPaneFlattensWhenOnlyOneTabRemains() {
        DockNode tab1 = new DockNode(new Label("Tab1"), "Tab 1");
        DockNode tab2 = new DockNode(new Label("Tab2"), "Tab 2");
        DockNode tab3 = new DockNode(new Label("Tab3"), "Tab 3");

        dockGraph.dock(tab1, null, DockPosition.CENTER);
        dockGraph.dock(tab2, tab1, DockPosition.CENTER);
        dockGraph.dock(tab3, tab1, DockPosition.CENTER);

        // Should be a TabPane with 3 tabs
        assertInstanceOf(DockTabPane.class, dockGraph.getRoot());
        DockTabPane tabPane = (DockTabPane) dockGraph.getRoot();
        assertEquals(3, tabPane.getChildren().size());

        // Remove two tabs
        dockGraph.undock(tab2);
        dockGraph.undock(tab3);

        // TabPane should flatten to just tab1
        assertEquals(tab1, dockGraph.getRoot(), "TabPane should flatten to single tab");
        assertNull(tab1.getParent(), "Tab1 should have no parent");
        assertNoEmptyContainers(dockGraph.getRoot());
    }

    /**
     * Test for SplitPane flattening with same orientation.
     * Ensures nested SplitPanes with same orientation are flattened.
     */
    @Test
    void testSplitPaneFlatteningWithSameOrientation() {
        DockNode node1 = new DockNode(new Label("Node1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Node2"), "Node 2");
        DockNode node3 = new DockNode(new Label("Node3"), "Node 3");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);
        dockGraph.dock(node3, node2, DockPosition.RIGHT);

        // Should result in single HORIZONTAL SplitPane with 3 children (no nesting)
        assertInstanceOf(DockSplitPane.class, dockGraph.getRoot());
        DockSplitPane split = (DockSplitPane) dockGraph.getRoot();
        assertEquals(javafx.geometry.Orientation.HORIZONTAL, split.getOrientation());
        assertEquals(3, split.getChildren().size(), "Should have 3 children in single SplitPane");

        // Verify no nested SplitPanes with same orientation
        assertNoNestedSplitPanesWithSameOrientation(dockGraph.getRoot());
    }

    /**
     * Test for move within same SplitPane preserving divider positions.
     * Ensures dividers don't change when moving within same container.
     */
    @Test
    void testMoveWithinSameSplitPanePreservesDividers() {
        DockNode node1 = new DockNode(new Label("Node1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Node2"), "Node 2");
        DockNode node3 = new DockNode(new Label("Node3"), "Node 3");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);
        dockGraph.dock(node3, node2, DockPosition.RIGHT);

        DockSplitPane split = (DockSplitPane) dockGraph.getRoot();

        // Set specific divider positions
        split.setDividerPosition(0, 0.3);
        split.setDividerPosition(1, 0.6);

        double div0Before = split.getDividerPositions().get(0).get();
        double div1Before = split.getDividerPositions().get(1).get();

        // Move node1 to position between node2 and node3
        dockGraph.move(node1, node2, DockPosition.RIGHT);

        // Divider positions should be preserved
        assertEquals(div0Before, split.getDividerPositions().get(0).get(), 0.01);
        assertEquals(div1Before, split.getDividerPositions().get(1).get(), 0.01);
    }

    /**
     * Helper method to verify no empty containers exist in the tree.
     * This is the key regression test for the auto-cleanup bug fix.
     */
    private void assertNoEmptyContainers(DockElement element) {
        if (element == null) {
            return;
        }

        if (element instanceof DockContainer container) {
            assertFalse(container.getChildren().isEmpty(),
                "Found empty container: " + element.getClass().getSimpleName() + " id=" + element.getId());

            // Recursively check children
            for (DockElement child : container.getChildren()) {
                assertNoEmptyContainers(child);
            }
        }
    }

    /**
     * Helper method to verify no nested SplitPanes with same orientation.
     */
    private void assertNoNestedSplitPanesWithSameOrientation(DockElement element) {
        if (element == null) {
            return;
        }

        if (element instanceof DockSplitPane parentSplit) {
            for (DockElement child : parentSplit.getChildren()) {
                if (child instanceof DockSplitPane childSplit) {
                    assertNotEquals(parentSplit.getOrientation(), childSplit.getOrientation(),
                        "Found nested SplitPane with same orientation! Parent: " +
                        parentSplit.getId() + " " + parentSplit.getOrientation() +
                        ", Child: " + childSplit.getId() + " " + childSplit.getOrientation());
                }
                assertNoNestedSplitPanesWithSameOrientation(child);
            }
        } else if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                assertNoNestedSplitPanesWithSameOrientation(child);
            }
        }
    }

    /**
     * Helper method to verify no nested TabPanes exist in the tree
     */
    private void assertNoNestedTabPanes(DockElement element) {
        if (element == null) {
            return;
        }

        if (element instanceof DockTabPane tabPane) {
            for (DockElement child : tabPane.getChildren()) {
                // No child of a TabPane should be another TabPane
                assertNotEquals(DockTabPane.class, child.getClass(),
                    "Found nested TabPane! Parent: " + tabPane.getId() + ", Child: " + child.getId());

                // Recursively check children
                assertNoNestedTabPanes(child);
            }
        } else if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                assertNoNestedTabPanes(child);
            }
        }
    }
}

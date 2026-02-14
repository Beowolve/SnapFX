package com.github.beowolve.snapfx.view;

import com.github.beowolve.snapfx.model.*;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DockLayoutEngine using TestFX.
 */
class DockLayoutEngineTest extends ApplicationTest {
    private DockGraph dockGraph;
    private DockLayoutEngine layoutEngine;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX for headless tests without Swing
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX is already running
        }
    }

    @BeforeEach
    void setUp() {
        dockGraph = new DockGraph();
        layoutEngine = new DockLayoutEngine(dockGraph, new com.github.beowolve.snapfx.dnd.DockDragService(dockGraph));
    }

    @Test
    void testBuildEmptyGraph() {
        Node view = layoutEngine.buildSceneGraph();
        assertNotNull(view);
    }

    @Test
    void testBuildSingleNode() {
        DockNode node = new DockNode(new Label("Test"), "Test Node");
        dockGraph.setRoot(node);

        Node view = layoutEngine.buildSceneGraph();
        assertNotNull(view);
        assertInstanceOf(DockNodeView.class, view);

        DockNodeView nodeView = (DockNodeView) view;
        assertEquals(node, nodeView.getDockNode());
    }

    @Test
    void testBuildTabPane() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        Node view = layoutEngine.buildSceneGraph();
        assertNotNull(view);
        assertInstanceOf(TabPane.class, view);

        TabPane tabPane = (TabPane) view;
        assertEquals(2, tabPane.getTabs().size());
    }

    @Test
    void testDockNodeTabTextUsesTitle() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        TabPane tabPane = (TabPane) layoutEngine.buildSceneGraph();
        assertEquals("Node 1", tabPane.getTabs().get(0).getText());
        assertTrue(tabPane.getTabs().get(0).getStyleClass().contains("dock-tab-graphic"));

        node1.setTitle("Renamed");
        assertEquals("Renamed", tabPane.getTabs().get(0).getText());
    }

    /**
     * Regression test: Tab close must use the same close handler as the title bar.
     * Bug: Closing a tab bypassed onNodeCloseRequest, so hidden nodes were not tracked.
     * Fix: Route tab close requests through the shared close handler.
     * Date: 2026-02-14
     */
    @Test
    void testTabCloseRequestUsesCloseHandler() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        AtomicReference<DockNode> closedNode = new AtomicReference<>();
        layoutEngine.setOnNodeCloseRequest(closedNode::set);

        TabPane tabPane = (TabPane) layoutEngine.buildSceneGraph();
        Tab tab = tabPane.getTabs().get(0);
        Event event = new Event(Tab.TAB_CLOSE_REQUEST_EVENT);

        tab.getOnCloseRequest().handle(event);

        assertEquals(node1, closedNode.get());
        assertTrue(event.isConsumed());
    }

    @Test
    void testCloseButtonModeTitleOnlyHidesTabClose() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        layoutEngine.setCloseButtonMode(DockCloseButtonMode.TITLE_ONLY);

        TabPane tabPane = (TabPane) layoutEngine.buildSceneGraph();
        for (Tab tab : tabPane.getTabs()) {
            assertFalse(tab.isClosable());
        }
    }

    @Test
    void testCloseButtonModeTabOnlyHidesTitleClose() {
        DockNode node = new DockNode(new Label("Test"), "Node 1");
        dockGraph.setRoot(node);

        layoutEngine.setCloseButtonMode(DockCloseButtonMode.TAB_ONLY);

        DockNodeView nodeView = (DockNodeView) layoutEngine.buildSceneGraph();
        assertFalse(nodeView.isCloseButtonVisible());
    }

    @Test
    void testTitleBarModeAutoHidesHeaderInTabPane() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        layoutEngine.setTitleBarMode(DockTitleBarMode.AUTO);

        layoutEngine.buildSceneGraph();
        DockNodeView nodeView = layoutEngine.getDockNodeView(node1);
        assertNotNull(nodeView);
        assertFalse(nodeView.getHeader().isVisible());
        assertFalse(nodeView.getHeader().isManaged());
    }

    @Test
    void testBuildSplitPane() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.LEFT);

        Node view = layoutEngine.buildSceneGraph();
        assertNotNull(view);
        assertInstanceOf(SplitPane.class, view);

        SplitPane splitPane = (SplitPane) view;
        assertEquals(2, splitPane.getItems().size());
    }

    @Test
    void testClearCache() {
        DockNode node = new DockNode(new Label("Test"), "Test Node");
        dockGraph.setRoot(node);

        layoutEngine.buildSceneGraph();
        assertNotNull(layoutEngine.getDockNodeView(node));

        // clearCache should remove cached views
        layoutEngine.clearCache();
        assertNull(layoutEngine.getDockNodeView(node));

        // Rebuild repopulates the cache
        layoutEngine.buildSceneGraph();
        assertNotNull(layoutEngine.getDockNodeView(node));

        // Remove node from graph
        dockGraph.undock(node);
        layoutEngine.clearCache();

        // Now the view should be removed since node is no longer in graph
        assertNull(layoutEngine.getDockNodeView(node));
    }

    @Test
    void testRebuildAfterChange() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);

        Node view1 = layoutEngine.buildSceneGraph();
        assertInstanceOf(DockNodeView.class, view1);

        dockGraph.dock(node2, node1, DockPosition.CENTER);

        Node view2 = layoutEngine.buildSceneGraph();
        assertInstanceOf(TabPane.class, view2);
    }
}

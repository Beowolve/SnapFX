package com.github.beowolve.snapfx.view;

import com.github.beowolve.snapfx.close.DockCloseSource;
import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.model.*;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DockLayoutEngine using TestFX.
 */
class DockLayoutEngineTest extends ApplicationTest {
    private DockGraph dockGraph;
    private DockLayoutEngine layoutEngine;
    private DockDragService dragService;

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
        dragService = new DockDragService(dockGraph);
        layoutEngine = new DockLayoutEngine(dockGraph, dragService);
    }

    @Test
    void testBuildEmptyGraph() {
        Node view = layoutEngine.buildSceneGraph();
        assertNotNull(view);
    }

    @Test
    void testBuildEmptyGraphReusesStableEmptyLayout() {
        Node first = layoutEngine.buildSceneGraph();
        Node second = layoutEngine.buildSceneGraph();

        assertInstanceOf(StackPane.class, first);
        assertSame(first, second);
    }

    @Test
    void testDefaultTitleBarModeIsAuto() {
        assertEquals(DockTitleBarMode.AUTO, layoutEngine.getTitleBarMode());
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
        AtomicReference<DockCloseSource> closeSource = new AtomicReference<>();
        layoutEngine.setOnNodeCloseRequest((node, source) -> {
            closedNode.set(node);
            closeSource.set(source);
        });

        TabPane tabPane = (TabPane) layoutEngine.buildSceneGraph();
        Tab tab = tabPane.getTabs().get(0);
        Event event = new Event(Tab.TAB_CLOSE_REQUEST_EVENT);

        tab.getOnCloseRequest().handle(event);

        assertEquals(node1, closedNode.get());
        assertEquals(DockCloseSource.TAB, closeSource.get());
        assertTrue(event.isConsumed());
    }

    @Test
    void testTabContextMenuContainsExpectedActions() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        TabPane tabPane = assertInstanceOf(TabPane.class, layoutEngine.buildSceneGraph());
        ContextMenu contextMenu = tabPane.getTabs().getFirst().getContextMenu();
        assertNotNull(contextMenu);

        List<String> itemLabels = contextMenu.getItems().stream().map(MenuItem::getText).toList();
        assertTrue(itemLabels.contains("Close"));
        assertTrue(itemLabels.contains("Close Others"));
        assertTrue(itemLabels.contains("Close All"));
        assertTrue(itemLabels.contains("Float"));
        assertMenuItemHasIcon(contextMenu, "Close", "dock-control-icon-close");
        assertMenuItemHasIcon(contextMenu, "Float", "dock-control-icon-float");
    }

    @Test
    void testTabContextMenuCloseOthersUsesTabCloseSource() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");
        DockNode node3 = new DockNode(new Label("Test3"), "Node 3");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);
        dockGraph.dock(node3, node2, DockPosition.CENTER);

        List<DockNode> closedNodes = new ArrayList<>();
        List<DockCloseSource> closeSources = new ArrayList<>();
        layoutEngine.setOnNodeCloseRequest((node, source) -> {
            closedNodes.add(node);
            closeSources.add(source);
        });

        TabPane tabPane = assertInstanceOf(TabPane.class, layoutEngine.buildSceneGraph());
        ContextMenu contextMenu = tabPane.getTabs().getFirst().getContextMenu();
        MenuItem closeOthersItem = contextMenu.getItems().stream()
            .filter(item -> "Close Others".equals(item.getText()))
            .findFirst()
            .orElseThrow();

        closeOthersItem.fire();

        assertEquals(2, closedNodes.size());
        assertTrue(closedNodes.contains(node2));
        assertTrue(closedNodes.contains(node3));
        assertTrue(closeSources.stream().allMatch(source -> source == DockCloseSource.TAB));
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
    void testTitleCloseButtonUsesCssCloseGlyph() {
        DockNode node = new DockNode(new Label("Test"), "Node 1");
        dockGraph.setRoot(node);

        DockNodeView nodeView = assertInstanceOf(DockNodeView.class, layoutEngine.buildSceneGraph());

        Button closeButton = nodeView.getHeader().getChildren().stream()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> !button.getStyleClass().contains("dock-node-float-button"))
            .findFirst()
            .orElseThrow();

        Node closeGraphic = closeButton.getGraphic();
        assertNotNull(closeGraphic);
        assertTrue(closeGraphic.getStyleClass().contains("dock-control-icon"));
        assertTrue(closeGraphic.getStyleClass().contains("dock-control-icon-close"));

        MouseEvent press = createPrimaryPressEvent(nodeView.getHeader(), closeButton);
        nodeView.getHeader().getOnMousePressed().handle(press);
        assertFalse(dragService.isDragging());
    }

    @Test
    void testTitleBarCloseReportsTitleBarSource() {
        DockNode node = new DockNode(new Label("Test"), "Node 1");
        dockGraph.setRoot(node);

        AtomicReference<DockNode> closedNode = new AtomicReference<>();
        AtomicReference<DockCloseSource> closeSource = new AtomicReference<>();
        layoutEngine.setOnNodeCloseRequest((dockNode, source) -> {
            closedNode.set(dockNode);
            closeSource.set(source);
        });

        DockNodeView nodeView = assertInstanceOf(DockNodeView.class, layoutEngine.buildSceneGraph());

        Button closeButton = nodeView.getHeader().getChildren().stream()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> !button.getStyleClass().contains("dock-node-float-button"))
            .findFirst()
            .orElseThrow();

        closeButton.fire();

        assertEquals(node, closedNode.get());
        assertEquals(DockCloseSource.TITLE_BAR, closeSource.get());
    }

    @Test
    void testTabFloatButtonUsesCssFloatGlyph() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        TabPane tabPane = assertInstanceOf(TabPane.class, layoutEngine.buildSceneGraph());
        Node tabHeader = tabPane.getTabs().getFirst().getGraphic();

        Button floatButton = ((HBox) tabHeader).getChildren().stream()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.getStyleClass().contains("dock-tab-float-button"))
            .findFirst()
            .orElseThrow();

        Node floatGraphic = floatButton.getGraphic();
        assertNotNull(floatGraphic);
        assertTrue(floatGraphic.getStyleClass().contains("dock-control-icon"));
        assertTrue(floatGraphic.getStyleClass().contains("dock-control-icon-float"));

        MouseEvent press = createPrimaryPressEvent((HBox) tabHeader, floatButton);
        ((HBox) tabHeader).getOnMousePressed().handle(press);
        assertFalse(dragService.isDragging());
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
    void testSplitPaneContextMenuResetItemAppliesBalancedRatios() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);

        SplitPane splitPane = assertInstanceOf(SplitPane.class, layoutEngine.buildSceneGraph());
        splitPane.setDividerPositions(0.8);

        ContextMenu contextMenu = splitPane.getContextMenu();
        assertNotNull(contextMenu);
        MenuItem resetItem = contextMenu.getItems().stream()
            .filter(item -> "Reset Splitter Ratios".equals(item.getText()))
            .findFirst()
            .orElseThrow();

        resetItem.fire();

        assertEquals(0.5, splitPane.getDividerPositions()[0], 0.0001);
        DockSplitPane model = assertInstanceOf(DockSplitPane.class, dockGraph.getRoot());
        assertEquals(0.5, model.getDividerPositions().get(0).get(), 0.0001);
    }

    @Test
    void testHeaderContextMenuFloatActionUsesCallback() {
        DockNode node = new DockNode(new Label("Test"), "Node 1");
        dockGraph.setRoot(node);

        AtomicReference<DockNode> floatedNode = new AtomicReference<>();
        layoutEngine.setOnNodeFloatRequest(floatedNode::set);

        DockNodeView nodeView = assertInstanceOf(DockNodeView.class, layoutEngine.buildSceneGraph());
        ContextMenu headerContextMenu = nodeView.getHeaderContextMenu();
        assertNotNull(headerContextMenu);

        MenuItem floatItem = headerContextMenu.getItems().stream()
            .filter(item -> "Float".equals(item.getText()))
            .findFirst()
            .orElseThrow();
        assertMenuItemHasIcon(headerContextMenu, "Close", "dock-control-icon-close");
        assertMenuItemHasIcon(headerContextMenu, "Float", "dock-control-icon-float");
        floatItem.fire();

        assertEquals(node, floatedNode.get());
    }

    @Test
    void testHeaderContextMenuHidesFloatWhenPredicateBlocksNode() {
        DockNode node = new DockNode(new Label("Test"), "Node 1");
        dockGraph.setRoot(node);
        layoutEngine.setCanFloatNodePredicate(ignored -> false);

        DockNodeView nodeView = assertInstanceOf(DockNodeView.class, layoutEngine.buildSceneGraph());
        ContextMenu headerContextMenu = nodeView.getHeaderContextMenu();
        assertNotNull(headerContextMenu);

        MenuItem floatItem = headerContextMenu.getItems().stream()
            .filter(item -> "Float".equals(item.getText()))
            .findFirst()
            .orElseThrow();
        invokeContextMenuOnShowing(headerContextMenu);

        assertFalse(floatItem.isVisible());
        assertTrue(floatItem.isDisable());
    }

    @Test
    void testTabContextMenuHidesFloatWhenPredicateBlocksNode() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");
        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);
        layoutEngine.setCanFloatNodePredicate(ignored -> false);

        TabPane tabPane = assertInstanceOf(TabPane.class, layoutEngine.buildSceneGraph());
        ContextMenu contextMenu = tabPane.getTabs().getFirst().getContextMenu();
        assertNotNull(contextMenu);
        MenuItem floatItem = contextMenu.getItems().stream()
            .filter(item -> "Float".equals(item.getText()))
            .findFirst()
            .orElseThrow();
        SeparatorMenuItem separator = contextMenu.getItems().stream()
            .filter(SeparatorMenuItem.class::isInstance)
            .map(SeparatorMenuItem.class::cast)
            .findFirst()
            .orElseThrow();

        invokeContextMenuOnShowing(contextMenu);

        assertFalse(floatItem.isVisible());
        assertTrue(floatItem.isDisable());
        assertFalse(separator.isVisible());
    }

    @Test
    void testSingleChildSplitRootIsOptimizedToNodeView() {
        DockNode node = new DockNode(new Label("Test"), "Node 1");
        DockSplitPane split = new DockSplitPane(Orientation.HORIZONTAL);
        split.addChild(node);
        dockGraph.setRoot(split);

        Node view = layoutEngine.buildSceneGraph();
        assertInstanceOf(DockNodeView.class, view);
    }

    @Test
    void testSingleChildTabRootIsOptimizedToNodeView() {
        DockNode node = new DockNode(new Label("Test"), "Node 1");
        DockTabPane tabPane = new DockTabPane();
        tabPane.addChild(node);
        dockGraph.setRoot(tabPane);

        Node view = layoutEngine.buildSceneGraph();
        assertInstanceOf(DockNodeView.class, view);
    }

    @Test
    void testEmptyContainerRootFallsBackToEmptyLayout() {
        DockSplitPane split = new DockSplitPane(Orientation.VERTICAL);
        dockGraph.setRoot(split);

        Node view = layoutEngine.buildSceneGraph();
        assertInstanceOf(StackPane.class, view);
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

    /**
     * Memory cleanup test: repeated rebuilds must not grow the internal view cache.
     * Ensures cache size remains bounded to the current graph size.
     */
    @Test
    void testViewCacheDoesNotGrowAcrossRepeatedRebuilds() {
        DockNode node1 = new DockNode(new Label("A"), "A");
        DockNode node2 = new DockNode(new Label("B"), "B");
        DockNode node3 = new DockNode(new Label("C"), "C");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);
        dockGraph.dock(node3, node2, DockPosition.BOTTOM);

        int expectedElementCount = countElements(dockGraph.getRoot());
        assertTrue(expectedElementCount > 0);

        for (int i = 0; i < 120; i++) {
            layoutEngine.buildSceneGraph();
            assertEquals(expectedElementCount, getViewCache().size());
        }
    }

    /**
     * Memory cleanup test: views for removed nodes must be cleared from cache after rebuild.
     * This prevents stale references after undock operations.
     */
    @Test
    void testUndockedNodeViewIsRemovedFromCacheAfterRebuild() {
        DockNode node1 = new DockNode(new Label("A"), "A");
        DockNode node2 = new DockNode(new Label("B"), "B");
        DockNode node3 = new DockNode(new Label("C"), "C");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);
        dockGraph.dock(node3, node2, DockPosition.CENTER);

        layoutEngine.buildSceneGraph();
        assertNotNull(layoutEngine.getDockNodeView(node2));

        dockGraph.undock(node2);
        layoutEngine.buildSceneGraph();

        assertNull(layoutEngine.getDockNodeView(node2));
        assertEquals(countElements(dockGraph.getRoot()), getViewCache().size());
    }

    /**
     * Memory cleanup stress test on a large layout: cache must stay bounded across rebuilds
     * and detach/attach cycles.
     * Uses 50+ nodes to match roadmap cleanup goals for larger layouts.
     */
    @Test
    void testLargeLayoutCacheCleanupDuringDetachAttachCycles() {
        List<DockNode> nodes = buildLargeLayout(55);
        DockNode transientNode = nodes.getLast();
        int expectedElementCount = countElements(dockGraph.getRoot());

        for (int i = 0; i < 40; i++) {
            layoutEngine.buildSceneGraph();
            assertEquals(expectedElementCount, getViewCache().size());

            if (i > 0 && i % 10 == 0) {
                dockGraph.undock(transientNode);
                layoutEngine.buildSceneGraph();
                assertNull(layoutEngine.getDockNodeView(transientNode));
                assertEquals(countElements(dockGraph.getRoot()), getViewCache().size());

                dockGraph.dock(transientNode, dockGraph.getRoot(), DockPosition.RIGHT);
                expectedElementCount = countElements(dockGraph.getRoot());
                layoutEngine.buildSceneGraph();
                assertNotNull(layoutEngine.getDockNodeView(transientNode));
                assertEquals(expectedElementCount, getViewCache().size());
            }
        }
    }

    private List<DockNode> buildLargeLayout(int nodeCount) {
        List<DockNode> nodes = new ArrayList<>(nodeCount);
        DockPosition[] positions = {
            DockPosition.RIGHT,
            DockPosition.BOTTOM,
            DockPosition.CENTER,
            DockPosition.LEFT,
            DockPosition.TOP
        };

        for (int i = 0; i < nodeCount; i++) {
            DockNode node = new DockNode(new Label("Node" + i), "Node " + i);
            if (nodes.isEmpty()) {
                dockGraph.dock(node, null, DockPosition.CENTER);
            } else {
                DockNode target = nodes.get((i * 13 + 7) % nodes.size());
                dockGraph.dock(node, target, positions[i % positions.length]);
            }
            nodes.add(node);
        }

        return nodes;
    }

    private int countElements(DockElement element) {
        if (element == null) {
            return 0;
        }
        if (element instanceof DockContainer container) {
            int count = 1;
            for (DockElement child : container.getChildren()) {
                count += countElements(child);
            }
            return count;
        }
        return 1;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Node> getViewCache() {
        try {
            Field field = DockLayoutEngine.class.getDeclaredField("viewCache");
            field.setAccessible(true);
            return (Map<String, Node>) field.get(layoutEngine);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to inspect DockLayoutEngine view cache", e);
        }
    }

    private void invokeContextMenuOnShowing(ContextMenu contextMenu) {
        if (contextMenu.getOnShowing() != null) {
            contextMenu.getOnShowing().handle(null);
        }
    }

    private void assertMenuItemHasIcon(ContextMenu contextMenu, String itemText, String iconClass) {
        MenuItem menuItem = contextMenu.getItems().stream()
            .filter(item -> itemText.equals(item.getText()))
            .findFirst()
            .orElseThrow();
        Node graphic = menuItem.getGraphic();
        assertNotNull(graphic, () -> "Missing graphic for menu item: " + itemText);
        assertTrue(graphic.getStyleClass().contains(iconClass),
            () -> "Menu item '" + itemText + "' missing icon class: " + iconClass);
    }

    private MouseEvent createPrimaryPressEvent(Node source, Node target) {
        return new MouseEvent(
            source,
            target,
            MouseEvent.MOUSE_PRESSED,
            0,
            0,
            0,
            0,
            MouseButton.PRIMARY,
            1,
            false,
            false,
            false,
            false,
            true,
            false,
            false,
            true,
            false,
            false,
            new PickResult(target, 0, 0)
        );
    }
}

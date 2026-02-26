package org.snapfx.model;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class DockGraphSplitTargetDockingTest {

    private DockGraph dockGraph;

    @BeforeAll
    static void setupJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit already started by another test class.
        }
    }

    @BeforeEach
    void setUp() {
        dockGraph = new DockGraph();
    }

    /**
     * Regression test: Docking against a SplitPane target edge must split only the edge segment.
     * Bug: Docking RIGHT against the root SplitPane re-split the whole container and shifted old dividers.
     * Fix: Resolve SplitPane edge drops to the concrete edge child before split insertion.
     * Date: 2026-02-14
     */
    @Test
    void testDockAgainstSplitTargetSplitsOnlyEdgeSegment() {
        DockNode editor = new DockNode(new Label("Editor"), "Editor");
        DockNode console = new DockNode(new Label("Console"), "Console");
        DockNode properties = new DockNode(new Label("Properties"), "Properties");

        dockGraph.dock(editor, null, DockPosition.CENTER);
        dockGraph.dock(console, editor, DockPosition.RIGHT);

        DockSplitPane rootSplit = assertInstanceOf(DockSplitPane.class, dockGraph.getRoot());
        rootSplit.setDividerPosition(0, 0.6);

        dockGraph.dock(properties, rootSplit, DockPosition.RIGHT);

        assertSame(rootSplit, dockGraph.getRoot());
        assertEquals(3, rootSplit.getChildren().size());
        assertEquals(editor, rootSplit.getChildren().get(0));
        assertEquals(console, rootSplit.getChildren().get(1));
        assertEquals(properties, rootSplit.getChildren().get(2));
        assertEquals(2, rootSplit.getDividerPositions().size());
        assertEquals(0.6, rootSplit.getDividerPositions().get(0).get(), 0.0001);
        assertEquals(0.8, rootSplit.getDividerPositions().get(1).get(), 0.0001);
    }
}

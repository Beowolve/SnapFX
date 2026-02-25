package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.close.DockCloseBehavior;
import com.github.beowolve.snapfx.close.DockCloseDecision;
import com.github.beowolve.snapfx.close.DockCloseResult;
import com.github.beowolve.snapfx.close.DockCloseSource;
import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.floating.DockFloatingPinButtonMode;
import com.github.beowolve.snapfx.floating.DockFloatingPinChangeEvent;
import com.github.beowolve.snapfx.floating.DockFloatingPinLockedBehavior;
import com.github.beowolve.snapfx.floating.DockFloatingPinSource;
import com.github.beowolve.snapfx.floating.DockFloatingSnapTarget;
import com.github.beowolve.snapfx.floating.DockFloatingWindow;
import com.github.beowolve.snapfx.model.DockContainer;
import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockGraph;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import com.github.beowolve.snapfx.model.DockSplitPane;
import com.github.beowolve.snapfx.model.DockTabPane;
import com.github.beowolve.snapfx.persistence.DockLayoutLoadException;
import com.github.beowolve.snapfx.sidebar.DockSideBarMode;
import com.github.beowolve.snapfx.theme.DockThemeStyleClasses;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.event.Event;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
        Platform.setImplicitExit(false);
    }

    @BeforeEach
    void setUp() {
        snapFX = new SnapFX();
    }

    @Test
    void testDefaultShortcutBindingsAreConfigured() {
        assertEquals(
            new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
            snapFX.getShortcut(DockShortcutAction.CLOSE_ACTIVE_NODE)
        );
        assertEquals(
            new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHORTCUT_DOWN),
            snapFX.getShortcut(DockShortcutAction.NEXT_TAB)
        );
        assertEquals(
            new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            snapFX.getShortcut(DockShortcutAction.PREVIOUS_TAB)
        );
        assertEquals(
            new KeyCodeCombination(KeyCode.ESCAPE),
            snapFX.getShortcut(DockShortcutAction.CANCEL_DRAG)
        );
        assertEquals(
            new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            snapFX.getShortcut(DockShortcutAction.TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP)
        );
    }

    @Test
    void testSideBarModeDefaultsToAutoAndNullFallsBackToAuto() {
        assertEquals(DockSideBarMode.AUTO, snapFX.getSideBarMode());

        snapFX.setSideBarMode(DockSideBarMode.ALWAYS);
        assertEquals(DockSideBarMode.ALWAYS, snapFX.getSideBarMode());

        snapFX.setSideBarMode(null);
        assertEquals(DockSideBarMode.AUTO, snapFX.getSideBarMode());
    }

    @Test
    void testSideBarModeAlwaysRendersEmptySideBarStrips() {
        snapFX.setSideBarMode(DockSideBarMode.ALWAYS);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        assertNotNull(findSideBarStrip(scene.getRoot(), Side.LEFT));
        assertNotNull(findSideBarStrip(scene.getRoot(), Side.RIGHT));
    }

    @Test
    void testSideBarModeNeverHidesRenderedSideBarsWhilePreservingPinnedState() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        assertTrue(snapFX.isPinnedToSideBar(tool));

        snapFX.setSideBarMode(DockSideBarMode.NEVER);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        assertNull(findSideBarStrip(scene.getRoot(), Side.LEFT));
        assertNull(findSideBarStrip(scene.getRoot(), Side.RIGHT));
        assertNull(findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_PINNED));
        assertTrue(snapFX.isPinnedToSideBar(tool), "Mode NEVER should hide sidebars without losing pinned model state");
    }

    @Test
    void testSetShortcutRemovesDuplicateBindingFromPreviousAction() {
        KeyCodeCombination ctrlW = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);

        snapFX.setShortcut(DockShortcutAction.NEXT_TAB, ctrlW);

        assertNull(snapFX.getShortcut(DockShortcutAction.CLOSE_ACTIVE_NODE));
        assertEquals(ctrlW, snapFX.getShortcut(DockShortcutAction.NEXT_TAB));
    }

    @Test
    void testResetShortcutsToDefaultsRestoresRemovedBinding() {
        snapFX.clearShortcut(DockShortcutAction.CLOSE_ACTIVE_NODE);
        assertNull(snapFX.getShortcut(DockShortcutAction.CLOSE_ACTIVE_NODE));

        snapFX.resetShortcutsToDefaults();

        assertEquals(
            new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
            snapFX.getShortcut(DockShortcutAction.CLOSE_ACTIVE_NODE)
        );
    }

    @Test
    void testShortcutActionCloseActiveNodeUsesSelectedTab() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.CENTER);

        TabPane tabPane = buildRootTabPane(snapFX);
        tabPane.getSelectionModel().select(0);

        assertTrue(snapFX.executeShortcutAction(DockShortcutAction.CLOSE_ACTIVE_NODE, tabPane));
        assertTrue(snapFX.getHiddenNodes().contains(node1));
        assertFalse(snapFX.getHiddenNodes().contains(node2));
    }

    @Test
    void testShortcutActionCyclesTabsForwardAndBackward() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.CENTER);

        TabPane tabPane = buildRootTabPane(snapFX);
        tabPane.getSelectionModel().select(0);

        assertTrue(snapFX.executeShortcutAction(DockShortcutAction.NEXT_TAB, tabPane));
        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());

        assertTrue(snapFX.executeShortcutAction(DockShortcutAction.PREVIOUS_TAB, tabPane));
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
    }

    @Test
    void testShortcutKeyEventUsesCustomBinding() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.CENTER);
        snapFX.setShortcut(
            DockShortcutAction.CLOSE_ACTIVE_NODE,
            new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN)
        );

        Scene scene = new Scene(snapFX.buildLayout(), 640, 480);

        KeyEvent ctrlW = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.W, false, true, false, false);
        Event.fireEvent(scene, ctrlW);
        assertTrue(snapFX.getHiddenNodes().isEmpty());

        KeyEvent ctrlQ = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.Q, false, true, false, false);
        Event.fireEvent(scene, ctrlQ);
        assertEquals(1, snapFX.getHiddenNodes().size());
    }

    @Test
    void testShortcutActionToggleActiveFloatingAlwaysOnTop() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat);
        assertTrue(floatingWindow.isAlwaysOnTop());

        assertTrue(snapFX.executeShortcutAction(DockShortcutAction.TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP, null));
        assertFalse(floatingWindow.isAlwaysOnTop());

        assertTrue(snapFX.executeShortcutAction(DockShortcutAction.TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP, null));
        assertTrue(floatingWindow.isAlwaysOnTop());
    }

    @Test
    void testShortcutActionToggleActiveFloatingAlwaysOnTopReturnsFalseWhenNoWindowExists() {
        assertFalse(snapFX.executeShortcutAction(DockShortcutAction.TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP, null));
    }

    @Test
    void testShortcutKeyEventToggleActiveFloatingAlwaysOnTopUsesDefaultBinding() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);
        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat);
        assertTrue(floatingWindow.isAlwaysOnTop());

        Scene scene = new Scene(snapFX.buildLayout(), 640, 480);
        KeyEvent ctrlShiftP = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.P, true, true, false, false);
        Event.fireEvent(scene, ctrlShiftP);

        assertFalse(floatingWindow.isAlwaysOnTop());
    }

    @Test
    void testDefaultThemeStylesheetResourcePath() {
        assertEquals("/snapfx.css", SnapFX.getDefaultThemeStylesheetResourcePath());
    }

    @Test
    void testDefaultThemeName() {
        assertEquals("Light", SnapFX.getDefaultThemeName());
    }

    @Test
    void testAvailableThemeStylesheetsExposeStableNamedMapAndList() {
        Map<String, String> stylesheets = SnapFX.getAvailableThemeStylesheets();

        assertEquals(List.of("Light", "Dark"), List.copyOf(stylesheets.keySet()));
        assertEquals(List.of("Light", "Dark"), SnapFX.getAvailableThemeNames());
        assertEquals("/snapfx.css", stylesheets.get("Light"));
        assertEquals("/snapfx-dark.css", stylesheets.get("Dark"));
    }

    @Test
    void testAvailableThemeStylesheetsMapIsUnmodifiable() {
        Map<String, String> stylesheets = SnapFX.getAvailableThemeStylesheets();

        assertThrows(UnsupportedOperationException.class, () -> stylesheets.put("Custom", "/custom.css"));
    }

    @Test
    void testInitializeAppliesDefaultThemeStylesheetToPrimaryScene() {
        runOnFxThreadAndWait(() -> {
            SnapFX framework = new SnapFX();
            DockNode node = new DockNode("node", new Label("Node"), "Node");
            framework.dock(node, null, DockPosition.CENTER);

            Stage stage = new Stage();
            try {
                Scene scene = new Scene(framework.buildLayout(), 640, 480);
                stage.setScene(scene);
                framework.initialize(stage);

                String defaultStylesheetUrl = SnapFX.class
                    .getResource(SnapFX.getDefaultThemeStylesheetResourcePath())
                    .toExternalForm();
                assertTrue(scene.getStylesheets().contains(defaultStylesheetUrl));
            } finally {
                stage.close();
                closeGhostStage(framework);
            }
        });
    }

    @Test
    void testSetThemeStylesheetUpdatesPrimaryAndFloatingScenes() {
        runOnFxThreadAndWait(() -> {
            SnapFX framework = new SnapFX();
            DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
            DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

            framework.dock(nodeMain, null, DockPosition.CENTER);
            framework.dock(nodeFloat, nodeMain, DockPosition.RIGHT);
            DockFloatingWindow floatingWindow = framework.floatNode(nodeFloat);

            Stage stage = new Stage();
            try {
                Scene scene = new Scene(framework.buildLayout(), 640, 480);
                stage.setScene(scene);
                framework.initialize(stage);

                String defaultStylesheetUrl = SnapFX.class
                    .getResource(SnapFX.getDefaultThemeStylesheetResourcePath())
                    .toExternalForm();
                String darkStylesheetUrl = SnapFX.class
                    .getResource("/snapfx-dark.css")
                    .toExternalForm();

                framework.setThemeStylesheet("/snapfx-dark.css");

                assertTrue(scene.getStylesheets().contains(darkStylesheetUrl));
                assertFalse(scene.getStylesheets().contains(defaultStylesheetUrl));
                assertNotNull(floatingWindow.getScene());
                assertTrue(floatingWindow.getScene().getStylesheets().contains(darkStylesheetUrl));
                assertFalse(floatingWindow.getScene().getStylesheets().contains(defaultStylesheetUrl));
                framework.closeFloatingWindows(false);
            } finally {
                stage.close();
                closeGhostStage(framework);
            }
        });
    }

    @Test
    void testSetThemeStylesheetBlankResetsToDefault() {
        runOnFxThreadAndWait(() -> {
            SnapFX framework = new SnapFX();
            DockNode node = new DockNode("node", new Label("Node"), "Node");
            framework.dock(node, null, DockPosition.CENTER);

            Stage stage = new Stage();
            try {
                Scene scene = new Scene(framework.buildLayout(), 640, 480);
                stage.setScene(scene);
                framework.initialize(stage);

                String defaultStylesheetUrl = SnapFX.class
                    .getResource(SnapFX.getDefaultThemeStylesheetResourcePath())
                    .toExternalForm();
                String darkStylesheetUrl = SnapFX.class
                    .getResource("/snapfx-dark.css")
                    .toExternalForm();

                framework.setThemeStylesheet("/snapfx-dark.css");
                framework.setThemeStylesheet(" ");

                assertTrue(scene.getStylesheets().contains(defaultStylesheetUrl));
                assertFalse(scene.getStylesheets().contains(darkStylesheetUrl));
            } finally {
                stage.close();
                closeGhostStage(framework);
            }
        });
    }

    @Test
    void testSetThemeStylesheetThrowsForMissingResource() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> snapFX.setThemeStylesheet("/missing-theme.css")
        );

        assertTrue(exception.getMessage().contains("/missing-theme.css"));
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
     * Test that close requests use configured callbacks and still resolve to hide by default.
     */
    @Test
    void testCloseRequestHandlerCallsHide() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node1, null, DockPosition.CENTER);

        AtomicReference<DockCloseSource> sourceRef = new AtomicReference<>();
        snapFX.setOnCloseRequest(request -> {
            sourceRef.set(request.source());
            return DockCloseDecision.DEFAULT;
        });

        snapFX.close(node1);

        // Verify node is in hidden list
        assertEquals(1, snapFX.getHiddenNodes().size());
        assertTrue(snapFX.getHiddenNodes().contains(node1));
        assertEquals(DockCloseSource.TITLE_BAR, sourceRef.get());
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
    void testProgrammaticCloseUsesHideBehaviorByDefault() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        snapFX.close(node1);

        assertFalse(isInGraph(snapFX, node1));
        assertEquals(1, snapFX.getHiddenNodes().size());
        assertTrue(snapFX.getHiddenNodes().contains(node1));
    }

    @Test
    void testProgrammaticCloseUsesRemoveBehaviorWhenConfigured() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);
        snapFX.setDefaultCloseBehavior(DockCloseBehavior.REMOVE);

        snapFX.close(node1);

        assertFalse(isInGraph(snapFX, node1));
        assertTrue(snapFX.getHiddenNodes().isEmpty());
    }

    @Test
    void testProgrammaticCloseCanBeCancelledByCallback() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);
        snapFX.setOnCloseRequest(request -> DockCloseDecision.CANCEL);

        snapFX.close(node1);

        assertTrue(isInGraph(snapFX, node1));
        assertTrue(snapFX.getHiddenNodes().isEmpty());
    }

    @Test
    void testFloatingWindowCloseUsesRemoveBehaviorWhenConfigured() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);
        snapFX.setDefaultCloseBehavior(DockCloseBehavior.REMOVE);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1);
        floatingWindow.close();

        assertTrue(snapFX.getFloatingWindows().isEmpty());
        assertTrue(snapFX.getHiddenNodes().isEmpty());
        assertFalse(isInGraph(snapFX, node1));
    }

    @Test
    void testFloatingWindowCloseCanBeCancelledByCallback() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);
        DockFloatingWindow floatingWindow = snapFX.floatNode(node1);
        snapFX.setOnCloseRequest(request -> DockCloseDecision.CANCEL);

        floatingWindow.close();

        assertEquals(1, snapFX.getFloatingWindows().size());
        assertTrue(floatingWindow.containsNode(node1));
        assertTrue(snapFX.getHiddenNodes().isEmpty());
    }

    @Test
    void testCloseHandledCallbackReceivesResult() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node1, null, DockPosition.CENTER);

        AtomicReference<DockCloseResult> resultRef = new AtomicReference<>();
        snapFX.setOnCloseHandled(resultRef::set);

        snapFX.close(node1);

        DockCloseResult result = resultRef.get();
        assertNotNull(result);
        assertEquals(DockCloseSource.TITLE_BAR, result.request().source());
        assertEquals(DockCloseBehavior.HIDE, result.appliedBehavior());
        assertFalse(result.canceled());
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
        snapFX.setFloatingWindowAlwaysOnTop(snapFX.getFloatingWindows().getFirst(), false);
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
        assertFalse(restoredWindow.isAlwaysOnTop());
    }

    @Test
    void testRestoreAfterClosingFloatingWindowReopensAsFloatingWindow() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(node1, 333.0, 144.0);
        snapFX.setFloatingWindowAlwaysOnTop(floatingWindow, false);
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
        assertFalse(restoredWindow.isAlwaysOnTop());
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
    void testAttachAfterButtonFloatFromFloatingSubLayoutRestoresIntoSourceFloatingWindow() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow sourceWindow = snapFX.floatNode(node1, 300.0, 200.0);
        snapFX.getDockGraph().undock(node2);
        sourceWindow.dockNode(node2, node1, DockPosition.CENTER, null);

        sourceWindow.requestFloatForNode(node2);

        DockFloatingWindow detachedWindow = findFloatingWindowContainingNode(snapFX, node2);
        assertNotNull(detachedWindow);
        assertNotSame(sourceWindow, detachedWindow);
        assertTrue(sourceWindow.containsNode(node1));
        assertFalse(sourceWindow.containsNode(node2));

        snapFX.attachFloatingWindow(detachedWindow);

        assertTrue(sourceWindow.containsNode(node2));
        assertFalse(isInGraph(snapFX, node2));
        assertEquals(1, snapFX.getFloatingWindows().size());
        assertSame(sourceWindow, snapFX.getFloatingWindows().getFirst());
    }

    @Test
    void testAttachAfterUnresolvedDragFloatFromFloatingSubLayoutRestoresIntoSourceFloatingWindow() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow sourceWindow = snapFX.floatNode(node1, 300.0, 200.0);
        snapFX.getDockGraph().undock(node2);
        sourceWindow.dockNode(node2, node1, DockPosition.CENTER, null);

        assertTrue(requestFloatDetach(snapFX, node2, 900.0, 700.0));

        DockFloatingWindow detachedWindow = findFloatingWindowContainingNode(snapFX, node2);
        assertNotNull(detachedWindow);
        assertNotSame(sourceWindow, detachedWindow);
        assertTrue(sourceWindow.containsNode(node1));
        assertFalse(sourceWindow.containsNode(node2));

        snapFX.attachFloatingWindow(detachedWindow);

        assertTrue(sourceWindow.containsNode(node2));
        assertFalse(isInGraph(snapFX, node2));
        assertEquals(1, snapFX.getFloatingWindows().size());
        assertSame(sourceWindow, snapFX.getFloatingWindows().getFirst());
    }

    @Test
    void testAttachAfterFloatFromFloatingSubLayoutFallsBackToMainLayoutWhenSourceWindowIsGone() {
        DockNode node1 = new DockNode("node1", new Label("Node 1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Node 2"), "Node 2");

        snapFX.dock(node1, null, DockPosition.CENTER);
        snapFX.dock(node2, node1, DockPosition.RIGHT);

        DockFloatingWindow sourceWindow = snapFX.floatNode(node1, 300.0, 200.0);
        snapFX.getDockGraph().undock(node2);
        sourceWindow.dockNode(node2, node1, DockPosition.CENTER, null);
        sourceWindow.requestFloatForNode(node2);

        DockFloatingWindow detachedWindow = findFloatingWindowContainingNode(snapFX, node2);
        assertNotNull(detachedWindow);
        assertNotSame(sourceWindow, detachedWindow);

        snapFX.attachFloatingWindow(sourceWindow);
        assertTrue(isInGraph(snapFX, node1));
        assertEquals(1, snapFX.getFloatingWindows().size());

        snapFX.attachFloatingWindow(detachedWindow);

        assertTrue(isInGraph(snapFX, node2));
        assertTrue(snapFX.getFloatingWindows().isEmpty());
    }

    @Test
    void testDetachAttachTopLeftNodeInThreeWindowFloatingLayoutRestoresOriginalStructure() {
        FloatingThreeWindowLayout layout = createThreeWindowFloatingLayout();
        assertDetachAttachRestoresFloatingLayout(layout, layout.topLeft());
    }

    @Test
    void testDetachAttachTopRightNodeInThreeWindowFloatingLayoutRestoresOriginalStructure() {
        FloatingThreeWindowLayout layout = createThreeWindowFloatingLayout();
        assertDetachAttachRestoresFloatingLayout(layout, layout.topRight());
    }

    @Test
    void testDetachAttachBottomNodeInThreeWindowFloatingLayoutRestoresOriginalStructure() {
        FloatingThreeWindowLayout layout = createThreeWindowFloatingLayout();
        assertDetachAttachRestoresFloatingLayout(layout, layout.bottom());
    }

    @Test
    void testDetachCloseRemainingAttachTopLeftNodeReturnsToFloatingLayout() {
        FloatingThreeWindowLayout layout = createThreeWindowFloatingLayout();
        assertDetachCloseRemainingAttachRestoresIntoFloatingHost(layout, layout.topLeft(), layout.topRight());
    }

    @Test
    void testDetachCloseRemainingAttachTopRightNodeReturnsToFloatingLayout() {
        FloatingThreeWindowLayout layout = createThreeWindowFloatingLayout();
        assertDetachCloseRemainingAttachRestoresIntoFloatingHost(layout, layout.topRight(), layout.topLeft());
    }

    @Test
    void testDetachCloseRemainingAttachBottomNodeReturnsToFloatingLayout() {
        FloatingThreeWindowLayout layout = createThreeWindowFloatingLayout();
        assertDetachCloseRemainingAttachRestoresIntoFloatingHost(layout, layout.bottom(), layout.topLeft());
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
    void testUnresolvedDropFromFloatingSubLayoutDetachesNodeToNewFloatingWindow() {
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

        assertTrue(requestFloatDetach(snapFX, node2, 900.0, 700.0));

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
    void testDefaultFloatingAlwaysOnTopAppliesToNewWindow() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);
        snapFX.setDefaultFloatingAlwaysOnTop(false);

        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat);

        assertNotNull(floatingWindow);
        assertFalse(floatingWindow.isAlwaysOnTop());
    }

    @Test
    void testFloatingWindowAlwaysOnTopCanBeChangedViaApiAndEmitsEvent() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);

        AtomicReference<DockFloatingPinChangeEvent> eventRef = new AtomicReference<>();
        snapFX.setOnFloatingPinChanged(eventRef::set);

        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat);
        snapFX.setFloatingWindowAlwaysOnTop(floatingWindow, false);

        DockFloatingPinChangeEvent event = eventRef.get();
        assertNotNull(event);
        assertEquals(floatingWindow, event.window());
        assertFalse(event.alwaysOnTop());
        assertEquals(DockFloatingPinSource.API, event.source());
    }

    @Test
    void testFloatingPinSettingsApplyToOpenWindows() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);
        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat);

        snapFX.setFloatingPinButtonMode(DockFloatingPinButtonMode.ALWAYS);
        snapFX.setAllowFloatingPinToggle(false);
        snapFX.setFloatingPinLockedBehavior(DockFloatingPinLockedBehavior.HIDE_BUTTON);

        assertEquals(DockFloatingPinButtonMode.ALWAYS, floatingWindow.getPinButtonMode());
        assertFalse(floatingWindow.isPinToggleEnabled());
        assertEquals(DockFloatingPinLockedBehavior.HIDE_BUTTON, floatingWindow.getPinLockedBehavior());
    }

    @Test
    void testFloatingSnapSettingsApplyToOpenAndNewWindows() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloatA = new DockNode("nodeFloatA", new Label("Float A"), "Float A");
        DockNode nodeFloatB = new DockNode("nodeFloatB", new Label("Float B"), "Float B");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloatA, nodeMain, DockPosition.RIGHT);
        snapFX.dock(nodeFloatB, nodeMain, DockPosition.BOTTOM);

        DockFloatingWindow firstWindow = snapFX.floatNode(nodeFloatA);
        assertTrue(firstWindow.isSnappingEnabled());

        snapFX.setFloatingWindowSnappingEnabled(false);
        snapFX.setFloatingWindowSnapDistance(20.0);
        snapFX.setFloatingWindowSnapTargets(Set.of(DockFloatingSnapTarget.FLOATING_WINDOWS));

        assertFalse(firstWindow.isSnappingEnabled());
        assertEquals(20.0, firstWindow.getSnapDistance(), 0.0001);
        assertEquals(Set.of(DockFloatingSnapTarget.FLOATING_WINDOWS), firstWindow.getSnapTargets());

        DockFloatingWindow secondWindow = snapFX.floatNode(nodeFloatB);
        assertFalse(secondWindow.isSnappingEnabled());
        assertEquals(20.0, secondWindow.getSnapDistance(), 0.0001);
        assertEquals(Set.of(DockFloatingSnapTarget.FLOATING_WINDOWS), secondWindow.getSnapTargets());
    }

    @Test
    void testFloatingSnapDistanceRejectsInvalidInput() {
        assertEquals(12.0, snapFX.getFloatingWindowSnapDistance(), 0.0001);

        snapFX.setFloatingWindowSnapDistance(Double.NaN);
        assertEquals(12.0, snapFX.getFloatingWindowSnapDistance(), 0.0001);

        snapFX.setFloatingWindowSnapDistance(-1.0);
        assertEquals(12.0, snapFX.getFloatingWindowSnapDistance(), 0.0001);
    }

    @Test
    void testSideBarApiPinsAndRestoresDockedNode() {
        DockNode left = new DockNode("left", new Label("Left"), "Left");
        DockNode right = new DockNode("right", new Label("Right"), "Right");

        snapFX.dock(left, null, DockPosition.CENTER);
        snapFX.dock(right, left, DockPosition.RIGHT);

        snapFX.pinToSideBar(left, Side.LEFT);

        assertTrue(snapFX.isPinnedToSideBar(left));
        assertEquals(Side.LEFT, snapFX.getPinnedSide(left));
        assertFalse(snapFX.isSideBarPinnedOpen(Side.LEFT));
        assertTrue(snapFX.getSideBarNodes(Side.LEFT).contains(left));
        assertFalse(isInGraph(snapFX, left));
        assertTrue(isInGraph(snapFX, right));

        snapFX.restoreFromSideBar(left);

        assertFalse(snapFX.isPinnedToSideBar(left));
        assertTrue(snapFX.getSideBarNodes(Side.LEFT).isEmpty());
        assertTrue(isInGraph(snapFX, left));
        DockSplitPane split = assertInstanceOf(DockSplitPane.class, snapFX.getDockGraph().getRoot());
        assertEquals(left, split.getChildren().getFirst());
        assertEquals(right, split.getChildren().getLast());
    }

    @Test
    void testPinnedOpenSideBarApiDelegatesAndRespectsLock() {
        assertFalse(snapFX.isSideBarPinnedOpen(Side.LEFT));

        snapFX.pinOpenSideBar(Side.LEFT);
        assertTrue(snapFX.isSideBarPinnedOpen(Side.LEFT));

        snapFX.setLocked(true);
        snapFX.collapsePinnedSideBar(Side.LEFT);
        assertTrue(snapFX.isSideBarPinnedOpen(Side.LEFT), "Pinned-open sidebar change should be blocked while locked");

        snapFX.setLocked(false);
        snapFX.collapsePinnedSideBar(Side.LEFT);
        assertFalse(snapFX.isSideBarPinnedOpen(Side.LEFT));
    }

    @Test
    void testSideBarPanelWidthApiValidatesAndAllowsChangesWhileLocked() {
        assertEquals(DockGraph.DEFAULT_SIDE_BAR_PANEL_WIDTH, snapFX.getSideBarPanelWidth(Side.LEFT), 0.0001);

        snapFX.setSideBarPanelWidth(Side.LEFT, 360.0);
        assertEquals(360.0, snapFX.getSideBarPanelWidth(Side.LEFT), 0.0001);

        snapFX.setLocked(true);
        snapFX.setSideBarPanelWidth(Side.LEFT, 340.0);
        assertEquals(340.0, snapFX.getSideBarPanelWidth(Side.LEFT), 0.0001);

        snapFX.setSideBarPanelWidth(Side.LEFT, Double.NaN);
        snapFX.setSideBarPanelWidth(Side.LEFT, -5.0);
        snapFX.setSideBarPanelWidth(null, 220.0);
        assertEquals(340.0, snapFX.getSideBarPanelWidth(Side.LEFT), 0.0001);
    }

    @Test
    void testSaveLoadRoundTripPreservesPinnedSideBarsViaSnapFxApi() throws DockLayoutLoadException {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode sideTool = new DockNode("sideTool", new Label("Side Tool"), "Side Tool");

        snapFX.setNodeFactory(this::createFactoryNode);
        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(sideTool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(sideTool, Side.RIGHT);
        snapFX.collapsePinnedSideBar(Side.RIGHT);
        snapFX.setSideBarPanelWidth(Side.LEFT, 345.0);
        snapFX.setSideBarPanelWidth(Side.RIGHT, 288.0);

        String json = snapFX.saveLayout();

        SnapFX restored = new SnapFX();
        restored.setNodeFactory(this::createFactoryNode);
        restored.loadLayout(json);

        assertEquals(1, restored.getSideBarNodes(Side.RIGHT).size());
        DockNode restoredPinnedNode = restored.getSideBarNodes(Side.RIGHT).getFirst();
        assertEquals("sideTool", restoredPinnedNode.getDockNodeId());
        assertFalse(restored.isSideBarPinnedOpen(Side.RIGHT));
        assertFalse(isInGraph(restored, restoredPinnedNode));
        assertEquals(345.0, restored.getSideBarPanelWidth(Side.LEFT), 0.0001);
        assertEquals(288.0, restored.getSideBarPanelWidth(Side.RIGHT), 0.0001);

        DockNode restoredMainNode = assertInstanceOf(DockNode.class, restored.getDockGraph().getRoot());
        assertEquals("main", restoredMainNode.getDockNodeId());
    }

    @Test
    void testBuildLayoutShowsCollapsedSideBarStripWithoutPinnedPanel() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");

        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        snapFX.collapsePinnedSideBar(Side.LEFT);

        Node root = snapFX.buildLayout();

        assertTrue(countNodesWithStyleClass(root, DockThemeStyleClasses.DOCK_SIDEBAR_STRIP) >= 1);
        assertEquals(0, countNodesWithStyleClass(root, DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_PINNED));
        assertEquals(0, countNodesWithStyleClass(root, DockThemeStyleClasses.DOCK_SIDEBAR_ICON_BUTTON_ACTIVE));
    }

    @Test
    void testBuildLayoutShowsPinnedSideBarPanelWhenSideBarIsPinnedOpen() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");

        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        snapFX.pinOpenSideBar(Side.LEFT);

        Node root = snapFX.buildLayout();

        assertTrue(countNodesWithStyleClass(root, DockThemeStyleClasses.DOCK_SIDEBAR_STRIP) >= 1);
        assertTrue(countNodesWithStyleClass(root, DockThemeStyleClasses.DOCK_SIDEBAR_PANEL) >= 1);
        assertTrue(countNodesWithStyleClass(root, DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_PINNED) >= 1);
        assertTrue(countNodesWithStyleClass(root, DockThemeStyleClasses.DOCK_SIDEBAR_ICON_BUTTON_ACTIVE) >= 1);
    }

    @Test
    void testPinnedSideBarActiveIconClickCollapsesPanelByDefault() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");

        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        snapFX.pinOpenSideBar(Side.LEFT);
        snapFX.buildLayout(); // Seeds sidebar selection state used by the icon-click handler

        assertTrue(snapFX.isCollapsePinnedSideBarOnActiveIconClick());
        assertTrue(snapFX.isSideBarPinnedOpen(Side.LEFT));

        invokeSideBarIconClick(snapFX, Side.LEFT, tool);

        assertTrue(snapFX.isSideBarPinnedOpen(Side.LEFT), "Pin mode should be preserved while the pinned panel is temporarily collapsed");
        Node rootAfterCollapse = snapFX.buildLayout();
        assertEquals(0, countNodesWithStyleClass(rootAfterCollapse, DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_PINNED));
        assertEquals(0, countNodesWithStyleClass(rootAfterCollapse, DockThemeStyleClasses.DOCK_SIDEBAR_ICON_BUTTON_ACTIVE));

        invokeSideBarIconClick(snapFX, Side.LEFT, tool);

        Node rootAfterReopen = snapFX.buildLayout();
        assertTrue(countNodesWithStyleClass(rootAfterReopen, DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_PINNED) >= 1);
    }

    @Test
    void testPinnedSideBarActiveIconClickCanStayOpenWhenConfigured() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");

        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        snapFX.pinOpenSideBar(Side.LEFT);
        snapFX.setCollapsePinnedSideBarOnActiveIconClick(false);
        snapFX.buildLayout(); // Seeds sidebar selection state used by the icon-click handler

        invokeSideBarIconClick(snapFX, Side.LEFT, tool);

        assertFalse(snapFX.isCollapsePinnedSideBarOnActiveIconClick());
        assertTrue(snapFX.isSideBarPinnedOpen(Side.LEFT));
        Node rootAfterClick = snapFX.buildLayout();
        assertTrue(countNodesWithStyleClass(rootAfterClick, DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_PINNED) >= 1);
    }

    @Test
    void testRestoreFromSideBarUsesNeighborFallbackWhenTabbedParentCollapsed() {
        DockNode firstTab = new DockNode("firstTab", new Label("First"), "First");
        DockNode secondTab = new DockNode("secondTab", new Label("Second"), "Second");

        snapFX.dock(firstTab, null, DockPosition.CENTER);
        snapFX.dock(secondTab, firstTab, DockPosition.CENTER);

        assertInstanceOf(DockTabPane.class, snapFX.getDockGraph().getRoot());

        snapFX.pinToSideBar(secondTab, Side.RIGHT);
        assertInstanceOf(DockNode.class, snapFX.getDockGraph().getRoot(), "Single remaining tab should flatten after pin");

        snapFX.restoreFromSideBar(secondTab);

        DockTabPane restoredTabs = assertInstanceOf(DockTabPane.class, snapFX.getDockGraph().getRoot());
        assertEquals(2, restoredTabs.getChildren().size());
        assertTrue(restoredTabs.getChildren().contains(firstTab));
        assertTrue(restoredTabs.getChildren().contains(secondTab));
    }

    @Test
    void testSidebarDropAtScenePointPinsDockedNodeAtExactInsertIndex() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode toolA = new DockNode("toolA", new Label("Tool A"), "Tool A");
        DockNode toolB = new DockNode("toolB", new Label("Tool B"), "Tool B");

        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(toolA, main, DockPosition.RIGHT);
        snapFX.dock(toolB, main, DockPosition.BOTTOM);
        snapFX.pinToSideBar(toolA, Side.LEFT);
        snapFX.pinToSideBar(toolB, Side.LEFT);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        javafx.scene.layout.VBox leftStrip = findSideBarStrip(scene.getRoot(), Side.LEFT);
        assertNotNull(leftStrip);
        List<Button> iconButtons = findSideBarIconButtons(leftStrip);
        assertEquals(2, iconButtons.size());

        Bounds firstBounds = iconButtons.get(0).localToScene(iconButtons.get(0).getBoundsInLocal());
        Bounds secondBounds = iconButtons.get(1).localToScene(iconButtons.get(1).getBoundsInLocal());
        double sceneX = (firstBounds.getMinX() + firstBounds.getMaxX()) / 2.0;
        double sceneY = (firstBounds.getMaxY() + secondBounds.getMinY()) / 2.0;

        assertTrue(tryDropIntoSideBarAtScenePoint(snapFX, main, sceneX, sceneY));
        assertEquals(List.of(toolA, main, toolB), List.copyOf(snapFX.getSideBarNodes(Side.LEFT)));
        assertTrue(snapFX.isPinnedToSideBar(main));
        assertFalse(isInGraph(snapFX, main));
    }

    @Test
    void testSidebarDropAtScenePointReordersPinnedNodeWithinStrip() {
        DockNode toolA = new DockNode("toolA", new Label("Tool A"), "Tool A");
        DockNode toolB = new DockNode("toolB", new Label("Tool B"), "Tool B");
        DockNode toolC = new DockNode("toolC", new Label("Tool C"), "Tool C");

        snapFX.pinToSideBar(toolA, Side.LEFT);
        snapFX.pinToSideBar(toolB, Side.LEFT);
        snapFX.pinToSideBar(toolC, Side.LEFT);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        javafx.scene.layout.VBox leftStrip = findSideBarStrip(scene.getRoot(), Side.LEFT);
        assertNotNull(leftStrip);
        List<Button> iconButtons = findSideBarIconButtons(leftStrip);
        assertEquals(3, iconButtons.size());

        Button lastButton = iconButtons.getLast();
        Bounds lastBounds = lastButton.localToScene(lastButton.getBoundsInLocal());
        double sceneX = (lastBounds.getMinX() + lastBounds.getMaxX()) / 2.0;
        double sceneY = lastBounds.getMaxY() + 8.0;

        assertTrue(tryDropIntoSideBarAtScenePoint(snapFX, toolB, sceneX, sceneY));
        assertEquals(List.of(toolA, toolC, toolB), List.copyOf(snapFX.getSideBarNodes(Side.LEFT)));
    }

    @Test
    void testSidebarStripIconButtonsExposeDragHandlersForDnDSourceParity() {
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.pinToSideBar(tool, Side.LEFT);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        javafx.scene.layout.VBox leftStrip = findSideBarStrip(scene.getRoot(), Side.LEFT);
        assertNotNull(leftStrip);
        Button iconButton = assertDoesNotThrow(() -> findSideBarIconButtons(leftStrip).getFirst());

        assertNotNull(iconButton.getOnMousePressed());
        assertNotNull(iconButton.getOnMouseDragged());
        assertNotNull(iconButton.getOnMouseReleased());
    }

    @Test
    void testSidebarStripIconContextMenuCanMovePinnedNodeToOtherSide() {
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.pinToSideBar(tool, Side.LEFT);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        javafx.scene.layout.VBox leftStrip = findSideBarStrip(scene.getRoot(), Side.LEFT);
        assertNotNull(leftStrip);
        Button iconButton = assertDoesNotThrow(() -> findSideBarIconButtons(leftStrip).getFirst());
        ContextMenu contextMenu = iconButton.getContextMenu();
        assertNotNull(contextMenu);

        invokeContextMenuOnShowing(contextMenu);
        MenuItem moveLeftItem = findMenuItem(contextMenu, "Move to Left Sidebar");
        MenuItem moveRightItem = findMenuItem(contextMenu, "Move to Right Sidebar");
        assertNotNull(moveLeftItem);
        assertNotNull(moveRightItem);
        assertTrue(moveLeftItem.isDisable());
        assertFalse(moveRightItem.isDisable());

        moveRightItem.fire();

        assertEquals(Side.RIGHT, snapFX.getPinnedSide(tool));
        assertTrue(snapFX.getSideBarNodes(Side.LEFT).isEmpty());
        assertEquals(List.of(tool), List.copyOf(snapFX.getSideBarNodes(Side.RIGHT)));
    }

    @Test
    void testSidebarStripIconContextMenuSupportsRestoreAndPinPanelActions() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        javafx.scene.layout.VBox leftStrip = findSideBarStrip(scene.getRoot(), Side.LEFT);
        assertNotNull(leftStrip);
        Button iconButton = assertDoesNotThrow(() -> findSideBarIconButtons(leftStrip).getFirst());
        ContextMenu contextMenu = iconButton.getContextMenu();
        assertNotNull(contextMenu);

        invokeContextMenuOnShowing(contextMenu);
        MenuItem pinPanelItem = findMenuItem(contextMenu, "Pin Sidebar Panel");
        MenuItem restoreItem = findMenuItem(contextMenu, "Restore from Sidebar");
        assertNotNull(pinPanelItem);
        assertNotNull(restoreItem);

        pinPanelItem.fire();
        assertTrue(snapFX.isSideBarPinnedOpen(Side.LEFT));

        restoreItem.fire();
        assertFalse(snapFX.isPinnedToSideBar(tool));
        assertTrue(isInGraph(snapFX, tool));
    }

    @Test
    void testSidebarPanelHeaderContextMenuExistsAndIsLockAware() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        snapFX.pinOpenSideBar(Side.LEFT);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        Node headerNode = findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_HEADER);
        assertInstanceOf(javafx.scene.layout.HBox.class, headerNode);
        ContextMenu contextMenu = readContextMenuFromNodeProperties(headerNode);
        assertNotNull(contextMenu);

        invokeContextMenuOnShowing(contextMenu);
        MenuItem restoreItem = findMenuItem(contextMenu, "Restore from Sidebar");
        MenuItem moveRightItem = findMenuItem(contextMenu, "Move to Right Sidebar");
        MenuItem pinPanelItem = findMenuItem(contextMenu, "Unpin Sidebar Panel");
        assertNotNull(restoreItem);
        assertNotNull(moveRightItem);
        assertNotNull(pinPanelItem);
        assertFalse(restoreItem.isDisable());
        assertFalse(moveRightItem.isDisable());
        assertFalse(pinPanelItem.isDisable());

        snapFX.setLocked(true);
        invokeContextMenuOnShowing(contextMenu);

        assertTrue(restoreItem.isDisable());
        assertTrue(moveRightItem.isDisable());
        assertTrue(pinPanelItem.isDisable());
    }

    @Test
    void testPinnedSideBarPanelUsesConfiguredWidthAndRendersResizeHandle() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        snapFX.setSideBarPanelWidth(Side.LEFT, 420.0);
        snapFX.pinOpenSideBar(Side.LEFT);

        Scene pinnedScene = new Scene(snapFX.buildLayout(), 800, 600);
        pinnedScene.getRoot().applyCss();
        pinnedScene.getRoot().layout();

        Node pinnedPanel = findNodeWithStyleClass(pinnedScene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_PINNED);
        assertInstanceOf(javafx.scene.layout.VBox.class, pinnedPanel);
        assertEquals(420.0, ((javafx.scene.layout.VBox) pinnedPanel).getPrefWidth(), 0.0001);
        assertNotNull(findNodeWithStyleClass(pinnedScene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_RESIZE_HANDLE));
    }

    @Test
    void testOverlaySideBarPanelUsesConfiguredWidthAndRendersResizeHandle() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        snapFX.setSideBarPanelWidth(Side.LEFT, 420.0);
        // Prebuilding here can schedule an async sidebar rebuild via icon-click handling and race the next buildLayout().
        invokeSideBarIconClick(snapFX, Side.LEFT, tool);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        Node overlayPanel = findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_OVERLAY);
        assertInstanceOf(javafx.scene.layout.VBox.class, overlayPanel);
        assertEquals(420.0, ((javafx.scene.layout.VBox) overlayPanel).getPrefWidth(), 0.0001);

        Node overlayHost = findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_OVERLAY_HOST);
        assertInstanceOf(javafx.scene.layout.HBox.class, overlayHost);
        assertEquals(461.0, ((javafx.scene.layout.HBox) overlayHost).getPrefWidth(), 0.5);
        assertNotNull(findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_RESIZE_HANDLE));
    }

    @Test
    void testRightOverlaySideBarResizeHandleIsPickableAndUpdatesWidth() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.RIGHT);
        snapFX.setSideBarPanelWidth(Side.RIGHT, 300.0);
        // Keep rootContainer uninitialized until the final scene build to avoid async rebuild races in CI.
        invokeSideBarIconClick(snapFX, Side.RIGHT, tool);

        Scene scene = new Scene(snapFX.buildLayout(), 1000, 700);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        Node handle = findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_RESIZE_HANDLE);
        assertNotNull(handle);

        Bounds handleBounds = handle.localToScene(handle.getBoundsInLocal());
        double centerSceneX = (handleBounds.getMinX() + handleBounds.getMaxX()) / 2.0;
        double centerSceneY = (handleBounds.getMinY() + handleBounds.getMaxY()) / 2.0;

        Node overlayPanel = findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_OVERLAY);
        assertNotNull(overlayPanel);
        assertTrue(handle.isPickOnBounds());
        assertTrue(handle.getViewOrder() < overlayPanel.getViewOrder());

        double startWidth = snapFX.getSideBarPanelWidth(Side.RIGHT);
        Event.fireEvent(handle, createMouseEvent(handle, handle, MouseEvent.MOUSE_PRESSED, centerSceneX, centerSceneY, centerSceneX, centerSceneY));
        Event.fireEvent(handle, createMouseEvent(handle, handle, MouseEvent.MOUSE_DRAGGED, centerSceneX - 40.0, centerSceneY, centerSceneX - 40.0, centerSceneY));
        Event.fireEvent(handle, createMouseEvent(handle, handle, MouseEvent.MOUSE_RELEASED, centerSceneX - 40.0, centerSceneY, centerSceneX - 40.0, centerSceneY));

        assertTrue(snapFX.getSideBarPanelWidth(Side.RIGHT) > startWidth);
    }

    @Test
    void testSideBarPanelWidthIsRuntimeClampedByAvailableLayoutWidth() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");
        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);
        snapFX.pinOpenSideBar(Side.LEFT);
        snapFX.setSideBarPanelWidth(Side.LEFT, 1000.0);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        Node pinnedPanel = findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_PANEL_PINNED);
        assertInstanceOf(javafx.scene.layout.VBox.class, pinnedPanel);
        assertEquals(480.0, ((javafx.scene.layout.VBox) pinnedPanel).getPrefWidth(), 0.5);
    }

    @Test
    void testSidebarDropPreviewLineIsShownForStripHoverAndClearedOutsideStrip() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode toolA = new DockNode("toolA", new Label("Tool A"), "Tool A");
        DockNode toolB = new DockNode("toolB", new Label("Tool B"), "Tool B");

        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(toolA, main, DockPosition.RIGHT);
        snapFX.dock(toolB, main, DockPosition.BOTTOM);
        snapFX.pinToSideBar(toolA, Side.LEFT);
        snapFX.pinToSideBar(toolB, Side.LEFT);

        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        javafx.scene.layout.VBox leftStrip = findSideBarStrip(scene.getRoot(), Side.LEFT);
        assertNotNull(leftStrip);
        List<Button> iconButtons = findSideBarIconButtons(leftStrip);
        assertEquals(2, iconButtons.size());

        Bounds firstBounds = iconButtons.get(0).localToScene(iconButtons.get(0).getBoundsInLocal());
        Bounds secondBounds = iconButtons.get(1).localToScene(iconButtons.get(1).getBoundsInLocal());
        double stripSceneX = (firstBounds.getMinX() + firstBounds.getMaxX()) / 2.0;
        double betweenSceneY = (firstBounds.getMaxY() + secondBounds.getMinY()) / 2.0;

        assertTrue(updateSideBarDropPreviewAtScenePoint(snapFX, main, stripSceneX, betweenSceneY));
        Node previewLine = findNodeWithStyleClass(scene.getRoot(), DockThemeStyleClasses.DOCK_SIDEBAR_DROP_INSERT_LINE);
        assertNotNull(previewLine);
        assertTrue(previewLine.isVisible());

        assertFalse(updateSideBarDropPreviewAtScenePoint(snapFX, main, 400.0, 300.0));
        assertFalse(previewLine.isVisible());
    }

    @Test
    void testResolvedDropRequestUnpinsSidebarNodeBeforeDockingToMainLayout() {
        DockNode left = new DockNode("left", new Label("Left"), "Left");
        DockNode right = new DockNode("right", new Label("Right"), "Right");

        snapFX.dock(left, null, DockPosition.CENTER);
        snapFX.dock(right, left, DockPosition.RIGHT);
        snapFX.pinToSideBar(left, Side.LEFT);

        invokeResolvedDropRequest(
            snapFX,
            new DockDragService.DropRequest(left, right, DockPosition.LEFT, null)
        );

        assertFalse(snapFX.isPinnedToSideBar(left));
        DockSplitPane split = assertInstanceOf(DockSplitPane.class, snapFX.getDockGraph().getRoot());
        assertEquals(left, split.getChildren().getFirst());
        assertEquals(right, split.getChildren().getLast());
    }

    @Test
    void testUnresolvedDropRequestCanFloatPinnedSidebarNode() {
        DockNode main = new DockNode("main", new Label("Main"), "Main");
        DockNode tool = new DockNode("tool", new Label("Tool"), "Tool");

        snapFX.dock(main, null, DockPosition.CENTER);
        snapFX.dock(tool, main, DockPosition.RIGHT);
        snapFX.pinToSideBar(tool, Side.LEFT);

        invokeUnresolvedDropRequest(
            snapFX,
            new DockDragService.FloatDetachRequest(tool, -10_000.0, -10_000.0)
        );

        assertFalse(snapFX.isPinnedToSideBar(tool));
        assertEquals(1, snapFX.getFloatingWindows().size());
        assertTrue(snapFX.getFloatingWindows().getFirst().getDockNodes().contains(tool));
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
        assertTrue(json.contains("\"alwaysOnTop\""));
    }

    @Test
    void testSaveLoadRoundTripRestoresFloatingWindowBoundsAndNode() throws DockLayoutLoadException {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.setNodeFactory(this::createFactoryNode);
        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);

        DockFloatingWindow floatingWindow = snapFX.floatNode(nodeFloat, 333.0, 144.0);
        floatingWindow.setPreferredSize(610.0, 390.0);
        snapFX.setFloatingWindowAlwaysOnTop(floatingWindow, false);

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
        assertFalse(restoredWindow.isAlwaysOnTop());

        List<DockNode> floatingNodes = restoredWindow.getDockNodes();
        assertEquals(1, floatingNodes.size());
        assertEquals("nodeFloat", floatingNodes.get(0).getDockNodeId());
        assertFalse(isInGraph(restored, floatingNodes.get(0)));

        DockNode restoredMainNode = assertInstanceOf(DockNode.class, restored.getDockGraph().getRoot());
        assertEquals("nodeMain", restoredMainNode.getDockNodeId());
    }

    @Test
    void testLoadLayoutRemainsCompatibleWithLegacyMainLayoutJson() throws DockLayoutLoadException {
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
    void testSaveLoadRoundTripRestoresComplexFloatingSubtrees() throws DockLayoutLoadException {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloatA = new DockNode("nodeFloatA", new Label("Float A"), "Float A");
        DockNode nodeFloatATab = new DockNode("nodeFloatATab", new Label("Float A Tab"), "Float A Tab");
        DockNode nodeFloatSplit = new DockNode("nodeFloatSplit", new Label("Float Split"), "Float Split");
        DockNode nodeFloatB = new DockNode("nodeFloatB", new Label("Float B"), "Float B");
        DockNode nodeFloatBTab = new DockNode("nodeFloatBTab", new Label("Float B Tab"), "Float B Tab");

        snapFX.setNodeFactory(this::createFactoryNode);
        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloatA, nodeMain, DockPosition.RIGHT);
        snapFX.dock(nodeFloatB, nodeMain, DockPosition.BOTTOM);

        DockFloatingWindow windowA = snapFX.floatNode(nodeFloatA, 280.0, 160.0);
        windowA.dockNode(nodeFloatATab, nodeFloatA, DockPosition.CENTER, null);
        windowA.dockNode(nodeFloatSplit, nodeFloatA, DockPosition.RIGHT, null);
        DockTabPane tabPaneA = findFirstTabPane(windowA.getDockGraph().getRoot());
        assertNotNull(tabPaneA);
        tabPaneA.setSelectedIndex(1);

        DockFloatingWindow windowB = snapFX.floatNode(nodeFloatB, 760.0, 220.0);
        windowB.dockNode(nodeFloatBTab, nodeFloatB, DockPosition.CENTER, null);
        DockTabPane tabPaneB = assertInstanceOf(DockTabPane.class, windowB.getDockGraph().getRoot());
        tabPaneB.setSelectedIndex(1);

        String json = snapFX.saveLayout();

        SnapFX restored = new SnapFX();
        restored.setNodeFactory(this::createFactoryNode);
        restored.loadLayout(json);

        assertEquals(2, restored.getFloatingWindows().size());

        List<List<String>> floatingNodeIds = restored.getFloatingWindows().stream()
            .map(window -> window.getDockNodes().stream()
                .map(DockNode::getDockNodeId)
                .sorted()
                .toList())
            .toList();

        assertTrue(floatingNodeIds.contains(List.of("nodeFloatA", "nodeFloatATab", "nodeFloatSplit")));
        assertTrue(floatingNodeIds.contains(List.of("nodeFloatB", "nodeFloatBTab")));

        for (DockFloatingWindow restoredWindow : restored.getFloatingWindows()) {
            DockTabPane restoredTabPane = findFirstTabPane(restoredWindow.getDockGraph().getRoot());
            assertNotNull(restoredTabPane);
            assertEquals(1, restoredTabPane.getSelectedIndex());
        }

        DockNode restoredMainNode = assertInstanceOf(DockNode.class, restored.getDockGraph().getRoot());
        assertEquals("nodeMain", restoredMainNode.getDockNodeId());
        assertFalse(containsDockNodeId(restored.getDockGraph().getRoot(), "nodeFloatA"));
        assertFalse(containsDockNodeId(restored.getDockGraph().getRoot(), "nodeFloatB"));
    }

    @Test
    void testLoadLayoutThrowsForInvalidFloatingSnapshotAndKeepsCurrentState() {
        DockNode nodeMain = new DockNode("nodeMain", new Label("Main"), "Main");
        DockNode nodeFloat = new DockNode("nodeFloat", new Label("Float"), "Float");

        snapFX.dock(nodeMain, null, DockPosition.CENTER);
        snapFX.dock(nodeFloat, nodeMain, DockPosition.RIGHT);
        snapFX.floatNode(nodeFloat, 420.0, 210.0);

        String json = snapFX.saveLayout();
        JsonObject snapshot = JsonParser.parseString(json).getAsJsonObject();
        JsonArray floatingWindows = snapshot.getAsJsonArray("floatingWindows");
        JsonObject floatingLayout = floatingWindows.get(0).getAsJsonObject().getAsJsonObject("layout");
        floatingLayout.getAsJsonObject("root").remove("title");

        DockNode originalRoot = assertInstanceOf(DockNode.class, snapFX.getDockGraph().getRoot());
        int originalFloatingCount = snapFX.getFloatingWindows().size();

        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> snapFX.loadLayout(snapshot.toString())
        );

        assertEquals("$.floatingWindows[0].layout.root.title", exception.getLocation());
        assertTrue(exception.getMessage().contains("Missing required field"));
        assertSame(originalRoot, snapFX.getDockGraph().getRoot());
        assertEquals(originalFloatingCount, snapFX.getFloatingWindows().size());
    }

    @Test
    void testLoadLayoutWithUnknownDockNodeTypeCreatesPlaceholderInsteadOfThrowing() throws DockLayoutLoadException {
        String json = """
            {
              "mainLayout": {
                "locked": false,
                "layoutIdCounter": 9,
                "root": {
                  "id": "split-root",
                  "type": "DockSplitPane",
                  "orientation": "VERTICAL",
                  "children": [
                    {
                      "id": "dock-6",
                      "dockNodeId": "consolePanel!",
                      "type": "DockNode!!!",
                      "title": "Console",
                      "closeable": true
                    },
                    {
                      "id": "dock-5",
                      "dockNodeId": "tasks",
                      "type": "DockNode",
                      "title": "Tasks",
                      "closeable": true
                    }
                  ],
                  "dividerPositions": [
                    0.3
                  ]
                }
              },
              "floatingWindows": []
            }
            """;
        snapFX.setNodeFactory(nodeId -> "tasks".equals(nodeId) ? createFactoryNode(nodeId) : null);

        snapFX.loadLayout(json);

        DockSplitPane root = assertInstanceOf(DockSplitPane.class, snapFX.getDockGraph().getRoot());
        DockNode placeholderNode = assertInstanceOf(DockNode.class, root.getChildren().getFirst());
        assertEquals("consolePanel!", placeholderNode.getDockNodeId());
        Label placeholderLabel = assertInstanceOf(Label.class, placeholderNode.getContent());
        assertTrue(
            placeholderLabel.getText().contains("DockNode!!!"),
            "Unexpected placeholder text: " + placeholderLabel.getText()
        );
        assertTrue(
            placeholderLabel.getText().contains("$.root.children[0].type"),
            "Unexpected placeholder text: " + placeholderLabel.getText()
        );
    }

    @Test
    void testLoadLayoutThrowsForInvalidJsonAndKeepsCurrentLayout() {
        DockNode node = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node, null, DockPosition.CENTER);

        DockNode originalRoot = assertInstanceOf(DockNode.class, snapFX.getDockGraph().getRoot());

        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> snapFX.loadLayout("{ invalid")
        );

        assertTrue(exception.getMessage().contains("Invalid JSON syntax"));
        assertSame(originalRoot, snapFX.getDockGraph().getRoot());
    }

    @Test
    void testLoadLayoutThrowsForBlankContentAndKeepsCurrentLayout() {
        DockNode node = new DockNode("node1", new Label("Node 1"), "Node 1");
        snapFX.dock(node, null, DockPosition.CENTER);

        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> snapFX.loadLayout("   ")
        );

        assertTrue(exception.getMessage().contains("empty"));
        assertSame(node, snapFX.getDockGraph().getRoot());
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

    private TabPane buildRootTabPane(SnapFX framework) {
        var root = assertInstanceOf(javafx.scene.layout.StackPane.class, framework.buildLayout());
        TabPane tabPane = findFirstTabPaneInView(root);
        assertNotNull(tabPane);
        return tabPane;
    }

    private boolean requestFloatDetach(SnapFX framework, DockNode node, double screenX, double screenY) {
        try {
            Method requestFloatDetach = DockDragService.class.getDeclaredMethod(
                "requestFloatDetach",
                DockNode.class,
                double.class,
                double.class
            );
            requestFloatDetach.setAccessible(true);
            return (boolean) requestFloatDetach.invoke(framework.getDragService(), node, screenX, screenY);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause();
            String detail = cause != null ? cause.toString() : exception.toString();
            fail("Failed to request float detach via drag service reflection: " + detail);
            return false;
        }
    }

    private void invokeSideBarIconClick(SnapFX framework, Side side, DockNode node) {
        try {
            Method method = SnapFX.class.getDeclaredMethod("onSideBarIconClicked", Side.class, DockNode.class);
            method.setAccessible(true);
            method.invoke(framework, side, node);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause();
            String detail = cause != null ? cause.toString() : exception.toString();
            fail("Failed to invoke sidebar icon click handler via reflection: " + detail);
        }
    }

    private boolean tryDropIntoSideBarAtScenePoint(SnapFX framework, DockNode node, double sceneX, double sceneY) {
        try {
            Method method = SnapFX.class.getDeclaredMethod(
                "tryDropIntoSideBarAtScenePoint",
                DockNode.class,
                double.class,
                double.class
            );
            method.setAccessible(true);
            return (boolean) method.invoke(framework, node, sceneX, sceneY);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause();
            String detail = cause != null ? cause.toString() : exception.toString();
            fail("Failed to invoke sidebar scene-point drop helper via reflection: " + detail);
            return false;
        }
    }

    private boolean updateSideBarDropPreviewAtScenePoint(SnapFX framework, DockNode node, double sceneX, double sceneY) {
        try {
            Method method = SnapFX.class.getDeclaredMethod(
                "updateSideBarDropPreviewAtScenePoint",
                DockNode.class,
                double.class,
                double.class
            );
            method.setAccessible(true);
            return (boolean) method.invoke(framework, node, sceneX, sceneY);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause();
            String detail = cause != null ? cause.toString() : exception.toString();
            fail("Failed to invoke sidebar drop-preview helper via reflection: " + detail);
            return false;
        }
    }

    private void invokeResolvedDropRequest(SnapFX framework, DockDragService.DropRequest request) {
        try {
            Method method = SnapFX.class.getDeclaredMethod("handleResolvedDropRequest", DockDragService.DropRequest.class);
            method.setAccessible(true);
            method.invoke(framework, request);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause();
            String detail = cause != null ? cause.toString() : exception.toString();
            fail("Failed to invoke resolved drop handler via reflection: " + detail);
        }
    }

    private void invokeUnresolvedDropRequest(SnapFX framework, DockDragService.FloatDetachRequest request) {
        try {
            Method method = SnapFX.class.getDeclaredMethod("handleUnresolvedDropRequest", DockDragService.FloatDetachRequest.class);
            method.setAccessible(true);
            method.invoke(framework, request);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause();
            String detail = cause != null ? cause.toString() : exception.toString();
            fail("Failed to invoke unresolved drop handler via reflection: " + detail);
        }
    }

    private javafx.scene.layout.VBox findSideBarStrip(Node current, Side side) {
        if (current == null) {
            return null;
        }
        if (current instanceof javafx.scene.layout.VBox strip) {
            String sideStyle = side == Side.LEFT
                ? DockThemeStyleClasses.DOCK_SIDEBAR_STRIP_LEFT
                : DockThemeStyleClasses.DOCK_SIDEBAR_STRIP_RIGHT;
            if (strip.getStyleClass().contains(DockThemeStyleClasses.DOCK_SIDEBAR_STRIP)
                && strip.getStyleClass().contains(sideStyle)) {
                return strip;
            }
        }
        if (current instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                javafx.scene.layout.VBox match = findSideBarStrip(child, side);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private List<Button> findSideBarIconButtons(javafx.scene.layout.VBox strip) {
        List<Button> result = new java.util.ArrayList<>();
        if (strip == null) {
            return result;
        }
        for (Node child : strip.getChildren()) {
            if (child instanceof Button button
                && button.getStyleClass().contains(DockThemeStyleClasses.DOCK_SIDEBAR_ICON_BUTTON)) {
                result.add(button);
            }
        }
        return result;
    }

    private ContextMenu readContextMenuFromNodeProperties(Node node) {
        if (node == null) {
            return null;
        }
        for (Object value : node.getProperties().values()) {
            if (value instanceof ContextMenu contextMenu) {
                return contextMenu;
            }
        }
        return null;
    }

    private MenuItem findMenuItem(ContextMenu contextMenu, String label) {
        if (contextMenu == null || label == null) {
            return null;
        }
        return contextMenu.getItems().stream()
            .filter(item -> label.equals(item.getText()))
            .findFirst()
            .orElse(null);
    }

    private void invokeContextMenuOnShowing(ContextMenu contextMenu) {
        if (contextMenu != null && contextMenu.getOnShowing() != null) {
            contextMenu.getOnShowing().handle(null);
        }
    }

    private MouseEvent createMouseEvent(
        Node source,
        Node target,
        javafx.event.EventType<MouseEvent> type,
        double x,
        double y,
        double screenX,
        double screenY
    ) {
        return new MouseEvent(
            source,
            target,
            type,
            x,
            y,
            screenX,
            screenY,
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
            new PickResult(target, x, y)
        );
    }

    private Node findNodeWithStyleClass(Node current, String styleClass) {
        if (current == null || styleClass == null) {
            return null;
        }
        if (current.getStyleClass().contains(styleClass)) {
            return current;
        }
        if (current instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = findNodeWithStyleClass(child, styleClass);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    // Helper method to check if node is in graph
    private boolean isInGraph(SnapFX snapFX, DockNode node) {
        return findInGraph(snapFX.getDockGraph().getRoot(), node);
    }

    private boolean findInGraph(DockElement current, DockNode target) {
        if (current == null) {
            return false;
        }
        if (current == target) {
            return true;
        }
        if (current instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                if (findInGraph(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private DockTabPane findFirstTabPane(DockElement current) {
        if (current == null) {
            return null;
        }
        if (current instanceof DockTabPane dockTabPane) {
            return dockTabPane;
        }
        if (current instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                DockTabPane tabPane = findFirstTabPane(child);
                if (tabPane != null) {
                    return tabPane;
                }
            }
        }
        return null;
    }

    private TabPane findFirstTabPaneInView(Node current) {
        if (current == null) {
            return null;
        }
        if (current instanceof TabPane tabPane) {
            return tabPane;
        }
        if (current instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TabPane tabPane = findFirstTabPaneInView(child);
                if (tabPane != null) {
                    return tabPane;
                }
            }
        }
        return null;
    }

    private int countNodesWithStyleClass(Node current, String styleClass) {
        if (current == null || styleClass == null || styleClass.isBlank()) {
            return 0;
        }

        int count = current.getStyleClass().contains(styleClass) ? 1 : 0;
        if (current instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                count += countNodesWithStyleClass(child, styleClass);
            }
        }
        return count;
    }

    private boolean containsDockNodeId(DockElement current, String dockNodeId) {
        if (current == null || dockNodeId == null || dockNodeId.isBlank()) {
            return false;
        }
        if (current instanceof DockNode dockNode) {
            return dockNodeId.equals(dockNode.getDockNodeId());
        }
        if (current instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                if (containsDockNodeId(child, dockNodeId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private DockFloatingWindow findFloatingWindowContainingNode(SnapFX framework, DockNode node) {
        if (framework == null || node == null) {
            return null;
        }
        return framework.getFloatingWindows().stream()
            .filter(window -> window != null && window.containsNode(node))
            .findFirst()
            .orElse(null);
    }

    private FloatingThreeWindowLayout createThreeWindowFloatingLayout() {
        DockNode mainNode = new DockNode("main", new Label("Main"), "Main");
        DockNode topLeftNode = new DockNode("top-left", new Label("Top Left"), "Top Left");
        DockNode topRightNode = new DockNode("top-right", new Label("Top Right"), "Top Right");
        DockNode bottomNode = new DockNode("bottom", new Label("Bottom"), "Bottom");

        snapFX.dock(mainNode, null, DockPosition.CENTER);
        snapFX.dock(topLeftNode, mainNode, DockPosition.RIGHT);

        DockFloatingWindow sourceWindow = snapFX.floatNode(topLeftNode, 320.0, 220.0);
        sourceWindow.dockNode(topRightNode, topLeftNode, DockPosition.RIGHT, null);
        sourceWindow.dockNode(bottomNode, sourceWindow.getDockGraph().getRoot(), DockPosition.BOTTOM, null);

        return new FloatingThreeWindowLayout(sourceWindow, topLeftNode, topRightNode, bottomNode);
    }

    private void assertDetachAttachRestoresFloatingLayout(FloatingThreeWindowLayout layout, DockNode detachedNode) {
        assertNotNull(layout);
        assertNotNull(detachedNode);
        assertNotNull(layout.sourceWindow());
        assertTrue(layout.sourceWindow().containsNode(layout.topLeft()));
        assertTrue(layout.sourceWindow().containsNode(layout.topRight()));
        assertTrue(layout.sourceWindow().containsNode(layout.bottom()));

        String baselineLayoutFingerprint = describeLayout(layout.sourceWindow().getDockGraph().getRoot());

        layout.sourceWindow().requestFloatForNode(detachedNode);

        DockFloatingWindow detachedWindow = findFloatingWindowContainingNode(snapFX, detachedNode);
        assertNotNull(detachedWindow);
        assertNotSame(layout.sourceWindow(), detachedWindow);
        assertEquals(2, snapFX.getFloatingWindows().size());

        snapFX.attachFloatingWindow(detachedWindow);

        assertEquals(1, snapFX.getFloatingWindows().size());
        assertSame(layout.sourceWindow(), snapFX.getFloatingWindows().getFirst());
        assertTrue(layout.sourceWindow().containsNode(layout.topLeft()));
        assertTrue(layout.sourceWindow().containsNode(layout.topRight()));
        assertTrue(layout.sourceWindow().containsNode(layout.bottom()));
        assertEquals(
            baselineLayoutFingerprint,
            describeLayout(layout.sourceWindow().getDockGraph().getRoot())
        );
    }

    private void assertDetachCloseRemainingAttachRestoresIntoFloatingHost(
        FloatingThreeWindowLayout layout,
        DockNode detachedNode,
        DockNode nodeToClose
    ) {
        assertNotNull(layout);
        assertNotNull(detachedNode);
        assertNotNull(nodeToClose);
        assertNotSame(detachedNode, nodeToClose);
        assertNotNull(layout.sourceWindow());
        assertTrue(layout.sourceWindow().containsNode(detachedNode));
        assertTrue(layout.sourceWindow().containsNode(nodeToClose));

        layout.sourceWindow().requestFloatForNode(detachedNode);

        DockFloatingWindow detachedWindow = findFloatingWindowContainingNode(snapFX, detachedNode);
        assertNotNull(detachedWindow);
        assertNotSame(layout.sourceWindow(), detachedWindow);
        assertEquals(2, snapFX.getFloatingWindows().size());

        snapFX.close(nodeToClose);

        assertTrue(snapFX.getHiddenNodes().contains(nodeToClose));
        assertFalse(layout.sourceWindow().containsNode(nodeToClose));
        assertEquals(2, snapFX.getFloatingWindows().size());
        assertEquals(1, layout.sourceWindow().getDockNodes().size());

        snapFX.attachFloatingWindow(detachedWindow);

        DockFloatingWindow postAttachHost = findFloatingWindowContainingNode(snapFX, detachedNode);
        assertTrue(
            layout.sourceWindow().containsNode(detachedNode),
            "Expected detached node to reattach into source floating window, but host was: "
                + (postAttachHost == null ? "none" : (postAttachHost == layout.sourceWindow() ? "source" : "other"))
                + ", inMain=" + isInGraph(snapFX, detachedNode)
                + ", floatingWindowCount=" + snapFX.getFloatingWindows().size()
        );
        assertFalse(isInGraph(snapFX, detachedNode));
        assertEquals(1, snapFX.getFloatingWindows().size());
        assertSame(layout.sourceWindow(), snapFX.getFloatingWindows().getFirst());
    }

    private String describeLayout(DockElement element) {
        if (element == null) {
            return "null";
        }
        if (element instanceof DockNode node) {
            return node.getDockNodeId();
        }
        if (element instanceof DockSplitPane splitPane) {
            StringBuilder builder = new StringBuilder();
            builder.append("S(").append(splitPane.getOrientation()).append(")[");
            List<DockElement> children = splitPane.getChildren();
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(describeLayout(children.get(i)));
            }
            builder.append("]");
            return builder.toString();
        }
        if (element instanceof DockTabPane tabPane) {
            StringBuilder builder = new StringBuilder();
            builder.append("T(").append(tabPane.getSelectedIndex()).append(")[");
            List<DockElement> children = tabPane.getChildren();
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(describeLayout(children.get(i)));
            }
            builder.append("]");
            return builder.toString();
        }
        return element.getClass().getSimpleName() + ":" + element.getId();
    }

    private record FloatingThreeWindowLayout(
        DockFloatingWindow sourceWindow,
        DockNode topLeft,
        DockNode topRight,
        DockNode bottom
    ) {
    }

    private void runOnFxThreadAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for JavaFX thread", e);
        }
        if (error.get() != null) {
            throw new AssertionError("JavaFX action failed", error.get());
        }
    }

    private void closeGhostStage(SnapFX framework) {
        if (framework == null) {
            return;
        }
        try {
            Field dragServiceField = SnapFX.class.getDeclaredField("dragService");
            dragServiceField.setAccessible(true);
            Object dragService = dragServiceField.get(framework);
            if (dragService == null) {
                return;
            }
            Field ghostStageField = DockDragService.class.getDeclaredField("ghostStage");
            ghostStageField.setAccessible(true);
            Object ghostStage = ghostStageField.get(dragService);
            if (ghostStage instanceof Stage stage) {
                stage.close();
            }
        } catch (ReflectiveOperationException ignored) {
            // Best-effort test cleanup.
        }
    }

    private DockNode createFactoryNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }
        return new DockNode(nodeId, new Label("Factory: " + nodeId), nodeId);
    }
}


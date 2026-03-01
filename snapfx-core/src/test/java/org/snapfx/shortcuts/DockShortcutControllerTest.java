package org.snapfx.shortcuts;

import org.snapfx.floating.DockFloatingWindow;
import org.snapfx.model.DockNode;
import org.snapfx.view.DockLayoutEngine;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockShortcutControllerTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // JavaFX runtime already started.
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void defaultsAreConfigured() {
        DockShortcutController controller = new DockShortcutController();

        assertEquals(
            new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
            controller.getShortcut(DockShortcutAction.CLOSE_ACTIVE_NODE)
        );
        assertEquals(
            new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHORTCUT_DOWN),
            controller.getShortcut(DockShortcutAction.NEXT_TAB)
        );
        assertEquals(
            new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            controller.getShortcut(DockShortcutAction.PREVIOUS_TAB)
        );
        assertEquals(
            new KeyCodeCombination(KeyCode.ESCAPE),
            controller.getShortcut(DockShortcutAction.CANCEL_DRAG)
        );
        assertEquals(
            new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            controller.getShortcut(DockShortcutAction.TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP)
        );
    }

    @Test
    void setShortcutRemovesDuplicateBindingFromOtherAction() {
        DockShortcutController controller = new DockShortcutController();
        KeyCodeCombination ctrlW = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);

        controller.setShortcut(DockShortcutAction.NEXT_TAB, ctrlW);

        assertNull(controller.getShortcut(DockShortcutAction.CLOSE_ACTIVE_NODE));
        assertEquals(ctrlW, controller.getShortcut(DockShortcutAction.NEXT_TAB));
    }

    @Test
    void resolveShortcutActionMatchesCurrentBinding() {
        DockShortcutController controller = new DockShortcutController();
        controller.setShortcut(
            DockShortcutAction.CLOSE_ACTIVE_NODE,
            new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN)
        );

        KeyEvent ctrlQ = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.Q, false, true, false, false);
        KeyEvent ctrlW = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.W, false, true, false, false);

        assertEquals(DockShortcutAction.CLOSE_ACTIVE_NODE, controller.resolveShortcutAction(ctrlQ));
        assertNull(controller.resolveShortcutAction(ctrlW));
    }

    @Test
    void clearShortcutRemovesActionBinding() {
        DockShortcutController controller = new DockShortcutController();

        controller.clearShortcut(DockShortcutAction.CANCEL_DRAG);

        assertNull(controller.getShortcut(DockShortcutAction.CANCEL_DRAG));
    }

    @Test
    void shortcutSnapshotIsUnmodifiable() {
        DockShortcutController controller = new DockShortcutController();
        Map<DockShortcutAction, KeyCombination> snapshot = controller.getShortcutsSnapshot();

        assertThrows(
            UnsupportedOperationException.class,
            () -> snapshot.put(DockShortcutAction.CLOSE_ACTIVE_NODE, new KeyCodeCombination(KeyCode.W))
        );
    }

    @Test
    void floatingShortcutSceneBindingBindsAndUnbindsKeyFilter() {
        runOnFxThreadAndWait(() -> {
            DockShortcutController controller = new DockShortcutController();
            DockFloatingWindow floatingWindow = new DockFloatingWindow(
                new DockNode("floating", new Label("Floating"), "Floating")
            );
            AtomicInteger invocations = new AtomicInteger();
            EventHandler<KeyEvent> handler = event -> invocations.incrementAndGet();

            floatingWindow.show(null);
            try {
                controller.bindFloatingShortcutScene(floatingWindow, handler);
                KeyEvent firstEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED,
                    "",
                    "",
                    KeyCode.W,
                    false,
                    true,
                    false,
                    false
                );
                Event.fireEvent(floatingWindow.getScene(), firstEvent);
                assertEquals(1, invocations.get());

                controller.unbindFloatingShortcutScene(floatingWindow, handler);
                KeyEvent secondEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED,
                    "",
                    "",
                    KeyCode.W,
                    false,
                    true,
                    false,
                    false
                );
                Event.fireEvent(floatingWindow.getScene(), secondEvent);
                assertEquals(1, invocations.get());
            } finally {
                floatingWindow.closeWithoutNotification();
            }
        });
    }

    @Test
    void nodeAndSceneResolversReturnExpectedTargets() {
        runOnFxThreadAndWait(() -> {
            DockShortcutController controller = new DockShortcutController();
            Label node = new Label("target");
            Scene scene = new Scene(new StackPane(node), 320, 200);

            assertEquals(node, controller.resolveNodeFromEventTarget(node));
            assertNull(controller.resolveNodeFromEventTarget(scene));

            assertEquals(scene, controller.resolveSceneFromEventTarget(scene));
            assertEquals(scene, controller.resolveSceneFromEventTarget(node));
            assertEquals(scene, controller.resolveSceneFromNode(node));
            assertNull(controller.resolveSceneFromNode(null));
        });
    }

    @Test
    void tabPaneResolversUseHierarchyThenFallbackRoot() {
        runOnFxThreadAndWait(() -> {
            DockShortcutController controller = new DockShortcutController();

            TabPane eventTabPane = new TabPane(new Tab("Event", new Label("content")));

            assertEquals(eventTabPane, controller.findTabPaneInHierarchy(eventTabPane));
            assertEquals(eventTabPane, controller.resolveActiveTabPane(eventTabPane, null, null));

            TabPane fallbackTabPane = new TabPane(new Tab("Fallback", new Label("fallback")));
            VBox fallbackRoot = new VBox(new StackPane(new Label("before")), fallbackTabPane);

            assertEquals(fallbackTabPane, controller.findFirstTabPane(fallbackRoot));
            assertEquals(fallbackTabPane, controller.resolveActiveTabPane(new Object(), null, fallbackRoot));
        });
    }

    @Test
    void selectTabRelativeMovesSelectionAndWraps() {
        runOnFxThreadAndWait(() -> {
            DockShortcutController controller = new DockShortcutController();
            TabPane tabPane = new TabPane(
                new Tab("First", new Label("first")),
                new Tab("Second", new Label("second"))
            );

            assertTrue(controller.selectTabRelative(1, tabPane, null, null));
            assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());

            assertTrue(controller.selectTabRelative(1, tabPane, null, null));
            assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());

            VBox fallbackRoot = new VBox(tabPane);
            assertTrue(controller.selectTabRelative(-1, new Object(), null, fallbackRoot));
            assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());

            TabPane singleTabPane = new TabPane(new Tab("Only", new Label("only")));
            assertFalse(controller.selectTabRelative(1, singleTabPane, null, null));
        });
    }

    @Test
    void resolveActiveDockNodeUsesTabMappingThenFallbackRoot() {
        runOnFxThreadAndWait(() -> {
            DockShortcutController controller = new DockShortcutController();
            DockNode mappedNode = new DockNode("mapped", new Label("Mapped"), "Mapped");
            TabPane tabPane = new TabPane();
            Tab mappedTab = new Tab("Mapped", new Label("mapped-content"));
            mappedTab.getProperties().put(DockLayoutEngine.TAB_DOCK_NODE_KEY, mappedNode);
            tabPane.getTabs().add(mappedTab);

            assertEquals(mappedNode, controller.resolveActiveDockNode(tabPane, null, null));

            DockNode fallbackNode = new DockNode("fallback", new Label("Fallback"), "Fallback");
            assertEquals(fallbackNode, controller.resolveActiveDockNode(new Object(), null, fallbackNode));
        });
    }

    private void runOnFxThreadAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] failure = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure[0] = throwable;
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for JavaFX action");
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for JavaFX action", interruptedException);
        }

        if (failure[0] != null) {
            throw new AssertionError("JavaFX action failed", failure[0]);
        }
    }
}

package org.snapfx.shortcuts;

import org.snapfx.floating.DockFloatingWindow;
import org.snapfx.model.DockNode;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

package org.snapfx.shortcuts;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DockShortcutControllerTest {

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
}

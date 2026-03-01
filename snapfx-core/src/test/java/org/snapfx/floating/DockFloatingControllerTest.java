package org.snapfx.floating;

import org.snapfx.model.DockNode;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DockFloatingControllerTest {

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
    void resolveActiveFloatingWindowReturnsNullForEmptyInput() {
        DockFloatingController controller = new DockFloatingController();

        assertNull(controller.resolveActiveFloatingWindow(null, null, null));
        assertNull(controller.resolveActiveFloatingWindow(List.of(), null, null));
    }

    @Test
    void resolveActiveFloatingWindowUsesActiveAndFallbackLastWindow() {
        DockFloatingController controller = new DockFloatingController();
        DockFloatingWindow first = createFloatingWindow("first");
        DockFloatingWindow second = createFloatingWindow("second");
        List<DockFloatingWindow> windows = List.of(first, second);

        assertEquals(second, controller.resolveActiveFloatingWindow(windows, null, null));

        controller.setActiveFloatingWindow(first);
        assertEquals(first, controller.resolveActiveFloatingWindow(windows, null, null));

        controller.clearActiveFloatingWindowIfMatches(first);
        assertEquals(second, controller.resolveActiveFloatingWindow(windows, null, null));
    }

    @Test
    void findFloatingWindowContainingNodeResolvesMatchingWindow() {
        DockFloatingController controller = new DockFloatingController();
        DockNode firstNode = new DockNode("first", new Label("First"), "First");
        DockNode secondNode = new DockNode("second", new Label("Second"), "Second");
        DockFloatingWindow firstWindow = new DockFloatingWindow(firstNode);
        DockFloatingWindow secondWindow = new DockFloatingWindow(secondNode);
        List<DockFloatingWindow> windows = List.of(firstWindow, secondWindow);

        assertEquals(firstWindow, controller.findFloatingWindowContainingNode(windows, firstNode));
        assertEquals(secondWindow, controller.findFloatingWindowContainingNode(windows, secondNode));
        assertNull(controller.findFloatingWindowContainingNode(windows, new DockNode("missing", new Label("M"), "M")));
    }

    private DockFloatingWindow createFloatingWindow(String id) {
        DockNode node = new DockNode(id, new Label(id), id);
        return new DockFloatingWindow(node);
    }
}

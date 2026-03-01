package org.snapfx.floating;

import org.snapfx.model.DockNode;
import org.snapfx.model.DockPosition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void resolveActivePlacementHostReturnsOnlyActiveHostFromCurrentList() {
        DockFloatingController controller = new DockFloatingController();
        DockFloatingWindow first = createFloatingWindow("first");
        DockFloatingWindow second = createFloatingWindow("second");
        List<DockFloatingWindow> windows = new ArrayList<>(List.of(first));

        assertEquals(first, controller.resolveActivePlacementHost(windows, first));
        assertNull(controller.resolveActivePlacementHost(windows, second));
        assertNull(controller.resolveActivePlacementHost(null, first));
        assertNull(controller.resolveActivePlacementHost(windows, null));
    }

    @Test
    void removeFloatingWindowRemovesWindowAndClearsActiveState() {
        DockFloatingController controller = new DockFloatingController();
        DockFloatingWindow first = createFloatingWindow("first");
        DockFloatingWindow second = createFloatingWindow("second");
        List<DockFloatingWindow> windows = new ArrayList<>(List.of(first, second));

        controller.setActiveFloatingWindow(first);
        assertTrue(controller.removeFloatingWindow(windows, first));
        assertEquals(List.of(second), windows);
        assertFalse(controller.removeFloatingWindow(windows, first));

        windows.addFirst(first);
        assertEquals(second, controller.resolveActiveFloatingWindow(windows, null, null));
    }

    @Test
    void findTopFloatingWindowAtReturnsNullForEmptyInput() {
        DockFloatingController controller = new DockFloatingController();

        assertNull(controller.findTopFloatingWindowAt(null, 42.0, 73.0));
        assertNull(controller.findTopFloatingWindowAt(List.of(), 42.0, 73.0));
    }

    @Test
    void isMainDropSuppressedByFloatingWindowReturnsFalseForMissingCoordinates() {
        DockFloatingController controller = new DockFloatingController();
        List<DockFloatingWindow> windows = List.of(createFloatingWindow("first"));

        assertFalse(controller.isMainDropSuppressedByFloatingWindow(windows, null, 10.0));
        assertFalse(controller.isMainDropSuppressedByFloatingWindow(windows, 10.0, null));
    }

    @Test
    void promoteFloatingWindowToFrontMovesWindowToListEnd() {
        DockFloatingController controller = new DockFloatingController();
        DockFloatingWindow first = createFloatingWindow("first");
        DockFloatingWindow second = createFloatingWindow("second");
        DockFloatingWindow third = createFloatingWindow("third");
        List<DockFloatingWindow> windows = new ArrayList<>(List.of(first, second, third));

        controller.promoteFloatingWindowToFront(windows, second);

        assertEquals(List.of(first, third, second), windows);

        controller.promoteFloatingWindowToFront(windows, second);
        controller.promoteFloatingWindowToFront(windows, null);
        assertEquals(List.of(first, third, second), windows);
    }

    @Test
    void applyRememberedFloatingBoundsAppliesNodeSnapshotToWindow() {
        DockFloatingController controller = new DockFloatingController();
        DockNode source = new DockNode("source", new Label("Source"), "Source");
        DockFloatingWindow floatingWindow = createFloatingWindow("target");

        source.setLastFloatingX(321.0);
        source.setLastFloatingY(222.0);
        source.setLastFloatingWidth(640.0);
        source.setLastFloatingHeight(420.0);
        source.setLastFloatingAlwaysOnTop(false);

        controller.applyRememberedFloatingBounds(source, floatingWindow);

        assertEquals(Double.valueOf(321.0), floatingWindow.getPreferredX());
        assertEquals(Double.valueOf(222.0), floatingWindow.getPreferredY());
        assertEquals(640.0, floatingWindow.getPreferredWidth(), 0.0001);
        assertEquals(420.0, floatingWindow.getPreferredHeight(), 0.0001);
        assertFalse(floatingWindow.isAlwaysOnTop());
    }

    @Test
    void rememberFloatingBoundsForNodesStoresWindowSnapshotForAllNodes() {
        DockFloatingController controller = new DockFloatingController();
        DockNode firstNode = new DockNode("first", new Label("First"), "First");
        DockNode secondNode = new DockNode("second", new Label("Second"), "Second");
        DockFloatingWindow floatingWindow = new DockFloatingWindow(firstNode);
        floatingWindow.dockNode(secondNode, firstNode, DockPosition.CENTER, null);
        floatingWindow.setPreferredPosition(450.0, 250.0);
        floatingWindow.setPreferredSize(700.0, 480.0);
        floatingWindow.setAlwaysOnTop(false, DockFloatingPinSource.API);

        controller.rememberFloatingBoundsForNodes(floatingWindow);

        assertFloatingSnapshot(firstNode, 450.0, 250.0, 700.0, 480.0, false);
        assertFloatingSnapshot(secondNode, 450.0, 250.0, 700.0, 480.0, false);
    }

    @Test
    void applyFloatingPinSettingsAppliesAllPinConfiguration() {
        DockFloatingController controller = new DockFloatingController();
        DockFloatingWindow floatingWindow = createFloatingWindow("pin");

        controller.applyFloatingPinSettings(
            floatingWindow,
            DockFloatingPinButtonMode.ALWAYS,
            false,
            DockFloatingPinLockedBehavior.HIDE_BUTTON
        );

        assertEquals(DockFloatingPinButtonMode.ALWAYS, floatingWindow.getPinButtonMode());
        assertFalse(floatingWindow.isPinToggleEnabled());
        assertEquals(DockFloatingPinLockedBehavior.HIDE_BUTTON, floatingWindow.getPinLockedBehavior());
    }

    @Test
    void applyFloatingSnapSettingsAppliesSnappingConfiguration() {
        DockFloatingController controller = new DockFloatingController();
        DockFloatingWindow floatingWindow = createFloatingWindow("snap");

        controller.applyFloatingSnapSettings(
            floatingWindow,
            false,
            18.0,
            EnumSet.of(DockFloatingSnapTarget.SCREEN, DockFloatingSnapTarget.FLOATING_WINDOWS),
            () -> List.of(floatingWindow)
        );

        assertFalse(floatingWindow.isSnappingEnabled());
        assertEquals(18.0, floatingWindow.getSnapDistance(), 0.0001);
        assertEquals(
            EnumSet.of(DockFloatingSnapTarget.SCREEN, DockFloatingSnapTarget.FLOATING_WINDOWS),
            floatingWindow.getSnapTargets()
        );
    }

    @Test
    void applyFloatingWindowInitialAlwaysOnTopUsesExplicitOrDefaultValue() {
        DockFloatingController controller = new DockFloatingController();
        DockFloatingWindow explicitWindow = createFloatingWindow("explicit");
        DockFloatingWindow defaultWindow = createFloatingWindow("default");

        controller.applyFloatingWindowInitialAlwaysOnTop(
            explicitWindow,
            false,
            true,
            DockFloatingPinSource.API
        );
        controller.applyFloatingWindowInitialAlwaysOnTop(
            defaultWindow,
            null,
            true,
            DockFloatingPinSource.WINDOW_CREATE_DEFAULT
        );

        assertFalse(explicitWindow.isAlwaysOnTop());
        assertTrue(defaultWindow.isAlwaysOnTop());
    }

    private void assertFloatingSnapshot(
        DockNode node,
        double x,
        double y,
        double width,
        double height,
        boolean alwaysOnTop
    ) {
        assertEquals(Double.valueOf(x), node.getLastFloatingX());
        assertEquals(Double.valueOf(y), node.getLastFloatingY());
        assertEquals(Double.valueOf(width), node.getLastFloatingWidth());
        assertEquals(Double.valueOf(height), node.getLastFloatingHeight());
        assertEquals(Boolean.valueOf(alwaysOnTop), node.getLastFloatingAlwaysOnTop());
    }

    private DockFloatingWindow createFloatingWindow(String id) {
        DockNode node = new DockNode(id, new Label(id), id);
        return new DockFloatingWindow(node);
    }
}

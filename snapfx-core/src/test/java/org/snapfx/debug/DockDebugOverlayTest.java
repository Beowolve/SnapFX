package org.snapfx.debug;

import org.snapfx.dnd.DockDragData;
import org.snapfx.dnd.DockDragService;
import org.snapfx.model.DockGraph;
import org.snapfx.model.DockNode;
import org.snapfx.model.DockPosition;
import org.snapfx.theme.DockThemeStyleClasses;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockDebugOverlayTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit already started by another test class.
        }
    }

    @Test
    void testHudUsesManagedPrefSizedPanel() {
        DockDebugOverlay overlay = runOnFxThreadAndWaitResult(() -> {
            DockGraph dockGraph = new DockGraph();
            DockDragService dragService = new DockDragService(dockGraph);
            return new DockDebugOverlay(dockGraph, dragService);
        });

        assertTrue(overlay.isManaged(), "HUD should be managed so StackPane alignment/margins layout it");
        assertTrue(overlay.getStyleClass().contains(DockThemeStyleClasses.DOCK_DEBUG_PANEL));
        assertEqualsUsePrefSize(overlay.getMaxWidth());
        assertEqualsUsePrefSize(overlay.getMaxHeight());
        assertNotNull(overlay.getBackground(), "HUD should render a visible panel background");
        assertFalse(overlay.isVisible(), "HUD should stay hidden while no drag is active");
    }

    @Test
    void testHudRefreshesTargetAndZoneForMutatedDragData() {
        DockDebugOverlay overlay = runOnFxThreadAndWaitResult(() -> {
            DockGraph dockGraph = new DockGraph();
            DockDragService dragService = new DockDragService(dockGraph);
            DockDebugOverlay created = new DockDebugOverlay(dockGraph, dragService);

            Scene scene = new Scene(new StackPane(), 300, 200);
            DockNode dragged = new DockNode(new Label("Dragged"), "Dragged");
            DockNode target = new DockNode(new Label("Target"), "Target");

            dragService.startDrag(dragged, createPrimaryPressEvent(scene));
            DockDragData data = dragService.getCurrentDrag();
            dragService.currentDragProperty().set(data);
            created.refreshFromDragService();

            assertTrue(created.getHudTextForTest().contains("Target: none"));
            assertTrue(created.getHudTextForTest().contains("Zone: none"));

            data.setDropTarget(target);
            data.setDropPosition(DockPosition.RIGHT);
            dragService.currentDragProperty().set(data); // same reference; overlay must still update on refresh
            created.refreshFromDragService();

            assertTrue(created.getHudTextForTest().contains("Target: Target"));
            assertTrue(created.getHudTextForTest().contains("Zone: RIGHT"));
            return created;
        });

        assertNotNull(overlay);
    }

    private static void assertEqualsUsePrefSize(double value) {
        assertTrue(Double.compare(value, Region.USE_PREF_SIZE) == 0, "Expected USE_PREF_SIZE but was " + value);
    }

    private <T> T runOnFxThreadAndWaitResult(FxSupplier<T> supplier) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(supplier.get());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for FX thread");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for FX thread", e);
        }
        if (error.get() != null) {
            throw new AssertionError("FX thread action failed", error.get());
        }
        return result.get();
    }

    private MouseEvent createPrimaryPressEvent(Scene scene) {
        var source = scene.getRoot();
        return new MouseEvent(
            source,
            source,
            MouseEvent.MOUSE_PRESSED,
            0,
            0,
            120,
            90,
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
            new PickResult(source, 0, 0)
        );
    }

    @FunctionalInterface
    private interface FxSupplier<T> {
        T get();
    }
}

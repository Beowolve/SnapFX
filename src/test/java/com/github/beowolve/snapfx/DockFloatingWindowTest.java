package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.model.DockNode;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockFloatingWindowTest {
    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX is already running.
        }
    }

    @Test
    void testTitleBarActionCandidateAllowsMaximizedWindow() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(1000);
            stage.setHeight(700);
            stage.setMaximized(true);

            Label titleTarget = new Label("Title");
            MouseEvent doubleClick = createMouseEvent(
                MouseEvent.MOUSE_CLICKED,
                420,
                180,
                320,
                100,
                titleTarget,
                titleTarget,
                MouseButton.PRIMARY,
                2
            );

            assertTrue(invokeTitleBarActionCandidate(floatingWindow, doubleClick, stage));
        });
    }

    @Test
    void testRestoreWindowForDragAppliesRememberedBounds() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(150);
            stage.setY(120);
            stage.setWidth(640);
            stage.setHeight(420);

            invokeRememberRestoreBounds(floatingWindow, stage);

            stage.setX(0);
            stage.setY(0);
            stage.setWidth(1400);
            stage.setHeight(900);
            stage.setMaximized(true);

            HBox titleBar = new HBox();
            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                700,
                20,
                350,
                10,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );

            invokeRestoreWindowForDrag(floatingWindow, stage, dragEvent, titleBar);

            assertFalse(stage.isMaximized());
            assertEquals(150.0, stage.getX(), 0.0001);
            assertEquals(120.0, stage.getY(), 0.0001);
            assertEquals(640.0, stage.getWidth(), 0.0001);
            assertEquals(420.0, stage.getHeight(), 0.0001);
            assertEquals(10.0, readDragOffsetY(floatingWindow), 0.0001);
        });
    }

    private boolean invokeTitleBarActionCandidate(DockFloatingWindow floatingWindow, MouseEvent event, Stage stage) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("isTitleBarActionCandidate", MouseEvent.class, Stage.class);
            method.setAccessible(true);
            return (boolean) method.invoke(floatingWindow, event, stage);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke title bar action candidate check", e);
        }
    }

    private void invokeRememberRestoreBounds(DockFloatingWindow floatingWindow, Stage stage) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("rememberRestoreBounds", Stage.class);
            method.setAccessible(true);
            method.invoke(floatingWindow, stage);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke rememberRestoreBounds", e);
        }
    }

    private void invokeRestoreWindowForDrag(DockFloatingWindow floatingWindow, Stage stage, MouseEvent event, HBox titleBar) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("restoreWindowForDrag", Stage.class, MouseEvent.class, HBox.class);
            method.setAccessible(true);
            method.invoke(floatingWindow, stage, event, titleBar);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke restoreWindowForDrag", e);
        }
    }

    private double readDragOffsetY(DockFloatingWindow floatingWindow) {
        try {
            Field field = DockFloatingWindow.class.getDeclaredField("dragOffsetY");
            field.setAccessible(true);
            return field.getDouble(floatingWindow);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read dragOffsetY", e);
        }
    }

    private MouseEvent createMouseEvent(
        javafx.event.EventType<MouseEvent> type,
        double screenX,
        double screenY,
        double x,
        double y,
        Node source,
        Node target,
        MouseButton button,
        int clickCount
    ) {
        return new MouseEvent(
            source,
            target,
            type,
            x,
            y,
            screenX,
            screenY,
            button,
            clickCount,
            false,
            false,
            false,
            false,
            button == MouseButton.PRIMARY,
            false,
            false,
            true,
            false,
            false,
            new PickResult(target, x, y)
        );
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
            throw new AssertionError("JavaFX test action failed", error.get());
        }
    }
}

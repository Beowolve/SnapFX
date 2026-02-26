package org.snapfx.floating;

import org.snapfx.model.DockNode;
import org.snapfx.model.DockPosition;
import org.snapfx.model.DockTabPane;
import org.snapfx.theme.DockThemeStyleClasses;
import org.snapfx.view.DockNodeView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    @Test
    void testSceneDragContinuesAfterTitleBarPress() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            Label contentTarget = new Label("Content");

            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                260,
                190,
                30,
                140,
                contentTarget,
                contentTarget,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(180.0, stage.getX(), 0.0001);
            assertEquals(150.0, stage.getY(), 0.0001);
        });
    }

    @Test
    void testSceneDragDoesNotMoveWindowWithoutActiveTitleBarDrag() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            Label contentTarget = new Label("Content");
            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                260,
                190,
                30,
                140,
                contentTarget,
                contentTarget,
                MouseButton.PRIMARY,
                1
            );

            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(100.0, stage.getX(), 0.0001);
            assertEquals(80.0, stage.getY(), 0.0001);
        });
    }

    @Test
    void testSceneDragStopsAfterMouseRelease() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            Label contentTarget = new Label("Content");

            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent releaseEvent = createMouseEvent(
                MouseEvent.MOUSE_RELEASED,
                180,
                120,
                80,
                12,
                contentTarget,
                contentTarget,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseReleased(floatingWindow, releaseEvent);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                260,
                190,
                30,
                140,
                contentTarget,
                contentTarget,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(100.0, stage.getX(), 0.0001);
            assertEquals(80.0, stage.getY(), 0.0001);
        });
    }

    @Test
    void testSceneDragSnapsToPeerFloatingWindowEdgeWhenEnabled() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            DockFloatingWindow peerWindow = new DockFloatingWindow(new DockNode(new Label("Peer"), "Peer"));

            Stage peerStage = new Stage();
            peerStage.setX(300);
            peerStage.setY(80);
            peerStage.setWidth(640);
            peerStage.setHeight(420);
            writeStage(peerWindow, peerStage);

            floatingWindow.setSnappingEnabled(true);
            floatingWindow.setSnapDistance(15.0);
            floatingWindow.setSnapTargets(EnumSet.of(DockFloatingSnapTarget.FLOATING_WINDOWS));
            floatingWindow.setSnapPeerWindowsSupplier(() -> List.of(peerWindow));

            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                369,
                190,
                30,
                140,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(300.0, stage.getX(), 0.0001);
        });
    }

    @Test
    void testSceneDragDoesNotSnapToPeerFloatingWindowEdgeWhenDisabled() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            DockFloatingWindow peerWindow = new DockFloatingWindow(new DockNode(new Label("Peer"), "Peer"));

            Stage peerStage = new Stage();
            peerStage.setX(300);
            peerStage.setY(80);
            peerStage.setWidth(640);
            peerStage.setHeight(420);
            writeStage(peerWindow, peerStage);

            floatingWindow.setSnappingEnabled(false);
            floatingWindow.setSnapDistance(15.0);
            floatingWindow.setSnapTargets(EnumSet.of(DockFloatingSnapTarget.FLOATING_WINDOWS));
            floatingWindow.setSnapPeerWindowsSupplier(() -> List.of(peerWindow));

            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                369,
                190,
                30,
                140,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(289.0, stage.getX(), 0.0001);
        });
    }

    @Test
    void testSceneDragCanSnapAdjacentToPeerFloatingWindowEdge() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            DockFloatingWindow peerWindow = new DockFloatingWindow(new DockNode(new Label("Peer"), "Peer"));

            Stage peerStage = new Stage();
            peerStage.setX(300);
            peerStage.setY(80);
            peerStage.setWidth(640);
            peerStage.setHeight(420);
            writeStage(peerWindow, peerStage);

            floatingWindow.setSnappingEnabled(true);
            floatingWindow.setSnapDistance(15.0);
            floatingWindow.setSnapTargets(EnumSet.of(DockFloatingSnapTarget.FLOATING_WINDOWS));
            floatingWindow.setSnapPeerWindowsSupplier(() -> List.of(peerWindow));

            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                1029,
                190,
                30,
                140,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(940.0, stage.getX(), 0.0001);
        });
    }

    @Test
    void testSceneDragDoesNotSnapToPeerWindowWhenNoPerpendicularOverlap() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            DockFloatingWindow peerWindow = new DockFloatingWindow(new DockNode(new Label("Peer"), "Peer"));

            Stage peerStage = new Stage();
            peerStage.setX(300);
            peerStage.setY(800);
            peerStage.setWidth(640);
            peerStage.setHeight(420);
            writeStage(peerWindow, peerStage);

            floatingWindow.setSnappingEnabled(true);
            floatingWindow.setSnapDistance(15.0);
            floatingWindow.setSnapTargets(EnumSet.of(DockFloatingSnapTarget.FLOATING_WINDOWS));
            floatingWindow.setSnapPeerWindowsSupplier(() -> List.of(peerWindow));

            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                369,
                190,
                30,
                140,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(289.0, stage.getX(), 0.0001);
        });
    }

    @Test
    void testSceneDragCanSnapAdjacentToMainWindowEdge() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            floatingWindow.setSnappingEnabled(true);
            floatingWindow.setSnapDistance(15.0);
            floatingWindow.setSnapTargets(EnumSet.of(DockFloatingSnapTarget.MAIN_WINDOW));

            Stage ownerStage = new Stage();
            ownerStage.setX(300);
            ownerStage.setY(80);
            ownerStage.setWidth(640);
            ownerStage.setHeight(420);

            Stage stage = new Stage();
            stage.initOwner(ownerStage);
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                1028,
                190,
                30,
                140,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(940.0, stage.getX(), 0.0001);
        });
    }

    @Test
    void testSceneDragDoesNotSnapToMainWindowWhenNoPerpendicularOverlap() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            floatingWindow.setSnappingEnabled(true);
            floatingWindow.setSnapDistance(15.0);
            floatingWindow.setSnapTargets(EnumSet.of(DockFloatingSnapTarget.MAIN_WINDOW));

            Stage ownerStage = new Stage();
            ownerStage.setX(300);
            ownerStage.setY(300);
            ownerStage.setWidth(640);
            ownerStage.setHeight(420);

            Stage stage = new Stage();
            stage.initOwner(ownerStage);
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                1200,
                347,
                30,
                140,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(307.0, stage.getY(), 0.0001);
        });
    }

    @Test
    void testSecondaryTitleBarPressDoesNotActivateSceneDrag() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            Label contentTarget = new Label("Content");

            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.SECONDARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent dragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                260,
                190,
                30,
                140,
                contentTarget,
                contentTarget,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, dragEvent, stage, titleBar);

            assertEquals(100.0, stage.getX(), 0.0001);
            assertEquals(80.0, stage.getY(), 0.0001);
        });
    }

    @Test
    void testTitleBarPrimaryPressHidesVisibleContextMenu() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);

            HBox titleBar = new HBox();
            TrackingContextMenu contextMenu = new TrackingContextMenu();
            writeTitleBarContextMenu(floatingWindow, contextMenu);

            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                180,
                120,
                80,
                12,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            assertTrue(contextMenu.isHideCalled());
        });
    }

    @Test
    void testSmallDragFromMaximizedTitleBarDoesNotRestoreWindow() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(1000);
            stage.setHeight(700);
            stage.setMaximized(true);

            HBox titleBar = new HBox();
            Label contentTarget = new Label("Content");

            MouseEvent pressEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                500,
                100,
                400,
                10,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeOnTitleBarMousePressed(floatingWindow, pressEvent, stage, titleBar);

            MouseEvent tinyDragEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                501,
                101,
                401,
                11,
                contentTarget,
                contentTarget,
                MouseButton.PRIMARY,
                1
            );
            invokeOnSceneMouseDragged(floatingWindow, tinyDragEvent, stage, titleBar);

            assertTrue(stage.isMaximized());
            assertEquals(100.0, stage.getX(), 0.0001);
            assertEquals(80.0, stage.getY(), 0.0001);
        });
    }

    @Test
    void testResizeRespectsStageMinimumWidthConstraint() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(700);
            stage.setHeight(420);
            stage.setMinWidth(500);

            HBox titleBar = new HBox();
            MouseEvent beginEvent = createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                700,
                100,
                600,
                20,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokeBeginResize(floatingWindow, beginEvent, stage, 2);

            MouseEvent resizeEvent = createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                300,
                100,
                200,
                20,
                titleBar,
                titleBar,
                MouseButton.PRIMARY,
                1
            );
            invokePerformResize(floatingWindow, resizeEvent, stage);

            assertEquals(500.0, stage.getWidth(), 0.0001);
        });
    }

    @Test
    void testResizeCursorIsAppliedToInteractiveContentTargetNearRightEdge() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));

            TextArea textArea = new TextArea();
            BorderPane root = new BorderPane(textArea);
            Stage stage = new Stage();
            stage.setX(100);
            stage.setY(80);
            stage.setWidth(640);
            stage.setHeight(420);
            stage.setScene(new javafx.scene.Scene(root, 640, 420));

            MouseEvent moveEvent = createMouseEvent(
                MouseEvent.MOUSE_MOVED,
                738,
                200,
                638,
                120,
                textArea,
                textArea,
                MouseButton.NONE,
                0
            );
            invokeUpdateResizeCursor(floatingWindow, root, stage, moveEvent);

            assertEquals(Cursor.E_RESIZE, textArea.getCursor());
        });
    }

    @Test
    void testFloatingWindowControlButtonsAreHiddenWhenLocked() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            HBox titleBar = invokeCreateTitleBar(floatingWindow, new Stage());

            var standardControlButtons = titleBar.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> button.getStyleClass().contains(DockThemeStyleClasses.DOCK_WINDOW_CONTROL_BUTTON))
                .filter(button -> !button.getStyleClass().contains(DockThemeStyleClasses.DOCK_WINDOW_PIN_BUTTON))
                .toList();

            assertEquals(3, standardControlButtons.size());
            standardControlButtons.forEach(button -> {
                assertTrue(button.isVisible());
                assertTrue(button.isManaged());
            });

            floatingWindow.getDockGraph().setLocked(true);
            standardControlButtons.forEach(button -> {
                assertFalse(button.isVisible());
                assertFalse(button.isManaged());
                assertTrue(button.isDisable());
            });

            floatingWindow.getDockGraph().setLocked(false);
            standardControlButtons.forEach(button -> {
                assertTrue(button.isVisible());
                assertTrue(button.isManaged());
                assertFalse(button.isDisable());
            });
        });
    }

    @Test
    void testPinButtonCanBeHiddenWhileLockedInAutoMode() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            floatingWindow.setPinButtonMode(DockFloatingPinButtonMode.AUTO);
            floatingWindow.setPinLockedBehavior(DockFloatingPinLockedBehavior.HIDE_BUTTON);

            HBox titleBar = invokeCreateTitleBar(floatingWindow, new Stage());
            Button pinButton = findPinButton(titleBar);
            assertNotNull(pinButton);
            assertTrue(pinButton.isVisible());
            assertTrue(pinButton.isManaged());

            floatingWindow.getDockGraph().setLocked(true);
            assertFalse(pinButton.isVisible());
            assertFalse(pinButton.isManaged());

            floatingWindow.getDockGraph().setLocked(false);
            assertTrue(pinButton.isVisible());
            assertTrue(pinButton.isManaged());
        });
    }

    @Test
    void testPinButtonRemainsVisibleInAlwaysModeWhileLocked() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            floatingWindow.setPinButtonMode(DockFloatingPinButtonMode.ALWAYS);
            floatingWindow.setPinLockedBehavior(DockFloatingPinLockedBehavior.HIDE_BUTTON);

            HBox titleBar = invokeCreateTitleBar(floatingWindow, new Stage());
            Button pinButton = findPinButton(titleBar);
            assertNotNull(pinButton);

            floatingWindow.getDockGraph().setLocked(true);
            assertTrue(pinButton.isVisible());
            assertTrue(pinButton.isManaged());
        });
    }

    @Test
    void testAlwaysOnTopCallbackReceivesApiSource() {
        DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
        AtomicReference<Boolean> state = new AtomicReference<>();
        AtomicReference<DockFloatingPinSource> source = new AtomicReference<>();
        floatingWindow.setOnAlwaysOnTopChanged((alwaysOnTop, eventSource) -> {
            state.set(alwaysOnTop);
            source.set(eventSource);
        });

        floatingWindow.setAlwaysOnTop(false, DockFloatingPinSource.API);

        assertEquals(Boolean.FALSE, state.get());
        assertEquals(DockFloatingPinSource.API, source.get());
        assertFalse(floatingWindow.isAlwaysOnTop());
    }

    @Test
    void testCloseIsIgnoredWhileFloatingWindowIsLocked() {
        DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
        AtomicBoolean closeNotified = new AtomicBoolean(false);
        floatingWindow.setOnWindowClosed(window -> closeNotified.set(true));

        floatingWindow.getDockGraph().setLocked(true);
        floatingWindow.close();
        assertFalse(closeNotified.get());

        floatingWindow.getDockGraph().setLocked(false);
        floatingWindow.close();
        assertTrue(closeNotified.get());
    }

    @Test
    void testSingleNodeFloatingLayoutHidesInnerNodeCloseAndFloatButtons() {
        DockNode node = new DockNode(new Label("Node"), "Node");
        DockFloatingWindow floatingWindow = new DockFloatingWindow(node);

        invokeRebuildLayout(floatingWindow);

        DockNodeView nodeView = floatingWindow.getDockNodeView(node);
        assertNotNull(nodeView);
        assertFalse(nodeView.isCloseButtonVisible());
        assertFalse(nodeView.isFloatButtonVisible());
    }

    @Test
    void testTitleBarContextMenuAlwaysOnTopToggleUsesUserSource() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            floatingWindow.setAlwaysOnTop(false, DockFloatingPinSource.API);
            invokeCreateTitleBar(floatingWindow, new Stage());

            ContextMenu contextMenu = readTitleBarContextMenu(floatingWindow);
            assertNotNull(contextMenu);
            CheckMenuItem alwaysOnTopItem = findAlwaysOnTopMenuItem(contextMenu);
            assertNotNull(alwaysOnTopItem);

            AtomicReference<Boolean> state = new AtomicReference<>();
            AtomicReference<DockFloatingPinSource> source = new AtomicReference<>();
            floatingWindow.setOnAlwaysOnTopChanged((alwaysOnTop, eventSource) -> {
                state.set(alwaysOnTop);
                source.set(eventSource);
            });

            alwaysOnTopItem.setSelected(true);
            alwaysOnTopItem.fire();

            assertTrue(floatingWindow.isAlwaysOnTop());
            assertEquals(Boolean.TRUE, state.get());
            assertEquals(DockFloatingPinSource.USER, source.get());
        });
    }

    @Test
    void testTitleBarContextMenuAlwaysOnTopItemReflectsStateAndLockMode() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            floatingWindow.setAlwaysOnTop(false, DockFloatingPinSource.API);
            invokeCreateTitleBar(floatingWindow, new Stage());

            ContextMenu contextMenu = readTitleBarContextMenu(floatingWindow);
            assertNotNull(contextMenu);
            CheckMenuItem alwaysOnTopItem = findAlwaysOnTopMenuItem(contextMenu);
            assertNotNull(alwaysOnTopItem);

            invokeContextMenuOnShowing(contextMenu);
            assertFalse(alwaysOnTopItem.isSelected());
            assertFalse(alwaysOnTopItem.isDisable());

            floatingWindow.setAlwaysOnTop(true, DockFloatingPinSource.API);
            invokeContextMenuOnShowing(contextMenu);
            assertTrue(alwaysOnTopItem.isSelected());

            floatingWindow.getDockGraph().setLocked(true);
            invokeContextMenuOnShowing(contextMenu);
            assertTrue(alwaysOnTopItem.isDisable());

            floatingWindow.getDockGraph().setLocked(false);
            floatingWindow.setPinToggleEnabled(false);
            invokeContextMenuOnShowing(contextMenu);
            assertTrue(alwaysOnTopItem.isDisable());
        });
    }

    @Test
    void testTitleBarContextMenuContainsAttachAndPinIcons() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            invokeCreateTitleBar(floatingWindow, new Stage());

            ContextMenu contextMenu = readTitleBarContextMenu(floatingWindow);
            assertNotNull(contextMenu);
            MenuItem attachItem = findMenuItem(contextMenu, "Attach to Layout");
            CheckMenuItem alwaysOnTopItem = findAlwaysOnTopMenuItem(contextMenu);

            assertNotNull(attachItem);
            assertNotNull(alwaysOnTopItem);
            assertNotNull(attachItem.getGraphic());
            assertTrue(hasStyleClass(attachItem.getGraphic(), DockThemeStyleClasses.DOCK_CONTROL_ICON_ATTACH));

            invokeContextMenuOnShowing(contextMenu);
            assertNotNull(alwaysOnTopItem.getGraphic());
            assertTrue(hasStyleClass(alwaysOnTopItem.getGraphic(), DockThemeStyleClasses.DOCK_CONTROL_ICON_PIN_ON)
                || hasStyleClass(alwaysOnTopItem.getGraphic(), DockThemeStyleClasses.DOCK_CONTROL_ICON_PIN_OFF));
        });
    }

    @Test
    void testTitleBarContextMenuAttachActionUsesCallback() {
        runOnFxThreadAndWait(() -> {
            DockFloatingWindow floatingWindow = new DockFloatingWindow(new DockNode(new Label("Node"), "Node"));
            AtomicBoolean attachRequested = new AtomicBoolean(false);
            floatingWindow.setOnAttachRequested(() -> attachRequested.set(true));
            invokeCreateTitleBar(floatingWindow, new Stage());

            ContextMenu contextMenu = readTitleBarContextMenu(floatingWindow);
            assertNotNull(contextMenu);
            MenuItem attachItem = findMenuItem(contextMenu, "Attach to Layout");
            assertNotNull(attachItem);
            invokeContextMenuOnShowing(contextMenu);

            attachItem.fire();
            assertTrue(attachRequested.get());
        });
    }

    @Test
    void testSingleNodeFloatingHeaderContextMenuHidesFloatAction() {
        runOnFxThreadAndWait(() -> {
            DockNode node = new DockNode(new Label("Node"), "Node");
            DockFloatingWindow floatingWindow = new DockFloatingWindow(node);
            invokeRebuildLayout(floatingWindow);

            DockNodeView nodeView = floatingWindow.getDockNodeView(node);
            assertNotNull(nodeView);
            ContextMenu headerContextMenu = readHeaderContextMenu(nodeView);
            assertNotNull(headerContextMenu);
            MenuItem floatItem = findMenuItem(headerContextMenu, "Float");
            assertNotNull(floatItem);

            invokeContextMenuOnShowing(headerContextMenu);
            assertFalse(floatItem.isVisible());
        });
    }

    @Test
    void testMultiNodeFloatingHeaderContextMenuShowsFloatAction() {
        runOnFxThreadAndWait(() -> {
            DockNode node1 = new DockNode(new Label("Node1"), "Node 1");
            DockNode node2 = new DockNode(new Label("Node2"), "Node 2");
            DockFloatingWindow floatingWindow = new DockFloatingWindow(node1);
            floatingWindow.dockNode(node2, node1, DockPosition.RIGHT, null);
            invokeRebuildLayout(floatingWindow);

            DockNodeView nodeView = floatingWindow.getDockNodeView(node1);
            assertNotNull(nodeView);
            ContextMenu headerContextMenu = readHeaderContextMenu(nodeView);
            assertNotNull(headerContextMenu);
            MenuItem floatItem = findMenuItem(headerContextMenu, "Float");
            assertNotNull(floatItem);

            invokeContextMenuOnShowing(headerContextMenu);
            assertTrue(floatItem.isVisible());
        });
    }

    @Test
    void testFloatingHeaderContextMenuMoveToSideBarActionUsesCallback() {
        runOnFxThreadAndWait(() -> {
            DockNode node = new DockNode(new Label("Node"), "Node");
            DockFloatingWindow floatingWindow = new DockFloatingWindow(node);
            AtomicReference<DockNode> pinnedNode = new AtomicReference<>();
            AtomicReference<Side> pinnedSide = new AtomicReference<>();
            floatingWindow.setOnNodePinToSideBarRequest((dockNode, side) -> {
                pinnedNode.set(dockNode);
                pinnedSide.set(side);
            });
            invokeRebuildLayout(floatingWindow);

            DockNodeView nodeView = floatingWindow.getDockNodeView(node);
            assertNotNull(nodeView);
            ContextMenu headerContextMenu = readHeaderContextMenu(nodeView);
            assertNotNull(headerContextMenu);
            MenuItem moveLeftItem = findMenuItem(headerContextMenu, "Move to Left Sidebar");
            assertNotNull(moveLeftItem);

            moveLeftItem.fire();

            assertEquals(node, pinnedNode.get());
            assertEquals(Side.LEFT, pinnedSide.get());
        });
    }

    @Test
    void testFloatingWindowAndDockNodeViewRenderIndependentIconViews() {
        runOnFxThreadAndWait(() -> {
            DockNode node = new DockNode(new Label("Node"), "Node");
            Image iconImage = new WritableImage(16, 16);
            node.setIcon(iconImage);

            DockFloatingWindow floatingWindow = new DockFloatingWindow(node);
            invokeCreateTitleBar(floatingWindow, new Stage());
            invokeRebuildLayout(floatingWindow);

            DockNodeView nodeView = floatingWindow.getDockNodeView(node);
            assertNotNull(nodeView);
            StackPane dockNodeIconPane = assertInstanceOf(StackPane.class, nodeView.getHeader().getChildren().getFirst());
            StackPane floatingTitleIconPane = readTitleIconPane(floatingWindow);

            assertFalse(dockNodeIconPane.getChildren().isEmpty());
            assertFalse(floatingTitleIconPane.getChildren().isEmpty());
            assertNotSame(dockNodeIconPane.getChildren().getFirst(), floatingTitleIconPane.getChildren().getFirst());
        });
    }

    @Test
    void testFloatingTitleBarIconFollowsSelectedTab() {
        runOnFxThreadAndWait(() -> {
            DockNode node1 = new DockNode(new Label("Node1"), "Node 1");
            DockNode node2 = new DockNode(new Label("Node2"), "Node 2");
            Image icon1 = new WritableImage(16, 16);
            Image icon2 = new WritableImage(16, 16);
            node1.setIcon(icon1);
            node2.setIcon(icon2);

            DockFloatingWindow floatingWindow = new DockFloatingWindow(node1);
            floatingWindow.dockNode(node2, node1, DockPosition.CENTER, null);
            invokeCreateTitleBar(floatingWindow, new Stage());
            invokeRebuildLayout(floatingWindow);

            DockTabPane tabPane = assertInstanceOf(DockTabPane.class, floatingWindow.getDockGraph().getRoot());
            int initialSelectedIndex = tabPane.getSelectedIndex();
            Image initialExpectedImage = initialSelectedIndex == 0 ? icon1 : icon2;
            StackPane floatingTitleIconPane = readTitleIconPane(floatingWindow);
            ImageView firstIconView = assertInstanceOf(ImageView.class, floatingTitleIconPane.getChildren().getFirst());
            assertEquals(initialExpectedImage, firstIconView.getImage());

            int nextSelectedIndex = initialSelectedIndex == 0 ? 1 : 0;
            tabPane.setSelectedIndex(nextSelectedIndex);
            Image nextExpectedImage = nextSelectedIndex == 0 ? icon1 : icon2;

            ImageView secondIconView = assertInstanceOf(ImageView.class, floatingTitleIconPane.getChildren().getFirst());
            assertEquals(nextExpectedImage, secondIconView.getImage());
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

    private void invokeOnTitleBarMousePressed(DockFloatingWindow floatingWindow, MouseEvent event, Stage stage, HBox titleBar) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod(
                "onTitleBarMousePressed",
                MouseEvent.class,
                Stage.class,
                HBox.class
            );
            method.setAccessible(true);
            method.invoke(floatingWindow, event, stage, titleBar);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke onTitleBarMousePressed", e);
        }
    }

    private void invokeOnSceneMouseDragged(DockFloatingWindow floatingWindow, MouseEvent event, Stage stage, HBox titleBar) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod(
                "onSceneMouseDragged",
                MouseEvent.class,
                Stage.class,
                HBox.class
            );
            method.setAccessible(true);
            method.invoke(floatingWindow, event, stage, titleBar);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke onSceneMouseDragged", e);
        }
    }

    private void invokeOnSceneMouseReleased(DockFloatingWindow floatingWindow, MouseEvent event) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("onSceneMouseReleased", MouseEvent.class);
            method.setAccessible(true);
            method.invoke(floatingWindow, event);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke onSceneMouseReleased", e);
        }
    }

    private void invokeBeginResize(DockFloatingWindow floatingWindow, MouseEvent event, Stage stage, int mask) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("beginResize", MouseEvent.class, Stage.class, int.class);
            method.setAccessible(true);
            method.invoke(floatingWindow, event, stage, mask);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke beginResize", e);
        }
    }

    private void invokePerformResize(DockFloatingWindow floatingWindow, MouseEvent event, Stage stage) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("performResize", MouseEvent.class, Stage.class);
            method.setAccessible(true);
            method.invoke(floatingWindow, event, stage);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke performResize", e);
        }
    }

    private void invokeUpdateResizeCursor(
        DockFloatingWindow floatingWindow,
        BorderPane root,
        Stage stage,
        MouseEvent event
    ) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("updateResizeCursor", BorderPane.class, Stage.class, MouseEvent.class);
            method.setAccessible(true);
            method.invoke(floatingWindow, root, stage, event);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke updateResizeCursor", e);
        }
    }

    private HBox invokeCreateTitleBar(DockFloatingWindow floatingWindow, Stage stage) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("createTitleBar", Stage.class);
            method.setAccessible(true);
            return (HBox) method.invoke(floatingWindow, stage);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke createTitleBar", e);
        }
    }

    private void invokeRebuildLayout(DockFloatingWindow floatingWindow) {
        try {
            Method method = DockFloatingWindow.class.getDeclaredMethod("rebuildLayout");
            method.setAccessible(true);
            method.invoke(floatingWindow);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke rebuildLayout", e);
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

    private Button findPinButton(HBox titleBar) {
        return titleBar.getChildren().stream()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.getStyleClass().contains(DockThemeStyleClasses.DOCK_WINDOW_PIN_BUTTON))
            .findFirst()
            .orElse(null);
    }

    private void writeStage(DockFloatingWindow floatingWindow, Stage stage) {
        try {
            Field field = DockFloatingWindow.class.getDeclaredField("stage");
            field.setAccessible(true);
            field.set(floatingWindow, stage);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to write stage", e);
        }
    }

    private ContextMenu readTitleBarContextMenu(DockFloatingWindow floatingWindow) {
        try {
            Field field = DockFloatingWindow.class.getDeclaredField("titleBarContextMenu");
            field.setAccessible(true);
            return (ContextMenu) field.get(floatingWindow);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read titleBarContextMenu", e);
        }
    }

    private void writeTitleBarContextMenu(DockFloatingWindow floatingWindow, ContextMenu contextMenu) {
        try {
            Field field = DockFloatingWindow.class.getDeclaredField("titleBarContextMenu");
            field.setAccessible(true);
            field.set(floatingWindow, contextMenu);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to write titleBarContextMenu", e);
        }
    }

    private CheckMenuItem findAlwaysOnTopMenuItem(ContextMenu contextMenu) {
        return contextMenu.getItems().stream()
            .filter(CheckMenuItem.class::isInstance)
            .map(CheckMenuItem.class::cast)
            .findFirst()
            .orElse(null);
    }

    private MenuItem findMenuItem(ContextMenu contextMenu, String label) {
        return contextMenu.getItems().stream()
            .filter(item -> label.equals(item.getText()))
            .findFirst()
            .orElse(null);
    }

    private ContextMenu readHeaderContextMenu(DockNodeView nodeView) {
        try {
            Field field = DockNodeView.class.getDeclaredField("headerContextMenu");
            field.setAccessible(true);
            return (ContextMenu) field.get(nodeView);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read header context menu", e);
        }
    }

    private StackPane readTitleIconPane(DockFloatingWindow floatingWindow) {
        try {
            Field field = DockFloatingWindow.class.getDeclaredField("iconPane");
            field.setAccessible(true);
            return (StackPane) field.get(floatingWindow);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read floating title icon pane", e);
        }
    }

    private boolean hasStyleClass(Node node, String styleClass) {
        if (!(node instanceof Region region)) {
            return false;
        }
        return region.getStyleClass().contains(styleClass);
    }

    private void invokeContextMenuOnShowing(ContextMenu contextMenu) {
        if (contextMenu.getOnShowing() != null) {
            contextMenu.getOnShowing().handle(new WindowEvent(new Stage(), WindowEvent.WINDOW_SHOWING));
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

    private static final class TrackingContextMenu extends ContextMenu {
        private boolean hideCalled;

        @Override
        public void hide() {
            hideCalled = true;
        }

        boolean isHideCalled() {
            return hideCalled;
        }
    }
}

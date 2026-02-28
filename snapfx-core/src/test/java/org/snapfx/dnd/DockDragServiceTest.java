package org.snapfx.dnd;

import org.snapfx.model.DockGraph;
import org.snapfx.model.DockNode;
import org.snapfx.model.DockPosition;
import org.snapfx.model.DockSplitPane;
import org.snapfx.model.DockTabPane;
import org.snapfx.view.DockDropZone;
import org.snapfx.view.DockDropZoneType;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockDragServiceTest {

    private DockGraph dockGraph;
    private DockDragService dragService;

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit already started by another test class.
        }
    }

    @BeforeEach
    void setUp() {
        dockGraph = new DockGraph();
        dragService = new DockDragService(dockGraph);
    }

    @Test
    void testZoneInInactiveTabIsRejectedForDrag() {
        DockNode activeNode = new DockNode(new Label("Active"), "Active");
        DockNode hiddenLeft = new DockNode(new Label("HiddenLeft"), "HiddenLeft");
        DockNode hiddenRight = new DockNode(new Label("HiddenRight"), "HiddenRight");
        DockSplitPane hiddenSplit = new DockSplitPane(Orientation.HORIZONTAL);
        hiddenSplit.addChild(hiddenLeft);
        hiddenSplit.addChild(hiddenRight);

        DockTabPane rootTabs = new DockTabPane();
        rootTabs.addChild(activeNode);
        rootTabs.addChild(hiddenSplit);
        rootTabs.setSelectedIndex(0);
        dockGraph.setRoot(rootTabs);

        DockNode draggedNode = new DockNode(new Label("Dragged"), "Dragged");
        DockDropZone hiddenZone = new DockDropZone(
            hiddenSplit,
            DockPosition.CENTER,
            DockDropZoneType.CENTER,
            new BoundingBox(0, 0, 10, 10),
            0,
            null,
            null
        );

        assertFalse(dragService.isZoneValidForDrag(hiddenZone, draggedNode));
        assertFalse(dragService.isElementVisibleForInteraction(hiddenSplit));
        assertTrue(dragService.isElementVisibleForInteraction(activeNode));
    }

    @Test
    void testActivateTabHoverSelectsHoveredTabByTabIndex() {
        DockNode first = new DockNode(new Label("First"), "First");
        DockNode second = new DockNode(new Label("Second"), "Second");
        DockNode third = new DockNode(new Label("Third"), "Third");

        DockTabPane rootTabs = new DockTabPane();
        rootTabs.addChild(first);
        rootTabs.addChild(second);
        rootTabs.addChild(third);
        rootTabs.setSelectedIndex(0);

        DockDropZone tabHeaderZone = new DockDropZone(
            rootTabs,
            DockPosition.CENTER,
            DockDropZoneType.TAB_HEADER,
            new BoundingBox(0, 0, 100, 20),
            0,
            2,
            null
        );

        assertTrue(dragService.activateTabHoverIfNeeded(tabHeaderZone));
        assertEquals(2, rootTabs.getSelectedIndex());
    }

    @Test
    void testActivateTabHoverClampsTabIndexToExistingTabs() {
        DockNode first = new DockNode(new Label("First"), "First");
        DockNode second = new DockNode(new Label("Second"), "Second");

        DockTabPane rootTabs = new DockTabPane();
        rootTabs.addChild(first);
        rootTabs.addChild(second);
        rootTabs.setSelectedIndex(0);

        DockDropZone tabHeaderZone = new DockDropZone(
            rootTabs,
            DockPosition.CENTER,
            DockDropZoneType.TAB_HEADER,
            new BoundingBox(0, 0, 100, 20),
            0,
            99,
            null
        );

        assertTrue(dragService.activateTabHoverIfNeeded(tabHeaderZone));
        assertEquals(1, rootTabs.getSelectedIndex());
    }

    @Test
    void testEmptyRootCenterZoneIsAcceptedForDrag() {
        DockNode draggedNode = new DockNode(new Label("Dragged"), "Dragged");
        DockDropZone emptyRootZone = new DockDropZone(
            null,
            DockPosition.CENTER,
            DockDropZoneType.CENTER,
            new BoundingBox(0, 0, 200, 120),
            0,
            null,
            null
        );

        assertNull(dockGraph.getRoot());
        assertTrue(dragService.isZoneValidForDrag(emptyRootZone, draggedNode));
    }

    @Test
    void testNullTargetZoneIsRejectedWhenMainLayoutIsNotEmpty() {
        DockNode rootNode = new DockNode(new Label("Root"), "Root");
        dockGraph.setRoot(rootNode);
        DockNode draggedNode = new DockNode(new Label("Dragged"), "Dragged");
        DockDropZone nullTargetZone = new DockDropZone(
            null,
            DockPosition.CENTER,
            DockDropZoneType.CENTER,
            new BoundingBox(0, 0, 200, 120),
            0,
            null,
            null
        );

        assertFalse(dragService.isZoneValidForDrag(nullTargetZone, draggedNode));
    }

    @Test
    void testRequestFloatDetachInvokesCallback() {
        DockNode dragged = new DockNode(new Label("Dragged"), "Dragged");
        AtomicReference<DockDragService.FloatDetachRequest> requestRef = new AtomicReference<>();
        dragService.setOnFloatDetachRequest(requestRef::set);

        boolean handled = dragService.requestFloatDetach(dragged, 123.0, 456.0);

        assertTrue(handled);
        assertEquals(dragged, requestRef.get().draggedNode());
        assertEquals(123.0, requestRef.get().screenX(), 0.0001);
        assertEquals(456.0, requestRef.get().screenY(), 0.0001);
    }

    @Test
    void testRequestFloatDetachWithoutHandlerReturnsFalse() {
        DockNode dragged = new DockNode(new Label("Dragged"), "Dragged");
        assertFalse(dragService.requestFloatDetach(dragged, 10.0, 20.0));
    }

    @Test
    void testMainDropSuppressionPredicateIsEvaluated() {
        assertFalse(dragService.shouldSuppressMainDropAt(120.0, 80.0));

        dragService.setSuppressMainDropAtScreenPoint((screenX, screenY) ->
            screenX != null && screenY != null && screenX > 100.0 && screenY > 50.0
        );

        assertTrue(dragService.shouldSuppressMainDropAt(120.0, 80.0));
        assertFalse(dragService.shouldSuppressMainDropAt(40.0, 20.0));

        dragService.setSuppressMainDropAtScreenPoint(null);
        assertFalse(dragService.shouldSuppressMainDropAt(120.0, 80.0));
    }

    @Test
    void testEscapeKeyCancelsActiveDrag() {
        Scene scene = new Scene(new StackPane(), 300, 200);
        DockNode dragged = new DockNode(new Label("Dragged"), "Dragged");

        dragService.startDrag(dragged, createPrimaryPressEvent(scene));
        assertTrue(dragService.isDragging());

        KeyEvent escape = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);
        Event.fireEvent(scene, escape);

        assertFalse(dragService.isDragging());
    }

    @Test
    void testNonEscapeKeyDoesNotCancelActiveDrag() {
        Scene scene = new Scene(new StackPane(), 300, 200);
        DockNode dragged = new DockNode(new Label("Dragged"), "Dragged");

        dragService.startDrag(dragged, createPrimaryPressEvent(scene));
        assertTrue(dragService.isDragging());

        KeyEvent enter = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false);
        Event.fireEvent(scene, enter);

        assertTrue(dragService.isDragging());
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
}

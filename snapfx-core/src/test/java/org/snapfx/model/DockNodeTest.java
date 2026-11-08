package org.snapfx.model;

import javafx.scene.control.Label;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockNodeTest {

    @Test
    void shouldBeFloatableByDefault() {
        DockNode node = new DockNode("node", new Label("Node"), "Node");

        assertTrue(node.isFloatable());
        assertTrue(node.floatableProperty().get());
    }

    @Test
    void shouldAllowFloatingToBeDisabled() {
        DockNode node = new DockNode("node", new Label("Node"), "Node");

        node.setFloatable(false);

        assertFalse(node.isFloatable());
        assertFalse(node.floatableProperty().get());
    }
}

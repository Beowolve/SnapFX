package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.SnapFX;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockSplitPane;
import com.github.beowolve.snapfx.persistence.DockNodeFactory;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoNodeFactoryTest {
    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX is already running.
        }
    }

    @Test
    void testUnknownNodeFallsBackToFrameworkPlaceholderByDefault() {
        DemoNodeFactory factory = new DemoNodeFactory();

        DockNode node = factory.createUnknownNode(
            new DockNodeFactory.UnknownElementContext(
                "DockNode!!!",
                "consolePanel!",
                "dock-6",
                "Console",
                "$.root.children[0].type"
            )
        );

        assertNull(node);
    }

    @Test
    void testUnknownNodeCanUseDemoSpecificFallbackWhenConfigured() {
        DemoNodeFactory factory = new DemoNodeFactory(false);

        DockNode node = factory.createUnknownNode(
            new DockNodeFactory.UnknownElementContext(
                "DockNode!!!",
                "consolePanel!",
                "dock-6",
                "Console",
                "$.root.children[0].type"
            )
        );

        assertNotNull(node);
        assertEquals("consolePanel!", node.getDockNodeId());
        assertEquals("Console", node.getTitle());
        Label content = assertInstanceOf(Label.class, node.getContent());
        assertTrue(content.getText().contains("DockNode!!!"));
    }

    @Test
    void testSnapFxLoadWithDefaultDemoFactoryRecoversUnsupportedNodeType() {
        String json = """
            {
              "mainLayout": {
                "locked": false,
                "layoutIdCounter": 9,
                "root": {
                  "id": "split-root",
                  "type": "DockSplitPane",
                  "orientation": "VERTICAL",
                  "children": [
                    {
                      "id": "dock-6",
                      "dockNodeId": "consolePanel!",
                      "type": "DockNode!!!",
                      "title": "Console",
                      "closeable": true
                    },
                    {
                      "id": "dock-5",
                      "dockNodeId": "tasks",
                      "type": "DockNode",
                      "title": "Tasks",
                      "closeable": true
                    }
                  ]
                }
              },
              "floatingWindows": []
            }
            """;
        SnapFX snapFX = new SnapFX();
        snapFX.setNodeFactory(new DemoNodeFactory());

        assertDoesNotThrow(() -> snapFX.loadLayout(json));

        DockSplitPane root = assertInstanceOf(DockSplitPane.class, snapFX.getDockGraph().getRoot());
        DockNode fallbackNode = assertInstanceOf(DockNode.class, root.getChildren().getFirst());
        Label content = assertInstanceOf(Label.class, fallbackNode.getContent());
        assertTrue(content.getText().contains("DockNode!!!"));
    }
}

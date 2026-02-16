package com.github.beowolve.snapfx.persistence;

import com.github.beowolve.snapfx.model.*;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for serialization and deserialization.
 */
class DockLayoutSerializerTest {
    private DockGraph dockGraph;
    private DockLayoutSerializer serializer;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX for headless tests without Swing
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX is already running
        }
    }

    @BeforeEach
    void setUp() {
        dockGraph = new DockGraph();
        serializer = new DockLayoutSerializer(dockGraph);
    }

    @Test
    void testSerializeEmptyGraph() {
        String json = serializer.serialize();
        assertNotNull(json);
        assertEquals("{}", json);
    }

    @Test
    void testSerializeSingleNode() {
        DockNode node = new DockNode(new Label("Test"), "Test Node");
        dockGraph.setRoot(node);

        String json = serializer.serialize();
        assertNotNull(json);
        assertTrue(json.contains("Test Node"));
        assertTrue(json.contains("DockNode"));
    }

    @Test
    void testSerializeTabPane() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.CENTER);

        String json = serializer.serialize();
        assertNotNull(json);
        assertTrue(json.contains("DockTabPane"));
        assertTrue(json.contains("Node 1"));
        assertTrue(json.contains("Node 2"));
    }

    @Test
    void testSerializeSplitPane() {
        DockNode node1 = new DockNode(new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode(new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.LEFT);

        String json = serializer.serialize();
        assertNotNull(json);
        assertTrue(json.contains("DockSplitPane"));
        assertTrue(json.contains("HORIZONTAL"));
    }

    @Test
    void testDeserializeEmptyGraph() throws DockLayoutLoadException {
        serializer.deserialize("{}");
        assertNull(dockGraph.getRoot());
    }

    @Test
    void testRoundTripSingleNode() throws DockLayoutLoadException {
        DockNode node = new DockNode("testNode1", new Label("Test"), "Test Node");
        dockGraph.setRoot(node);

        String json = serializer.serialize();

        // New graph with factory
        DockGraph newGraph = new DockGraph();
        DockLayoutSerializer newSerializer = new DockLayoutSerializer(newGraph);

        // Setup factory to recreate node
        newSerializer.setNodeFactory(nodeId -> {
            if ("testNode1".equals(nodeId)) {
                return new DockNode(nodeId, new Label("Test"), "Test Node");
            }
            return null;
        });

        newSerializer.deserialize(json);

        assertNotNull(newGraph.getRoot());
        assertInstanceOf(DockNode.class, newGraph.getRoot());
        DockNode deserializedNode = (DockNode) newGraph.getRoot();
        assertEquals("Test Node", deserializedNode.getTitle());
        assertEquals("testNode1", deserializedNode.getDockNodeId());
    }

    @Test
    void testRoundTripComplexStructure() throws DockLayoutLoadException {
        DockNode node1 = new DockNode("node1", new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Test2"), "Node 2");
        DockNode node3 = new DockNode("node3", new Label("Test3"), "Node 3");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);
        dockGraph.dock(node3, node2, DockPosition.CENTER);

        String json = serializer.serialize();

        // New graph with factory
        DockGraph newGraph = new DockGraph();
        DockLayoutSerializer newSerializer = new DockLayoutSerializer(newGraph);

        newSerializer.setNodeFactory(nodeId -> switch (nodeId) {
            case "node1" -> new DockNode(nodeId, new Label("Test1"), "Node 1");
            case "node2" -> new DockNode(nodeId, new Label("Test2"), "Node 2");
            case "node3" -> new DockNode(nodeId, new Label("Test3"), "Node 3");
            default -> null;
        });

        newSerializer.deserialize(json);

        assertNotNull(newGraph.getRoot());
        assertInstanceOf(DockSplitPane.class, newGraph.getRoot());

        DockSplitPane splitPane = (DockSplitPane) newGraph.getRoot();
        assertEquals(2, splitPane.getChildren().size());

        assertInstanceOf(DockTabPane.class, splitPane.getChildren().get(1));
    }

    @Test
    void testSerializeLockedState() throws DockLayoutLoadException {
        DockNode node = new DockNode(new Label("Test"), "Test Node");
        dockGraph.setRoot(node);
        dockGraph.setLocked(true);

        String json = serializer.serialize();
        assertTrue(json.contains("\"locked\": true"));

        dockGraph.setLocked(false);
        DockLayoutSerializer newSerializer = new DockLayoutSerializer(dockGraph);
        newSerializer.registerNode(node);
        newSerializer.deserialize(json);

        assertTrue(dockGraph.isLocked());
    }

    /**
     * Test for locked state round-trip.
     * Regression test: Ensures locked state is correctly serialized and deserialized.
     * Bug: UI locked property was not synchronized after loading layout (2026-02-10).
     * Fix: Application must sync UI property with DockGraph.isLocked() after load.
     */
    @Test
    void testLockedStateRoundTrip() throws DockLayoutLoadException {
        // Setup: Create layout with locked=true
        DockNode node1 = new DockNode("node1", new Label("Test1"), "Node 1");
        DockNode node2 = new DockNode("node2", new Label("Test2"), "Node 2");

        dockGraph.dock(node1, null, DockPosition.CENTER);
        dockGraph.dock(node2, node1, DockPosition.RIGHT);
        dockGraph.setLocked(true);

        // Serialize
        String json = serializer.serialize();
        assertTrue(json.contains("\"locked\": true"), "Locked state should be serialized");

        // New graph with locked=false
        DockGraph newGraph = new DockGraph();
        assertFalse(newGraph.isLocked(), "New graph should start unlocked");

        // Deserialize with factory
        DockLayoutSerializer newSerializer = new DockLayoutSerializer(newGraph);
        newSerializer.setNodeFactory(nodeId -> switch (nodeId) {
            case "node1" -> new DockNode(nodeId, new Label("Test1"), "Node 1");
            case "node2" -> new DockNode(nodeId, new Label("Test2"), "Node 2");
            default -> null;
        });
        newSerializer.deserialize(json);

        // Verify: Locked state should be restored
        assertTrue(newGraph.isLocked(), "Locked state should be restored from JSON");

        // Round-trip back
        String json2 = newSerializer.serialize();
        assertTrue(json2.contains("\"locked\": true"), "Locked state should persist in second serialization");

        // Test unlocked state as well
        newGraph.setLocked(false);
        String json3 = newSerializer.serialize();
        assertTrue(json3.contains("\"locked\": false"), "Unlocked state should be serialized");

        // Deserialize unlocked state
        DockGraph newGraph2 = new DockGraph();
        newGraph2.setLocked(true); // Start with opposite state
        DockLayoutSerializer newSerializer2 = new DockLayoutSerializer(newGraph2);
        newSerializer2.setNodeFactory(nodeId -> switch (nodeId) {
            case "node1" -> new DockNode(nodeId, new Label("Test1"), "Node 1");
            case "node2" -> new DockNode(nodeId, new Label("Test2"), "Node 2");
            default -> null;
        });
        newSerializer2.deserialize(json3);

        assertFalse(newGraph2.isLocked(), "Unlocked state should be restored from JSON");
    }

    @Test
    void testDeserializeRejectsBlankContent() {
        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> serializer.deserialize("   ")
        );
        assertTrue(exception.getMessage().contains("empty"));
        assertEquals("$", exception.getLocation());
    }

    @Test
    void testDeserializeRejectsInvalidJsonSyntax() {
        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> serializer.deserialize("{ invalid")
        );
        assertTrue(exception.getMessage().contains("Invalid JSON syntax"));
        assertEquals("$", exception.getLocation());
    }

    @Test
    void testDeserializeRejectsMissingElementType() {
        String json = """
            {
              "locked": false,
              "layoutIdCounter": 4,
              "root": {
                "id": "layout-root-1",
                "title": "Root"
              }
            }
            """;
        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> serializer.deserialize(json)
        );
        assertEquals("$.root.type", exception.getLocation());
        assertTrue(exception.getMessage().contains("Missing required field"));
    }

    @Test
    void testDeserializeRejectsOutOfRangeTabSelection() {
        String json = """
            {
              "locked": false,
              "layoutIdCounter": 7,
              "root": {
                "id": "layout-tab-root",
                "type": "DockTabPane",
                "selectedIndex": 3,
                "children": [
                  {
                    "id": "layout-node-1",
                    "dockNodeId": "node1",
                    "type": "DockNode",
                    "title": "One",
                    "closeable": true
                  }
                ]
              }
            }
            """;
        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> serializer.deserialize(json)
        );
        assertEquals("$.root.selectedIndex", exception.getLocation());
        assertTrue(exception.getMessage().contains("out of range"));
    }

    @Test
    void testDeserializeUnknownFactoryNodeUsesPlaceholderWithNodeIdHint() throws DockLayoutLoadException {
        String json = """
            {
              "locked": false,
              "layoutIdCounter": 4,
              "root": {
                "id": "layout-node-1",
                "dockNodeId": "unknownEditorType",
                "type": "DockNode",
                "title": "Recovered Editor",
                "closeable": true
              }
            }
            """;

        DockGraph graph = new DockGraph();
        DockLayoutSerializer serializerWithFactory = new DockLayoutSerializer(graph);
        serializerWithFactory.setNodeFactory(nodeId -> null);

        serializerWithFactory.deserialize(json);

        DockNode placeholderNode = assertInstanceOf(DockNode.class, graph.getRoot());
        assertEquals("Recovered Editor", placeholderNode.getTitle());
        Label placeholderContent = assertInstanceOf(Label.class, placeholderNode.getContent());
        assertTrue(placeholderContent.getText().contains("unknownEditorType"));
    }

    @Test
    void testDeserializeUnknownDockNodeElementTypeFallsBackToPlaceholder() throws DockLayoutLoadException {
        String json = """
            {
              "locked": false,
              "layoutIdCounter": 4,
              "root": {
                "id": "layout-node-1",
                "dockNodeId": "consolePanel!",
                "type": "DockNode!!!",
                "title": "Console",
                "closeable": true
              }
            }
            """;

        serializer.setNodeFactory(nodeId -> null);

        serializer.deserialize(json);

        DockNode placeholderNode = assertInstanceOf(DockNode.class, dockGraph.getRoot());
        assertEquals("Console", placeholderNode.getTitle());
        Label placeholderContent = assertInstanceOf(Label.class, placeholderNode.getContent());
        assertTrue(placeholderContent.getText().contains("DockNode!!!"));
    }

    @Test
    void testDeserializeUnknownDockNodeElementTypeCanUseFactoryCustomFallback() throws DockLayoutLoadException {
        String json = """
            {
              "locked": false,
              "layoutIdCounter": 4,
              "root": {
                "id": "layout-node-1",
                "dockNodeId": "consolePanel!",
                "type": "DockNode!!!",
                "title": "Console",
                "closeable": true
              }
            }
            """;
        serializer.setNodeFactory(new DockNodeFactory() {
            @Override
            public DockNode createNode(String nodeId) {
                return null;
            }

            @Override
            public DockNode createUnknownNode(UnknownElementContext context) {
                Label content = new Label("Custom unknown node fallback");
                return new DockNode("customUnknown", content, "Custom");
            }
        });

        serializer.deserialize(json);

        DockNode restoredNode = assertInstanceOf(DockNode.class, dockGraph.getRoot());
        assertEquals("Console", restoredNode.getTitle());
        Label content = assertInstanceOf(Label.class, restoredNode.getContent());
        assertEquals("Custom unknown node fallback", content.getText());
        assertEquals("customUnknown", restoredNode.getDockNodeId());
    }
}

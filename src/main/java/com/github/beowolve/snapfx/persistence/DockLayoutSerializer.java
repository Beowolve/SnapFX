package com.github.beowolve.snapfx.persistence;

import com.github.beowolve.snapfx.model.*;
import com.google.gson.*;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializes and deserializes DockGraph structures to/from JSON.
 * Stores the full tree structure including positions and split percentages.
 */
public class DockLayoutSerializer {
    private final DockGraph dockGraph;
    private final Gson gson;
    private final Map<String, DockNode> nodeRegistry;
    private DockNodeFactory nodeFactory;

    public DockLayoutSerializer(DockGraph dockGraph) {
        this.dockGraph = dockGraph;
        this.nodeRegistry = new HashMap<>();

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeAdapter(DockElement.class, new DockElementAdapter());
        builder.registerTypeAdapter(Orientation.class, new OrientationAdapter());
        this.gson = builder.create();
    }

    /**
     * Sets the factory used to create DockNodes during deserialization.
     * This is the recommended way to restore nodes across sessions.
     *
     * @param factory Factory that creates nodes from their IDs
     */
    public void setNodeFactory(DockNodeFactory factory) {
        this.nodeFactory = factory;
    }

    /**
     * Registers a DockNode for serialization.
     * Note: With a DockNodeFactory set, registration is not strictly required,
     * as the factory will recreate nodes during deserialization.
     */
    public void registerNode(DockNode node) {
        nodeRegistry.put(node.getId(), node);
    }

    /**
     * Serializes the DockGraph to JSON.
     */
    public String serialize() {
        DockElement root = dockGraph.getRoot();
        if (root == null) {
            return "{}";
        }

        // Collect all DockNodes in the tree
        collectNodes(root);

        LayoutData data = new LayoutData();
        data.locked = dockGraph.isLocked();
        data.layoutIdCounter = dockGraph.getLayoutIdCounter();
        data.root = serializeElement(root);

        return gson.toJson(data);
    }

    private void collectNodes(DockElement element) {
        if (element instanceof DockNode node) {
            registerNode(node);
        } else if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                collectNodes(child);
            }
        }
    }

    private ElementData serializeElement(DockElement element) {
        ElementData data = new ElementData();
        data.id = element.getId(); // layoutId
        data.type = element.getClass().getSimpleName();

        if (element instanceof DockNode node) {
            data.dockNodeId = node.getDockNodeId(); // Type-based ID for factory
            data.title = node.getTitle();
            data.closeable = node.isCloseable();

            // Check if content implements DockNodeContentSerializer
            if (node.getContent() instanceof DockNodeContentSerializer serializer) {
                data.contentData = serializer.serializeContent();
            }
        } else if (element instanceof DockSplitPane splitPane) {
            data.orientation = splitPane.getOrientation().toString();
            data.children = new ElementData[splitPane.getChildren().size()];

            for (int i = 0; i < splitPane.getChildren().size(); i++) {
                data.children[i] = serializeElement(splitPane.getChildren().get(i));
            }

            // Divider Positionen
            data.dividerPositions = new double[splitPane.getDividerPositions().size()];
            for (int i = 0; i < splitPane.getDividerPositions().size(); i++) {
                data.dividerPositions[i] = splitPane.getDividerPositions().get(i).get();
            }
        } else if (element instanceof DockTabPane tabPane) {
            data.selectedIndex = tabPane.getSelectedIndex();
            data.children = new ElementData[tabPane.getChildren().size()];

            for (int i = 0; i < tabPane.getChildren().size(); i++) {
                data.children[i] = serializeElement(tabPane.getChildren().get(i));
            }
        }

        return data;
    }

    /**
     * Deserializes JSON into a DockGraph.
     */
    public void deserialize(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("{}")) {
            dockGraph.setRoot(null);
            return;
        }

        try {
            LayoutData data = gson.fromJson(json, LayoutData.class);
            dockGraph.setLocked(data.locked);

            // Restore layout ID counter to continue from where we left off
            if (data.layoutIdCounter > 0) {
                dockGraph.setLayoutIdCounter(data.layoutIdCounter);
            }

            if (data.root != null) {
                DockElement root = deserializeElement(data.root);
                dockGraph.setRoot(root);
            }
        } catch (Exception e) {
            System.err.println("Error while deserializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DockElement deserializeElement(ElementData data) {
        return switch (data.type) {
            case "DockNode" -> deserializeDockNode(data);
            case "DockSplitPane" -> deserializeSplitPane(data);
            case "DockTabPane" -> deserializeTabPane(data);
            default -> null;
        };
    }

    private DockNode deserializeDockNode(ElementData data) {
        // Try factory first (recommended for cross-session persistence)
        if (nodeFactory != null && data.dockNodeId != null) {
            // Use dockNodeId for factory (type-based)
            DockNode node = nodeFactory.createNode(data.dockNodeId);
            if (node != null) {
                // Restore layoutId (unique instance ID)
                if (data.id != null) {
                    node.setLayoutId(data.id);
                }

                // Update properties from saved data
                node.setTitle(data.title);
                node.setCloseable(data.closeable);

                // Restore content data if available
                if (data.contentData != null && node.getContent() instanceof DockNodeContentSerializer serializer) {
                    serializer.deserializeContent(data.contentData);
                }

                return node;
            }
        }

        // Fallback: look up in registry (for backward compatibility)
        DockNode node = nodeRegistry.get(data.id);
        if (node != null) {
            node.setTitle(data.title);
            node.setCloseable(data.closeable);
            return node;
        }

        // Last resort: create placeholder node
        // This happens when neither factory nor registry provides the node
        Label placeholder = new Label("Node: " + data.title);
        node = new DockNode(data.id, placeholder, data.title);
        node.setCloseable(data.closeable);


        return node;
    }

    private DockSplitPane deserializeSplitPane(ElementData data) {
        Orientation orientation = Orientation.valueOf(data.orientation);
        DockSplitPane splitPane = new DockSplitPane(orientation);

        if (data.children != null) {
            for (ElementData childData : data.children) {
                DockElement child = deserializeElement(childData);
                if (child != null) {
                    splitPane.addChild(child);
                }
            }
        }

        // Apply divider positions
        if (data.dividerPositions != null) {
            for (int i = 0; i < data.dividerPositions.length; i++) {
                splitPane.setDividerPosition(i, data.dividerPositions[i]);
            }
        }

        return splitPane;
    }

    private DockTabPane deserializeTabPane(ElementData data) {
        DockTabPane tabPane = new DockTabPane();

        if (data.children != null) {
            for (ElementData childData : data.children) {
                DockElement child = deserializeElement(childData);
                if (child != null) {
                    tabPane.addChild(child);
                }
            }
        }

        tabPane.setSelectedIndex(data.selectedIndex);
        return tabPane;
    }

    // Data classes for JSON
    private static class LayoutData {
        boolean locked;
        long layoutIdCounter; // Counter for unique layout IDs
        ElementData root;
    }

    private static class ElementData {
        String id; // layoutId (unique instance ID)
        String dockNodeId; // Type-based ID for factory
        String type;
        String title;
        boolean closeable;
        String orientation;
        int selectedIndex;
        ElementData[] children;
        double[] dividerPositions;
        JsonObject contentData; // For serializable content
    }

    // Gson adapters
    private static class DockElementAdapter implements JsonSerializer<DockElement>, JsonDeserializer<DockElement> {
        @Override
        public JsonElement serialize(DockElement src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src, src.getClass());
        }

        @Override
        public DockElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();

            return switch (type) {
                case "DockNode" -> context.deserialize(json, DockNode.class);
                case "DockSplitPane" -> context.deserialize(json, DockSplitPane.class);
                case "DockTabPane" -> context.deserialize(json, DockTabPane.class);
                default -> null;
            };
        }
    }

    private static class OrientationAdapter implements JsonSerializer<Orientation>, JsonDeserializer<Orientation> {
        @Override
        public JsonElement serialize(Orientation src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Orientation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Orientation.valueOf(json.getAsString());
        }
    }
}

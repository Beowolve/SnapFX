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

        switch (element) {
            case DockNode node -> {
                data.dockNodeId = node.getDockNodeId(); // Type-based ID for factory

                data.title = node.getTitle();
                data.closeable = node.isCloseable();

                // Check if content implements DockNodeContentSerializer
                if (node.getContent() instanceof DockNodeContentSerializer serializer) {
                    data.contentData = serializer.serializeContent();
                }
            }
            case DockSplitPane splitPane -> {
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
            }
            case DockTabPane tabPane -> {
                data.selectedIndex = tabPane.getSelectedIndex();
                data.children = new ElementData[tabPane.getChildren().size()];

                for (int i = 0; i < tabPane.getChildren().size(); i++) {
                    data.children[i] = serializeElement(tabPane.getChildren().get(i));
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + element);
        }

        return data;
    }

    /**
     * Deserializes JSON into a DockGraph.
     *
     * @throws DockLayoutLoadException if the JSON is invalid or cannot be mapped to a valid layout
     */
    public void deserialize(String json) throws DockLayoutLoadException {
        if (json == null || json.isBlank()) {
            throw loadError("Layout content is empty.", "$");
        }

        String normalizedJson = json.trim();
        JsonObject rootObject = parseRootJsonObject(normalizedJson);
        if (rootObject.isEmpty()) {
            dockGraph.setRoot(null);
            return;
        }
        if (!rootObject.has("root")) {
            throw missingFieldError("$.root");
        }

        LayoutData data = parseLayoutData(rootObject);
        DockElement root = data.root == null ? null : deserializeElement(data.root, "$.root");

        dockGraph.setLocked(data.locked);
        if (data.layoutIdCounter > 0) {
            dockGraph.setLayoutIdCounter(data.layoutIdCounter);
        }
        dockGraph.setRoot(root);
    }

    private JsonObject parseRootJsonObject(String json) throws DockLayoutLoadException {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                throw loadError("Layout must be a JSON object.", "$");
            }
            return parsed.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw loadError("Invalid JSON syntax: " + e.getMessage(), "$", e);
        }
    }

    private LayoutData parseLayoutData(JsonObject rootObject) throws DockLayoutLoadException {
        try {
            LayoutData data = gson.fromJson(rootObject, LayoutData.class);
            if (data == null) {
                throw loadError("Layout JSON is empty or malformed.", "$");
            }
            return data;
        } catch (JsonParseException e) {
            throw loadError("Layout JSON could not be parsed.", "$", e);
        }
    }

    private DockElement deserializeElement(ElementData data, String path) throws DockLayoutLoadException {
        if (data == null) {
            throw loadError("Layout element is missing.", path);
        }
        if (isBlank(data.type)) {
            throw missingFieldError(path + ".type");
        }
        return switch (data.type) {
            case "DockNode" -> deserializeDockNode(data, path);
            case "DockSplitPane" -> deserializeSplitPane(data, path);
            case "DockTabPane" -> deserializeTabPane(data, path);
            default -> throw loadError("Unsupported element type '" + data.type + "'.", path + ".type");
        };
    }

    private DockNode deserializeDockNode(ElementData data, String path) throws DockLayoutLoadException {
        if (isBlank(data.id)) {
            throw missingFieldError(path + ".id");
        }
        if (isBlank(data.title)) {
            throw missingFieldError(path + ".title");
        }

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
                    try {
                        serializer.deserializeContent(data.contentData);
                    } catch (RuntimeException e) {
                        throw loadError("DockNode content could not be deserialized: " + e.getMessage(),
                            path + ".contentData", e);
                    }
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

    private DockSplitPane deserializeSplitPane(ElementData data, String path) throws DockLayoutLoadException {
        if (isBlank(data.orientation)) {
            throw missingFieldError(path + ".orientation");
        }

        Orientation orientation;
        try {
            orientation = Orientation.valueOf(data.orientation);
        } catch (IllegalArgumentException e) {
            throw loadError("Unsupported split orientation '" + data.orientation + "'.",
                path + ".orientation", e);
        }

        if (data.children == null || data.children.length == 0) {
            throw loadError("Split pane must define at least one child.", path + ".children");
        }

        DockSplitPane splitPane = new DockSplitPane(orientation);

        for (int i = 0; i < data.children.length; i++) {
            DockElement child = deserializeElement(data.children[i], path + ".children[" + i + "]");
            if (child != null) {
                splitPane.addChild(child);
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

    private DockTabPane deserializeTabPane(ElementData data, String path) throws DockLayoutLoadException {
        if (data.children == null || data.children.length == 0) {
            throw loadError("Tab pane must define at least one child.", path + ".children");
        }

        DockTabPane tabPane = new DockTabPane();

        for (int i = 0; i < data.children.length; i++) {
            DockElement child = deserializeElement(data.children[i], path + ".children[" + i + "]");
            if (child != null) {
                tabPane.addChild(child);
            }
        }

        if (data.selectedIndex < 0 || data.selectedIndex >= tabPane.getChildren().size()) {
            throw loadError(
                "Selected tab index " + data.selectedIndex + " is out of range for " + tabPane.getChildren().size() + " tab(s).",
                path + ".selectedIndex"
            );
        }
        tabPane.setSelectedIndex(data.selectedIndex);
        return tabPane;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private DockLayoutLoadException missingFieldError(String path) {
        return loadError("Missing required field.", path);
    }

    private DockLayoutLoadException loadError(String message, String path) {
        return new DockLayoutLoadException(message, path);
    }

    private DockLayoutLoadException loadError(String message, String path, Throwable cause) {
        return new DockLayoutLoadException(message, path, cause);
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

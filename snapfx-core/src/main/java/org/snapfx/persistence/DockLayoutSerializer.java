package org.snapfx.persistence;

import org.snapfx.model.*;
import com.google.gson.*;
import javafx.geometry.Side;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes and deserializes DockGraph structures to/from JSON.
 * Stores the full tree structure including positions and split percentages.
 */
public class DockLayoutSerializer {
    public static final String DOCK_NODE = "DockNode";
    public static final String DOCK_SPLIT_PANE = "DockSplitPane";
    public static final String DOCK_TAB_PANE = "DockTabPane";

    public static final String TYPE_JSON_SUFFIX = ".type";
    public static final String ID_JSON_SUFFIX = ".id";
    public static final String TITLE_JSON_SUFFIX = ".title";

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
        boolean hasSideBars = hasSerializedSideBarState();
        if (root == null && !hasSideBars) {
            return "{}";
        }

        // Collect all DockNodes in the tree
        collectNodes(root);
        collectSideBarNodes();

        LayoutData data = new LayoutData();
        data.locked = dockGraph.isLocked();
        data.layoutIdCounter = dockGraph.getLayoutIdCounter();
        data.root = root == null ? null : serializeElement(root);
        data.sideBars = serializeSideBars();

        return gson.toJson(data);
    }

    private void collectNodes(DockElement element) {
        if (element == null) {
            return;
        }
        if (element instanceof DockNode node) {
            registerNode(node);
        } else if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                collectNodes(child);
            }
        }
    }

    private void collectSideBarNodes() {
        for (Side side : Side.values()) {
            for (DockNode node : dockGraph.getSideBarNodes(side)) {
                registerNode(node);
            }
        }
    }

    private boolean hasSerializedSideBarState() {
        for (Side side : List.of(Side.LEFT, Side.RIGHT)) {
            if (dockGraph.isSideBarPinnedOpen(side)
                || !dockGraph.getSideBarNodes(side).isEmpty()
                || Double.compare(dockGraph.getSideBarPanelWidth(side), DockGraph.DEFAULT_SIDE_BAR_PANEL_WIDTH) != 0) {
                return true;
            }
        }
        return false;
    }

    private SideBarData[] serializeSideBars() {
        List<SideBarData> sideBars = new ArrayList<>();
        for (Side side : List.of(Side.LEFT, Side.RIGHT)) {
            var entries = dockGraph.getSideBarNodes(side);
            boolean pinnedOpen = dockGraph.isSideBarPinnedOpen(side);
            double panelWidth = dockGraph.getSideBarPanelWidth(side);
            boolean hasCustomPanelWidth = Double.compare(panelWidth, DockGraph.DEFAULT_SIDE_BAR_PANEL_WIDTH) != 0;
            if (entries.isEmpty() && !pinnedOpen && !hasCustomPanelWidth) {
                continue;
            }

            SideBarData sideBarData = new SideBarData();
            sideBarData.side = side.name();
            sideBarData.pinnedOpen = pinnedOpen;
            sideBarData.panelWidth = hasCustomPanelWidth ? panelWidth : null;
            sideBarData.entries = new SideBarEntryData[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                DockNode node = entries.get(i);
                SideBarEntryData entryData = new SideBarEntryData();
                entryData.node = serializeElement(node);
                DockElement restoreTarget = node.getLastKnownTarget();
                entryData.restoreTargetId = restoreTarget == null ? null : restoreTarget.getId();
                DockPosition restorePosition = node.getLastKnownPosition();
                entryData.restorePosition = restorePosition == null ? null : restorePosition.name();
                entryData.restoreTabIndex = restorePosition == DockPosition.CENTER ? node.getLastKnownTabIndex() : null;
                sideBarData.entries[i] = entryData;
            }
            sideBars.add(sideBarData);
        }
        return sideBars.isEmpty() ? null : sideBars.toArray(SideBarData[]::new);
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
            boolean previouslyLocked = dockGraph.isLocked();
            dockGraph.setLocked(false);
            dockGraph.setRoot(null);
            dockGraph.clearSideBars();
            dockGraph.setLocked(previouslyLocked);
            return;
        }

        LayoutData data = parseLayoutData(rootObject);
        if (!rootObject.has("root") && (data.sideBars == null || data.sideBars.length == 0)) {
            throw missingFieldError("$.root");
        }
        DockElement root = data.root == null ? null : deserializeElement(data.root, "$.root");
        List<DeserializedSideBar> sideBars = deserializeSideBars(data.sideBars, root);

        dockGraph.setLocked(false);
        dockGraph.setRoot(root);
        dockGraph.clearSideBars();
        applyDeserializedSideBars(sideBars);
        if (data.layoutIdCounter > 0) {
            dockGraph.setLayoutIdCounter(data.layoutIdCounter);
        }
        dockGraph.setLocked(data.locked);
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

    private List<DeserializedSideBar> deserializeSideBars(SideBarData[] sideBars, DockElement root)
        throws DockLayoutLoadException {
        List<DeserializedSideBar> result = new ArrayList<>();
        if (sideBars == null || sideBars.length == 0) {
            return result;
        }

        for (int i = 0; i < sideBars.length; i++) {
            SideBarData sideBarData = sideBars[i];
            String sideBarPath = "$.sideBars[" + i + "]";
            if (sideBarData == null) {
                throw loadError("Sidebar entry is missing.", sideBarPath);
            }

            Side side = parseSideBarSide(sideBarData.side, sideBarPath + ".side");
            List<DeserializedSideBarEntry> entries = new ArrayList<>();
            if (sideBarData.entries != null) {
                for (int j = 0; j < sideBarData.entries.length; j++) {
                    SideBarEntryData entryData = sideBarData.entries[j];
                    String entryPath = sideBarPath + ".entries[" + j + "]";
                    if (entryData == null || entryData.node == null) {
                        throw loadError("Sidebar node entry is missing.", entryPath + ".node");
                    }

                    DockElement element = deserializeElement(entryData.node, entryPath + ".node");
                    if (!(element instanceof DockNode node)) {
                        throw loadError("Sidebar entries must be DockNode elements.", entryPath + ".node" + TYPE_JSON_SUFFIX);
                    }

                    DockPosition restorePosition = parseOptionalDockPosition(
                        entryData.restorePosition,
                        entryPath + ".restorePosition"
                    );
                    Integer restoreTabIndex = restorePosition == DockPosition.CENTER ? entryData.restoreTabIndex : null;
                    DockElement restoreTarget = resolveElementById(root, entryData.restoreTargetId);
                    entries.add(new DeserializedSideBarEntry(node, restoreTarget, restorePosition, restoreTabIndex));
                }
            }
            Double panelWidth = sideBarData.panelWidth;
            if (panelWidth != null && (!Double.isFinite(panelWidth) || panelWidth <= 0.0)) {
                panelWidth = null;
            }
            result.add(new DeserializedSideBar(side, sideBarData.pinnedOpen, panelWidth, entries));
        }

        return result;
    }

    private void applyDeserializedSideBars(List<DeserializedSideBar> sideBars) {
        if (sideBars == null || sideBars.isEmpty()) {
            return;
        }

        for (DeserializedSideBar sideBar : sideBars) {
            if (sideBar == null || sideBar.side() == null) {
                continue;
            }
            for (DeserializedSideBarEntry entry : sideBar.entries()) {
                if (entry == null || entry.node() == null) {
                    continue;
                }
                dockGraph.pinToSideBar(entry.node(), sideBar.side());
                entry.node().setLastKnownTarget(entry.restoreTarget());
                entry.node().setLastKnownPosition(entry.restorePosition());
                entry.node().setLastKnownTabIndex(
                    entry.restorePosition() == DockPosition.CENTER ? entry.restoreTabIndex() : null
                );
            }
            if (sideBar.panelWidth() != null) {
                dockGraph.setSideBarPanelWidth(sideBar.side(), sideBar.panelWidth());
            }
            dockGraph.setSideBarPinnedOpen(sideBar.side(), sideBar.pinnedOpen());
        }
    }

    private DockElement deserializeElement(ElementData data, String path) throws DockLayoutLoadException {
        if (data == null) {
            throw loadError("Layout element is missing.", path);
        }
        if (isBlank(data.type)) {
            throw missingFieldError(path + TYPE_JSON_SUFFIX);
        }
        return switch (data.type) {
            case DOCK_NODE -> deserializeDockNode(data, path);
            case DOCK_SPLIT_PANE -> deserializeSplitPane(data, path);
            case DOCK_TAB_PANE -> deserializeTabPane(data, path);
            default -> deserializeUnknownElement(data, path);
        };
    }

    private DockNode deserializeDockNode(ElementData data, String path) throws DockLayoutLoadException {
        return deserializeDockNode(data, path, null);
    }

    private DockNode deserializeUnknownElement(ElementData data, String path) throws DockLayoutLoadException {
        if (data.children != null && data.children.length > 0) {
            throw loadError(
                "Unsupported container element type '" + data.type + "'.",
                path + TYPE_JSON_SUFFIX
            );
        }
        return deserializeDockNode(data, path, data.type);
    }

    private DockNode deserializeDockNode(ElementData data, String path, String unsupportedType) throws DockLayoutLoadException {
        boolean strictFields = isBlank(unsupportedType);
        if (strictFields && isBlank(data.id)) {
            throw missingFieldError(path + ID_JSON_SUFFIX);
        }
        if (strictFields && isBlank(data.title)) {
            throw missingFieldError(path + TITLE_JSON_SUFFIX);
        }

        String resolvedDockNodeId = resolveDockNodeId(data, unsupportedType);
        String resolvedTitle = resolveNodeTitle(data, resolvedDockNodeId, unsupportedType);
        String resolvedLayoutId = isBlank(data.id) ? null : data.id;
        String typePath = isBlank(unsupportedType) ? path : path + TYPE_JSON_SUFFIX;

        DockNode node = createNodeViaFactory(
            data,
            path,
            unsupportedType,
            typePath,
            resolvedDockNodeId,
            resolvedLayoutId,
            resolvedTitle
        );
        if (node != null) {
            return node;
        }

        if (!isBlank(data.id)) {
            node = nodeRegistry.get(data.id);
            if (node != null) {
                applyRestoredNodeState(node, resolvedLayoutId, resolvedTitle, data.closeable);
                return node;
            }
        }

        Label placeholder = new Label(
            buildPlaceholderMessage(unsupportedType, resolvedDockNodeId, resolvedLayoutId, typePath)
        );
        node = new DockNode(resolvedDockNodeId, placeholder, resolvedTitle);
        applyRestoredNodeState(node, resolvedLayoutId, resolvedTitle, data.closeable);
        return node;
    }

    private DockNode createNodeViaFactory(
        ElementData data,
        String path,
        String unsupportedType,
        String typePath,
        String resolvedDockNodeId,
        String resolvedLayoutId,
        String resolvedTitle
    ) throws DockLayoutLoadException {
        if (nodeFactory == null) {
            return null;
        }

        DockNode node = null;
        if (!isBlank(resolvedDockNodeId)) {
            node = nodeFactory.createNode(resolvedDockNodeId);
        }
        if (node == null && !isBlank(unsupportedType)) {
            node = nodeFactory.createUnknownNode(
                new DockNodeFactory.UnknownElementContext(
                    unsupportedType,
                    resolvedDockNodeId,
                    resolvedLayoutId,
                    resolvedTitle,
                    typePath
                )
            );
        }
        if (node == null) {
            return null;
        }

        applyRestoredNodeState(node, resolvedLayoutId, resolvedTitle, data.closeable);
        restoreNodeContentData(node, data.contentData, path);
        return node;
    }

    private void applyRestoredNodeState(DockNode node, String layoutId, String title, boolean closeable) {
        if (node == null) {
            return;
        }
        if (!isBlank(layoutId)) {
            node.setLayoutId(layoutId);
        }
        if (!isBlank(title)) {
            node.setTitle(title);
        }
        node.setCloseable(closeable);
    }

    private void restoreNodeContentData(DockNode node, JsonObject contentData, String path) throws DockLayoutLoadException {
        if (node == null || contentData == null || !(node.getContent() instanceof DockNodeContentSerializer serializer)) {
            return;
        }
        try {
            serializer.deserializeContent(contentData);
        } catch (RuntimeException e) {
            throw loadError("DockNode content could not be deserialized: " + e.getMessage(),
                path + ".contentData", e);
        }
    }

    private String resolveDockNodeId(ElementData data, String unsupportedType) {
        if (!isBlank(data.dockNodeId)) {
            return data.dockNodeId;
        }
        if (!isBlank(data.id)) {
            return data.id;
        }
        if (!isBlank(unsupportedType)) {
            return "unknown:" + unsupportedType;
        }
        return "unknown";
    }

    private String resolveNodeTitle(ElementData data, String resolvedDockNodeId, String unsupportedType) {
        if (!isBlank(data.title)) {
            return data.title;
        }
        if (isBlank(unsupportedType)) {
            return "Untitled";
        }
        if (!isBlank(resolvedDockNodeId)) {
            return "Unavailable Node (" + resolvedDockNodeId + ")";
        }
        return "Unavailable Node";
    }

    private String buildPlaceholderMessage(
        String unsupportedType,
        String resolvedDockNodeId,
        String resolvedLayoutId,
        String typePath
    ) {
        String savedType = isBlank(unsupportedType) ? DOCK_NODE : unsupportedType;
        String nodeId = isBlank(resolvedDockNodeId) ? "<unknown>" : resolvedDockNodeId;
        String layoutId = isBlank(resolvedLayoutId) ? "<unknown>" : resolvedLayoutId;
        return "Unavailable node restored as placeholder.\n"
            + "Saved type: " + savedType + "\n"
            + "Node ID: " + nodeId + "\n"
            + "Layout ID: " + layoutId + "\n"
            + "JSON path: " + typePath;
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

    private Side parseSideBarSide(String sideValue, String path) throws DockLayoutLoadException {
        if (isBlank(sideValue)) {
            throw missingFieldError(path);
        }
        try {
            return Side.valueOf(sideValue);
        } catch (IllegalArgumentException e) {
            throw loadError("Unsupported sidebar side '" + sideValue + "'.", path, e);
        }
    }

    private DockPosition parseOptionalDockPosition(String positionValue, String path) throws DockLayoutLoadException {
        if (isBlank(positionValue)) {
            return null;
        }
        try {
            return DockPosition.valueOf(positionValue);
        } catch (IllegalArgumentException e) {
            throw loadError("Unsupported dock position '" + positionValue + "'.", path, e);
        }
    }

    private DockElement resolveElementById(DockElement root, String id) {
        if (root == null || isBlank(id)) {
            return null;
        }
        if (id.equals(root.getId())) {
            return root;
        }
        if (root instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                DockElement resolved = resolveElementById(child, id);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
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
        SideBarData[] sideBars;
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

    private static class SideBarData {
        String side;
        boolean pinnedOpen;
        Double panelWidth;
        SideBarEntryData[] entries;
    }

    private static class SideBarEntryData {
        ElementData node;
        String restoreTargetId;
        String restorePosition;
        Integer restoreTabIndex;
    }

    private record DeserializedSideBar(Side side, boolean pinnedOpen, Double panelWidth, List<DeserializedSideBarEntry> entries) {
    }

    private record DeserializedSideBarEntry(
        DockNode node,
        DockElement restoreTarget,
        DockPosition restorePosition,
        Integer restoreTabIndex
    ) {
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
                case DOCK_NODE -> context.deserialize(json, DockNode.class);
                case DOCK_SPLIT_PANE -> context.deserialize(json, DockSplitPane.class);
                case DOCK_TAB_PANE -> context.deserialize(json, DockTabPane.class);
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

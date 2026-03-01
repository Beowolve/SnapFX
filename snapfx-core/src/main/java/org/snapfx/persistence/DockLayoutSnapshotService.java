package org.snapfx.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.snapfx.model.DockGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles snapshot JSON composition, parsing, and validation for layouts that include floating windows.
 *
 * <p>This service encapsulates snapshot-specific JSON handling so {@code SnapFX} can delegate
 * snapshot concerns instead of owning all parsing/validation helpers directly.</p>
 */
public final class DockLayoutSnapshotService {
    private static final String SNAPSHOT_MAIN_LAYOUT_KEY = "mainLayout";
    private static final String SNAPSHOT_FLOATING_WINDOWS_KEY = "floatingWindows";
    private static final String SNAPSHOT_FLOATING_LAYOUT_KEY = "layout";
    private static final String SNAPSHOT_FLOATING_X_KEY = "x";
    private static final String SNAPSHOT_FLOATING_Y_KEY = "y";
    private static final String SNAPSHOT_FLOATING_WIDTH_KEY = "width";
    private static final String SNAPSHOT_FLOATING_HEIGHT_KEY = "height";
    private static final String SNAPSHOT_FLOATING_ALWAYS_ON_TOP_KEY = "alwaysOnTop";

    private final Gson snapshotGson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Creates a snapshot service with default JSON configuration.
     */
    public DockLayoutSnapshotService() {
    }

    /**
     * Builds snapshot JSON from main-layout JSON and floating-window entries.
     *
     * @param mainLayoutJson main-layout JSON payload
     * @param floatingWindows serialized floating-window entries
     * @return full snapshot JSON string
     */
    public String createSnapshotJson(String mainLayoutJson, JsonArray floatingWindows) {
        JsonObject snapshot = new JsonObject();
        snapshot.add(SNAPSHOT_MAIN_LAYOUT_KEY, parseJsonObjectOrEmpty(mainLayoutJson));
        snapshot.add(SNAPSHOT_FLOATING_WINDOWS_KEY, floatingWindows == null ? new JsonArray() : floatingWindows);
        return snapshotGson.toJson(snapshot);
    }

    /**
     * Builds one floating-window snapshot entry.
     *
     * @param layoutJson serialized floating-layout JSON
     * @param x preferred x position, or {@code null}
     * @param y preferred y position, or {@code null}
     * @param width preferred width
     * @param height preferred height
     * @param alwaysOnTop always-on-top state
     * @return snapshot entry JSON object
     */
    public JsonObject createFloatingWindowEntry(
        String layoutJson,
        Double x,
        Double y,
        double width,
        double height,
        boolean alwaysOnTop
    ) {
        JsonObject floatingData = new JsonObject();
        floatingData.add(SNAPSHOT_FLOATING_LAYOUT_KEY, parseJsonObjectOrEmpty(layoutJson));
        addOptionalNumber(floatingData, SNAPSHOT_FLOATING_X_KEY, x);
        addOptionalNumber(floatingData, SNAPSHOT_FLOATING_Y_KEY, y);
        floatingData.addProperty(SNAPSHOT_FLOATING_WIDTH_KEY, width);
        floatingData.addProperty(SNAPSHOT_FLOATING_HEIGHT_KEY, height);
        floatingData.addProperty(SNAPSHOT_FLOATING_ALWAYS_ON_TOP_KEY, alwaysOnTop);
        return floatingData;
    }

    /**
     * Attempts to parse snapshot JSON that includes {@code mainLayout} and optional floating windows.
     *
     * @param json snapshot JSON string
     * @return parsed snapshot, or {@code null} when the payload is not snapshot-shaped
     */
    public DockLayoutSnapshot tryParseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject snapshotJson = parsed.getAsJsonObject();
            JsonElement mainLayoutElement = snapshotJson.get(SNAPSHOT_MAIN_LAYOUT_KEY);
            if (mainLayoutElement == null || !mainLayoutElement.isJsonObject()) {
                return null;
            }

            JsonObject mainLayout = mainLayoutElement.getAsJsonObject();
            List<DockFloatingWindowSnapshot> snapshots = new ArrayList<>();
            JsonElement floatingWindowsElement = snapshotJson.get(SNAPSHOT_FLOATING_WINDOWS_KEY);
            if (floatingWindowsElement != null && floatingWindowsElement.isJsonArray()) {
                for (JsonElement floatingElement : floatingWindowsElement.getAsJsonArray()) {
                    if (!floatingElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject floatingJson = floatingElement.getAsJsonObject();
                    JsonElement floatingLayoutElement = floatingJson.get(SNAPSHOT_FLOATING_LAYOUT_KEY);
                    if (floatingLayoutElement == null || !floatingLayoutElement.isJsonObject()) {
                        continue;
                    }
                    snapshots.add(new DockFloatingWindowSnapshot(
                        floatingLayoutElement.getAsJsonObject(),
                        readOptionalFiniteDouble(floatingJson, SNAPSHOT_FLOATING_X_KEY),
                        readOptionalFiniteDouble(floatingJson, SNAPSHOT_FLOATING_Y_KEY),
                        readOptionalFiniteDouble(floatingJson, SNAPSHOT_FLOATING_WIDTH_KEY),
                        readOptionalFiniteDouble(floatingJson, SNAPSHOT_FLOATING_HEIGHT_KEY),
                        readOptionalBoolean(floatingJson, SNAPSHOT_FLOATING_ALWAYS_ON_TOP_KEY)
                    ));
                }
            }
            return new DockLayoutSnapshot(mainLayout, snapshots);
        } catch (JsonSyntaxException ignored) {
            return null;
        }
    }

    /**
     * Validates a parsed snapshot payload including all floating-layout subtrees.
     *
     * @param snapshot parsed snapshot
     * @param nodeFactory optional node factory used by validation deserializers
     * @throws DockLayoutLoadException when validation fails
     */
    public void validateSnapshot(DockLayoutSnapshot snapshot, DockNodeFactory nodeFactory) throws DockLayoutLoadException {
        if (snapshot == null || snapshot.mainLayout() == null) {
            throw new DockLayoutLoadException("Snapshot is missing main layout data.", "$.mainLayout");
        }
        validateLayoutJson(snapshotGson.toJson(snapshot.mainLayout()), "$.mainLayout", nodeFactory);
        List<DockFloatingWindowSnapshot> floatingSnapshots = snapshot.floatingWindows();
        if (floatingSnapshots == null || floatingSnapshots.isEmpty()) {
            return;
        }
        for (int i = 0; i < floatingSnapshots.size(); i++) {
            DockFloatingWindowSnapshot floatingSnapshot = floatingSnapshots.get(i);
            if (floatingSnapshot == null || floatingSnapshot.layout() == null) {
                throw new DockLayoutLoadException(
                    "Floating window snapshot is missing layout data.",
                    "$.floatingWindows[" + i + "].layout"
                );
            }
            validateLayoutJson(
                snapshotGson.toJson(floatingSnapshot.layout()),
                "$.floatingWindows[" + i + "].layout",
                nodeFactory
            );
        }
    }

    /**
     * Validates plain layout JSON.
     *
     * @param layoutJson layout JSON payload
     * @param rootPath root path used for error rebasing
     * @param nodeFactory optional node factory used by validation deserializer
     * @throws DockLayoutLoadException when layout deserialization fails
     */
    public void validateLayoutJson(String layoutJson, String rootPath, DockNodeFactory nodeFactory) throws DockLayoutLoadException {
        DockLayoutSerializer validationSerializer = new DockLayoutSerializer(new DockGraph());
        if (nodeFactory != null) {
            validationSerializer.setNodeFactory(nodeFactory);
        }
        try {
            validationSerializer.deserialize(layoutJson);
        } catch (DockLayoutLoadException e) {
            throw rebaseLoadException(e, rootPath);
        }
    }

    /**
     * Serializes arbitrary payloads using snapshot JSON configuration.
     *
     * @param payload payload to serialize
     * @return serialized JSON
     */
    public String toJson(Object payload) {
        return snapshotGson.toJson(payload);
    }

    private DockLayoutLoadException rebaseLoadException(DockLayoutLoadException exception, String basePath) {
        if (exception == null) {
            return new DockLayoutLoadException("Layout could not be loaded.", basePath);
        }
        String location = combineJsonPath(basePath, exception.getLocation());
        return new DockLayoutLoadException(exception.getMessage(), location, exception.getCause());
    }

    private String combineJsonPath(String basePath, String nestedPath) {
        String base = (basePath == null || basePath.isBlank()) ? "$" : basePath;
        if (nestedPath == null || nestedPath.isBlank() || "$".equals(nestedPath)) {
            return base;
        }
        if (!nestedPath.startsWith("$")) {
            return base + "." + nestedPath;
        }
        String nestedSuffix = nestedPath.substring(1);
        if (nestedSuffix.isBlank()) {
            return base;
        }
        if (nestedSuffix.startsWith(".")) {
            return base + nestedSuffix;
        }
        return base + "." + nestedSuffix;
    }

    private JsonObject parseJsonObjectOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (JsonSyntaxException ignored) {
            // Ignore invalid data and return empty object fallback.
        }
        return new JsonObject();
    }

    private void addOptionalNumber(JsonObject object, String key, Double value) {
        if (object == null || key == null || !isFiniteNumber(value)) {
            return;
        }
        object.addProperty(key, value);
    }

    private Double readOptionalFiniteDouble(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return null;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        double parsed = value.getAsDouble();
        if (!Double.isFinite(parsed)) {
            return null;
        }
        return parsed;
    }

    private Boolean readOptionalBoolean(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return null;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            return null;
        }
        return value.getAsBoolean();
    }

    private boolean isFiniteNumber(Double value) {
        return value != null && Double.isFinite(value);
    }

    /**
     * Parsed snapshot payload containing main layout and optional floating windows.
     *
     * @param mainLayout parsed main-layout JSON object
     * @param floatingWindows parsed floating-window snapshots
     */
    public record DockLayoutSnapshot(JsonObject mainLayout, List<DockFloatingWindowSnapshot> floatingWindows) {
    }

    /**
     * Parsed floating-window snapshot payload.
     *
     * @param layout floating-layout JSON object
     * @param x preferred x position, or {@code null}
     * @param y preferred y position, or {@code null}
     * @param width preferred width, or {@code null}
     * @param height preferred height, or {@code null}
     * @param alwaysOnTop preferred always-on-top state, or {@code null}
     */
    public record DockFloatingWindowSnapshot(
        JsonObject layout,
        Double x,
        Double y,
        Double width,
        Double height,
        Boolean alwaysOnTop
    ) {
    }
}

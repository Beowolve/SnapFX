package org.snapfx.persistence;

import com.google.gson.JsonObject;

/**
 * Optional interface for DockNode content that needs to be serialized.
 *
 * <p>Nodes that implement this interface can save and restore their content state
 * across application sessions. This is useful for editors, property panels, etc.
 * that contain user data that should be persisted.</p>
 *
 * <p><b>Example implementation:</b></p>
 * <pre>{@code
 * public class SerializableTextArea extends TextArea implements DockNodeContentSerializer {
 *     @Override
 *     public JsonObject serializeContent() {
 *         JsonObject data = new JsonObject();
 *         data.addProperty("text", getText());
 *         data.addProperty("caretPosition", getCaretPosition());
 *         return data;
 *     }
 *
 *     @Override
 *     public void deserializeContent(JsonObject data) {
 *         if (data.has("text")) {
 *             setText(data.get("text").getAsString());
 *         }
 *         if (data.has("caretPosition")) {
 *             positionCaret(data.get("caretPosition").getAsInt());
 *         }
 *     }
 * }
 * }</pre>
 */
public interface DockNodeContentSerializer {

    /**
     * Serializes the content state to a JSON object.
     *
     * @return JSON object containing the content state, or null if nothing to serialize
     */
    JsonObject serializeContent();

    /**
     * Deserializes the content state from a JSON object.
     *
     * @param data JSON object containing the previously serialized content state
     */
    void deserializeContent(JsonObject data);
}


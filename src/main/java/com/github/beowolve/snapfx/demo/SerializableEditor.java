package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.persistence.DockNodeContentSerializer;
import com.google.gson.JsonObject;
import javafx.scene.control.TextArea;

/**
 * Demo implementation of a serializable text editor.
 * This editor can save and restore its content across application sessions.
 */
public class SerializableEditor extends TextArea implements DockNodeContentSerializer {

    /**
     * Creates a new serializable editor with default content.
     */
    public SerializableEditor() {
        this("");
    }

    /**
     * Creates a new serializable editor with initial content.
     */
    public SerializableEditor(String initialContent) {
        super(initialContent);
        setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px;");
    }

    @Override
    public JsonObject serializeContent() {
        JsonObject data = new JsonObject();
        data.addProperty("text", getText());
        data.addProperty("caretPosition", getCaretPosition());
        return data;
    }

    @Override
    public void deserializeContent(JsonObject data) {
        if (data.has("text")) {
            setText(data.get("text").getAsString());
        }
        if (data.has("caretPosition")) {
            int caretPos = data.get("caretPosition").getAsInt();
            // Ensure caret position is valid
            if (caretPos >= 0 && caretPos <= getText().length()) {
                positionCaret(caretPos);
            }
        }
    }
}


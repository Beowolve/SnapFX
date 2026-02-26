package org.snapfx.demo;

import org.snapfx.persistence.DockNodeContentSerializer;
import com.google.gson.JsonObject;
import javafx.scene.control.TextArea;

/**
 * Demo implementation of a serializable text editor.
 * This editor can save and restore its content across application sessions.
 */
public class SerializableEditor extends TextArea implements DockNodeContentSerializer {

    public static final String CARET_POSITION = "caretPosition";
    public static final String TEXT = "text";

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
        data.addProperty(TEXT, getText());
        data.addProperty(CARET_POSITION, getCaretPosition());
        return data;
    }

    @Override
    public void deserializeContent(JsonObject data) {
        if (data.has(TEXT)) {
            setText(data.get(TEXT).getAsString());
        }
        if (data.has(CARET_POSITION)) {
            int caretPos = data.get(CARET_POSITION).getAsInt();
            // Ensure caret position is valid
            if (caretPos >= 0 && caretPos <= getText().length()) {
                positionCaret(caretPos);
            }
        }
    }
}


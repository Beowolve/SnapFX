package com.github.beowolve.snapfx.persistence;

import com.github.beowolve.snapfx.model.DockNode;
import javafx.scene.Node;

/**
 * Factory interface for creating DockNode instances during deserialization.
 *
 * <p>When loading a saved layout, the framework needs to recreate the DockNodes
 * that were part of the original layout. Since only the node IDs are stored in
 * the JSON (not the actual JavaFX content), the application must provide a way
 * to recreate nodes from their IDs.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * DockNodeFactory factory = nodeId -> {
 *     return switch (nodeId) {
 *         case "projectExplorer" -> createProjectExplorer();
 *         case "mainEditor" -> createMainEditor();
 *         case "console" -> createConsole();
 *         default -> null;
 *     };
 * };
 *
 * snapFX.setNodeFactory(factory);
 * try {
 *     snapFX.loadLayout(json);
 * } catch (DockLayoutLoadException e) {
 *     // Handle invalid/corrupt layout JSON.
 * }
 * }</pre>
 *
 * <p><b>Important:</b> The factory must create DockNodes with the same ID that
 * was used when the layout was saved. Use the {@link DockNode#DockNode(String, Node, String)}
 * constructor with the provided nodeId.</p>
 */
@FunctionalInterface
public interface DockNodeFactory {
    /**
     * Creates a DockNode for the given ID.
     *
     * <p>This method is called during layout deserialization for each node ID
     * found in the saved layout. The implementation must create and return a
     * DockNode with the given ID and appropriate content.</p>
     *
     * @param nodeId The unique identifier of the node to create (same ID as used when saving)
     * @return A DockNode with the specified ID and recreated content, or null if the ID is unknown
     */
    DockNode createNode(String nodeId);

    /**
     * Optional hook for unsupported serialized element types.
     *
     * <p>The serializer invokes this callback when it encounters an unknown element
     * type in the saved layout (for example "DockNode!!!"). Returning {@code null}
     * keeps the framework default behavior and inserts the built-in placeholder node.</p>
     *
     * @param context Details about the unsupported element and its JSON location
     * @return Custom replacement node, or {@code null} to use the framework placeholder
     */
    default DockNode createUnknownNode(UnknownElementContext context) {
        return null;
    }

    /**
     * Context passed to {@link #createUnknownNode(UnknownElementContext)}.
     *
     * @param elementType unsupported serialized element type
     * @param dockNodeId serialized DockNode type ID (may be {@code null})
     * @param layoutId serialized layout ID (may be {@code null})
     * @param title serialized title (may be {@code null})
     * @param jsonPath JSON path of the unsupported type field
     */
    record UnknownElementContext(
        String elementType,
        String dockNodeId,
        String layoutId,
        String title,
        String jsonPath
    ) {
    }
}


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
}


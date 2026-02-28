package org.snapfx.dnd;

import org.snapfx.model.DockElement;
import org.snapfx.model.DockNode;
import org.snapfx.model.DockPosition;

/**
 * Data transfer object for drag &amp; drop operations.
 */
public class DockDragData {
    private final DockNode draggedNode;
    private DockElement dropTarget;
    private DockPosition dropPosition;
    private Integer dropTabIndex;
    private double mouseX;
    private double mouseY;

    /**
     * Creates drag-state data for one dragged node.
     *
     * @param draggedNode node currently being dragged
     */
    public DockDragData(DockNode draggedNode) {
        this.draggedNode = draggedNode;
    }

    /**
     * Returns the node currently being dragged.
     *
     * @return dragged node
     */
    public DockNode getDraggedNode() {
        return draggedNode;
    }

    /**
     * Returns the current drop target under the pointer.
     *
     * @return drop target or {@code null} when none is active
     */
    public DockElement getDropTarget() {
        return dropTarget;
    }

    /**
     * Updates the active drop target.
     *
     * @param dropTarget target under pointer, or {@code null}
     */
    public void setDropTarget(DockElement dropTarget) {
        this.dropTarget = dropTarget;
    }

    /**
     * Returns the proposed dock position for the current target.
     *
     * @return drop position or {@code null}
     */
    public DockPosition getDropPosition() {
        return dropPosition;
    }

    /**
     * Updates the proposed dock position.
     *
     * @param dropPosition dock position or {@code null}
     */
    public void setDropPosition(DockPosition dropPosition) {
        this.dropPosition = dropPosition;
    }

    /**
     * Returns the target tab index for tab insert operations.
     *
     * @return tab index or {@code null}
     */
    public Integer getDropTabIndex() {
        return dropTabIndex;
    }

    /**
     * Sets the target tab index for tab insert operations.
     *
     * @param dropTabIndex target tab index or {@code null}
     */
    public void setDropTabIndex(Integer dropTabIndex) {
        this.dropTabIndex = dropTabIndex;
    }

    /**
     * Returns the current pointer screen x-coordinate.
     *
     * @return screen x-coordinate in pixels
     */
    public double getMouseX() {
        return mouseX;
    }

    /**
     * Sets the current pointer screen x-coordinate.
     *
     * @param mouseX screen x-coordinate in pixels
     */
    public void setMouseX(double mouseX) {
        this.mouseX = mouseX;
    }

    /**
     * Returns the current pointer screen y-coordinate.
     *
     * @return screen y-coordinate in pixels
     */
    public double getMouseY() {
        return mouseY;
    }

    /**
     * Sets the current pointer screen y-coordinate.
     *
     * @param mouseY screen y-coordinate in pixels
     */
    public void setMouseY(double mouseY) {
        this.mouseY = mouseY;
    }
}

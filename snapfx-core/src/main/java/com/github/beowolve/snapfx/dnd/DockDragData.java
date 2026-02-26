package com.github.beowolve.snapfx.dnd;

import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;

/**
 * Data transfer object for drag & drop operations.
 */
public class DockDragData {
    private final DockNode draggedNode;
    private DockElement dropTarget;
    private DockPosition dropPosition;
    private Integer dropTabIndex;
    private double mouseX;
    private double mouseY;

    public DockDragData(DockNode draggedNode) {
        this.draggedNode = draggedNode;
    }

    public DockNode getDraggedNode() {
        return draggedNode;
    }

    public DockElement getDropTarget() {
        return dropTarget;
    }

    public void setDropTarget(DockElement dropTarget) {
        this.dropTarget = dropTarget;
    }

    public DockPosition getDropPosition() {
        return dropPosition;
    }

    public void setDropPosition(DockPosition dropPosition) {
        this.dropPosition = dropPosition;
    }

    public Integer getDropTabIndex() {
        return dropTabIndex;
    }

    public void setDropTabIndex(Integer dropTabIndex) {
        this.dropTabIndex = dropTabIndex;
    }

    public double getMouseX() {
        return mouseX;
    }

    public void setMouseX(double mouseX) {
        this.mouseX = mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public void setMouseY(double mouseY) {
        this.mouseY = mouseY;
    }
}

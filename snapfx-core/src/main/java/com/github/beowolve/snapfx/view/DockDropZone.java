package com.github.beowolve.snapfx.view;

import com.github.beowolve.snapfx.model.DockElement;
import com.github.beowolve.snapfx.model.DockPosition;
import javafx.geometry.Bounds;

/**
 * Represents a single drop zone for DnD targeting.
 */
public class DockDropZone {
    private final DockElement target;
    private final DockPosition position;
    private final DockDropZoneType type;
    private final Bounds bounds;
    private final int depth;
    private final Integer tabIndex;
    private final Double insertLineX;

    public DockDropZone(DockElement target, DockPosition position, DockDropZoneType type,
                        Bounds bounds, int depth, Integer tabIndex, Double insertLineX) {
        this.target = target;
        this.position = position;
        this.type = type;
        this.bounds = bounds;
        this.depth = depth;
        this.tabIndex = tabIndex;
        this.insertLineX = insertLineX;
    }

    public DockElement getTarget() {
        return target;
    }

    public DockPosition getPosition() {
        return position;
    }

    public DockDropZoneType getType() {
        return type;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public int getDepth() {
        return depth;
    }

    public Integer getTabIndex() {
        return tabIndex;
    }

    public Double getInsertLineX() {
        return insertLineX;
    }

    public boolean contains(double sceneX, double sceneY) {
        return bounds != null && bounds.contains(sceneX, sceneY);
    }

    public double area() {
        if (bounds == null) {
            return 0.0;
        }
        return bounds.getWidth() * bounds.getHeight();
    }
}

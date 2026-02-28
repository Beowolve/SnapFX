package org.snapfx.view;

import org.snapfx.model.DockElement;
import org.snapfx.model.DockPosition;
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

    /**
     * Creates one resolved drop-zone candidate.
     *
     * @param target drop target element
     * @param position proposed dock position on the target
     * @param type drop-zone type
     * @param bounds scene-space zone bounds
     * @param depth target depth in the layout tree
     * @param tabIndex target tab index for tab operations, or {@code null}
     * @param insertLineX scene x-position for tab insert indicator, or {@code null}
     */
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

    /**
     * Returns the target element this zone docks into.
     *
     * @return target element
     */
    public DockElement getTarget() {
        return target;
    }

    /**
     * Returns the resolved dock position for this zone.
     *
     * @return drop position
     */
    public DockPosition getPosition() {
        return position;
    }

    /**
     * Returns the semantic drop-zone type.
     *
     * @return zone type
     */
    public DockDropZoneType getType() {
        return type;
    }

    /**
     * Returns the scene-space bounds used for hit testing.
     *
     * @return zone bounds, or {@code null}
     */
    public Bounds getBounds() {
        return bounds;
    }

    /**
     * Returns the target depth in the layout tree.
     *
     * @return depth (higher is deeper)
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Returns the tab index used for tab-header drops.
     *
     * @return tab index or {@code null}
     */
    public Integer getTabIndex() {
        return tabIndex;
    }

    /**
     * Returns the tab insert-line x-coordinate in scene space.
     *
     * @return scene x-coordinate or {@code null}
     */
    public Double getInsertLineX() {
        return insertLineX;
    }

    /**
     * Tests whether a scene-space point is inside this zone.
     *
     * @param sceneX scene x-coordinate
     * @param sceneY scene y-coordinate
     * @return {@code true} when the point is inside the zone bounds
     */
    public boolean contains(double sceneX, double sceneY) {
        return bounds != null && bounds.contains(sceneX, sceneY);
    }

    /**
     * Returns the area of this zone in square pixels.
     *
     * @return zone area, or {@code 0.0} when bounds are unavailable
     */
    public double area() {
        if (bounds == null) {
            return 0.0;
        }
        return bounds.getWidth() * bounds.getHeight();
    }
}

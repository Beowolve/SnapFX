package com.github.beowolve.snapfx.view;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * Factory for small vector icons used in dock title/control bars.
 */
public final class DockControlIcons {
    private static final double ICON_BOX_SIZE = 12.0;
    private static final Color ICON_COLOR = Color.web("#404040");

    private DockControlIcons() {
    }

    public static Node createCloseIcon() {
        Line l1 = new Line(2.5, 2.5, 9.5, 9.5);
        Line l2 = new Line(9.5, 2.5, 2.5, 9.5);
        styleStroke(l1, 1.5);
        styleStroke(l2, 1.5);
        return wrap(l1, l2);
    }

    public static Node createMaximizeIcon() {
        Rectangle rect = new Rectangle(2.0, 2.0, 8.0, 8.0);
        rect.setFill(Color.TRANSPARENT);
        styleStroke(rect, 1.3);
        return wrap(rect);
    }

    public static Node createRestoreIcon() {
        Rectangle back = new Rectangle(2.0, 3.2, 6.2, 6.2);
        Rectangle front = new Rectangle(3.8, 1.6, 6.2, 6.2);
        back.setFill(Color.TRANSPARENT);
        front.setFill(Color.TRANSPARENT);
        styleStroke(back, 1.2);
        styleStroke(front, 1.2);
        return wrap(back, front);
    }

    public static Node createAttachIcon() {
        Rectangle frame = new Rectangle(2.0, 2.0, 8.0, 8.0);
        frame.setFill(Color.TRANSPARENT);
        styleStroke(frame, 1.2);

        Line stem = new Line(8.0, 4.0, 4.2, 7.8);
        styleStroke(stem, 1.5);

        Polygon head = new Polygon(
            3.4, 7.8,
            5.8, 7.8,
            4.6, 9.8
        );
        head.setFill(ICON_COLOR);

        return wrap(frame, stem, head);
    }

    public static Node createFloatIcon() {
        Rectangle frame = new Rectangle(2.0, 3.0, 7.0, 7.0);
        frame.setFill(Color.TRANSPARENT);
        styleStroke(frame, 1.2);

        Line stem = new Line(5.8, 6.4, 10.0, 2.2);
        styleStroke(stem, 1.5);

        Polygon head = new Polygon(
            8.3, 2.2,
            10.2, 2.2,
            10.2, 4.1
        );
        head.setFill(ICON_COLOR);

        return wrap(frame, stem, head);
    }

    private static void styleStroke(Shape shape, double width) {
        shape.setStroke(ICON_COLOR);
        shape.setStrokeWidth(width);
    }

    private static StackPane wrap(Node... nodes) {
        StackPane wrapper = new StackPane(nodes);
        wrapper.setMinSize(ICON_BOX_SIZE, ICON_BOX_SIZE);
        wrapper.setPrefSize(ICON_BOX_SIZE, ICON_BOX_SIZE);
        wrapper.setMaxSize(ICON_BOX_SIZE, ICON_BOX_SIZE);
        wrapper.setMouseTransparent(true);
        return wrapper;
    }
}

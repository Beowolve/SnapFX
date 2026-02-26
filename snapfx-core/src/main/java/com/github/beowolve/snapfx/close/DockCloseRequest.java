package com.github.beowolve.snapfx.close;

import com.github.beowolve.snapfx.floating.DockFloatingWindow;
import com.github.beowolve.snapfx.model.DockNode;

import java.util.List;

/**
 * Contains context about a close request before it is handled.
 *
 * @param source where the request originated
 * @param nodes nodes affected by the request
 * @param floatingWindow source floating window, if applicable
 * @param defaultBehavior configured default behavior
 */
public record DockCloseRequest(
    DockCloseSource source,
    List<DockNode> nodes,
    DockFloatingWindow floatingWindow,
    DockCloseBehavior defaultBehavior
) {
    public DockCloseRequest {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        source = source == null ? DockCloseSource.TITLE_BAR : source;
        defaultBehavior = defaultBehavior == null ? DockCloseBehavior.HIDE : defaultBehavior;
    }
}

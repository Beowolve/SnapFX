package org.snapfx.close;

/**
 * Describes the outcome of a processed close request.
 *
 * @param request original request context
 * @param appliedBehavior resolved behavior that was applied; {@code null} when canceled
 * @param canceled whether the request was canceled
 */
public record DockCloseResult(
    DockCloseRequest request,
    DockCloseBehavior appliedBehavior,
    boolean canceled
) {
}

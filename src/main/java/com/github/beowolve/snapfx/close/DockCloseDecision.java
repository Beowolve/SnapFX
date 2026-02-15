package com.github.beowolve.snapfx.close;

/**
 * Decision returned by close-request callbacks.
 */
public enum DockCloseDecision {
    /**
     * Uses the configured default close behavior.
     */
    DEFAULT,
    /**
     * Cancels the close request.
     */
    CANCEL,
    /**
     * Forces hide behavior for this request.
     */
    HIDE,
    /**
     * Forces remove behavior for this request.
     */
    REMOVE
}

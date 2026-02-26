package com.github.beowolve.snapfx.persistence;

/**
 * Signals a failure while loading a dock layout JSON document.
 * Includes the JSON location where the failure was detected.
 */
public class DockLayoutLoadException extends Exception {
    private final String location;

    /**
     * Creates a new layout-load exception.
     *
     * @param message human-readable error message
     * @param location JSON location/path (for example {@code $.root.children[0]})
     */
    public DockLayoutLoadException(String message, String location) {
        super(message);
        this.location = location;
    }

    /**
     * Creates a new layout-load exception with an underlying cause.
     *
     * @param message human-readable error message
     * @param location JSON location/path (for example {@code $.root.children[0]})
     * @param cause underlying exception
     */
    public DockLayoutLoadException(String message, String location, Throwable cause) {
        super(message, cause);
        this.location = location;
    }

    /**
     * Returns the JSON location where the error was detected.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns a user-facing message including JSON location details when available.
     */
    public String toDisplayMessage() {
        if (location == null || location.isBlank()) {
            return getMessage();
        }
        return getMessage() + " (at " + location + ")";
    }
}

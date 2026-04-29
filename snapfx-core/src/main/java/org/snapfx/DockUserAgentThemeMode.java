package org.snapfx;

/**
 * Controls how SnapFX applies optional user-agent-theme compatibility adjustments.
 *
 * <p>The compatibility layer is intended for external user-agent stylesheets (for example AtlantaFX)
 * while keeping Modena as the default baseline.</p>
 */
public enum DockUserAgentThemeMode {
    /**
     * Detect the active global JavaFX user-agent stylesheet and enable compatibility automatically
     * when an AtlantaFX stylesheet is detected.
     */
    AUTO,

    /**
     * Force Modena-style behavior by disabling the compatibility layer.
     */
    MODENA,

    /**
     * Force the compatibility layer regardless of the currently active user-agent stylesheet.
     */
    ATLANTAFX_COMPAT
}

package com.github.beowolve.snapfx.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in SnapFX theme catalog with stable theme names and stylesheet paths.
 */
public final class DockThemeCatalog {
    private static final String DEFAULT_THEME_NAME = "Light";
    private static final String DARK_THEME_NAME = "Dark";
    private static final String DEFAULT_THEME_STYLESHEET_RESOURCE_PATH = "/snapfx.css";
    private static final String DARK_THEME_STYLESHEET_RESOURCE_PATH = "/snapfx-dark.css";
    private static final Map<String, String> AVAILABLE_THEME_STYLESHEETS = createAvailableThemeStylesheets();

    private DockThemeCatalog() {
    }

    /**
     * Returns the default built-in theme name.
     */
    public static String getDefaultThemeName() {
        return DEFAULT_THEME_NAME;
    }

    /**
     * Returns the default stylesheet resource path.
     */
    public static String getDefaultThemeStylesheetResourcePath() {
        return DEFAULT_THEME_STYLESHEET_RESOURCE_PATH;
    }

    /**
     * Returns all built-in themes as an ordered map of {@code themeName -> stylesheetResourcePath}.
     */
    public static Map<String, String> getAvailableThemeStylesheets() {
        return AVAILABLE_THEME_STYLESHEETS;
    }

    /**
     * Returns all built-in theme names in deterministic order.
     */
    public static List<String> getAvailableThemeNames() {
        return List.copyOf(AVAILABLE_THEME_STYLESHEETS.keySet());
    }

    private static Map<String, String> createAvailableThemeStylesheets() {
        Map<String, String> themes = new LinkedHashMap<>();
        themes.put(DEFAULT_THEME_NAME, DEFAULT_THEME_STYLESHEET_RESOURCE_PATH);
        themes.put(DARK_THEME_NAME, DARK_THEME_STYLESHEET_RESOURCE_PATH);
        return Collections.unmodifiableMap(themes);
    }
}

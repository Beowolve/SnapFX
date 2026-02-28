package org.snapfx.theme;

import javafx.collections.ObservableList;
import javafx.scene.Scene;

import java.net.URL;

/**
 * Resolves, stores, and applies the active SnapFX stylesheet across managed scenes.
 */
public final class DockThemeStylesheetManager {
    private String stylesheetResourcePath;
    private String stylesheetUrl;

    /**
     * Creates a stylesheet manager initialized with the default built-in stylesheet.
     */
    public DockThemeStylesheetManager() {
        this.stylesheetResourcePath = DockThemeCatalog.getDefaultThemeStylesheetResourcePath();
        this.stylesheetUrl = resolveStylesheetUrl(stylesheetResourcePath);
        if (this.stylesheetUrl == null) {
            throw new IllegalStateException("Default stylesheet could not be resolved: " + stylesheetResourcePath);
        }
    }

    /**
     * Returns the current stylesheet resource path (or absolute stylesheet URL).
     *
     * @return configured stylesheet resource path or absolute URL
     */
    public String getStylesheetResourcePath() {
        return stylesheetResourcePath;
    }

    /**
     * Sets a new stylesheet resource path or URL and returns the previous resolved stylesheet URL.
     *
     * @param stylesheetResourcePath classpath resource path or absolute URL
     * @return previously resolved stylesheet URL
     */
    public String setStylesheetResourcePath(String stylesheetResourcePath) {
        String normalizedPath = normalizeStylesheetPath(stylesheetResourcePath);
        String resolvedUrl = resolveStylesheetUrl(normalizedPath);
        if (resolvedUrl == null) {
            throw new IllegalArgumentException("Stylesheet resource could not be resolved: " + normalizedPath);
        }

        String previousStylesheetUrl = this.stylesheetUrl;
        this.stylesheetResourcePath = normalizedPath;
        this.stylesheetUrl = resolvedUrl;
        return previousStylesheetUrl;
    }

    /**
     * Applies the active stylesheet to the given scene and removes the previous managed stylesheet URL when needed.
     *
     * @param scene scene to update
     * @param previousStylesheetUrl previously managed stylesheet URL, or {@code null}
     */
    public void applyToScene(Scene scene, String previousStylesheetUrl) {
        if (scene == null || stylesheetUrl == null) {
            return;
        }
        ObservableList<String> stylesheets = scene.getStylesheets();
        if (previousStylesheetUrl != null && !previousStylesheetUrl.equals(stylesheetUrl)) {
            stylesheets.removeIf(previousStylesheetUrl::equals);
        }
        if (!stylesheets.contains(stylesheetUrl)) {
            stylesheets.add(stylesheetUrl);
        }
    }

    private String normalizeStylesheetPath(String stylesheetResourcePath) {
        if (stylesheetResourcePath == null || stylesheetResourcePath.isBlank()) {
            return DockThemeCatalog.getDefaultThemeStylesheetResourcePath();
        }
        String trimmedPath = stylesheetResourcePath.trim();
        if (isAbsoluteUrl(trimmedPath) || trimmedPath.startsWith("/")) {    // NOSONAR - False positive, trimmedPath can't be null here
            return trimmedPath;
        }
        return "/" + trimmedPath;
    }

    private String resolveStylesheetUrl(String stylesheetResourcePath) {
        if (stylesheetResourcePath == null || stylesheetResourcePath.isBlank()) {
            return null;
        }
        if (isAbsoluteUrl(stylesheetResourcePath)) {
            return stylesheetResourcePath;
        }

        URL moduleResource = DockThemeStylesheetManager.class.getResource(stylesheetResourcePath);
        if (moduleResource != null) {
            return moduleResource.toExternalForm();
        }

        String classpathPath = stylesheetResourcePath.startsWith("/")
            ? stylesheetResourcePath.substring(1)
            : stylesheetResourcePath;
        ClassLoader classLoader = DockThemeStylesheetManager.class.getClassLoader();
        URL classpathResource = classLoader != null
            ? classLoader.getResource(classpathPath)
            : ClassLoader.getSystemResource(classpathPath);
        return classpathResource == null ? null : classpathResource.toExternalForm();
    }

    private boolean isAbsoluteUrl(String path) {
        return path != null && path.contains("://");
    }
}

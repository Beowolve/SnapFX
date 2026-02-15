package com.github.beowolve.snapfx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides runtime build metadata resolved from generated resources.
 */
public final class BuildInfo {
    private static final String VERSION_RESOURCE = "/snapfx-version.properties";
    private static final String VERSION_KEY = "version";
    private static final String UNKNOWN_VERSION = "dev";
    private static final String VERSION = loadVersion();

    private BuildInfo() {
    }

    public static String getVersion() {
        return VERSION;
    }

    private static String loadVersion() {
        Properties properties = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (in == null) {
                return UNKNOWN_VERSION;
            }
            properties.load(in);
            String value = properties.getProperty(VERSION_KEY);
            if (value == null || value.isBlank()) {
                return UNKNOWN_VERSION;
            }
            return value.trim();
        } catch (IOException ignored) {
            return UNKNOWN_VERSION;
        }
    }
}

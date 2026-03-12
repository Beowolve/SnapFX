package org.snapfx.demo;

import org.junit.jupiter.api.Test;
import org.snapfx.localization.DockResourceBundleLocalizationProvider;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoLocalizationBundleTest {
    @Test
    void testFrenchDemoBundleCoversAllFrameworkLocalizationKeys() {
        ResourceBundle frameworkEnglishBundle = ResourceBundle.getBundle("org.snapfx.i18n.snapfx", Locale.ENGLISH);
        ResourceBundle demoFrenchBundle = ResourceBundle.getBundle(
            "org.snapfx.demo.i18n.snapfx",
            Locale.FRENCH,
            MainDemo.class.getModule()
        );

        Set<String> missingKeys = new TreeSet<>();
        for (String key : frameworkEnglishBundle.keySet()) {
            if (!demoFrenchBundle.containsKey(key)) {
                missingKeys.add(key);
            }
        }

        assertTrue(missingKeys.isEmpty(), "Missing French localization keys: " + missingKeys);
    }

    @Test
    void testFrenchDemoBundleDoesNotContainBlankValues() {
        ResourceBundle demoFrenchBundle = ResourceBundle.getBundle(
            "org.snapfx.demo.i18n.snapfx",
            Locale.FRENCH,
            MainDemo.class.getModule()
        );

        Set<String> blankKeys = new TreeSet<>();
        for (String key : demoFrenchBundle.keySet()) {
            String value = demoFrenchBundle.getString(key);
            if (value == null || value.isBlank()) {
                blankKeys.add(key);
            }
        }

        assertTrue(blankKeys.isEmpty(), "French localization contains blank values for keys: " + blankKeys);
    }

    @Test
    void testFrenchDemoProviderResolvesFrenchValuesEvenWithGermanJvmDefaultLocale() {
        DockResourceBundleLocalizationProvider provider = new DockResourceBundleLocalizationProvider(
            "org.snapfx.demo.i18n.snapfx",
            MainDemo.class.getModule()
        );

        Locale previousDefault = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMAN);
            assertEquals("Détacher", provider.resolve(Locale.FRENCH, "dock.layout.menu.float"));
        } finally {
            Locale.setDefault(previousDefault);
        }
    }
}

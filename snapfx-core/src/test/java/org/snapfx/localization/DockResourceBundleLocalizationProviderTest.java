package org.snapfx.localization;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DockResourceBundleLocalizationProviderTest {

    @Test
    void testResolveReturnsBundleValueForLocale() {
        DockResourceBundleLocalizationProvider provider = new DockResourceBundleLocalizationProvider(
            "org.snapfx.testi18n.messages",
            DockResourceBundleLocalizationProviderTest.class.getClassLoader()
        );

        assertEquals("Bonjour", provider.resolve(Locale.FRENCH, "demo.greeting"));
    }

    @Test
    void testResolveWithModuleConstructorReturnsBundleValueForLocale() {
        DockResourceBundleLocalizationProvider provider = new DockResourceBundleLocalizationProvider(
            "org.snapfx.testi18n.messages",
            DockResourceBundleLocalizationProviderTest.class.getModule()
        );

        assertEquals("Bonjour", provider.resolve(Locale.FRENCH, "demo.greeting"));
    }

    @Test
    void testResolveFallsBackFromRegionalLocaleToLanguageBundle() {
        DockResourceBundleLocalizationProvider provider = new DockResourceBundleLocalizationProvider(
            "org.snapfx.testi18n.messages",
            DockResourceBundleLocalizationProviderTest.class.getClassLoader()
        );

        assertEquals("Bonjour", provider.resolve(Locale.CANADA_FRENCH, "demo.greeting"));
    }

    @Test
    void testResolveReturnsNullForMissingKey() {
        DockResourceBundleLocalizationProvider provider = new DockResourceBundleLocalizationProvider(
            "org.snapfx.testi18n.messages",
            DockResourceBundleLocalizationProviderTest.class.getClassLoader()
        );

        assertNull(provider.resolve(Locale.ENGLISH, "demo.missing"));
    }

    @Test
    void testResolveReturnsNullForMissingBundle() {
        DockResourceBundleLocalizationProvider provider = new DockResourceBundleLocalizationProvider(
            "org.snapfx.testi18n.missing",
            DockResourceBundleLocalizationProviderTest.class.getClassLoader()
        );

        assertNull(provider.resolve(Locale.ENGLISH, "demo.greeting"));
    }

    @Test
    void testResolveReadsUtf8Characters() {
        DockResourceBundleLocalizationProvider provider = new DockResourceBundleLocalizationProvider(
            "org.snapfx.testi18n.messages",
            DockResourceBundleLocalizationProviderTest.class.getClassLoader()
        );

        assertEquals("Éditeur", provider.resolve(Locale.FRENCH, "demo.special"));
    }

    @Test
    void testResolveDoesNotFallBackToJvmDefaultLocaleWhenRequestedBundleIsMissing() {
        DockResourceBundleLocalizationProvider provider = new DockResourceBundleLocalizationProvider(
            "org.snapfx.testi18n.messages",
            DockResourceBundleLocalizationProviderTest.class.getClassLoader()
        );

        Locale previousDefault = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMAN);
            assertNull(provider.resolve(Locale.ITALIAN, "demo.greeting"));
        } finally {
            Locale.setDefault(previousDefault);
        }
    }
}

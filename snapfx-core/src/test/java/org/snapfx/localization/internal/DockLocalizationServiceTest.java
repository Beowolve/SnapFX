package org.snapfx.localization.internal;

import org.snapfx.localization.DockLocalizationProvider;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockLocalizationServiceTest {

    @Test
    void testUserProviderOverridesBuiltInValue() {
        DockLocalizationProvider builtInProvider = (locale, key) -> "dock.key".equals(key) ? "Built-in" : null;
        DockLocalizationService service = new DockLocalizationService(builtInProvider);
        service.setLocale(Locale.ENGLISH);
        service.setUserProvider((locale, key) -> "dock.key".equals(key) ? "User" : null);

        assertEquals("User", service.text("dock.key"));
    }

    @Test
    void testFallbackUsesBuiltInLocaleThenEnglishThenRawKey() {
        DockLocalizationProvider builtInProvider = (locale, key) -> {
            if ("dock.deOnly".equals(key) && Locale.GERMAN.getLanguage().equals(locale.getLanguage())) {
                return "Deutsch";
            }
            if ("dock.enOnly".equals(key) && Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
                return "English";
            }
            return null;
        };
        DockLocalizationService service = new DockLocalizationService(builtInProvider);
        service.setLocale(Locale.GERMAN);

        assertEquals("Deutsch", service.text("dock.deOnly"));
        assertEquals("English", service.text("dock.enOnly"));
        assertEquals("dock.missing", service.text("dock.missing"));
    }

    @Test
    void testMessageFormatUsesResolvedPattern() {
        DockLocalizationProvider builtInProvider = (locale, key) -> {
            if ("dock.welcome".equals(key) && Locale.GERMAN.getLanguage().equals(locale.getLanguage())) {
                return "Hallo {0}";
            }
            return null;
        };
        DockLocalizationService service = new DockLocalizationService(builtInProvider);
        service.setLocale(Locale.GERMAN);

        assertEquals("Hallo SnapFX", service.text("dock.welcome", "SnapFX"));
    }

    @Test
    void testNullLocaleFallsBackToDefaultLocale() {
        DockLocalizationProvider builtInProvider = (locale, key) -> {
            if ("dock.default".equals(key) && Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
                return "Default";
            }
            return null;
        };
        DockLocalizationService service = new DockLocalizationService(builtInProvider);
        service.setLocale(null);

        assertEquals("Default", service.text("dock.default"));
    }
}

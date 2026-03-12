package org.snapfx.localization;

import java.util.Locale;

/**
 * Resolves localized message patterns for SnapFX translation keys.
 *
 * <p>Implementations should return {@code null} when a key is not available for
 * the requested locale so SnapFX can continue with its fallback chain.</p>
 */
@FunctionalInterface
public interface DockLocalizationProvider {
    /**
     * Resolves the message pattern for a localization key.
     *
     * @param locale requested locale, never {@code null}
     * @param key translation key, never {@code null}
     * @return resolved message pattern, or {@code null} when this provider has no value
     */
    String resolve(Locale locale, String key);
}

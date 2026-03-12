package org.snapfx.localization.internal;

import org.snapfx.localization.DockLocalizationProvider;
import org.snapfx.localization.DockResourceBundleLocalizationProvider;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal localization service with provider fallback and message formatting.
 */
public final class DockLocalizationService {
    private static final System.Logger LOGGER = System.getLogger(DockLocalizationService.class.getName());

    /** Default SnapFX locale used when no explicit locale is configured. */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    /** Locales for which SnapFX ships built-in translations. */
    public static final List<Locale> BUILT_IN_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);
    /** Base bundle name for built-in SnapFX translations. */
    public static final String BUILT_IN_BUNDLE_BASE_NAME = "org.snapfx.i18n.snapfx";

    private final DockLocalizationProvider builtInProvider;
    private final Set<String> missingKeyLogDedup;
    private Locale locale;
    private DockLocalizationProvider userProvider;

    /**
     * Creates a localization service using SnapFX built-in resource bundles.
     */
    public DockLocalizationService() {
        this(new DockResourceBundleLocalizationProvider(
            BUILT_IN_BUNDLE_BASE_NAME,
            DockLocalizationService.class.getClassLoader()
        ));
    }

    /**
     * Creates a localization service with an explicit built-in provider.
     *
     * @param builtInProvider built-in provider used as framework fallback
     */
    public DockLocalizationService(DockLocalizationProvider builtInProvider) {
        this.builtInProvider = Objects.requireNonNull(builtInProvider, "builtInProvider");
        this.missingKeyLogDedup = ConcurrentHashMap.newKeySet();
        this.locale = DEFAULT_LOCALE;
    }

    /**
     * Returns the active locale.
     *
     * @return active locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the active locale.
     *
     * @param locale new locale; {@code null} falls back to {@link #DEFAULT_LOCALE}
     */
    public void setLocale(Locale locale) {
        this.locale = locale == null ? DEFAULT_LOCALE : locale;
    }

    /**
     * Returns the optional user provider.
     *
     * @return user provider, or {@code null}
     */
    public DockLocalizationProvider getUserProvider() {
        return userProvider;
    }

    /**
     * Sets the optional user provider.
     *
     * @param userProvider user provider, or {@code null}
     */
    public void setUserProvider(DockLocalizationProvider userProvider) {
        this.userProvider = userProvider;
    }

    /**
     * Resolves and formats a localized message.
     *
     * @param key translation key
     * @param args optional message-format arguments
     * @return resolved localized text
     */
    public String text(String key, Object... args) {
        String resolvedKey = Objects.requireNonNull(key, "key");
        Locale activeLocale = locale == null ? DEFAULT_LOCALE : locale;

        String pattern = resolvePattern(userProvider, activeLocale, resolvedKey);
        if (pattern == null) {
            pattern = resolvePattern(builtInProvider, activeLocale, resolvedKey);
        }
        if (pattern == null) {
            pattern = resolvePattern(builtInProvider, DEFAULT_LOCALE, resolvedKey);
        }
        if (pattern == null) {
            pattern = resolvedKey;
            logMissingLocalizationKey(activeLocale, resolvedKey);
        }

        return format(pattern, activeLocale, args);
    }

    private String resolvePattern(DockLocalizationProvider provider, Locale locale, String key) {
        if (provider == null || locale == null || key == null) {
            return null;
        }
        try {
            String value = provider.resolve(locale, key);
            if (value == null || value.isBlank()) {
                return null;
            }
            return value;
        } catch (RuntimeException ex) {
            LOGGER.log(System.Logger.Level.WARNING,
                "Localization provider failed for key '" + key + "' and locale '" + locale + "'.", ex);
            return null;
        }
    }

    private void logMissingLocalizationKey(Locale locale, String key) {
        String dedupKey = (locale == null ? "<null>" : locale.toLanguageTag()) + "|" + key;
        if (!missingKeyLogDedup.add(dedupKey)) {
            return;
        }
        LOGGER.log(System.Logger.Level.DEBUG,
            "Localization key '" + key + "' is missing for locale '"
                + (locale == null ? "<null>" : locale.toLanguageTag()) + "'. Using raw key fallback.");
    }

    private String format(String pattern, Locale locale, Object... args) {
        if (pattern == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        Locale formatLocale = locale == null ? DEFAULT_LOCALE : locale;
        return new MessageFormat(pattern, formatLocale).format(args);
    }
}

package org.snapfx.demo.i18n;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Resolves and binds demo-localized strings for the SnapFX demo application.
 */
public final class DemoLocalizationService {
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final String BUNDLE_BASE_NAME = "org.snapfx.demo.i18n.demo";
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH);

    private final Module module;
    private final ObjectProperty<Locale> localeProperty = new SimpleObjectProperty<>(DEFAULT_LOCALE);

    /**
     * Creates a localization service backed by module-scoped resource bundles.
     *
     * @param module module that contains the demo resource bundles
     */
    public DemoLocalizationService(Module module) {
        this.module = Objects.requireNonNull(module, "module");
    }

    /**
     * Returns the active locale property.
     *
     * @return read-only locale property
     */
    public ReadOnlyObjectProperty<Locale> localeProperty() {
        return localeProperty;
    }

    /**
     * Returns the currently active locale.
     *
     * @return active locale
     */
    public Locale getLocale() {
        Locale locale = localeProperty.get();
        return locale == null ? DEFAULT_LOCALE : locale;
    }

    /**
     * Sets the locale used for localized demo strings.
     *
     * @param locale locale to apply; {@code null} falls back to English
     */
    public void setLocale(Locale locale) {
        localeProperty.set(locale == null ? DEFAULT_LOCALE : locale);
    }

    /**
     * Resolves a localized string for the active locale.
     *
     * @param key localization key
     * @param args optional format arguments
     * @return localized text, or the raw key if no translation exists
     */
    public String text(String key, Object... args) {
        Locale activeLocale = getLocale();
        String pattern = resolve(activeLocale, key);
        if (pattern == null && !DEFAULT_LOCALE.equals(activeLocale)) {
            pattern = resolve(DEFAULT_LOCALE, key);
        }
        if (pattern == null) {
            return key;
        }
        return args == null || args.length == 0
            ? pattern
            : new MessageFormat(pattern, activeLocale).format(args);
    }

    /**
     * Returns whether the given value matches the resolved translation in any supported locale.
     *
     * @param key localization key
     * @param value localized text candidate
     * @return {@code true} when the value matches in at least one supported locale
     */
    public boolean matchesInSupportedLocales(String key, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (Locale locale : SUPPORTED_LOCALES) {
            String resolved = resolve(locale, key);
            if (value.equals(resolved)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a locale-aware binding for the given key.
     *
     * @param key localization key
     * @param args optional format arguments
     * @return string binding that tracks locale changes
     */
    public StringBinding createBinding(String key, Object... args) {
        return Bindings.createStringBinding(() -> text(key, args), localeProperty);
    }

    /**
     * Binds a text property to a localized key.
     *
     * @param property property to bind
     * @param key localization key
     * @param args optional format arguments
     */
    public void bind(StringProperty property, String key, Object... args) {
        property.bind(createBinding(key, args));
    }

    /**
     * Binds a labeled control to a localized key.
     *
     * @param labeled labeled control
     * @param key localization key
     * @param args optional format arguments
     */
    public void bind(Labeled labeled, String key, Object... args) {
        if (labeled != null) {
            bind(labeled.textProperty(), key, args);
        }
    }

    /**
     * Binds a menu item to a localized key.
     *
     * @param menuItem menu item
     * @param key localization key
     * @param args optional format arguments
     */
    public void bind(MenuItem menuItem, String key, Object... args) {
        if (menuItem != null) {
            bind(menuItem.textProperty(), key, args);
        }
    }

    /**
     * Binds a tab title to a localized key.
     *
     * @param tab tab to bind
     * @param key localization key
     * @param args optional format arguments
     */
    public void bind(Tab tab, String key, Object... args) {
        if (tab != null) {
            bind(tab.textProperty(), key, args);
        }
    }

    /**
     * Binds a tooltip to a localized key.
     *
     * @param tooltip tooltip to bind
     * @param key localization key
     * @param args optional format arguments
     */
    public void bind(Tooltip tooltip, String key, Object... args) {
        if (tooltip != null) {
            bind(tooltip.textProperty(), key, args);
        }
    }

    /**
     * Binds a stage title to a localized key.
     *
     * @param stage stage to bind
     * @param key localization key
     * @param args optional format arguments
     */
    public void bind(Stage stage, String key, Object... args) {
        if (stage != null) {
            bind(stage.titleProperty(), key, args);
        }
    }

    private String resolve(Locale locale, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Locale resolvedLocale = locale == null ? DEFAULT_LOCALE : locale;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, resolvedLocale, module);
            if (!isBundleLocaleCompatible(resolvedLocale, bundle.getLocale())) {
                return null;
            }
            return bundle.containsKey(key) ? bundle.getString(key) : null;
        } catch (MissingResourceException ignored) {
            return null;
        }
    }

    private boolean isBundleLocaleCompatible(Locale requestedLocale, Locale resolvedLocale) {
        if (resolvedLocale == null) {
            return false;
        }
        if (Locale.ROOT.equals(resolvedLocale)) {
            return true;
        }
        if (requestedLocale.equals(resolvedLocale)) {
            return true;
        }
        String requestedLanguage = requestedLocale.getLanguage();
        String resolvedLanguage = resolvedLocale.getLanguage();
        return !requestedLanguage.isBlank() && requestedLanguage.equals(resolvedLanguage);
    }
}

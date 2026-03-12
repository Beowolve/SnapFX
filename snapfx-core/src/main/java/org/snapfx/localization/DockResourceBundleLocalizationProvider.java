package org.snapfx.localization;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * {@link DockLocalizationProvider} implementation backed by Java {@link ResourceBundle}s.
 *
 * <p>The provider returns {@code null} when a bundle or key is unavailable.</p>
 *
 * <p>Resource bundles are read as UTF-8 on modern Java runtimes (Java 9+).</p>
 */
public final class DockResourceBundleLocalizationProvider implements DockLocalizationProvider {
    private final String baseName;
    private final ClassLoader classLoader;
    private final Module module;

    /**
     * Creates a provider for a bundle base name using the context class loader.
     *
     * @param baseName resource-bundle base name (for example {@code org.snapfx.i18n.snapfx})
     */
    public DockResourceBundleLocalizationProvider(String baseName) {
        this(baseName, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a provider for a bundle base name and explicit class loader.
     *
     * @param baseName resource-bundle base name (for example {@code org.snapfx.i18n.snapfx})
     * @param classLoader class loader used to load bundles; when {@code null}, this class' loader is used
     */
    public DockResourceBundleLocalizationProvider(String baseName, ClassLoader classLoader) {
        this.baseName = Objects.requireNonNull(baseName, "baseName");
        this.classLoader = classLoader == null
            ? DockResourceBundleLocalizationProvider.class.getClassLoader()
            : classLoader;
        this.module = null;
    }

    /**
     * Creates a provider for a bundle base name and explicit module.
     *
     * <p>Use this constructor when bundles are stored in a named module and should
     * be resolved directly from that module at runtime.</p>
     *
     * @param baseName resource-bundle base name (for example {@code org.snapfx.i18n.snapfx})
     * @param module module that contains the bundle resources
     */
    public DockResourceBundleLocalizationProvider(String baseName, Module module) {
        this.baseName = Objects.requireNonNull(baseName, "baseName");
        this.module = Objects.requireNonNull(module, "module");
        this.classLoader = this.module.getClassLoader();
    }

    /**
     * Resolves a translation pattern from the configured resource bundle.
     *
     * <p>Lookup only accepts the resolved bundle when it matches the requested locale language
     * (or root bundle). This prevents implicit fallback to the JVM default locale.</p>
     *
     * @param locale requested locale, never {@code null}
     * @param key translation key, never {@code null}
     * @return message pattern, or {@code null} if the bundle/key is unavailable
     */
    @Override
    public String resolve(Locale locale, String key) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(key, "key");
        try {
            ResourceBundle bundle = module == null
                ? ResourceBundle.getBundle(baseName, locale, classLoader)
                : ResourceBundle.getBundle(baseName, locale, module);
            if (!isBundleLocaleCompatible(locale, bundle.getLocale())) {
                return null;
            }
            if (!bundle.containsKey(key)) {
                return null;
            }
            return bundle.getString(key);
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

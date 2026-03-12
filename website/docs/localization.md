# Localization

SnapFX supports runtime localization with a JavaFX-native API and provider extension points.

## Built-in Languages

Built-in bundles currently include:

- `Locale.ENGLISH` (`en`) - default
- `Locale.GERMAN` (`de`)

Discover them at runtime:

```java
List<Locale> locales = SnapFX.getAvailableLocales();
Locale defaultLocale = SnapFX.getDefaultLocale();
```

## Runtime Locale Switching

Use either setter/getter style or property style:

```java
SnapFX snapFX = new SnapFX();

snapFX.setLocale(Locale.GERMAN);
Locale active = snapFX.getLocale();

// JavaFX-property style
snapFX.localeProperty().set(Locale.ENGLISH);
```

Locale changes are applied immediately to currently open SnapFX UI surfaces (main layout and floating windows).

## Thread Safety

`setLocale(...)`, `setLocalizationProvider(...)`, and direct mutation of their properties must run on the JavaFX Application Thread.

If called from a non-FX thread, SnapFX throws `IllegalStateException`.

## Extend Missing Languages with `.properties`

If SnapFX does not provide a language, ship your own ResourceBundle and register the adapter:

```java
SnapFX snapFX = new SnapFX();
snapFX.setLocale(Locale.FRENCH);
snapFX.setLocalizationProvider(
    new DockResourceBundleLocalizationProvider("com.example.i18n.snapfx")
);
```

Example bundle files:

- `com/example/i18n/snapfx_fr.properties`
- `com/example/i18n/snapfx_en.properties` (optional fallback in your own provider space)

## Custom Provider (Non-ResourceBundle)

For custom translation sources (database, remote config, etc.), implement `DockLocalizationProvider`:

```java
DockLocalizationProvider provider = (locale, key) -> {
    if (Locale.FRENCH.getLanguage().equals(locale.getLanguage())
        && "dock.floating.tooltip.attachToLayout".equals(key)) {
        return "Attacher à la disposition";
    }
    return null; // let SnapFX fallback continue
};

snapFX.setLocalizationProvider(provider);
```

## Resolution Fallback Chain

For each key, SnapFX resolves in this order:

1. user provider value
2. built-in bundle for active locale
3. built-in English bundle
4. raw key fallback

When final raw-key fallback is used, SnapFX emits a deduplicated DEBUG log entry (per `locale+key`) to help detect missing translations.

## UTF-8 Note for ResourceBundles

SnapFX uses standard `ResourceBundle` loading behavior. On Java 9+, `.properties` files are read as UTF-8 by default.

## Pluralization and Formatting

SnapFX uses `MessageFormat`.  
For plural-like behavior, use `choice` patterns in your message values:

```properties
items.count={0,choice,0#No items|1#One item|1<{0} items}
```

Then pass arguments through the regular text pipeline.

## RTL Note

Localization bundles do not automatically apply RTL layout behavior.  
If your language is RTL (for example Arabic/Hebrew), application code should configure JavaFX node orientation (`NodeOrientation`) where required.

## Locale Persistence (Application Responsibility)

SnapFX intentionally does not persist locale preferences.  
Typical application pattern:

```java
Preferences prefs = Preferences.userNodeForPackage(MyApp.class);
String savedTag = prefs.get("snapfx.locale", Locale.ENGLISH.toLanguageTag());
snapFX.setLocale(Locale.forLanguageTag(savedTag));

// later on locale change:
prefs.put("snapfx.locale", snapFX.getLocale().toLanguageTag());
```

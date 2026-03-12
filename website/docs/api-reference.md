# API Reference

SnapFX API documentation is generated from JavaDoc and published at:

- [SnapFX API JavaDoc](https://snapfx.org/api/)

## Main Entry Points

- `org.snapfx.SnapFX`
- `org.snapfx.localization.DockLocalizationProvider`
- `org.snapfx.localization.DockResourceBundleLocalizationProvider`
- `org.snapfx.model.DockGraph`
- `org.snapfx.model.DockNode`
- `org.snapfx.persistence.DockLayoutSerializer`

## Localization

Localization runtime APIs are exposed on `SnapFX`:

- `getDefaultLocale()`, `getAvailableLocales()`
- `localeProperty()`, `getLocale()`, `setLocale(...)`
- `localizationProviderProperty()`, `getLocalizationProvider()`, `setLocalizationProvider(...)`

For usage and extension examples, see [Localization](/localization).

## Notes

- The API docs are rebuilt and deployed via GitHub Actions.
- Public/protected API changes require JavaDoc updates in the same change.

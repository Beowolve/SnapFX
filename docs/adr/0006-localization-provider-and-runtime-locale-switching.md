# ADR 0006: Localization Provider and Runtime Locale Switching

- **Status**: Accepted
- **Date**: 2026-03-12

## Context

SnapFX required built-in localization support that can be switched at runtime without recreating the framework instance.  
The API also needed an extension point so framework consumers can add language packs that SnapFX does not ship by default.

Additional constraints:

- JavaFX-first API style (property-oriented configuration)
- Fail-fast threading behavior for mutable runtime configuration
- Backward-compatible English baseline for existing applications
- No framework-managed persistence of user locale preference

## Decision Drivers

- Modular, testable localization architecture
- Simple public API (`localeProperty()` + provider SPI)
- Deterministic fallback behavior
- Immediate UI refresh across main layout, floating windows, demo/debug surfaces
- Safe runtime mutation rules on JavaFX thread

## Considered Options

### Option A: `setLocale/getLocale` only with hardcoded framework bundles

- Pros: smallest API surface
- Cons: not JavaFX-property-native, no user-extensible language packs

### Option B: ResourceBundle-only API with base-name configuration

- Pros: familiar Java API
- Cons: weaker abstraction, less flexible for non-bundle backends

### Option C (accepted): Property API + provider SPI + ResourceBundle adapter

- `SnapFX.localeProperty()` and `SnapFX.localizationProviderProperty()`
- `DockLocalizationProvider` SPI
- `DockResourceBundleLocalizationProvider` adapter for `.properties` bundles

## Decision

We use **Option C**.

Public API:

- `localeProperty()` + `getLocale()/setLocale()`
- `localizationProviderProperty()` + `getLocalizationProvider()/setLocalizationProvider()`
- `getAvailableLocales()` for built-ins

Built-in locales for v1 scope:

- `Locale.ENGLISH`
- `Locale.GERMAN`

Default locale:

- `Locale.ENGLISH`

## Fallback Chain

Text resolution uses this fixed order:

1. User provider (`DockLocalizationProvider`)
2. Built-in bundle for active locale
3. Built-in English bundle
4. Raw key (with deduplicated DEBUG log entry)

Formatting uses `MessageFormat` with active locale.

## Threading Contract

Mutations are JavaFX-thread-only:

- `setLocale(...)`
- `setLocalizationProvider(...)`
- direct mutation of `localeProperty()` / `localizationProviderProperty()`

Violation behavior:

- fail fast with `IllegalStateException`

## Explicit Non-Scope

- Persisting locale preference inside SnapFX (application should persist externally and restore via API)
- Automatic RTL layout adaptation (`NodeOrientation` handling remains application-owned)

## Consequences

Positive:

- Runtime language switching without framework restart
- Clean extension path for unsupported languages
- JavaFX-native property integration
- Deterministic, testable fallback behavior

Trade-offs:

- Additional internal wiring to propagate localization updates to all active UI surfaces
- Applications must own locale persistence strategy
- Pluralization remains `MessageFormat`-driven (no ICU rule engine in this scope)

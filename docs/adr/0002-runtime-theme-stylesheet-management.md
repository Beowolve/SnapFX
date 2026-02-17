# ADR 0002: Runtime Theme Stylesheet Management

- Status: Accepted
- Date: 2026-02-17
- Deciders: Beo

## Context

SnapFX is still pre-release, so there are no public third-party integrations yet.

Before publishing the first public version, I decided to lock down a clear theme contract that fits a docking framework:

- one API owner should control stylesheet lifecycle for all managed scenes
- the default look should work without consumer boilerplate
- runtime switching should stay deterministic for both main and floating windows
- built-in themes should be discoverable as named metadata for UI pickers
- custom stylesheet overrides must remain possible

This approach was chosen proactively as framework architecture.

## Decision

SnapFX now owns theme stylesheet application through framework API.

Implemented behavior:

1. `initialize(stage)` now applies the active SnapFX stylesheet to the managed scenes.
2. New API: `setThemeStylesheet(String stylesheetResourcePath)` for runtime changes.
3. New API: `getThemeStylesheetResourcePath()` and `getDefaultThemeStylesheetResourcePath()`.
4. New API: `getAvailableThemeStylesheets()` / `getAvailableThemeNames()` and `getDefaultThemeName()` for named theme discovery (`Light`, `Dark`).
5. Theme changes update both the primary scene and all active floating window scenes.
6. `null`/blank stylesheet input resets to the default `"/snapfx.css"`.

Implementation is now split into dedicated theme classes:

- `DockThemeCatalog` for built-in named theme metadata
- `DockThemeStylesheetManager` for stylesheet resolution and scene application

MainDemo exposes this API in the Settings tab via a theme selector and ships `snapfx-dark.css` as an alternate theme.

## Consequences

Positive:

- default theming works out of the box for future consumers
- runtime theme switching is centralized and consistent
- floating windows stay synchronized with the selected theme
- built-in themes can be listed directly by name in settings UIs

Trade-offs:

- SnapFX now manages one additional scene concern (stylesheet lifecycle)
- invalid stylesheet inputs now surface as API errors instead of silent no-op

## Alternatives Considered

1. Keep manual stylesheet loading in demos/apps only.
   - Rejected: weak default ergonomics and easy to misuse in future integrations.

2. Provide theme switching only in MainDemo.
   - Rejected: does not establish a framework-level contract for future consumers.

3. Use root style classes only (for example `.root.dark`) without stylesheet switching.
   - Rejected: less explicit API contract and harder resource separation for custom themes.

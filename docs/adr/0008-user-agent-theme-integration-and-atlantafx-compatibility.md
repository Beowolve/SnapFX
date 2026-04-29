# ADR 0008: User-Agent Theme Integration and Optional AtlantaFX Compatibility

- **Status**: Accepted
- **Date**: 2026-04-12

## Context

SnapFX should stay dependency-light and Modena-ready by default before the first public Maven release.
At the same time, users requested compatibility when applications switch the global JavaFX user-agent stylesheet to AtlantaFX.

Constraints:

- no hard compile/runtime dependency from `snapfx-core` to AtlantaFX
- preserve Modena as the default baseline
- support runtime user-agent stylesheet changes for already initialized scenes
- keep MainDemo as the manual verification surface for every public API

## Decision Drivers

- Keep `snapfx-core` integration surface small and stable
- Allow zero-config behavior for common setups (global user-agent stylesheet set before `SnapFX.initialize(...)`)
- Provide explicit override controls for deterministic behavior
- Avoid duplicated styling/integration logic between main and floating scenes

## Considered Options

### Option A: Add direct AtlantaFX dependency in `snapfx-core`

- Pros: direct type-safe integration with AtlantaFX APIs
- Cons: hard dependency and tighter coupling to a third-party theme library

### Option B: Keep SnapFX Modena-only and document external workarounds

- Pros: minimal implementation effort
- Cons: poor interoperability and no framework-level compatibility contract

### Option C (accepted): Generic user-agent integration with optional compatibility layer

- Pros: no hard AtlantaFX dependency, Modena default preserved, deterministic API override
- Cons: auto-detection is heuristic (stylesheet string marker), not type-coupled

## Decision

We use **Option C**.

Implemented behavior:

1. New API mode enum: `DockUserAgentThemeMode` with `AUTO`, `MODENA`, `ATLANTAFX_COMPAT`.
2. New API methods in `SnapFX`:
   - `setUserAgentThemeMode(...)`
   - `getUserAgentThemeMode()`
   - `refreshUserAgentThemeIntegration()`
3. `AUTO` mode inspects `Application.getUserAgentStylesheet()` and enables compatibility when an AtlantaFX marker is detected.
4. Compatibility is applied via additive stylesheet (`/snapfx-atlantafx-compat.css`) across primary and floating managed scenes.
5. MainDemo ships with AtlantaFX (`snapfx-demo` dependency scope) and exposes a simplified `Theme Source` flow:
   - `Internal`: SnapFX built-in themes (`Light`, `Dark`)
   - `AtlantaFX`: bundled AtlantaFX theme list
6. Theme-source switching applies the corresponding global user-agent stylesheet and SnapFX integration mode automatically, without extra manual refresh controls.

## Consequences

Positive:

- SnapFX remains Modena-default and dependency-light in `snapfx-core`
- AtlantaFX-compatible behavior is available without hard coupling
- Runtime user-agent stylesheet changes can be re-applied explicitly and consistently
- MainDemo preserves API-to-settings parity for manual verification

Trade-offs:

- Auto-detection depends on a stylesheet-string heuristic (`atlantafx`)
- Compatibility layer is intentionally minimal and may need iterative tuning for additional external themes

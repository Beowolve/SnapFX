# Architecture Overview

SnapFX separates model state from JavaFX rendering:

- `DockGraph` handles layout structure and mutation logic.
- `SnapFX` is the high-level facade API.
- Rendering and interaction services apply model state to JavaFX scenes.
- Runtime localization is coordinated through `SnapFX` locale/provider properties with a deterministic fallback chain.

Detailed architecture documentation remains in the repository:

- [ARCHITECTURE.md](https://github.com/Beowolve/SnapFX/blob/main/ARCHITECTURE.md)
- [docs/adr](https://github.com/Beowolve/SnapFX/tree/main/docs/adr)
- [ADR 0006: Localization provider and runtime locale switching](https://github.com/Beowolve/SnapFX/blob/main/docs/adr/0006-localization-provider-and-runtime-locale-switching.md)

For API-level details, use:

- [API JavaDoc](https://snapfx.org/api/)

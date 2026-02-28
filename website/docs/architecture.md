# Architecture Overview

SnapFX separates model state from JavaFX rendering:

- `DockGraph` handles layout structure and mutation logic.
- `SnapFX` is the high-level facade API.
- Rendering and interaction services apply model state to JavaFX scenes.

Detailed architecture documentation remains in the repository:

- [ARCHITECTURE.md](https://github.com/Beowolve/SnapFX/blob/main/ARCHITECTURE.md)
- [docs/adr](https://github.com/Beowolve/SnapFX/tree/main/docs/adr)

For API-level details, use:

- `https://snapfx.org/api/`

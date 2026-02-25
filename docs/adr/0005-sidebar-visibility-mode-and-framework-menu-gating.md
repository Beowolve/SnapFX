# ADR-0005: Sidebar Visibility Mode and Framework Menu Gating

- Status: Accepted
- Date: 2026-02-25

## Context

SnapFX sidebars now provide substantial workflow coverage (pin/restore, framework context menus, strip DnD, resizable widths).
Host applications need a simple way to:

- keep empty sidebars visible as direct D&D targets
- use the current auto-render behavior
- disable sidebar UI and related framework move actions entirely

This policy should be controlled by a single framework API, similar to `DockTitleBarMode`.

## Decision

Introduce `DockSideBarMode` in `com.github.beowolve.snapfx.sidebar` with three values:

- `ALWAYS`: always render left/right sidebar strips, even when empty
- `AUTO` (default): render sidebars only when pinned sidebar nodes exist
- `NEVER`: suppress framework sidebar UI and hide built-in `Move to Left/Right Sidebar` context-menu actions

`DockSideBarMode` is implemented as a SnapFX view-layer policy:

1. Sidebar model state in `DockGraph` (pinned nodes, pinned-open state, panel widths) remains preserved.
2. `NEVER` hides framework sidebar UI and disables framework sidebar move callbacks/menu entries.
3. `ALWAYS` renders empty strips so users can drop directly into sidebars without pre-existing entries.

## Consequences

### Positive

- Host apps can enable/disable sidebar workflows without custom menu filtering.
- Empty sidebars can be discoverable and usable as D&D targets.
- Sidebar state survives temporary mode changes (e.g. `NEVER` -> `AUTO`).

### Trade-offs

- `NEVER` is a UI policy, so sidebar state may remain stored but hidden until mode changes.
- SnapFX now owns one additional cross-cutting sidebar render/menu policy branch.

# ADR 0004: Sidebar Panel Width State and Runtime Clamping

- Status: Accepted
- Date: 2026-02-25
- Deciders: Beo

## Context

Phase D extends framework side bars beyond pinned/open behavior and DnD parity.

Users need:

- per-side sidebar panel widths (`LEFT` / `RIGHT`)
- runtime resizing via a visible handle
- consistent width behavior for both pinned panels and overlay panels
- layout save/load roundtrip persistence

The existing architecture already splits sidebar state between:

- persisted model state in `DockGraph` (pinned entries, pinned-open flags)
- transient interaction state in `SnapFX` (selected sidebar node / overlay-open state)

The new width feature introduces a design choice:

- store widths in `SnapFX` only (easy UI updates, harder serializer integration)
- or store widths in `DockGraph` (serializer-friendly, shared persistence source of truth)

## Decision

Store sidebar panel widths as persisted per-side preference state in `DockGraph`, and let `SnapFX` apply runtime clamping and resize interaction.

Implemented behavior:

1. `DockGraph` stores a preferred sidebar panel width per side and exposes `get/set` plus a width property for UI bindings.
2. Width state is serialized in `DockLayoutSerializer` side-bar entries via optional `panelWidth`.
3. Missing or invalid `panelWidth` values fall back to the default sidebar width.
4. `SnapFX` binds both pinned and overlay sidebar panel widths to the same per-side `DockGraph` width property.
5. `SnapFX` applies runtime clamping for rendering based on current layout width:
   - minimum width floor
   - maximum width cap
   - scene/layout-width-dependent cap (fraction of available width)
6. Overlay and pinned panels on the same side share the same width.
7. A sidebar resize handle is rendered on the panel inner edge and updates the same per-side width preference.
8. Sidebar width changes are allowed while the layout is locked because they are view-preference changes, not docking-structure mutations.

## Consequences

Positive:

- width persistence roundtrips naturally with existing sidebar serialization
- one source of truth for sidebar widths across API, resize handle, and save/load
- pinned and overlay modes stay visually consistent without duplicated state
- runtime clamping remains a `SnapFX` concern (view/layout-aware), not a model concern

Trade-offs:

- `DockGraph` now stores a view-oriented preference in addition to strict docking topology
- `SnapFX` sidebar rendering grows more binding/interaction logic (width bindings + resize handles)
- serializer side-bar payload now contains optional width metadata

## Alternatives Considered

1. Store widths only in `SnapFX`.
   - Rejected: `DockLayoutSerializer` cannot persist widths without a parallel persistence path or SnapFX-only wrapper fields.

2. Store clamped widths in `DockGraph` only (no runtime clamping in `SnapFX`).
   - Rejected: valid width depends on current scene/layout size, which the model does not know.

3. Separate widths for overlay and pinned modes.
   - Rejected: increases state complexity and surprises users when the same tool window changes width across modes.

# ADR 0003: Sidebar Overlay and Pin Rendering State Split

- Status: Accepted
- Date: 2026-02-24
- Deciders: Beo

## Context

Phase C adds pinned side bars to SnapFX.

The model already stores:

- pinned `DockNode` entries per side (`LEFT`/`RIGHT`)
- a side-bar pinned-open flag
- restore anchors so pinned nodes can return deterministically to the main layout

For IDE-like behavior, the UI also needs transient interaction state that should not be persisted:

- which side-bar entry is currently selected on each side
- whether a collapsed side bar currently has an overlay panel open

The feature also needs these interaction rules:

- icon-only side-bar stripes
- immediate title tooltips
- click to open overlay
- same-icon click closes overlay
- different-icon click switches overlay content
- outside-click closes collapsed overlays
- pin toggle turns overlay mode into a layout-consuming side panel

## Decision

SnapFX keeps sidebar persistence in `DockGraph` and stores overlay interaction state in `SnapFX` view composition only.

Implemented behavior:

1. `DockGraph` remains the source of truth for pinned entries and the persistent "pinned open" flag (`pinOpenSideBar` / `collapsePinnedSideBar`).
2. `SnapFX.buildLayout()` composes the main layout with sidebar UI layers:
   - `BorderPane` side hosts for icon strips and pinned-open side panels
   - `StackPane` overlay layer for collapsed side-bar panels
3. Collapsed side bars use transient `SnapFX` state for selected entry and overlay-open status.
4. Per side, only one panel can be open at a time (overlay or pinned).
5. New pinned entries keep the current sidebar state and therefore remain collapsed by default unless explicitly opened.
6. Overlay panels close on outside click; pinned panels stay open until explicitly collapsed/unpinned.
7. Active-icon click behavior in pinned-open mode is configurable; the default collapses the pinned panel to preserve toggle consistency.
8. Pinned-panel icon-collapse is transient UI state and does not clear pinned mode in the `DockGraph` pinned-open flag.
9. `SnapFX.restoreFromSideBar(...)` reuses the same placement-memory restore pipeline as floating attach so sidebar restore keeps neighbor-aware fallback behavior when parent containers collapse.

## Consequences

Positive:

- persistence stays deterministic and serializer-friendly
- overlay behavior does not pollute the model with ephemeral UI state
- IDE-like click semantics are implemented without changing the docking tree model
- pin/unpin transitions can reuse the same pinned side-bar entries
- default pinning stays non-intrusive (icon-strip first) for discovery and manual workflows

Trade-offs:

- `SnapFX` now owns more scene-graph composition logic (sidebar chrome + overlay interaction state)
- `pinOpenSideBar` / `collapsePinnedSideBar` explicitly model pinned-open/collapsed behavior (instead of ambiguous "visible/hidden" naming)
- a dedicated sidebar view component class can still be introduced later if `SnapFX` composition grows further

## Alternatives Considered

1. Store overlay-open state in `DockGraph`.
   - Rejected: transient UI focus/overlay state should not be persisted or serialized.

2. Render side bars only in `MainDemo`.
   - Rejected: side bars must be framework behavior, not demo-only validation UI.

3. Create separate floating `Stage`s for sidebar overlays.
   - Rejected: outside-click handling and alignment are simpler and more deterministic inside the main scene graph.

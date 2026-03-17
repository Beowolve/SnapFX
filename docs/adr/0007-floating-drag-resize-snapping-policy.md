# ADR 0007: Floating Drag/Resize Snapping Policy

- **Status**: Accepted
- **Date**: 2026-03-17

## Context

SnapFX floating-window snapping was originally implemented for title-bar dragging only.
Floating windows already support edge/corner resize, but resize did not participate in the same snapping system.

The framework needed resize snapping with these constraints:

- keep the existing public API stable (no additional snap settings required)
- preserve current snap-target semantics (`SCREEN`, `MAIN_WINDOW`, `FLOATING_WINDOWS`)
- avoid parallel snap-logic implementations for drag and resize
- keep resize behavior deterministic with minimum-size constraints

## Decision Drivers

- Consistent user experience between moving and resizing floating windows
- No public API expansion for pre-release maintainability
- Reuse of existing snap engine rules and overlap-aware targeting
- Clear, predictable resize behavior for active edge/corner interactions

## Considered Options

### Option A: Add dedicated resize-snapping settings

- Pros: maximum configurability
- Cons: larger API surface and settings complexity for marginal value

### Option B: Keep existing settings for drag, force resize snapping always on

- Pros: simple implementation
- Cons: inconsistent semantics and surprising behavior when drag snapping is disabled

### Option C (accepted): Reuse existing floating snapping settings for drag and resize

- Pros: stable API, consistent behavior, minimal cognitive overhead
- Cons: no independent resize-only toggle

## Decision

We use **Option C**.

Policy:

- `setFloatingWindowSnappingEnabled(...)` applies to drag and resize.
- `setFloatingWindowSnapDistance(...)` applies to drag and resize.
- `setFloatingWindowSnapTargets(...)` applies to drag and resize.
- `MAIN_WINDOW` and `FLOATING_WINDOWS` keep overlap-aware candidate filtering for both interactions.
- During resize, only the active edge(s) are eligible for snapping; the opposite edge remains fixed.
- After resize snapping, minimum width/height constraints are re-applied while preserving fixed opposite edges.

## Consequences

Positive:

- One coherent snapping mental model across floating move and resize interactions
- No breaking API changes and no new settings required
- Shared engine helpers reduce logic duplication and maintenance risk

Trade-offs:

- Users cannot configure drag and resize snapping independently
- Resize snapping introduces additional edge-candidate evaluation during resize drag

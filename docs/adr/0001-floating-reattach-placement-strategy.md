# ADR 0001: Floating Reattach Placement Strategy

- Status: Accepted
- Date: 2026-02-17
- Deciders: SnapFX maintainers

## Context

When users detach a `DockNode` from a floating layout (for example by unresolved drag release or float action) and later invoke `Attach to Layout`, the previous behavior always inserted the node into the main layout.

This caused behavioral drift:

- nodes detached from a floating sub-layout did not return to that sub-layout
- users lost the original local context (tab group or split neighborhood)
- attach behavior varied depending on node history, not only current intent

The system also has to handle layout drift between detach and attach:

- original neighbors might no longer exist
- original floating host might be closed
- hidden windows must not be used as restore hosts

The attach flow must remain non-blocking and must never show error dialogs.

## Decision

SnapFX now uses host-aware placement memory for reattach operations.

Before a node is undocked, SnapFX stores:

- source host context (main layout or specific floating window)
- preferred target/position/tab-index
- previous and next neighbor anchors

During `attachFloatingWindow(...)`, each node is restored using deterministic best-effort ordering:

1. preferred anchor in the original host
2. neighbor anchors in the original host
3. remembered main-layout anchor (when applicable)
4. fallback docking

Fallback policy:

- if the original floating host is still active, fallback may dock into that host root
- otherwise fallback docks into the main layout

No dialogs are shown when anchors are invalid; attach always completes via fallback.

For multi-node attach, placement restore runs in retry passes so nodes depending on other nodes from the same attach batch can still resolve anchors before fallback.

## Consequences

Positive:

- attach behavior is stable and context-preserving for floating-sub-layout workflows
- unresolved drag detach and float-button detach now share the same reattach semantics
- fallback remains predictable and silent

Trade-offs:

- additional in-memory placement tracking per `DockNode`
- more complex attach algorithm (anchor attempts + pass-based resolution)

## Alternatives Considered

1. Always attach to main layout.
   - Rejected: loses context and violates expected round-trip behavior.

2. Only store one target reference (`lastKnownTarget`) without host context.
   - Rejected: cannot reliably restore into floating hosts and fails when host topology changes.

3. Show user-facing conflict/error dialogs when restore fails.
   - Rejected: interrupts workflow; explicit requirement is silent fallback.

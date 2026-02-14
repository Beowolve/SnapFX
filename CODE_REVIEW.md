# Code Review (Current State)

## Scope
- Focus on model/view logic (`DockGraph`, `DockSplitPane`, `DockLayoutEngine`)
- Validation of functional correctness for docking and hit-testing behavior

## Open Findings

None.

## Resolved Findings (2026-02-14)

### 1) Fragile tab-header detection in hit-testing (high)
**File:** `src/main/java/com/github/beowolve/snapfx/view/DockLayoutEngine.java`

The method `isOverTabHeader(...)` currently uses an artificial modulo heuristic on the X coordinate (`x % HEADER_THRESHOLD < HEADER_THRESHOLD / 2`). As a result, detection depends on whether a global X coordinate coincidentally falls into a grid pattern, rather than whether the pointer is actually over a tab header.

**Risk:** Incorrect drop-target prioritization during drag-and-drop (unpredictable behavior depending on window position).

**Recommendation:** Compare real header bounds against actual mouse coordinates (e.g., via concrete tab-header nodes using CSS lookup or explicit header regions) instead of using a modulo heuristic.

---

### 2) Tab-header prioritization does not use the real pointer position (high)
**File:** `src/main/java/com/github/beowolve/snapfx/view/DockLayoutEngine.java`

In `compareTabHeaderPriority(...)`, `isOverTabHeader(...)` is called with the **center point** of candidate bounds (`bounds.getCenterX()/getCenterY()`), not with the actual pointer position from `findElementAt(sceneX, sceneY)`.

**Risk:** The “header vs. non-header” prioritization is decoupled from the real cursor location and can prefer incorrect targets.

**Recommendation:** Store pointer coordinates in the candidate and use them consistently throughout prioritization.

---

### 3) Divider position update missing in split flattening path (medium)
**File:** `src/main/java/com/github/beowolve/snapfx/model/DockSplitPane.java`

During flattening in `addChild(...)` (when the child is a `DockSplitPane` with the same orientation), children are merged directly and the method returns early. In this path, `updateDividerPositions()` is not called.

**Risk:** Inconsistency between `children.size()` and `dividerPositions.size()`, potentially causing incorrect divider state or later UI inconsistencies.

**Recommendation:** Call `updateDividerPositions()` after merging and before returning.

## Positives
- Good separation between model (`DockGraph`) and view (`DockLayoutEngine`).
- Multiple guard clauses for no-op scenarios in `move(...)` / `dock(...)`.
- Broad test coverage for core operations is already present.
